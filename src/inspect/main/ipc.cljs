(ns inspect.main.ipc
  (:require [clojure.string :as str]
            [taoensso.timbre :as timbre :refer-macros [info debug]]
            [electron :refer [ipcMain]]
            [cljs.reader :refer [read-string]]
            [clojure.pprint :as pp]))

(defn state-fn [put-fn]
  (let [state (atom {})
        relay (fn [ev m]
                (let [parsed (read-string m)
                      msg-type (first parsed)
                      {:keys [msg-payload msg-meta]} (second parsed)
                      msg (with-meta [msg-type msg-payload] msg-meta)]
                  (info "IPC relay:" (with-out-str (pp/pprint msg)))
                  (put-fn msg)))]
    (.on ipcMain "relay" relay)
    {:state state}))

(defn cmp-map [cmp-id]
  {:cmp-id   cmp-id
   :state-fn state-fn})
