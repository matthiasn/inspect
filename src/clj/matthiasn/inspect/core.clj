(ns matthiasn.inspect.core
  (:gen-class)
  (:require
    [matthiasn.inspect.matcher :as matcher]
    [matthiasn.inspect.http :as http]
    [com.stuartsierra.component :as component]
    [clj-time.core :as t]
    [clj-time.format :as f]
    [clojure.core.async :refer [<! chan put! mult tap pub sub timeout go-loop sliding-buffer]]
    [clojure.pprint :as pp]))

;; in-chan is multiplied into event-mult. That way, the matcher component can attach on start and detach on stop.
;; With no channel tapped into the data, the messages are simply dropped.
(defonce in-chan (chan (sliding-buffer 10000)))
(defonce event-mult (mult in-chan))

(def built-in-formatter (f/formatters :date-time)) ; used for timestamping the inspected messages

(defn inspect
  "Send message to inspect sub-system with msg-type. Only does anything when system active."
  [msg-type msg]
  (put! in-chan [:event {:origin msg-type :received (f/unparse built-in-formatter (t/now)) :payload msg}]))

(defn get-system
  "Create system by wiring individual components so that component/start
   will bring up the individual components in the correct order."
  [conf]
  (component/system-map
   :matcher (matcher/new-matcher event-mult inspect)
   :http    (component/using (http/new-http-server conf) {:matcher :matcher})))

(def port (get (System/getenv) "INSPECT_PORT" "8000"))

(def default-conf {:port      (. Integer parseInt port)
                   :title     "inspect"
                   :header    "inspect"
                   :subheader "println no more"})

;; system with default configuration
(def system (atom (get-system default-conf)))

(defn configure
  "Partially or complety override system configuratio with the provided
   configuration map."
  [conf]
  (reset! system (get-system (merge default-conf conf))))

(defn start!
  "Start the inspect system."
  []
  (swap! system component/start))

(defn stop!
  "Stop the inspect system."
  []
  (swap! system (fn [s] (when s (component/stop s)))))

(defonce started (start!))
