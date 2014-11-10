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
  (def ch-chsk ch-recv) ; ChannelSocket's receive channel
  (def chsk-send! send-fn) ; ChannelSocket's send API fn
  (def chsk-state state))  ; Watchable, read-only atom

(defn- event-handler [{:keys [event]}]
  (match event
         [:chsk/recv payload]
         (match payload
                [:info/msg msg] (swap! appstate/app assoc :events (conj (:events @appstate/app) msg))
                [:info/known-event-types event-types] (do
                                                        (reset! appstate/known-event-types event-types)
                                                        (reset! appstate/selected-event-types event-types))
                :else (print "Unkown msg-type " payload))
         [:chsk/state state] (when (:open? state) (chsk-send! [:cmd/get-event-types]))
         :else (print "Unmatched event: %s" event))
  (print appstate/known-event-types))

(defonce chsk-router (sente/start-chsk-router! ch-chsk event-handler))
