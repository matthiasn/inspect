(ns com.matthiasnehlsen.inspect.matcher
  (:gen-class)
  (:require
   [clojure.pprint :as pp]
   [clojure.tools.logging :as log]
   [clojure.core.match :as match :refer (match)]
   [taoensso.sente :as sente]
   [com.stuartsierra.component :as component]
   [clojure.core.async :as async :refer [chan <! put! go-loop]]))

(defn- user-id-fn
  "generates unique ID for request"
  [req]
  (let [uid (str (java.util.UUID/randomUUID))]
    (log/info "Connected:" (:remote-addr req) uid)
    uid))

(def app (atom {:clients {}}))

(defn get-next [params full-event]
  (let [next-n (:next-n params)]
  (swap! app update-in [:clients (subs (:client-uuid full-event) 0 36)] (fn [n] (if n (+ n next-n) next-n)))
    (log/info app)))

(defn- make-handler
  "create event handler function for the websocket connection"
  [inspect-fn]
  (fn [full-event]
    (let [event (:event full-event)]
      (inspect-fn :ws/event-in full-event)
      (match event
             [:cmd/get-next params] (get-next params full-event)
             [:chsk/ws-ping]    () ; currently just do nothing with ping (no logging either)
             :else              (log/debug "Unmatched event:" (pp/pprint event))))))

(defn send-loop
  "run loop, call f with message on channel"
  [channel uids chsk-send!]
  (go-loop [] (let [msg (<! channel)]
                (when-not (= msg :stop-loop)
                  (doseq [uid (:any @uids)]
                    (when (pos? (get (:clients @app) uid 0))
                      (chsk-send! uid [:info/msg (assoc msg :payload (with-out-str (pp/pprint (:payload msg))))])
                      (swap! app update-in [:clients uid] dec)))
                  (recur)))))

(defrecord Matcher [inspect-chan inspect-fn chsk-router]
  component/Lifecycle
  (start [component] (log/info "Starting Communicator Component")
         (let [{:keys [ch-recv send-fn ajax-post-fn ajax-get-or-ws-handshake-fn connected-uids]}
               (sente/make-channel-socket! {:user-id-fn user-id-fn})
               event-handler (make-handler inspect-fn)
               chsk-router (sente/start-chsk-router! ch-recv event-handler)]
           (send-loop inspect-chan connected-uids send-fn)
           (assoc component :ajax-post-fn ajax-post-fn
             :ajax-get-or-ws-handshake-fn ajax-get-or-ws-handshake-fn
             :chsk-router chsk-router)))

  (stop [component] (log/info "Stopping Communicator Component")
        (chsk-router) ;; stops router loop
        (put! inspect-chan :stop-loop)
        (assoc component :chsk-router nil :ajax-post-fn nil :ajax-get-or-ws-handshake-fn nil)))

(defn new-matcher [inspect-chan inspect-fn] (map->Matcher {:inspect-chan inspect-chan
                                                           :inspect-fn inspect-fn}))
