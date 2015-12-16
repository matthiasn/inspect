(ns matthiasn.inspect-probe.kafka-producer
  (:gen-class)
  (:require
    [clojure.tools.logging :as log]
    [clj-kafka.new.producer :as kp]
    [taoensso.nippy :as nippy]))

(defn kafka-producer-state-fn
  "Returns function for making state while using provided configuration."
  [put-fn]
  (let [prod (kp/producer {"bootstrap.servers" "127.0.0.1:9092"
                           "client-id"         "producer-cmp"}
                          (kp/byte-array-serializer)
                          (kp/byte-array-serializer))]
    {:state (atom {:producer prod})}))

(defn probe-msg-handler
  [{:keys [cmp-state msg-payload]}]
  (binding [nippy/*final-freeze-fallback* nippy/freeze-fallback-as-str]
    (let [prod (:producer @cmp-state)
          frozen (nippy/freeze msg-payload)]
      (log/info msg-payload)
      (kp/send prod
               (kp/record "inspect-probe-events" frozen)
               (fn [m err]
                 (when err
                   (log/info "producer future err:" err m)))))))

(defn cmp-map
  "Create Kafka producer component, which sends serialized message on the 'inspect-probe' topic."
  [cmp-id]
  {:cmp-id      cmp-id
   :state-fn    kafka-producer-state-fn
   :handler-map {:inspect/probe probe-msg-handler}
   :opts        {:in-chan               [:sliding 10000]
                 :msgs-on-firehose      false
                 :snapshots-on-firehose false}})
