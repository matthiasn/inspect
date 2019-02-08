(ns inspect.main.sled
  (:require [taoensso.timbre :refer-macros [info error debug]]
            [clojure.pprint :as pp]
    ;[neon-sled]
            [cognitect.transit :as t]
            [fs :refer [existsSync unlinkSync]]))

(def DB_PATH "/tmp/inspect.db")

(def r (t/reader :json))
(def w (t/writer :json))

#_(defn state-fn [put-fn]
    (when (existsSync DB_PATH) (unlinkSync DB_PATH))
    (let [sled (neon-sled DB_PATH)
          state (atom {:db sled})]
      ;(.syncAndClose sled)
      {:state state}))

#_(defn get-msg [{:keys [current-state msg-payload]}]
    (let [sled (:db current-state)
          {:keys [k]} msg-payload
          res (t/read r (.get sled (str k)))]
      (debug :get-msg msg-payload)
      {:emit-msg [:sled/res {:k k :v res}]}))

#_(defn put-msg [{:keys [current-state msg-payload]}]
    (let [sled (:db current-state)
          {:keys [k v]} msg-payload]
      (.set sled (str k) (t/write w v))
      (debug :put-msg msg-payload)
      {:emit-msg [:sled/res {:k k :status :saved}]}))

(defn state-fn [put-fn]
  (let [state (atom {})]
    {:state state}))

(defn get-msg [{:keys [current-state msg-payload]}]
  (let [k (str (:k msg-payload))
        res (get current-state k)]
    (info "retrieved" k res)
    {:emit-msg [:sled/res {:k k :v res}]}))

(defn put-msg [{:keys [current-state msg-payload]}]
  (let [{:keys [k v]} msg-payload
        new-state (assoc current-state k v)]
    {:new-state new-state
     :emit-msg  [:sled/res {:k k :status :saved}]}))

(defn cmp-map [cmp-id]
  {:cmp-id      cmp-id
   :state-fn    state-fn
   :handler-map {:sled/get get-msg
                 :sled/put put-msg}})
