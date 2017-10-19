(ns re-val.events
 "API / data container / validator for a form. This says nothing, or indeed knows nothing of the UI.
  This is an API for the UI to listen to and talk to."
  (:require-macros [reagent.ratom :refer [reaction]])
  (:require [ajax.core :as ajax]
            [cljs-time.coerce :as coerce]
            [cljs-time.core :as t]
            [cljs-time.format :as fmt]
            [re-frame.core :as rf]
            [reagent.core :as r]
            [taoensso.timbre :as timbre
             :refer-macros (log  trace  debug  info  warn  error  fatal  report
                                 logf tracef debugf infof warnf errorf fatalf reportf
                                 spy get-env log-env)]))

;; TODO spec this
;; ----
;; A field is a map with keys: :id, :title, :validators (which is a vector of validator functions)
;; ----
;; A validator is a function that takes a value, and either returns a vector, with
;; first element being a boolean true or false, (indicating success/failure),
;; and the second element an error message, if the first is false

(defn update-validation-store
  "Updates the validation store"
  [{:keys [new-fields invalid-fields valid-fields] :as validation-store}
   validators k v]
  (let [errors (reduce
                (fn [error-list validator]
                  (let [[valid? msg] (validator v)]
                    (if valid?
                      error-list
                      (conj error-list msg))))
                [] ;;list of error messages, empty implies no errors, ie. valid
                validators)
        valid? (empty? errors)]
    (cond-> validation-store
      true         (update :new-fields (fn [c k'] (disj (set c) k')) k)
      valid?       (update :valid-fields (fn [c k'] (conj (set c) k')) k)
      valid?       (update :invalid-fields dissoc k)
      (not valid?) (update :invalid-fields assoc k errors)
      (not valid?) (update :valid-fields (fn [c k'] (disj (set c) k')) k))))

(defn lookup-validators
  [fields k]
  (or
   (some->> fields
            (filter #(= (:id %) k))
            first
            :validators)
   []))

(defn empty-validation-store
  [fields]
  {:new-fields (set (->> fields (map :id)))
   :invalid-fields {}
   :valid-fields #{}})

(rf/reg-event-db
 :destroy-form
 (fn [db [_ id]]
   (-> db
       (update-in [:data] dissoc id))))

;;Utility handler that merges the current value of the specified form in some other path (key-seq) in the db.
;;can be used to update a list of items.
;;does a merge, and uses (:primary_key options) of the form to determine how to merge
(rf/reg-event-db
 :merge-data-in-db-path
 (fn [db [_ form-id path]]
   (let [primary-key (get-in db [:data form-id :options :primary-key])
         data (get-in db [:data form-id :data])]
     (when (nil? data)
       (warn "Data is nil, merging nil into path " path))
     (update-in db path
                (fn [current-data record]
                  (cond
                    (map? current-data)
                    (merge current-data record)

                    (or (seq? current-data) (list? current-data) (vector? current-data))
                    (let [updated-record (->
                                          (filter (fn [r]
                                                    (= (get r primary-key)
                                                       (get record primary-key)))
                                                  current-data)
                                          first
                                          (merge record))]
                      (->
                       (filter (fn [r]
                                 (not= (get r primary-key)
                                       (get record primary-key))) current-data)
                       (conj updated-record)))))
                data))))

(rf/reg-event-db
 :initialize-form-from-server
 (fn [db [_
          {:keys [id fields url] :as options}
          primary-key-value]]
   (let [success-fn (fn [data]
                      (rf/dispatch [:initialize-form-from-server-complete id data]))
         error-fn #(error %)]
     (ajax/GET (str url "/" primary-key-value)
               {:handler success-fn
                :error-handler error-fn}))
     (-> db
         (assoc-in [:data id :options] options))))

(rf/reg-event-db
 :initialize-form-from-server-complete
 (fn [db [_ id data]] ;;id is the form-id
   (let [options (get-in db [:data id :options])
         fields (:fields options)
         validation (reduce
                     (fn [validation-store [k v]]
                       (update-validation-store
                        validation-store
                        (lookup-validators fields k)
                        k v))
                     ;;initial value
                     (empty-validation-store fields)
                     data)]
     (-> db
         (assoc-in [:data id :options]    options)
         (assoc-in [:data id :validation] validation)
         (assoc-in [:data id :data]       data)))))

(rf/reg-event-db
 :initialize-form
 (fn [db [_
          {:keys [id fields] :as options}
          initial-data]]
   ;; validate initial-data
   (let [validation (reduce
                     (fn [validation-store [k v]]
                       (update-validation-store
                        validation-store
                        (lookup-validators fields k)
                        k v))
                     ;;initial value
                     {:new-fields (set (->> fields (map :id)))
                      :invalid-fields {}
                      :valid-fields #{}}
                     initial-data)]
     (-> db
         (assoc-in [:data id :options]    options)
         (assoc-in [:data id :validation] validation)
         (assoc-in [:data id :data]       initial-data)))))

(rf/reg-event-db
 :update-form-field
 (fn [db [_ id k v]]
   (info "updating form" id ":" k v)
   (let [options    (get-in db [:data id :options])
         callback   (:on-form-update-dispatch options)
         fields     (:fields options)
         ;;       data (get-in db [:data id :data])
         validation (get-in db [:data id :validation])
         validators (lookup-validators fields k)]
     (when callback
       (rf/dispatch callback))
     (-> db
         (update-in [:data id :validation]
                    update-validation-store validators k v)
         (assoc-in [:data id :data k] v)))))

(defn validate-all-fields
  "returns new validation store where everything is validated"
  [validation-store fields data]
  (reduce
   (fn [validation-store field]
     (let [k (:id field)
           v (get data k)]
       (update-validation-store
        validation-store
        (lookup-validators fields k)
        k v)))
   validation-store
   fields))

(rf/reg-event-db
 :persist-form
 (fn [db [_ id optional-mergeable-params]]
   (let [{:keys [options data]} (get-in db [:data id])
         fields                            (:fields options)
         optional-mergeable-params         (or optional-mergeable-params {})
         old-data                          (merge data optional-mergeable-params)
         success-fn                        (fn [new-data]
                                             (do
                                               (rf/dispatch [:persist-form-complete id new-data old-data])
                                               (when (:success-fn options)
                                                 (apply (:success-fn options) new-data old-data))))
         error-fn                          (fn [new-data]
                                             (do
                                               (rf/dispatch [:persist-form-error id new-data])
                                               (when (:error-fn options)
                                                 (apply (:error-fn options) new-data old-data))))
         validation                        (validate-all-fields (empty-validation-store fields) fields data)
         valid?                            (empty? (:invalid-fields validation))]

     (if-not valid?
       (assoc-in db [:data id :validation] validation)
       ;;else, persist
       (do
         (if (get data (:primary-key options))
           (ajax/PUT (:url options)
                     {:params        (merge data optional-mergeable-params)
                      :handler       success-fn
                      :error-handler error-fn})
           ;;else
           (ajax/POST (:url options)
                      {:params        (merge data optional-mergeable-params)
                       :handler       success-fn
                       :error-handler error-fn}))
         (assoc-in db [:data id :validation] validation))))))

(defn mysql-fmt
  [s]
  (when s
    (let [s (coerce/from-date s)]
      (fmt/unparse (fmt/formatter "dd-MM-yyyy") s))))

(rf/reg-event-db
 :persist-form-complete
 (fn [db [_ id new-data old-data]]
   (rf/dispatch [:show-growl (or (get-in db [:data id :options :save-message]) "Gegevens opgeslagen")])
   (if (or (list? new-data)
           (vector? new-data)
           (seq? new-data))
     ;;its a list, just return the database
     db
     ;;else, it's a map, validate and re-enter into the database
     (let [fields (get-in db [:data id :options :fields])
           validation (reduce ;;revalidate everything
                       (fn [validation-store [k v]]
                         (update-validation-store
                          validation-store
                          (lookup-validators fields k)
                          k v))
                       (get-in db [:data id :validation])
                       new-data)]
       (-> db
           (assoc-in [:data id :data] new-data)
           (assoc-in [:data id :validation] new-data))))))

(rf/reg-event-db
 :persist-form-error
 (fn [db [_ id new-data]]
   (rf/dispatch [:show-growl (str "Er ging iets mis met het opslaan van de gegevens: " new-data)])
   db))

(defn valid-form?
  [fields data validation]
  (let [;;validate whole record, THEN judge
        {:keys [invalid-fields]}
        (reduce
         (fn [validation-store [k v]]
           (update-validation-store
            validation-store
            (lookup-validators fields k)
            k v))
         validation
         data)]
    (empty? invalid-fields)))
