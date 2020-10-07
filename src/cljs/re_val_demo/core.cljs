(ns re-val-demo.core
(:require [re-val-demo.config :as config]
          [re-val-demo.views :as my-views]
          [re-val-demo.db]
          [re-val-demo.events]
          [re-val-demo.subs]
          [re-val.events]
          [re-val.subs]
          [re-val.views :as views]
          [re-frame.core :as re-frame]
          [reagent.core :as reagent]))

(defn dev-setup []
  (when config/debug?
    (devtools.core/enable-feature! :sanity-hints)
    (devtools.core/install!)
    (enable-console-print!)
    (println "dev mode")))

(defn mount-root
  []
  (re-frame/clear-subscription-cache!)
  (reagent.dom/render [my-views/main-panel]
                      (.getElementById js/document "app")))

(defn ^:export init []
  (re-frame/dispatch-sync [:initialize-db])
  (dev-setup)
  (mount-root))
