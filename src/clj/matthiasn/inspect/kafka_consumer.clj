(ns matthiasn.inspect.kafka-consumer
  (:gen-class)
  (:require
    [clojure.tools.logging :as log]
    [fipp.clojure :as fipp]
    [clj-kafka.consumer.zk :as kcz]
    [clj-kafka.admin :as admin]
    [taoensso.nippy :as nippy]
    [clojure.pprint :as pp]))

(def broker-config
  {:zookeeper-port 2181
   :kafka-port     9092
   :topic          "inspect-probe-events"})

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
        (future
          (binding [nippy/*final-freeze-fallback* nippy/freeze-fallback-as-str]
            (let [messages (kcz/messages c (:topic broker-config))]
              (doseq [msg messages]
                (let [thawed (nippy/thaw (:value msg))
                      pprinted (merge thawed
                                      {:args         (with-out-str (fipp/pprint (:args thawed)))
                                       :return-value (with-out-str (fipp/pprint (:return-value thawed)))
                                       :datetime     (str (:datetime thawed))})]
                  (put-fn [:inspect/probe pprinted]))))))
        {:state (atom {:consumer c})}))))

(defn cmp-map
  "Create Kafka consumer component, which receives serialized message on the 'inspect-probe' topic
  and puts them on the out-channel."
  [cmp-id]
  {:cmp-id   cmp-id
   :state-fn kafka-consumer-state-fn})
