(defproject re-val "0.1.1"
  :description "A form component and form validation library/API for re-frame."
  :url "https://github.com/Kah0ona/re-val.git"
  :license {:name "MIT"}
  :dependencies [[org.clojure/clojure "1.10.0"]
                 [org.clojure/clojurescript "1.10.597"]
                 [reagent "1.0.0-alpha2"]
                 [re-frame-utils "0.1.0"]
                 [com.taoensso/timbre "4.10.0"]
                 [cljsjs/quill "1.3.5-0"]
                 [cljs-ajax "0.8.0"]
                 [fipp "0.6.10"] ;;pretty printer cljs
                 [com.andrewmcveigh/cljs-time "0.5.2"] ;date/time
                 [re-frame "1.1.1"]]

  :plugins [[lein-cljsbuild "1.1.5"]]

  :min-lein-version "2.5.3"

  :source-paths ["src/clj" "src/cljs"]

  :clean-targets ^{:protect false} ["resources/public/js/compiled" "target" "test/js"]

  :figwheel {:css-dirs ["resources/public/css"]}

  :repl-options {:nrepl-middleware [cemerick.piggieback/wrap-cljs-repl]}

  :aliases {"dev"   ["do" "clean"
                     ["pdo" ["figwheel" "dev"]]]
            "demo"  ["do" "clean"
                     ["pdo" ["figwheel" "demo"]]]
            "build" ["do" "clean"
                     ["cljsbuild" "once" "min"]]}

  :profiles
  {:dev
   {:dependencies [[binaryage/devtools "0.9.7"]
                   [figwheel-sidecar "0.5.13"]
                   [com.cemerick/piggieback "0.2.2"]]

    :plugins [[lein-figwheel "0.5.13"]
              [lein-doo "0.1.8"]
              [lein-pdo "0.1.1"]]

    :nrepl-middleware [cemerick.piggieback/wrap-cljs-repl]}}

  :cljsbuild
  {:builds
   [{:id           "dev"
     :source-paths ["src/cljs"]
     :figwheel     {:on-jsload "re-val-demo.core/mount-root"}
     :compiler     {:main                 re-val.core
                    :output-to            "resources/public/js/compiled/app.js"
                    :output-dir           "resources/public/js/compiled/out"
                    :asset-path           "js/compiled/out"
                    :source-map-timestamp true
                    :closure-defines      {goog.DEBUG true}
                    :preloads             [devtools.preload]
                    :external-config      {:devtools/config {:features-to-install :all}}}}
    {:id           "min"
     :source-paths ["src/cljs"]
     :compiler     {:main            re-val.core
                    :output-to       "resources/public/js/compiled/app.js"
                    :optimizations   :advanced
                    :closure-defines {goog.DEBUG false}
                    :pretty-print    false}}

    {:id           "demo"
     :source-paths ["src/cljs"]
     :compiler     {:main            re-val-demo.core
                    :output-to       "resources/public/js/compiled/app-demo.js"
                    :output-dir      "resources/public/js/compiled/demo/out"
                    :optimizations   :advanced
                    :closure-defines {goog.DEBUG false}
                    :pseudo-names    false
                    :pretty-print    false}}
    {:id           "test"
     :source-paths ["src/cljs" "test/cljs"]
     :compiler     {:main          re-val.runner
                    :output-to     "resources/public/js/compiled/test.js"
                    :output-dir    "resources/public/js/compiled/test/out"
                    :optimizations :none}}
    ]})
