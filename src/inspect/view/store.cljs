(ns inspect.view.store
  (:require [taoensso.timbre :refer-macros [info debug]]
            [linked.core :as linked]
            [clojure.data.avl :as avl]
            [inspect.view.util :as u]))

(defn cmps-msgs [{:keys [current-state msg-payload]}]
  (let [prev (:cmps-msgs current-state)
        new-state (-> current-state
                      (assoc-in [:cmps-msgs] msg-payload)
                      (assoc-in [:cmps-msgs-prev] prev))]
    (info "Received:" (keys msg-payload))
    {:new-state new-state}))

(defn freeze [{:keys [current-state msg]}]
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

(defn active-cmps [{:keys [current-state msg-payload]}]
  (let [active #(if (contains? % msg-payload)
                  (disj % msg-payload)
                  (conj % msg-payload))
        new-state (update-in current-state [:active-cmps] active)]
    (info "active components" (:active-cmps new-state))
    {:new-state new-state}))

(defn kafka-status [{:keys [current-state msg-payload]}]
  (let [new-state (assoc-in current-state [:kafka-status] msg-payload)]
    (debug msg-payload)
    {:new-state new-state}))

(defn svg-overview [{:keys [current-state msg-payload]}]
  (let [new-state (assoc-in current-state [:svg-overview] msg-payload)]
    (debug msg-payload)
    {:new-state new-state}))

(defn show-flow [{:keys [current-state msg-payload]}]
  (let [toggle #(when-not (= (:tag msg-payload) (:tag %)) msg-payload)
        new-state (update-in current-state [:show-flow] toggle)]
    (debug msg-payload)
    {:new-state new-state}))

(defn update-flow [state tag tag-ts]
  (let [msgs (get-in state [:ordered-msgs tag])
        first-ts (apply min (map #(-> % second :ts) msgs))
        first-seen (u/format-time first-ts)
        last-ts (apply max (map #(-> % second :ts) msgs))
        max-size (apply max (map #(-> % second :msg second) msgs))
        max-per-type (reduce (fn [acc m]
                               (let [msg-type (-> m second :msg first)
                                     size (-> m second :msg second)]
                                 (update-in acc [msg-type] max size)))
                             {}
                             msgs)
        processing-time (apply + (map #(-> % second :duration) msgs))
        last-seen (u/format-time last-ts)
        duration (- last-ts first-ts)]
    {:duration        duration
     :tag             tag
     :tag-ts          tag-ts
     :msgs            msgs
     :first-seen      first-seen
     :first-seen-ts   first-ts
     :last-seen       last-seen
     :processing-time processing-time
     :max-per-type    max-per-type
     :max-size        max-size}))

(defn match [{:keys [current-state msg-payload]}]
  (let [firehose-id (:firehose-id msg-payload)
        ; TODO: all messages MUST have a tag -> s-t (see publish state)
        tag (-> msg-payload :msg-meta :tag)
        tag-ts (-> msg-payload :msg-meta :tag-ts)
        new-state (-> current-state
                      (assoc-in [:ordered-msgs tag firehose-id] msg-payload))
        flow (update-flow new-state tag tag-ts)
        new-state (-> new-state
                      (assoc-in [:flows tag] flow)
                      (assoc-in [:avl-map tag-ts tag] flow))]
    (debug "Match" msg-payload)
    {:new-state new-state}))

(defn msg-res [{:keys [current-state msg-payload]}]
  (let [new-state (if-let [v (:v msg-payload)]
                    (assoc-in current-state [:detailed-msg] (:v msg-payload))
                    (update-in current-state [:db-counter] inc))]
    (debug :msg-res msg-payload)
    {:new-state new-state}))

(defn spec-error [{:keys [current-state msg-payload]}]
  (let [new-state (update-in current-state [:spec-errors] conj msg-payload)]
    (info :spec-error msg-payload)
    {:new-state new-state}))

(defn save-hosts [{:keys [current-state msg-payload]}]
  (let [new-state (assoc-in current-state [:known-hosts] msg-payload)]
    {:new-state new-state}))

(defn cmp-map [cmp-id]
  {:cmp-id      cmp-id
   :state-fn    (fn [_] {:state (atom {:ordered-msgs (linked/map)
                                       :flows        (linked/map)
                                       :db-counter   0
                                       :active-cmps  #{}
                                       :avl-map      (avl/sorted-map)})})
   :handler-map {:observer/cmps-msgs cmps-msgs
                 :cell/active        cell-active
                 :cmp/active         active-cmps
                 :state/freeze       freeze
                 :state/clear        clear
                 :flow/show          show-flow
                 :subscription/match match
                 :sled/res           msg-res
                 :spec/error         spec-error
                 :svg/overview       svg-overview
                 :kafka/hosts        save-hosts
                 :kafka/status       kafka-status}})
