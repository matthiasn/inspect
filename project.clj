(defproject matthiasn/inspect "0.0-SNAPSHOT"
  :dependencies [[org.clojure/clojure "1.10.0"]
                 [org.clojure/clojurescript "1.10.516"]
                 [re-frame "0.10.6"]
                 [reagent "0.8.1"]
                 [com.taoensso/timbre "4.10.0"]
                 [matthiasn/systems-toolbox "0.6.38"]
                 [matthiasn/systems-toolbox-electron "0.6.29"]
                 [timbre-ns-pattern-level "0.1.2"]
                 [org.clojure/data.avl "0.0.18"]
                 [frankiesardo/linked "1.3.0"]
                 [com.cognitect/transit-cljs "0.8.256"]]

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
                        :source-paths ["src/cljs"]
                        :compiler     {:main           inspect.main.core
                                       :target         :nodejs
                                       :output-to      "prod/main/main.js"
                                       :output-dir     "out/main"
                                       :optimizations  :simple
                                       :npm-deps       true
                                       :language-in    :ecmascript5
                                       :language-out   :ecmascript5
                                       :parallel-build true}}
                       {:id           "view"
                        :source-paths ["src/cljs"]
                        :compiler     {:main           inspect.view.core
                                       :output-to      "prod/view/core.js"
                                       :target         :nodejs
                                       :npm-deps       true
                                       :output-dir     "out/view"
                                       :optimizations  :simple
                                       :parallel-build true}}
                       {:id           "view-dev"
                        :source-paths ["src/cljs"]
                        :compiler     {:main           inspect.view.core
                                       :output-to      "dev/view/core.js"
                                       :target         :nodejs
                                       :output-dir     "dev/view"
                                       :source-map     true
                                       :npm-deps       true
                                       :optimizations  :none
                                       :parallel-build true}}
                       {:id           "updater"
                        :source-paths ["src/cljs"]
                        :compiler     {:main           inspect.update.core
                                       :output-to      "prod/updater/update.js"
                                       :target         :nodejs
                                       :output-dir     "prod/updater"
                                       :optimizations  :simple
                                       :npm-deps       true
                                       :parallel-build true}}]})
