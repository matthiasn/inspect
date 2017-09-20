(ns inspect.main.store
  (:require [taoensso.timbre :as timbre :refer-macros [info debug]]
            [clojure.pprint :as pp]
            [clojure.set :as set]))

(defn state-fn [put-fn]
  (let [state (atom {:count      0
                     :cmp-ids    #{}
                     :components {}
                     :msg-types  #{}})]
    {:state state}))

(defn firehose-msg [{:keys [current-state msg-type msg msg-payload msg-meta]}]
  (let [cnt (:cnt current-state)
        in-out (if (= msg-type :firehose/cmp-recv) :in :out)
        {:keys [cmp-id msg msg-meta]} msg-payload
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
        new-state (-> current-state
                      (update-in [:cnt] inc)
                      (update-in [:cmp-ids] conj cmp-id)
                      (update-in [:components cmp-id in-out msg-type] #(inc (or % 0)))
                      (update-in [:edges] add-edge)
                      (update-in [:edges-by-type msg-type] add-edge)
                      (update-in [:msg-types] conj msg-type))
        subscription (:subscription current-state)
        type-and-size (update-in msg-payload [:msg] (fn [[t m]] [t (count (str m))]))
        #_#_match (when (= msg-type (:msg-type subscription))
                    (with-meta [:subscription/match msg-payload]
                               (:msg-meta subscription)))
        match (when (-> type-and-size :msg-meta :tag)
                (with-meta [:subscription/match type-and-size]
                           (:msg-meta subscription)))]
    (when match (debug "Subscription match:" match))
    {:new-state new-state
     :emit-msg  match}))

(defn state-publish [{:keys [current-state]}]
  (let [{:keys [cnt prev-cnt]} current-state
        new-state (assoc-in current-state [:prev-cnt] cnt)]
    (when (not= cnt prev-cnt) (debug "STORE received:" cnt))
    {:new-state new-state
     :emit-msg  (when (not= cnt prev-cnt)
                  (with-meta [:observer/cmps-msgs new-state]
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

