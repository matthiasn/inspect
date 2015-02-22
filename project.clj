(defproject com.matthiasnehlsen/inspect "0.1.9-SNAPSHOT"
  :description "Log to a web application to inspect what's going on in your application"
  :url "https://github.com/matthiasn/inspect"
  :license {:name "Eclipse Public License"
            :url "http://www.eclipse.org/legal/epl-v10.html"}
  :dependencies [[org.clojure/clojure "1.6.0"]
                 [org.clojure/core.async "0.1.346.0-17112a-alpha"]
                 [org.clojure/tools.logging "0.3.1"]
                 [org.clojure/tools.namespace "0.2.9"]
                 [ch.qos.logback/logback-classic "1.1.1"]
                 [com.taoensso/sente "1.3.0"]
                 [org.clojure/core.match "0.2.1"]
                 [http-kit "2.1.19"]
                 [compojure "1.3.1"]
                 [mvxcvi/puget "0.7.0"]
                 [ring "1.3.2"]
                 [ring/ring-defaults "0.1.4"]
                 [clj-time "0.9.0"]
                 [org.clojure/clojurescript "0.0-2850"]
                 [reagent "0.5.0-alpha3"]
                 [com.stuartsierra/component "0.2.2"]]

  :source-paths ["src/clj/"]

  :plugins [[lein-cljsbuild "1.0.4"]]

  :cljsbuild {:builds [{:id "release"
                        :source-paths ["src/cljs"]
                        :compiler {:output-to "resources/public/inspect/js/build/inspect-opt.js"
                                   :optimizations :advanced
                                   :preamble ["reagent/react.min.js"]
                                   :externs ["externs/misc.js"]}}]})
