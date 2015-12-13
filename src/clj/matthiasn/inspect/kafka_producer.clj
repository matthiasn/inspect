(ns matthiasn.inspect.kafka-producer
  (:gen-class)
  (:require
    [clojure.tools.logging :as log]
    [matthiasn.inspect.core :as i]
    [clj-kafka.new.producer :as kp]
    [taoensso.nippy :as nippy]
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
  [{:keys [cmp-state msg-type msg-payload]}]
  (let [prod (:producer @cmp-state)
        pprinted (with-out-str (pp/pprint msg-payload))
        frozen (nippy/freeze [msg-type pprinted])]
    (kp/send prod
             (kp/record "inspect-probe" frozen)
             (fn [m err]
               (when err
                 (log/info "producer future err:" err m))))))

(defn cmp-map
  "Create Kafka producer component, which sends serialized message on the 'inspect-probe' topic."
  [cmp-id]
  {:cmp-id      cmp-id
   :state-fn    kafka-producer-state-fn
   :handler-map {:inspect/args       args-handler
                 :inspect/return-val args-handler}})
