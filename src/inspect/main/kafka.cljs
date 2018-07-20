(ns inspect.main.kafka
  (:require-macros [cljs.core.async.macros :refer [go]])
  (:require [taoensso.timbre :refer-macros [info debug warn error]]
            [inspect.main.runtime :as rt]
            [electron :refer [dialog]]
            [always-tail :as Tail]
            [cognitect.transit :as t]
            [cljs.core.async :refer [put! chan <! >! close!]]
            [fs :refer [existsSync readFileSync writeFileSync]]
            [cljs.nodejs :refer [process]]
            [matthiasn.systems-toolbox.component :as stc]
            [clojure.string :as str]
            [cljs.reader :as edn]))

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
  (when-let [tail (:tail current-state)]
    (info "stopping consumer")
    (swap! cmp-state assoc-in [:count] 0)
    (put-fn [:kafka/status {:status :stopped :text "stopped"}])
    (put-fn [:observer/stop])
    (.unwatch tail)))

(def hosts-file (str (:user-data rt/runtime-info) "/kafka-hosts.edn"))

(defn read-known-files []
  (if (existsSync hosts-file)
    (edn/read-string (readFileSync hosts-file "utf-8"))
    #{}))

(defn add-file [kafka-host]
  (let [known-hosts (read-known-files)
        updated (conj known-hosts kafka-host)]
    (writeFileSync hosts-file (pr-str updated) "utf-8")))

(defn get-known-files [_]
  (let [hosts (read-known-files)]
    (info "read known hosts" hosts)
    {:emit-msg [:kafka/hosts hosts]}))

(defn on-line [cmp-state put-chan put-fn]
  (fn [data]
    (try
      (swap! cmp-state update-in [:count] inc)
      (let [cnt (:count @cmp-state)
            parsed (edn/read-string data)
            {:keys [msg-type msg-payload msg-meta]} parsed
            msg (with-meta [msg-type msg-payload] msg-meta)]
        (go (>! put-chan msg))
        (when (zero? (mod cnt 100))
          (let [last-ts (:last-ts @cmp-state)
                now (stc/now)
                duration (- now last-ts)
                per-sec (Math/floor (/ 100 (/ duration 1000)))]
            (swap! cmp-state assoc-in [:last-ts] now)
            (put-fn [:kafka/status
                     {:status :connected
                      :text   (str per-sec " msg/s")}])
            (info "Messages received:" cnt
                  "-" per-sec "msg/s"))))
      (catch :default e (error "Something went wrong" e)))))

(defn start [{:keys [put-fn cmp-state put-chan current-state msg-payload]
              :as   msg-map}]
  (info "Tailing" msg-payload)
  (try
    (stop msg-map)
    (put-fn [:kafka/status {:status :starting
                            :text   (str "attempting to connect to "
                                         msg-payload)}])
    (let [file msg-payload
          tail (new Tail file)]
      (info "Starting firehose file reader" file)
      (.on tail "error" #(error %))
      (.on tail "line" (on-line cmp-state put-chan put-fn))
      (.watch tail)
      (add-file file)
      {:new-state (assoc-in current-state [:tail] tail)})
    (catch :default e (do (error "start fn" e)
                          {:emit-msg [:kafka/status
                                      {:status :error
                                       :text   "Firehose Reader Error"}]}))))

(defn cmp-map [cmp-id]
  {:cmp-id      cmp-id
   :state-fn    state-fn
   :handler-map {:tail/start     start
                 :tail/stop      stop
                 :tail/get-files get-known-files}})
