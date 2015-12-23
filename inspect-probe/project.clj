(defproject matthiasn/inspect-probe "0.2.1"
  :description "Log to a web application to inspect what's going on in your application"
  :url "https://github.com/matthiasn/inspect"
  :license {:name "Eclipse Public License"
            :url "http://www.eclipse.org/legal/epl-v10.html"}
  :dependencies [[org.clojure/clojure "1.7.0"]
                 [matthiasn/systems-toolbox "0.4.8"]
                 [clj-kafka "0.3.4"]
                 [com.taoensso/nippy "2.11.0-beta1"]
                 [fipp "0.6.3"]
                 [clj-time "0.11.0"]]

  :source-paths ["src/clj/"]

  :plugins [[codox "0.8.10"]])
