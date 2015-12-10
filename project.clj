(defproject matthiasn/inspect "0.2.1"
  :description "Log to a web application to inspect what's going on in your application"
  :url "https://github.com/matthiasn/inspect"
  :license {:name "Eclipse Public License"
            :url "http://www.eclipse.org/legal/epl-v10.html"}
  :dependencies [[org.clojure/clojure "1.7.0"]
                 [org.clojure/tools.reader "1.0.0-alpha1"]
                 [org.clojure/clojurescript "1.7.170"]
                 [org.clojure/core.async "0.2.374"]
                 [org.clojure/tools.logging "0.3.1"]
                 [org.clojure/tools.namespace "0.2.10"]
                 [ch.qos.logback/logback-classic "1.1.2"]
                 [com.taoensso/sente "1.4.1"]
                 [org.clojure/core.match "0.2.2"]
                 [http-kit "2.1.19"]
                 [compojure "1.3.2"]
                 [mvxcvi/puget "0.8.0"]
                 [hiccup "1.0.5"]
                 [hiccup-bridge "1.0.1"]
                 [garden "1.2.5"]
                 [ring "1.3.2"]
                 [ring/ring-defaults "0.1.4"]
                 [clj-time "0.9.0"]
                 [org.clojure/clojurescript "0.0-3126"]
                 [reagent "0.5.0"]
                 [com.stuartsierra/component "0.2.2"]]

  :source-paths ["src/clj/"]

  :plugins [[lein-cljsbuild "1.0.5"]
            [codox "0.8.10"]]

            ;:clean-targets ^{:protect false} ["resources/public/inspect/js/build/"]

  :cljsbuild {:builds [{:id "release"
                        :source-paths ["src/cljs"]
                        :compiler {:output-to "resources/public/inspect/js/build/inspect.js"
                                   :optimizations :advanced}}]})
