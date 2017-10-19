(ns re-val.typeahead-list
  (:require-macros [reagent-forms.macros :refer [render-element]])
  (:require [re-frame.core :as rf]
            [clojure.string :refer [split trim]]
            [taoensso.timbre :as timbre
             :refer-macros (log  trace  debug  info  warn  error  fatal  report
                                logf tracef debugf infof warnf errorf fatalf reportf
                                spy get-env log-env)]
            [reagent-forms.core :refer [bind-fields init-field value-of]]
            [reagent.core :as r]
            [cljs-time.core :as t]
            [cljs-time.format :as fmt]
            [reagent-forms.core :refer [bind-fields init-field value-of]]
            [schema.core :as s]))

(s/defschema TypeAheadOptions
  {:data-path [s/Any]
   :title s/Str
   ;list of currently populated data
   :id-field s/Keyword
   :screen-state-path [s/Keyword]
   :create-url s/Str
   :post-process-fn s/Any

   ; if set to true, if someone types something without suggestion, it will do a POST anyway.
   ; it's then up to decide what to do
   ; useful ie for tagging; create a tag when it doesnt exist.
   (s/optional-key :auto-create) s/Bool

   ; function that should prepare the data to be created based on selected suggestion of combobox and other stuff
   ; being closed over. it will dispatch to :persist-data-to-server, with as params what is returned from this function
   :create-data-fn s/Any

   :delete-url s/Str

   ; function that should prepare the data to be deleted based on the record that is clicked.
   ;it will dispatch to :persist-data-to-server, with as params what is returned from this function
   :delete-data-fn s/Any

   ;formats the data in the list below
   :list-formatter s/Any
   ;function that takes current input of search box, and returns a list of matching results
   :suggestion-match-fn s/Any

   })

(defmethod init-field :typeahead-autocreate
  [[type {:keys [id data-source input-class list-class item-class highlight-class
                 input-placeholder result-fn choice-fn clear-on-focus?
                 can-create can-delete post-process-fn]
          :as attrs
          :or {result-fn identity
               choice-fn identity
               can-create true
               can-delete true
               clear-on-focus? true}}] {:keys [doc get save!]}]
  (let [typeahead-hidden? (atom true)
        mouse-on-list? (atom false)
        selected-index (atom -1)
        selections (atom [])
        choose-selected #(if (and (not-empty @selections) (> @selected-index -1))
                           (let [choice (nth @selections @selected-index)]
                             (save! id choice)
                             (choice-fn choice)
                             (reset! typeahead-hidden? true))
                           ;auto create, fire callback with what was typed
                           (choice-fn (get id)))]
    (render-element attrs doc
                    [type
                     [:input.form-control {:type        :text
                              :placeholder input-placeholder
                              :class       input-class
                              :disabled    (not can-create)
                              :value       (let [v (get id)]
                                             (if-not (iterable? v)
                                               v (first v)))
                              :on-focus    #(when clear-on-focus? (save! id nil))
                              :on-blur     #(when-not @mouse-on-list?
                                              (reset! typeahead-hidden? true)
                                              (reset! selected-index -1))
                              :on-change   #(when-let [value (trim (value-of %))]
                                              (reset! selections (data-source (.toLowerCase value)))
                                              (save! id (value-of %))
                                              (reset! typeahead-hidden? false)
                                              (reset! selected-index -1))
                              :on-key-down #(do
;                                              (debug (.-which %))
                                              (case (.-which %)
                                                38 (do
                                                     (.preventDefault %)
                                                     (when-not (= @selected-index 0)
                                                       (swap! selected-index dec)))
                                                40 (do
                                                     (.preventDefault %)
                                                     (when-not (= @selected-index (dec (count @selections)))
                                                       (save! id (value-of %))
                                                       (swap! selected-index inc)))
                                                9  (do
                                                     (reset! typeahead-hidden? true)
                                                     (choose-selected))
                                                13 (do
                                                     (reset! typeahead-hidden? true)
                                                     (choose-selected))
                                                27 (do (reset! typeahead-hidden? true)
                                                       (reset! selected-index 0))
                                                "default"))}]

                     [:ul {:style {:display (if (or (empty? @selections) @typeahead-hidden?) :none :block) }
                           :class list-class
                           :on-mouse-enter #(reset! mouse-on-list? true)
                           :on-mouse-leave #(reset! mouse-on-list? false)}
                      (doall
                       (map-indexed
                        (fn [index result]
                          [:li {:tab-index     index
                                :key           index
                                :class         (if (= @selected-index index) highlight-class item-class)
                                :on-mouse-over #(do
                                                  (reset! selected-index (js/parseInt (.getAttribute (.-target %) "tabIndex"))))
                                :on-click      #(do
                                                  (reset! typeahead-hidden? true)
                                                  (save! id result)
                                                  (choice-fn result))}
                           (result-fn result)])
                        @selections))]])))



(defn tal-row
  [opts record]
  (let [formatted-value ((:list-formatter opts) record)]
    [:tr {:key (get record (:id-field opts)) }
     [:td.text-left formatted-value]
     [:td.commands
      [:span.delete
       [:button.btn.btn-xs.btn-danger.waves-effect.waves-circle.waves-float
        {:on-click #(do
                       (rf/dispatch [:persist-data-to-server
                                  :delete
                                  opts
                                  ((:delete-data-fn opts) record)])
                      (when (:post-process-fn opts)
                        ((:post-process-fn opts) %)))}
        [:i.zmdi.zmdi-close]]]]]))

(s/defn typeahead-list
  "Widget that shows a type ahead box, showing suggestions from data source (should be preloaded).
  Hitting return will add it to the list below.
  After hitting return, the data is saved.
  The listed data can be removed, so the options should provide the following keys:
  :suggestions - List of suggestions that the typeahead uses
  :data-path where to save/read/update/delete the selected data to (path in the local db),
  this means the widget hooks into to the central app database, and doesn't need to use local atoms for the data itself."
  [opts :- TypeAheadOptions
   data :- s/Any]
  (let [doc (r/atom {})
        tal-tpl [:div {:field (if :auto-create :typeahead-autocreate :typeahead)
                       :id :internal-key
                       :input-placeholder (:input-placeholder opts)
                       :data-source (:suggestion-match-fn opts)
                       :input-class "form-control"
                       :list-class "typeahead-list"
                       :item-class "typeahead-item"
                       :highlight-class "highlighted"
                       :choice-fn #((reset! doc {})
                                    ;save to the server, such that it appears in the list below.
                                    (debug %1)
                                    (let [rec ((:create-data-fn opts) %1)
                                          create-dispatch [:persist-data-to-server
                                                           :create
                                                           opts
                                                           rec
                                                           (vec (concat [:screen-state]
                                                                        (:screen-state-path opts)))]]
                                      (rf/dispatch create-dispatch)
                                      (when (:post-process-fn opts)
                                        ((:post-process-fn opts) %1))
                                      ))}]]
    (fn typeahead-list-renderer [opts data]
      (let [rows (doall (map #(tal-row opts %1) data))]
        [:div.typeaheadlist
         [bind-fields tal-tpl doc]
         [:div.table-responsive
          [:table.table.bootgrid-table
           [:thead [:tr [:th ""][:th ""]]]
           [:tbody rows]]]]))))
