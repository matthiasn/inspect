(ns inspect.view.store
  (:require [taoensso.timbre :as timbre :refer-macros [info debug]]
            [linked.core :as linked]
            [matthiasn.systems-toolbox.component :as stc]))

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

(defn clear [{:keys [current-state]}]
  (let [new-state (assoc-in current-state [:ordered-msgs] (linked/map))]
    {:new-state new-state}))

(defn cell-active [{:keys [current-state msg-payload]}]
  (let [active #(when-not (= % msg-payload) msg-payload)
        new-state (update-in current-state [:active-type] active)]
    (info "activated" (:active-type new-state))
    {:new-state new-state}))

(defn kafka-status [{:keys [current-state msg-payload]}]
  (let [new-state (assoc-in current-state [:kafka-status] msg-payload)]
    (debug msg-payload)
    {:new-state new-state}))

(defn show-flow [{:keys [current-state msg-payload]}]
  (let [toggle #(when-not (= (:tag msg-payload) (:tag %)) msg-payload)
        new-state (update-in current-state [:show-flow] toggle)]
    (debug msg-payload)
    {:new-state new-state}))

(defn match [{:keys [current-state msg-payload]}]
  (let [add (fn [matches match] (take 10 (conj matches match)))
        firehose-id (:firehose-id msg-payload)
        ; TODO: all messages MUST have a tag -> s-t (see publish state)
        tag (-> msg-payload :msg-meta :tag)
        new-state (-> current-state
                      (update-in [:matches] add msg-payload)
                      (assoc-in [:ordered-msgs tag firehose-id] msg-payload))]
    (debug "Match" msg-payload)
    {:new-state new-state}))

(defn msg-res [{:keys [current-state msg-payload]}]
  (debug :msg-res msg-payload)
  {:new-state (assoc-in current-state [:detailed-msg] msg-payload)})

(defn cmp-map [cmp-id]
  {:cmp-id      cmp-id
   :state-fn    (fn [_] {:state (atom {:ordered-msgs (linked/map)})})
   :handler-map {:observer/cmps-msgs cmps-msgs
                 :cell/active        cell-active
                 :state/freeze       freeze
                 :state/clear        clear
                 :flow/show          show-flow
                 :subscription/match match
                 :msg/res            msg-res
                 :kafka/status       kafka-status}})
