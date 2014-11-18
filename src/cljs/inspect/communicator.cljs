(ns inspect.communicator
  (:require-macros [cljs.core.match.macros :refer (match)]
                   [cljs.core.async.macros :refer [go-loop go]])
  (:require [inspect.appstate :as appstate]
            [cljs.core.match]
            [taoensso.sente  :as sente :refer (cb-success?)]
            [cljs.core.async :as async :refer [<! >! chan put! alts! timeout]]))

(enable-console-print!)

(let [{:keys [chsk ch-recv send-fn state]} (sente/make-channel-socket! "/chsk")]
  (def chsk chsk)
  (def ch-chsk ch-recv)    ; ChannelSocket's receive channel
  (def chsk-send! send-fn) ; ChannelSocket's send API fn
  (def chsk-state state))  ; Watchable, read-only atom

(defn- receive-stats
  [stats]
  (when (empty? @appstate/stats)
    (reset! appstate/selected (into #{} (map (fn [[t _]] t) (seq stats)))))
  (reset! appstate/stats stats))

(defn- event-handler [{:keys [event]}]
  (match event
         [:chsk/recv payload]
         (match payload
                [:info/msg msg] (swap! appstate/app assoc :events (conj (:events @appstate/app) msg))
                [:info/stats stats] (receive-stats stats)
                [:info/client-map client-map] (reset! appstate/client-map client-map)
                :else (print "Unkown msg-type " payload))
         [:chsk/state state] (when (:open? state)
                               (chsk-send! [:cmd/get-stats])
                               (chsk-send! [:cmd/initialize {:n 10}]))
         :else (print "Unmatched event: %s" event)))

(defonce chsk-router (sente/start-chsk-router! ch-chsk event-handler))
