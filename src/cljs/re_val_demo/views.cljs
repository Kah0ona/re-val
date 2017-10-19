(ns re-val-demo.views
  (:require [fipp.clojure :refer [pprint]]
            [re-frame.core :as rf]
            [re-val.views :as form]
            [re-val.layout :as layout]
            [re-val.validators :as v]
            [reagent.core :as r]
            [taoensso.timbre :as timbre
             :refer-macros (log  trace  debug  info  warn  error  fatal  report
                                 logf tracef debugf infof warnf errorf fatalf reportf
                                 spy get-env log-env)]))

(def form-id :form)

(def form-fields
  [{:id :firstname
    :title "Voornaam"
    :validators [v/required-validator]}
   {:id :surname
    :title "Achternaam"
    :validators [v/required-validator]}
   {:id :bio
    :title "Bio"}
   {:id :number
    :title "Nummer"}])

(def form-opts
  {:id form-id
   :url "/something" ;;TODO make this a 'persist dispatch?'
   :success-fn #(rf/dispatch [:do-something])
   :primary-key :id
   :fields form-fields})

(def initial-data
  {:id 1
   :firstname "J.J."
   :surname "Cale"
   :number "Number"
   :bio "Lorem ipsum \n bla bla"})

(defn main-panel
  []
  (rf/dispatch [:initialize-form form-opts initial-data])
  (fn []
    [:div.container
     [:h1 "Form Demo"]
     [layout/input-group-2 "Firstname" "Surname"
      [form/text-input form-id :firstname]
      [form/text-input form-id :surname]]

     ;;TODO add more

     [form/form-error-listing]

     ]))
