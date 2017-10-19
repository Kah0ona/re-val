(ns re-val.layout
  (:require [reagent.core :as r]
            [taoensso.timbre :as timbre
             :refer-macros (log  trace  debug  info  warn  error  fatal  report
                                 logf tracef debugf infof warnf errorf fatalf reportf
                                 spy get-env log-env)]))

(defn input-group
  ([label input]
   (input-group label input nil))
  ([label input icon]
   (let []
     (fn [label input icon]
       [:div.xinput-group
        [:div.row>div.col-sm-12>div.form-group.fg-float
         (when icon
           [:span.input-group-addon
            [:i {:className (str "zmdi zmdi-"icon)}]])
         [:div {:className "fg-line fg-toggled" }
          input
          [:label.fg-label label]]]]))))

(defn input-group-2
  ([label-1 label-2 input-1 input-2]
   (input-group-2 label-1 label-2 input-1 input-2 nil))
  ([label-1 label-2 input-1 input-2 icon]
   (let []
     (fn [label-1 label-2 input-1 input-2 icon]
       [:div
        [:div.row
         [:div.col-md-6.col-sm-12
          [:div.form-group.fg-float
           (when icon
             [:span.input-group-addon
              [:i {:className (str "zmdi zmdi-"icon)}]])

           [:div {:className "fg-line fg-toggled" }
            input-1
            [:label.fg-label label-1]]]]
         (when label-2
           [:div.col-md-6.col-sm-12
            [:div.form-group.fg-float
             [:div {:className "fg-line fg-toggled" }
              input-2
              (when label-2
                [:label.fg-label label-2])]]])]
        [:br]]))))

(defn input-group-checkbox
  [title body]
  [:div.xinput-group.checkbox.m-b-15
   [:label
    body
    title]])
