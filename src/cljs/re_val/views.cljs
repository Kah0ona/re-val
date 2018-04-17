(ns re-val.views
  (:require-macros [reagent.ratom :refer [reaction]])
  (:require [cljs-time.coerce :as coerce]
            [cljs-time.core :as t]
            [cljs-time.format :as fmt]
            [re-frame.core :as rf]
            [re-val.datepicker-component :as dpc]
            [re-val.datepicker-util :as dutil]
            [re-val.richtexteditor :as quill]
            [reagent.core :as r]
            [taoensso.timbre :as timbre
             :refer-macros (log  trace  debug  info  warn  error  fatal  report
                                 logf tracef debugf infof warnf errorf fatalf reportf
                                 spy get-env log-env)]))

(defn get-class
  [k {:keys [invalid-fields valid-fields]}]
  (if (contains? valid-fields k)
    (str "success input-" (name k))
    (if (nil? (get invalid-fields k))
      (str "input-" (name k))
      (str "error input-" (name k)))))

(defn rich-text-editor
  [form-id k]
  (let [doc (rf/subscribe [:get-form-field-data form-id k])]
    (fn [form-id k]
      (debug "rta" @doc)
      [quill/editor
       {:id (str "quill-"(name form-id) "-" (name k))
        :content (or @doc "")
        :selection nil
        :on-change-fn #(rf/dispatch [:update-form-field form-id k %2])}])))

(defn text-input
  [form-id k & rest]
  (let [validation (rf/subscribe [:get-form-validation form-id])
        doc        (rf/subscribe [:get-form-field-data form-id k])]
    (fn [form-id k]
      #_(debug @doc @validation)
      [:input.form-control
       {:type      :text
        :class     (get-class k @validation)
        :on-change #(rf/dispatch [:update-form-field form-id k (-> % .-target .-value)])
        :value     @doc}])))

(defn password-input
  [form-id k & rest]
  (let [validation (rf/subscribe [:get-form-validation form-id])
        doc        (rf/subscribe [:get-form-field-data form-id k])]
    (fn [form-id k]
      [:input.form-control
       {:type      :password
        :class     (get-class k @validation)
        :on-change #(rf/dispatch [:update-form-field form-id k (-> % .-target .-value)])
        :value     @doc}])))

(defn number-input
  [form-id k & rest]
  (let [validation (rf/subscribe [:get-form-validation form-id])
        doc        (rf/subscribe [:get-form-field-data form-id k])]
    (fn [form-id k]
      [:input.form-control
       {:type      :numeric
        :class     (get-class k @validation)
        :on-change #(rf/dispatch [:update-form-field form-id k (-> % .-target .-value)])
        :value     @doc}])))

(defn datepicker-input
  "This thing works with cljs-time date-time"
  [form-id k & [edit?]]
  (let [doc (rf/subscribe [:get-form-field-data form-id k])]
    (fn [form-id k & [edit?]]
      [:span.datepicker-wrap
       [dpc/datepicker
        {:id          k
         :date-format "dd-mm-yyyy"
         :inline      false
         :auto-close? true
         :disabled    (if (nil? edit?) false (not edit?))
         :placeholder "DD-MM-YYYY"
         :lang        :nl-NL}
        {:get   (fn [_]
                  (or
                   (dutil/date-picker-parser @doc)
                   ""))
         :save! #(rf/dispatch [:update-form-field form-id k (dutil/date-picker-unparser %2)])}]])))

(defn text-area
  [form-id k & rest]
  (let [validation (rf/subscribe [:get-form-validation form-id])
        doc        (rf/subscribe [:get-form-field-data form-id k])
        chg        #(rf/dispatch [:update-form-field form-id k (-> % .-target .-value)])]
    (fn [form-id k]
      [:textarea.form-control
       {:type      :text
        :class     (get-class k @validation)
        :on-change #(rf/dispatch [:update-form-field form-id k (-> % .-target .-value)])
        :value     @doc}])))

(defn checkbox-input
  [form-id k & [edit?]]
  (let [validation (rf/subscribe [:get-form-validation form-id])
        doc        (rf/subscribe [:get-form-field-data form-id k])
        chg #(rf/dispatch [:update-form-field form-id k (not (get doc k))])]
    [:span
     [:input.form-control
      {:type :checkbox
       :class (get-class k @validation)
       :on-change chg
       :disabled (if (nil? edit?)
                   false
                   (not edit?))
       :checked (get @doc k)
       :value (get @doc k)}]
     [:i.input-helper]]))

(defn select-item
  "options should be a map of value -> title"
  [form-id k options & rest]
  (let [validation (rf/subscribe [:get-form-validation form-id])
        doc        (rf/subscribe [:get-form-field-data form-id k])
        on-change  (first rest)
        chg        #(do
                      (let [rv (-> % .-target .-value)
                            v  (if (= ":nothing-selected" rv) nil rv)]
                        (rf/dispatch [:update-form-field form-id k v])
                        (when on-change
                          (on-change v))))]
    (fn
      [form-id k options & [on-change]]
      [:div {:class (get-class k @validation)}
       [:select.form-control
        {:on-change chg
         :value     (if (nil? @doc)
                      ":nothing-selected"
                      @doc)}
        (concat
         [^{:key -1}
          [:option {:value ":nothing-selected"} "-- Selecteer --"]]
         (map-indexed
          (fn [i [value title]]
            ^{:key i}
            [:option {:value value} title])
          options))]])))

(defn fields->titles
  [f]
  (into {} (map (fn [{:keys [id title]}]
                  [id title]) f)))

(defn form-error-listing
  [form-id]
  (let [invalid-fields (rf/subscribe [:get-form-invalid-fields form-id])
        fields         (rf/subscribe [:get-form-fields form-id])]
    (fn []
      (let [titles (fields->titles @fields)]
        (debug @invalid-fields)
        (when-not (empty? @invalid-fields)
          [:div.alert.alert-danger
           "Sommige velden zijn niet goed ingevuld: "
           [:ul (doall
                 (map (fn [[k e]]
                        [:li {:key k}
                         (str (get titles k) ": " (first e))])
                      @invalid-fields))]])))))

(defn colorpicker-inner
  [options]
  (let [;update dom evt, via directe js calls, nav events
        update (fn [opts]
                 (debug "updated")
                 (let [k     (:key (r/props opts))
                       value (:value (r/props opts))]
                   (.installByClassName js/jscolor "jscolor" )))]

    (r/create-class
      ; basis container, doet niets met props, hierin wordt de component gerenderd
     {:display-name   "colorpicker inner"
      :reagent-render (fn [options]
                        (debug options)
                        [:div.colorpicker-wrap
                         [:input.jscolor
                          {:id           (:key options)
                           :defaultValue (:value options)}]])

      :component-did-mount          update
      :component-did-update         update
      :component-will-receive-props update})))

(defn colorpicker
  "outer component"
  [opts value]
    (fn [opts value]
     [colorpicker-inner
      {:key (:key opts)
       :options opts
       :value value}]))
