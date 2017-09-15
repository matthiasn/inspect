(ns inspect.view.ipc
  (:require [electron :refer [ipcRenderer]]
            [taoensso.timbre :as timbre :refer-macros [info debug]]
            [cljs.reader :refer [read-string]]))

(defn state-fn [put-fn]
  (let [state (atom {})
        relay (fn [ev m]
                (let [parsed (read-string m)
                      msg-type (first parsed)
                      {:keys [msg-payload msg-meta]} (second parsed)
                      msg (with-meta [msg-type msg-payload] msg-meta)]
                  (debug "IPC received:" (str msg-type))
                  (put-fn msg)))
        id-handler (fn [ev window-id]
                     (info "window-id" window-id)
                     (swap! state assoc-in [:window-id] window-id))]
    (.on ipcRenderer "relay" relay)
    (.on ipcRenderer "window-id" id-handler)
    (info "Starting IPC Component")
    {:state state}))

(defn relay-msg [{:keys [current-state msg-type msg-meta msg-payload]}]
  (let [msg-meta (assoc-in msg-meta [:window-id] (:window-id current-state))
        serializable [msg-type {:msg-payload msg-payload
                                :msg-meta    msg-meta}]]
    (debug "Relay to MAIN:" (str msg-type) (str msg-payload))
    (.send ipcRenderer "relay" (pr-str serializable)))
  {})

(defn cmp-map [cmp-id relay-types]
  {:cmp-id      cmp-id
   :state-fn    state-fn
   :handler-map (zipmap relay-types (repeat relay-msg))})
