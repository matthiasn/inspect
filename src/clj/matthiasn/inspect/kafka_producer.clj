(ns matthiasn.inspect.kafka-producer
  (:gen-class)
  (:require
    [clojure.tools.logging :as log]
    [matthiasn.inspect.core :as i]
    [clj-kafka.new.producer :as kp]
    [clojure.pprint :as pp]))

(defn kafka-producer-state-fn
  "Returns function for making state while using provided configuration."
  [conf]
  (fn [put-fn]
    (let [prod (kp/producer {"bootstrap.servers" "127.0.0.1:9999"}
                            (kp/byte-array-serializer)
                            (kp/byte-array-serializer))]
      (Thread/sleep 3000)
      {:state (atom {:producer prod})})))

(defn args-handler
  "Handle incoming messages: process / add to application state."
  [{:keys [cmp-state msg msg-payload]}]
  ;  (swap! cmp-state assoc-in [:connected-uids] msg-payload)
  (let [prod (:producer @cmp-state)
        pprinted (with-out-str (pp/pprint msg))
        send-future (kp/send prod
                             (kp/record "test-topic" (.getBytes pprinted))
                             (fn [m err] (pp/pprint m) (pp/pprint err)))]
    ;(log/info pprinted)
    (apply i/inspect msg)))

(defn cmp-map
  "Create component for starting percolation in ElasticSearch and delivering matches."
  [cmp-id conf] {:cmp-id      cmp-id
                 :state-fn    (kafka-producer-state-fn conf)
                 :handler-map {:inspect/args       args-handler
                               :inspect/return-val args-handler}})
