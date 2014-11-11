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

(def clients (atom {}))
(def client-maps (atom {}))
(def known-event-types (atom #{}))

(defn strip-uid
  "for some reason, the :client-uuid is longer than expected. This strips the
   uuid string to the expected length as used elsewhere"
  [client-uuid]
  (subs client-uuid 0 36))

(defn get-next
  "get next items for the specified event types"
  [params full-event]
  (let [next-n (:next-n params)]
    (swap! clients update-in [(strip-uid (:client-uuid full-event))] (fn [n] (if n (+ n next-n) next-n)))))

(defn get-next-items
  "start listener for all event types, limited to the next n for each"
  [client-map full-event]
  (let [uid (strip-uid (:client-uuid full-event))]
    (swap! client-maps assoc-in [uid] client-map)))

(defn initialize-inspector
  "start listener for all event types, limited to the next n for each"
  [params full-event]
  (let [uid (strip-uid (:client-uuid full-event))
        n (:n params)
        client-map (into {} (map (fn [t] [t n]) (seq @known-event-types)))]
    (swap! client-maps assoc-in [uid] client-map)))

(defn send-event-types
  "send set with known event types to connected UIs"
  [uids chsk-send!]
  (doseq [uid (:any @uids)]
    (chsk-send! uid [:info/known-event-types @known-event-types])))

(defn- make-handler
  "create event handler function for the websocket connection"
  [inspect-fn chsk-send! uids]
  (fn [full-event]
    (let [event (:event full-event)]
      (inspect-fn :ws/event-in full-event)
      (match event
             [:cmd/get-next params]       (get-next params full-event)
             [:cmd/get-next-items params] (get-next-items params full-event)
             [:cmd/initialize params]     (initialize-inspector params full-event)
             [:cmd/get-event-types]       (send-event-types uids chsk-send!)
             [:chsk/ws-ping]              () ; currently just do nothing with ping (no logging either)
             :else                        (log/debug "Unmatched event:" (pp/pprint event))))))

(defn send-loop
  "run loop, call chsk-send! with message on channel"
  [channel uids chsk-send!]
  (go-loop [] (let [msg (<! channel)
                    origin (:origin msg)]
                (when-not (= msg :stop-loop)
                  (when-not (contains? @known-event-types origin)
                    (swap! known-event-types conj origin)
                    (send-event-types uids chsk-send!))
                  (doseq [uid (:any @uids)]
                    (when (pos? (get-in @client-maps [uid origin] 0))
                      (chsk-send! uid [:info/msg (assoc msg :payload (with-out-str (pp/pprint (:payload msg))))])
                      (swap! client-maps update-in [uid origin] dec)))
                  (recur)))))

(defrecord Matcher [inspect-chan inspect-fn chsk-router]
  component/Lifecycle
  (start [component] (log/info "Starting Communicator Component")
         (let [{:keys [ch-recv send-fn ajax-post-fn ajax-get-or-ws-handshake-fn connected-uids]}
               (sente/make-channel-socket! {:user-id-fn user-id-fn})
               event-handler (make-handler inspect-fn send-fn connected-uids)
               chsk-router (sente/start-chsk-router! ch-recv event-handler)]
           (send-loop inspect-chan connected-uids send-fn)
           (assoc component
             :ajax-post-fn ajax-post-fn
             :ajax-get-or-ws-handshake-fn ajax-get-or-ws-handshake-fn
             :chsk-router chsk-router)))

  (stop [component] (log/info "Stopping Communicator Component")
        (chsk-router) ;; stops router loop
        (put! inspect-chan :stop-loop)
        (assoc component
          :chsk-router nil
          :ajax-post-fn nil
          :ajax-get-or-ws-handshake-fn nil)))

(defn new-matcher [inspect-chan inspect-fn] (map->Matcher {:inspect-chan inspect-chan
                                                           :inspect-fn inspect-fn}))
