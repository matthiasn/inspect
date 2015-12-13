(ns matthiasn.inspect.kafka-consumer
  (:gen-class)
  (:require
    [clojure.tools.logging :as log]
    [matthiasn.inspect.core :as i]
    [clj-kafka.consumer.zk :as kcz]
    [clj-kafka.admin :as admin]
    [taoensso.nippy :as nippy]))

(def broker-config
  {:zookeeper-port 2181
   :kafka-port     9092
   :topic          "inspect-probe"})

(def config
  {"zookeeper.connect"  "localhost:2181"
   "group.id"           "clj-kafka.consumer"
   "port"               "9092"
   "auto.offset.reset"  "smallest"
   "auto.commit.enable" "true"})

(defn kafka-consumer-state-fn
  "Returns function for making state while using provided configuration."
  [put-fn]
  (let [topic (:topic broker-config)]
    (let [zk-client (admin/zk-client (str "127.0.0.1:" (:zookeeper-port broker-config))
                                     {:session-timeout-ms    500
                                      :connection-timeout-ms 500})]
      (when-not (admin/topic-exists? zk-client topic)
        (admin/create-topic zk-client topic))
      (let [c (kcz/consumer config)]
        (future (let [messages (kcz/messages c (:topic broker-config))]
                  (doseq [msg messages]
                    (let [thawed (nippy/thaw (:value msg))]
                      ;(log/info "kafka-consumer-state-fn" thawed)
                      (apply i/inspect thawed)))))
        {:state (atom {:consumer c})}))))

(defn args-handler
  "Handle incoming messages: process / add to application state."
  [{:keys [msg]}]
  (log/info msg)
  (apply i/inspect msg))

(defn cmp-map
  "Create Kafka consumer component, which receives serialized message on the 'inspect-probe' topic
  and puts them on the out-channel."
  [cmp-id]
  {:cmp-id      cmp-id
   :state-fn    kafka-consumer-state-fn
   :handler-map {:inspect/args       args-handler
                 :inspect/return-val args-handler}})
