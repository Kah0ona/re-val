(ns re-val.validators
  (:require [cljs-time.core :as t]
            [cljs-time.format :as fmt]
            [reagent.core :as r]
            [taoensso.timbre :as timbre
             :refer-macros (log  trace  debug  info  warn  error  fatal  report
                                 logf tracef debugf infof warnf errorf fatalf reportf
                                 spy get-env log-env)]))

;;TODO i18n
(defn required-validator
  [s]
  (debug s)
  (if (or (nil? s)
          (and (string? s)
               (= "" (clojure.string/trim s))))
    [false "dit veld is verplicht"]
    [true ]))


(defn something-selected-validator
  [s]
  (if (or
       (= "-- selecteer --" s)
       (nil? s)
       (= "" s))
    [false "dit veld is verplicht."]
    [true ]))

(defn checked-validator
  [s]
  (debug s)
  (if (and (not (nil? s)) (boolean? s) s)
    [true ]
    [false "dit veld moet geselecteerd zijn."]))
