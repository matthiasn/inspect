(ns matthiasn.inspect.kafka-producer
  (:gen-class)
  (:require
    [clojure.tools.logging :as log]
    [matthiasn.inspect.core :as i]))

(defn kafka-producer-state-fn
  "Returns function for making state while using provided configuration."
  [conf]
  (fn [put-fn]
    (let []
      {:state (atom {})})))

(defn args-handler
  "Handle incoming messages: process / add to application state."
  [{:keys [cmp-state msg msg-payload]}]
  ;  (swap! cmp-state assoc-in [:connected-uids] msg-payload)
  (log/info msg)
  (apply i/inspect msg))

(defn cmp-map
  "Create component for starting percolation in ElasticSearch and delivering matches."
  [cmp-id conf] {:cmp-id            cmp-id
                 :state-fn          (kafka-producer-state-fn conf)
                 :handler-map       {:inspect/args        args-handler
                                     :inspect/return-val  args-handler}})
