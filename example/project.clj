(defproject example "0.1.6-SNAPSHOT"
  :description "Example Application for Inspect Library"
  :url "https://github.com/matthiasn/inspectE"
  :license {:name "Eclipse Public License"
            :url "http://www.eclipse.org/legal/epl-v10.html"}
  :dependencies [[org.clojure/clojure "1.6.0"]
                 [com.matthiasnehlsen/inspect "0.1.6-SNAPSHOT"]
                 [clj-pid "0.1.1"]
                 [org.clojure/core.async "0.1.346.0-17112a-alpha"]]

  :main example.core
  :aot [example.core])
