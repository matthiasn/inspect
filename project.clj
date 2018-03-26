(defproject matthiasn/inspect "0.0-SNAPSHOT"
  :dependencies [[org.clojure/clojure "1.9.0"]
                 [org.clojure/clojurescript "1.10.238"]
                 [reagent "0.8.0-alpha2" :exclusions [cljsjs/react cljsjs/react-dom]]
                 [re-frame "0.10.5"]
                 [com.taoensso/timbre "4.10.0"]
                 [matthiasn/systems-toolbox "0.6.34"]
                 [matthiasn/systems-toolbox-electron "0.6.22"]
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
                                       :optimizations  :simple
                                       :parallel-build true}}
                       {:id           "view-dev"
                        :source-paths ["src/inspect/specs" "src/inspect/view"]
                        :compiler     {:main           inspect.view.core
                                       :output-to      "dev/view/core.js"
                                       :target         :nodejs
                                       :output-dir     "dev/view"
                                       :source-map     true
                                       :optimizations  :none
                                       :parallel-build true}}
                       {:id           "updater"
                        :source-paths ["src/inspect/specs" "src/inspect/update"]
                        :compiler     {:main           inspect.update.core
                                       :output-to      "prod/updater/update.js"
                                       :target         :nodejs
                                       :output-dir     "prod/updater"
                                       :optimizations  :simple
                                       :parallel-build true}}]})
