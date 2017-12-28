(ns inspect.main.sled
  (:require [taoensso.timbre :refer-macros [info error debug]]
            [clojure.pprint :as pp]
            [neon-sled]
            [cognitect.transit :as t]
            [fs :refer [existsSync unlinkSync]]))

(def DB_PATH "/tmp/inspect.db")

(def r (t/reader :json))
(def w (t/writer :json))

(defn state-fn [put-fn]
  (when (existsSync DB_PATH) (unlinkSync DB_PATH))
  (let [sled (neon-sled DB_PATH)
        state (atom {:db sled})]
    ;(.syncAndClose sled)
    {:state state}))

(defn get-msg [{:keys [current-state put-fn msg-payload]}]
  (let [sled (:db current-state)
        {:keys [k]} msg-payload
        res (t/read r (.get sled (str k)))]
    (debug :get-msg msg-payload)
    {:emit-msg [:sled/res {:k k :v res}]}))

(defn put-msg [{:keys [current-state put-fn msg-payload]}]
  (let [sled (:db current-state)
        {:keys [k v]} msg-payload]
    (.set sled (str k) (t/write w v))
    (debug :put-msg msg-payload)
    {:emit-msg [:sled/res {:k k :status :saved}]}))

(defn db-bench [{:keys [current-state put-fn msg-payload]}]
  (let [sled (:db current-state)
        sum (atom 0)
        from-db (atom 0)]
    (info "Writing 100,000 values into sled")
    (time (dotimes [n 100000]
            (swap! sum + n)
            (.set sled (str n) (t/write w {:n n}))))
    (info "Finished writing 100,000 values, check sum" @sum)
    (time (dotimes [n 100000]
            (let [res (t/read r (.get sled (str n)))]
              (swap! from-db + (:n res)))))
    (info "Finished reading 1,000,000 messages, check sum" @from-db)
    {}))

(defn cmp-map [cmp-id]
  {:cmp-id      cmp-id
   :state-fn    state-fn
   :handler-map {:sled/get  get-msg
                 :sled/put  put-msg
                 :sled/bench db-bench}})
