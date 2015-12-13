(ns matthiasn.inspect.kafka-consumer
  (:gen-class)
  (:require
    [clojure.tools.logging :as log]
    [matthiasn.inspect.core :as i]
    [clj-kafka.consumer.simple :as kcs]
    [clj-kafka.consumer.zk :as kcz]
    [matthiasn.inspect.clj-kafka-test-utils :as ku]
    [clojure.pprint :as pp]
    [clj-kafka.admin :as admin])
  (:import [org.apache.commons.io FileUtils]))

(def test-broker-config {:zookeeper-port 2181
                         :kafka-port     9092
                         :topic          "test-topic"})

(def config {"zookeeper.connect" "localhost:2181"
             "group.id" "clj-kafka.consumer"
             "port" "9092"
             "auto.offset.reset" "smallest"
             "auto.commit.enable" "false"})

(def xform (comp (map pp/pprint)))

(defn kafka-consumer-state-fn
  "Returns function for making state while using provided configuration."
  [conf]
  (fn [put-fn]
    ;    (ku/delete-tmp-dir)
    (let [                                                  ;zk (ku/create-zookeeper test-broker-config)
          ;kafka (ku/create-broker test-broker-config)
          topic (:topic test-broker-config)
          ]
      ;(.startup kafka)
      ;(Thread/sleep 5000)
      (let [zk-client (admin/zk-client (str "127.0.0.1:" (:zookeeper-port test-broker-config))
                                        {:session-timeout-ms    500
                                         :connection-timeout-ms 500})]
        (Thread/sleep 5000)
        (when-not (admin/topic-exists? zk-client topic)
          (admin/create-topic zk-client topic)
          ;(ku/wait-until-initialised kafka topic)
          )
        (let [c (kcz/consumer config)
              ;; stream (kcz/create-message-stream c "test-topic")
              ;messages (kcz/messages c "test-topic")
              ]
          ;(future (take 10 (map pp/pprint messages)))
          (future (let [messages (kcz/messages c "test-topic")]
                    (log/info (take 10 messages))
                    (doseq [msg messages]
                      (log/info "kafka-consumer-state-fn" (String. (:value msg)) (:value msg)))))
          {:state (atom {:consumer c})})))))

(defn args-handler
  "Handle incoming messages: process / add to application state."
  [{:keys [cmp-state msg msg-payload]}]
  ;  (swap! cmp-state assoc-in [:connected-uids] msg-payload)
  (log/info msg)
  (apply i/inspect msg))

(defn cmp-map
  "Create component for starting percolation in ElasticSearch and delivering matches."
  [cmp-id conf] {:cmp-id      cmp-id
                 :state-fn    (kafka-consumer-state-fn conf)
                 :handler-map {:inspect/args       args-handler
                               :inspect/return-val args-handler}})
