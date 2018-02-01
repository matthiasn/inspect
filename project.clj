(defproject matthiasn/inspect "0.0-SNAPSHOT"
  :dependencies [[org.clojure/clojure "1.9.0"]
                 [org.clojure/clojurescript "1.9.946"]
                 [re-frame "0.10.2"]
                 [com.taoensso/timbre "4.10.0"]
                 [matthiasn/systems-toolbox "0.6.29"]
                 [matthiasn/systems-toolbox-electron "0.6.20"]
                 [timbre-ns-pattern-level "0.1.2"]
                 [org.clojure/data.avl "0.0.17"]
                 [frankiesardo/linked "1.2.9"]
                 [com.cognitect/transit-cljs "0.8.243"]]

  :plugins [[lein-cljsbuild "1.1.7"]
            [lein-shell "0.5.0"]
            [deraen/lein-sass4clj "0.3.1"]
            [lein-ancient "0.6.15"]]

  :sass {:source-paths ["src/scss/"]
         :target-path  "resources/public/css/"}

  :clean-targets ^{:protect false} ["target/" "prod/" "dev/" "out"]

  :aliases {"dist" ["do"
                    ["clean"]
                    ["cljsbuild" "once" "main"]
                    ["cljsbuild" "once" "view"]
                    ["cljsbuild" "once" "updater"]
                    ["sass4clj" "once"]]}

  :cljsbuild {:builds [{:id           "main"
                        :source-paths ["src/inspect/specs" "src/inspect/main"]
                        :compiler     {:main           inspect.main.core
                                       :target         :nodejs
                                       :output-to      "prod/main/main.js"
                                       :output-dir     "prod/main"
                                       :npm-deps       {:electron-log     "2.2.7"
                                                        :moment           "2.18.1"
                                                        :react            "15.6.1"
                                                        :react-dom        "15.6.1"
                                                        :electron-dl      "1.10.0"
                                                        :decompress       "4.2.0"
                                                        :viz.js           "1.8.0"
                                                        :level            "1.7.0"
                                                        :electron-updater "2.8.7"
                                                        :kafka-node       "2.2.1"
                                                        :sinek            "6.0.3"
                                                        :electron         "1.7.6"}
                                       :optimizations  :simple
                                       :language-in    :ecmascript5
                                       :language-out   :ecmascript5
                                       :parallel-build true}}
                       {:id           "view"
                        :source-paths ["src/inspect/specs" "src/inspect/view"]
                        :compiler     {:main           inspect.view.core
                                       :output-to      "prod/view/core.js"
                                       :target         :nodejs
                                       :output-dir     "prod/view"
                                       :npm-deps       {:electron-log     "2.2.7"
                                                        :moment           "2.18.1"
                                                        :d3-force         "1.0.6"
                                                        :d3               "4.10.0"
                                                        :d3-ellipse-force "0.1.1"
                                                        :randomcolor      "0.5.3"
                                                        :viz.js           "1.8.0"
                                                        :react            "15.6.1"
                                                        :react-dom        "15.6.1"
                                                        :electron         "1.7.8"}
                                       :optimizations  :simple
                                       :parallel-build true}}
                       {:id           "view-dev"
                        :source-paths ["src/inspect/specs" "src/inspect/view"]
                        :compiler     {:main           inspect.view.core
                                       :output-to      "dev/view/core.js"
                                       :target         :nodejs
                                       :output-dir     "dev/view"
                                       :source-map     true
                                       :npm-deps       {:electron-log     "2.2.7"
                                                        :moment           "2.18.1"
                                                        :d3-force         "1.0.6"
                                                        :d3               "4.10.0"
                                                        :d3-ellipse-force "0.1.1"
                                                        :randomcolor      "0.5.3"
                                                        :viz.js           "1.8.0"
                                                        :react            "15.6.1"
                                                        :react-dom        "15.6.1"
                                                        :electron         "1.7.8"}
                                       :optimizations  :none
                                       :parallel-build true}}
                       {:id           "updater"
                        :source-paths ["src/inspect/specs" "src/inspect/update"]
                        :compiler     {:main           inspect.update.core
                                       :output-to      "prod/updater/update.js"
                                       :target         :nodejs
                                       :output-dir     "prod/updater"
                                       :npm-deps       {:electron-log     "2.2.7"
                                                        :moment           "2.18.1"
                                                        :react            "15.6.1"
                                                        :react-dom        "15.6.1"
                                                        :electron-updater "2.8.7"
                                                        :electron         "1.7.8"}
                                       :optimizations  :simple
                                       :parallel-build true}}]})
