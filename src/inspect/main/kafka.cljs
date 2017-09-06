(ns inspect.main.kafka
  (:require-macros [cljs.core.async.macros :refer [go]])
  (:require [taoensso.timbre :as timbre :refer-macros [info debug warn error]]
            [cljs.reader :refer [read-string]]
            [cljs.spec.alpha :as s]
            [sinek :refer [Consumer]]
            [cognitect.transit :as t]
            [cljs.core.async :as async :refer [put! chan <! >! close!]]
            [clojure.pprint :as pp]
            [cljs.nodejs :as nodejs :refer [process]]
            [matthiasn.systems-toolbox.component :as stc]))

(def config
  {:kafkaHost          (or (aget process "env" "KAFKA_HOST") "localhost:9092")
   :logger             {:info  #(info %)
                        :warn  #(warn %)
                        :debug #(debug %)
                        :error #(error %)}
   :groupId            (stc/make-uuid)
   :clientName         (str "client-name-" (stc/now))
   :workerPerPartition 1
   :options            {:sessionTimeout       8000
                        :protocol             ["roundrobin"]
                        :fromOffset           "latest"
                        :fetchMaxBytes        (* 16 1024 1024)
                        :fetchMinBytes        1
                        :fetchMaxWaitMs       10
                        :heartbeatInterval    250
                        :retryMinTimeout      250
                        :autoCommit           true
                        :autoCommitIntervalMs 1000
                        :requireAcks          0
                        :ackTimeoutMs         100
                        :partitionerType      3}})

(s/def :subscription/subscribe string?)

(defn state-fn
  [put-fn]
  (let [state (atom {:count 0 :last-ts (stc/now)})]
    {:state state}))

(defn start
  [{:keys [put-fn cmp-state put-chan current-state]}]
  (info "Kafka config" config)
  (let [consumer (Consumer. "firehose" (clj->js config))
        msg-handler (fn [kafka-msg cb]
                      (swap! cmp-state update-in [:count] inc)
                      (let [cnt (:count @cmp-state)
                            r (t/reader :json)
                            parsed (t/read r (.-value kafka-msg))
                            {:keys [msg-type msg-payload msg-meta]} parsed
                            msg (with-meta [msg-type msg-payload] msg-meta)]
                        (go (>! put-chan msg)
                            (cb nil))
                        (when (zero? (mod cnt 100))
                          (let [last-ts (:last-ts @cmp-state)
                                now (stc/now)
                                duration (- now last-ts)
                                per-sec (Math/floor (/ 100 (/ duration 1000)))]
                            (swap! cmp-state assoc-in [:last-ts] now)
                            (info "KAFKA received:" cnt
                                  "-" per-sec "msg/s"
                                  "- Offset" (.-offset kafka-msg))))))]
    (-> consumer
        (.connect true)
        (.then (fn [_] (.consume consumer msg-handler))))
    (info "Starting KAFKA component with back-pressure")
    {}))

(defn cmp-map
  [cmp-id]
  {:cmp-id      cmp-id
   :state-fn    state-fn
   :handler-map {:cmd/start start}})
