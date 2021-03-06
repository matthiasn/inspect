(ns inspect.main.store
  (:require [taoensso.timbre :refer-macros [info error debug]]
            [cljs.reader :refer [read-string]]
            [clojure.set :as set]))

(defn state-fn [put-fn]
  (let [state (atom {:stats {:count      0
                             :cmp-ids    #{}
                             :components {}
                             :msg-types  {}}})]
    {:state state}))

(defn firehose-msg [{:keys [current-state msg-type msg-payload put-fn]}]
  (let [in-out (if (= msg-type :firehose/cmp-recv) :in :out)
        {:keys [cmp-id msg msg-meta firehose-id spec-error]} msg-payload
        firehose-type msg-type
        msg-type (first msg)
        add-edge (fn [prev-edges]
                   (let [cmps (:cmp-seq msg-meta)
                         [from to] (take-last 2 cmps)
                         edges (if (and from
                                        to
                                        (or (= firehose-type :firehose/cmp-recv)
                                            (not= (namespace from)
                                                  (namespace to))))
                                 #{{:source      (str from)
                                    :target      (str to)
                                    :from        from
                                    :to          to
                                    :source-name (name from)
                                    :source-ns   (namespace from)
                                    :target-name (name to)
                                    :target-ns   (namespace to)
                                    :msg-type    msg-type}}
                                 #{})]
                     (debug (set/union prev-edges edges))
                     (set/union prev-edges edges)))
        inc-cnt #(inc (or % 0))
        new-state (-> current-state
                      (update-in [:stats :cnt] inc)
                      (update-in [:stats :cmp-ids] conj cmp-id)
                      (update-in [:stats :components cmp-id in-out msg-type] inc-cnt)
                      (update-in [:stats :msg-types msg-type] inc-cnt)
                      (update-in [:stats :edges] add-edge)
                      (update-in [:stats :edges-by-type msg-type] add-edge))
        subscription (:subscription current-state)
        map-stats (fn [m]
                    (when (map? m)
                      (into {} (map (fn [[k v]] [k (count (str v))]) m))))
        msg-stats (map-stats (-> msg-payload :msg second))
        type-and-size (-> msg-payload
                          (update-in [:msg] (fn [[t m]] [t (count (str m))]))
                          (assoc-in [:msg-stats] msg-stats)
                          (update-in [:msg-meta :cmp-seq] #(take-last 20 %))
                          (assoc-in [:firehose-type] firehose-type))
        match (when (-> type-and-size :msg-meta :tag)
                (with-meta [:subscription/match type-and-size]
                           (:msg-meta subscription)))]
    (when match
      (debug "Subscription match:" match) (put-fn match))
    (when spec-error
      (put-fn [:spec/error msg-payload]))
    {:new-state new-state
     :emit-msg  [[:sled/put {:k (str firehose-id) :v msg-payload}]]}))

(defn state-publish [{:keys [current-state put-fn]}]
  (let [stats (:stats current-state)
        {:keys [cnt prev-cnt]} stats
        new-state (-> current-state
                      (assoc-in [:prev-cnt] cnt)
                      (assoc-in [:prev-stats] stats))
        svg-data (select-keys stats [:cmp-ids :edges])]
    (when (not= cnt prev-cnt)
      (debug "STORE received:" cnt)
      (put-fn (with-meta [:observer/cmps-msgs (:stats new-state)]
                         {:window-id :broadcast})))
    {:new-state new-state
     :emit-msg  [:svg/gen-overview svg-data]}))

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
