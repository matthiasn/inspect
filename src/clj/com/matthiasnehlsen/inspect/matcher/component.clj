(ns com.matthiasnehlsen.inspect.matcher.component
  (:gen-class)
  (:import java.io.StringWriter)
  (:require
   [clojure.pprint :as pp]
   [clojure.tools.logging :as log]
   [clojure.core.match :as match :refer (match)]
   [taoensso.sente :as sente]
   [com.stuartsierra.component :as component]
   [clojure.core.async :as async :refer [chan <! put! go-loop]]))

(defn user-id-fn
  "generates unique ID for request"
  [req]
  (let [uid (str (java.util.UUID/randomUUID))]
    (log/info "Connected:" (:remote-addr req) uid)
    uid))

(defn make-handler
  "create event handler function for the websocket connection"
  []
  (fn [{event :event}]
    (match event
           [:cmd/next params] ()#_(put! register-percolation-chan params)
           [:chsk/ws-ping]         () ; currently just do nothing with ping (no logging either)
           :else                   (log/debug "Unmatched event:" (pp/pprint event)))))

(defn send-loop
  "run loop, call f with message on channel"
  [channel uids chsk-send!]
  (go-loop [] (let [msg (<! channel)]
                (when-not (= msg :stop-loop)
                  (doseq [uid (:any @uids)]
                    (chsk-send! uid [:info/msg {:pp (with-out-str (pp/pprint msg))}]))
                  (recur)))))

(defrecord Matcher [inspect-chan chsk-router]
  component/Lifecycle
  (start [component] (log/info "Starting Communicator Component")
         (let [{:keys [ch-recv send-fn ajax-post-fn ajax-get-or-ws-handshake-fn connected-uids]}
               (sente/make-channel-socket! {:user-id-fn user-id-fn})
               event-handler (make-handler)
               chsk-router (sente/start-chsk-router! ch-recv event-handler)]
           (send-loop inspect-chan connected-uids send-fn)
           (assoc component :ajax-post-fn ajax-post-fn
             :ajax-get-or-ws-handshake-fn ajax-get-or-ws-handshake-fn
             :chsk-router chsk-router)))

  (stop [component] (log/info "Stopping Communicator Component")
        (chsk-router) ;; stops router loop
        (put! inspect-chan :stop-loop)
        (assoc component :chsk-router nil :ajax-post-fn nil :ajax-get-or-ws-handshake-fn nil)))

(defn new-matcher [inspect-chan] (map->Matcher {:inspect-chan inspect-chan}))
