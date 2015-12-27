(ns matthiasn.inspect-probe.probe-all
  (:gen-class)
  (:require [matthiasn.inspect-probe.probe :as inspect]))

(alter-var-root #'clojure.core/defn (constantly #'inspect/defn))
