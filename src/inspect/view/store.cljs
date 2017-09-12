(ns inspect.view.store
  (:require [taoensso.timbre :as timbre :refer-macros [info debug]]))

(defn cmps-msgs [{:keys [current-state msg msg-type msg-meta msg-payload]}]
  (let [prev (:cmps-msgs current-state)
        new-state (-> current-state
                      (assoc-in [:cmps-msgs] msg-payload)
                      (assoc-in [:cmps-msgs-prev] prev))]
    (debug "Received:" msg-payload)
    {:new-state new-state}))

(defn freeze [{:keys [current-state msg msg-type msg-meta msg-payload]}]
  (let [prev (:cmps-msgs current-state)
        new-state (assoc-in current-state [:frozen] prev)]
    (info msg)
    {:new-state new-state}))

(defn cell-active [{:keys [current-state msg-payload]}]
  (let [active  #(when-not (= % msg-payload) msg-payload)
        new-state (update-in current-state [:active-type] active)]
    (info "activated" (:active-type new-state))
    {:new-state new-state}))

(defn kafka-status [{:keys [current-state msg-payload]}]
  (let [new-state (assoc-in current-state [:kafka-status] msg-payload)]
    (prn msg-payload)
    {:new-state new-state}))

(defn cmp-map [cmp-id]
  {:cmp-id      cmp-id
   :handler-map {:observer/cmps-msgs cmps-msgs
                 :cell/active        cell-active
                 :state/freeze       freeze
                 :kafka/status       kafka-status}})
