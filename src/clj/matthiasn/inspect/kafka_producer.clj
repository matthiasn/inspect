(ns matthiasn.inspect.kafka-producer
  (:gen-class)
  (:require
    [clojure.tools.logging :as log]
    [matthiasn.inspect.core :as i]
    [clj-kafka.new.producer :as kp]
    [clojure.pprint :as pp]))

(defn kafka-producer-state-fn
  "Returns function for making state while using provided configuration."
  [put-fn]
  (let [prod (kp/producer {"bootstrap.servers" "127.0.0.1:9092"
                           "client-id"         "producer-cmp"}
                          (kp/byte-array-serializer)
                          (kp/byte-array-serializer))]
    {:state (atom {:producer prod})}))

(defn args-handler
  "Handle incoming messages: process / add to application state."
  [{:keys [cmp-state msg]}]
  (let [prod (:producer @cmp-state)
        pprinted (with-out-str (pp/pprint msg))]
    (kp/send prod
             (kp/record "test-topic" (.getBytes pprinted))
             (fn [m err]
               (log/info "producer future result:" m)
               (log/info "producer future err:" err)))
    (apply i/inspect msg)))

(defn cmp-map
  "Create component for starting percolation in ElasticSearch and delivering matches."
  [cmp-id]
  {:cmp-id      cmp-id
   :state-fn    kafka-producer-state-fn
   :handler-map {:inspect/args       args-handler
                 :inspect/return-val args-handler}})
