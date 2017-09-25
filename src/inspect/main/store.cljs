(ns inspect.main.store
  (:require [taoensso.timbre :as timbre :refer-macros [info debug]]
            [clojure.pprint :as pp]
            [clojure.set :as set]))

(defn state-fn [put-fn]
  (let [state (atom {:stats {:count      0
                             :cmp-ids    #{}
                             :components {}
                             :msg-types  #{}}})]
    {:state state}))

(defn firehose-msg [{:keys [current-state msg-type msg msg-payload msg-meta]}]
  (let [cnt (:cnt (:stats current-state))
        in-out (if (= msg-type :firehose/cmp-recv) :in :out)
        {:keys [cmp-id msg msg-meta firehose-id]} msg-payload
        msg-type (first msg)
        add-edge (fn [prev-edges]
                   (let [cmps (:cmp-seq msg-meta)
                         pairs (set/union (set (partition 2 cmps))
                                          (set (partition 2 (drop 1 cmps))))
                         edges (set (map (fn [[from to]]
                                           {:source      (str from)
                                            :target      (str to)
                                            :from        from
                                            :to          to
                                            :source-name (name from)
                                            :source-ns   (namespace from)
                                            :target-name (name to)
                                            :target-ns   (namespace to)
                                            :msg-type    msg-type})
                                         pairs))]
                     (debug (set/union prev-edges edges))
                     (set/union prev-edges edges)))
        inc-cnt #(inc (or % 0))
        new-state (-> current-state
                      (update-in [:stats :cnt] inc)
                      (update-in [:stats :cmp-ids] conj cmp-id)
                      (update-in [:stats :components cmp-id in-out msg-type] inc-cnt)
                      (assoc-in [:messages firehose-id] msg-payload)
                      (update-in [:stats :edges] add-edge)
                      (update-in [:stats :edges-by-type msg-type] add-edge)
                      (update-in [:stats :msg-types] conj msg-type))
        subscription (:subscription current-state)
        map-stats (fn [m]
                    (when (map? m)
                      (into {} (map (fn [[k v]] [k (count (str v))]) m))))
        msg-stats (map-stats (-> msg-payload :msg second))
        type-and-size (-> msg-payload
                          (update-in [:msg] (fn [[t m]] [t (count (str m))]))
                          (assoc-in [:msg-stats] msg-stats))
        match (when (-> type-and-size :msg-meta :tag)
                (with-meta [:subscription/match type-and-size]
                           (:msg-meta subscription)))]

    (let [msg-type (-> msg-payload :msg first)
          msg (-> msg-payload :msg second)]
      (when (= msg-type :state/stats-tags)
        (info msg-type (-> msg-payload :msg first) msg-stats)
        (info (take 10 (:hashtags msg)))
        (info (map-stats (:hashtags msg)))))

    (when match (debug "Subscription match:" match))
    {:new-state new-state
     :emit-msg  match}))

(defn state-publish [{:keys [current-state]}]
  (let [{:keys [cnt prev-cnt]} (:stats current-state)
        new-state (assoc-in current-state [:prev-cnt] cnt)]
    (when (not= cnt prev-cnt) (debug "STORE received:" cnt))
    {:new-state new-state
     :emit-msg  (when (not= cnt prev-cnt)
                  (with-meta [:observer/cmps-msgs (:stats new-state)]
                             {:window-id :broadcast}))}))

(defn subscribe [{:keys [current-state msg-payload msg-meta]}]
  (let [subscription (assoc-in msg-payload [:msg-meta] msg-meta)
        new-state (assoc-in current-state [:subscription] subscription)]
    (info "OBSERVER: subscribe" msg-payload)
    {:new-state new-state}))

(defn stop [{:keys [current-state]}]
  (let [new-state (assoc-in current-state [:subscription] nil)]
    (info "OBSERVER: subscription stopped")
    {:new-state new-state}))

(defn cmp-map [cmp-id]
  {:cmp-id      cmp-id
   :state-fn    state-fn
   :handler-map {:firehose/cmp-recv  firehose-msg
                 :firehose/cmp-put   firehose-msg
                 :observer/subscribe subscribe
                 :observer/stop      stop
                 :state/publish      state-publish}})

