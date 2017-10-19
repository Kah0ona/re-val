(ns re-val.datepicker-util
  (:require [cljs-time.coerce :as coerce]
            [cljs-time.core :as t]
            [cljs-time.format :as fmt]
            [reagent.core :as r]
            [taoensso.timbre :as timbre
             :refer-macros (log  trace  debug  info  warn  error  fatal  report
                                logf tracef debugf infof warnf errorf fatalf reportf
                                spy get-env log-env)]))

(defn date-picker-parser
  [dt]
  (if (nil? dt)
    nil
    (let [parsed (if (string? dt)
                   (coerce/from-string dt)
                   (coerce/from-date dt))]
      {:year (t/year parsed) :month (t/month parsed) :day (t/day parsed)})))

(defn date-picker-unparser
  [date]
  (debug date)
  (if (nil? date)
    nil
    (let [formatter (fmt/formatters :date-time-no-ms)]
      (fmt/unparse formatter (t/date-time (:year date )
                                          (:month date)
                                          (:day date))))))

(defn date-picker-native-date-unparser
  [date]
  (if (nil? date)
    nil
    (coerce/to-date (t/date-time (:year date ) (:month date) (:day date)))))

(def default-date-picker-settings
  {:field :datepicker
   :lang :nl-NL
   :date-format "dd-mm-yyyy"
   :auto-close? true
   :in-fn date-picker-parser
   :out-fn date-picker-unparser})

(defn date-formatter
  [s]
;  (debug "date formatter " s)
  (let [parsed (coerce/from-date s)
        formatter (fmt/formatter "dd-MM-yyyy")]
    (fmt/unparse formatter parsed)))
