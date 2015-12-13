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

(def test-broker-config {:zookeeper-port 2182
                         :kafka-port     9999
                         :topic          "test-topic"})

(def config {"zookeeper.connect" "localhost:2182"
             "group.id" "clj-kafka.consumer"
             "port" "9999"
             "auto.offset.reset" "smallest"
             "auto.commit.enable" "false"})

(def xform (comp (map pp/pprint)))

(defn kafka-producer-state-fn
  "Returns function for making state while using provided configuration."
  [conf]
  (fn [put-fn]
    ;(ku/delete-tmp-dir)
    (let [zk (ku/create-zookeeper test-broker-config)
          kafka (ku/create-broker test-broker-config)
          topic (:topic test-broker-config)]
      (.startup kafka)
      (let [zk-client (admin/zk-client (str "127.0.0.1:" (:zookeeper-port test-broker-config))
                                        {:session-timeout-ms    500
                                         :connection-timeout-ms 500})]
        (Thread/sleep 3000)
        (when-not (admin/topic-exists? zk-client topic)
          (admin/create-topic zk-client topic)
          (ku/wait-until-initialised kafka topic))
        (let [c (kcz/consumer config)
              stream (kcz/create-message-stream c "test-topic")]
          (future (eduction xform stream))
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
                 :state-fn    (kafka-producer-state-fn conf)
                 :handler-map {:inspect/args       args-handler
                               :inspect/return-val args-handler}})
