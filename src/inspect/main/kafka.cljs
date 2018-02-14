(ns inspect.main.kafka
  (:require-macros [cljs.core.async.macros :refer [go]])
  (:require [taoensso.timbre :refer-macros [info debug warn error]]
            [sinek :refer [Consumer]]
            [inspect.main.runtime :as rt]
            [electron :refer [dialog]]
            [cognitect.transit :as t]
            [cljs.core.async :refer [put! chan <! >! close!]]
            [fs :refer [existsSync readFileSync writeFileSync]]
            [cljs.nodejs :refer [process]]
            [matthiasn.systems-toolbox.component :as stc]
            [clojure.string :as str]
            [cljs.tools.reader.edn :as edn]))

(defn config [kafka-host]
  (clj->js {:kafkaHost          kafka-host
            :logger             {:info  #(info %)
                                 :warn  #(warn %)
                                 :debug #(debug %)
                                 :error #(error %)}
            :groupId            (str (stc/make-uuid))
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
                                 :partitionerType      3}}))

(defn state-fn [put-fn]
  (let [state (atom {:count 0 :last-ts (stc/now)})
        err-handler (fn [title content]
                      (error "showErrorBox" title content)
                      (when (str/includes? content "ETIMEDOUT")
                        (put-fn [:kafka/status {:status :error
                                                :text   "connection timeout"}])))]
    (aset dialog "showErrorBox" err-handler)
    {:state state}))

(defn stop [{:keys [put-fn cmp-state current-state]}]
  (when-let [consumer (:consumer current-state)]
    (info "stopping consumer")
    (swap! cmp-state assoc-in [:count] 0)
    (put-fn [:kafka/status {:status :stopped :text "stopped"}])
    (put-fn [:observer/stop])
    (.close consumer)))

(def hosts-file (str (:user-data rt/runtime-info) "/kafka-hosts.edn"))

(defn read-known-hosts []
  (if (existsSync hosts-file)
    (edn/read-string (readFileSync hosts-file "utf-8"))
    #{}))

(defn add-host [kafka-host]
  (let [known-hosts (read-known-hosts)
        updated (conj known-hosts kafka-host)]
    (writeFileSync hosts-file (pr-str updated) "utf-8")))

(defn get-known-hosts [_]
  (let [hosts (read-known-hosts)]
    (info "read known hosts" hosts)
    {:emit-msg [:kafka/hosts hosts]}))

(defn start [{:keys [put-fn cmp-state put-chan current-state msg-payload]
              :as   msg-map}]
  (info "Kafka config" msg-payload)
  (try
    (stop msg-map)
    (put-fn [:kafka/status {:status :starting
                            :text   (str "attempting to connect to "
                                         msg-payload)}])
    (let [kafka-host msg-payload
          kafka-cfg (config kafka-host)
          consumer (Consumer. "firehose" kafka-cfg)
          r (t/reader :json)
          msg-handler (fn [kafka-msg cb]
                        (try
                          (swap! cmp-state update-in [:count] inc)
                          (let [cnt (:count @cmp-state)
                                parsed (t/read r (.-value kafka-msg))
                                {:keys [msg-type msg-payload msg-meta]} parsed
                                msg (with-meta [msg-type msg-payload] msg-meta)]
                            (go (>! put-chan msg)
                                (try
                                  (cb nil)
                                  (catch :default e (error "Error on callback" e))))
                            (when (zero? (mod cnt 100))
                              (let [last-ts (:last-ts @cmp-state)
                                    now (stc/now)
                                    duration (- now last-ts)
                                    per-sec (Math/floor (/ 100 (/ duration 1000)))]
                                (swap! cmp-state assoc-in [:last-ts] now)
                                (put-fn [:kafka/status
                                         {:status :connected
                                          :text   (str per-sec " msg/s")}])
                                (info "KAFKA received:" cnt
                                      "-" per-sec "msg/s"
                                      "- Offset" (.-offset kafka-msg)))))
                          (catch :default e (error "Something went wrong" e))))]
      (add-host kafka-host)
      (-> consumer
          (.connect true)
          (.then (fn [_]
                   (put-fn [:kafka/status
                            {:status :connected
                             :text   (str "connected to " kafka-host)}])
                   (try
                     (.consume consumer msg-handler)
                     (catch :default e (error "promise then" e)))))
          (.catch #(let [err (str (.-message %))]
                     (error "failed promise" err)
                     (put-fn [:kafka/status {:status :error :text err}]))))
      (info "Starting KAFKA consumer")
      {:new-state (assoc-in current-state [:consumer] consumer)})
    (catch :default e (let []
                        (error "start fn" e)
                        {:emit-msg [:kafka/status {:status :error
                                                   :text   "KAFKA Error"}]}))))

(defn cmp-map [cmp-id]
  {:cmp-id      cmp-id
   :state-fn    state-fn
   :handler-map {:kafka/start     start
                 :kafka/stop      stop
                 :kafka/get-hosts get-known-hosts}})
