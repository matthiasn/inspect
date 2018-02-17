(ns inspect.view.ui
  (:require-macros [reagent.ratom :refer [reaction]])
  (:require [reagent.core :as r]
            [re-frame.core :refer [reg-sub subscribe]]
            [re-frame.db :as rdb]
            [taoensso.timbre :refer-macros [info debug]]
            [inspect.view.graphviz :as gv]
            [inspect.view.ui.matches :as um]
            [inspect.view.ui.detail :as ud]
            [inspect.view.ui.flow :as uf]
            [inspect.view.ui.cmp :as uc]
            [inspect.view.util :as u]
            [clojure.pprint :as pp]
            [clojure.string :as s]))

;; Subscription Handlers
(reg-sub :cmps-msgs (fn [db _] (:cmps-msgs db)))
(reg-sub :spec-errors (fn [db _] (:spec-errors db)))
(reg-sub :flows (fn [db _] (:flows db)))
(reg-sub :detailed-msg (fn [db _] (:detailed-msg db)))
(reg-sub :svg-overview (fn [db _] (:svg-overview db)))
(reg-sub :avl-map (fn [db _] (:avl-map db)))
(reg-sub :show-flow (fn [db _] (:show-flow db)))
(reg-sub :ordered-msgs (fn [db _] (:ordered-msgs db)))
(reg-sub :active-type (fn [db _] (:active-type db)))
(reg-sub :kafka-status (fn [db _] (:kafka-status db)))
(reg-sub :known-hosts (fn [db _] (:known-hosts db)))
(reg-sub :components (fn [db _] (:components (:cmps-msgs db))))
(reg-sub :prev-cmps (fn [db _] (:components (:cmps-msgs-prev db))))
(reg-sub :frozen (fn [db _] (:components (:frozen db))))
(reg-sub :cmp-ids (fn [db _] (:cmp-ids (:cmps-msgs db))))
(reg-sub :msg-types (fn [db _] (:msg-types (:cmps-msgs db))))
(reg-sub :edges (fn [db _] (:edges (:cmps-msgs db))))
(reg-sub :cnt (fn [db _] (-> db :cmps-msgs :cnt)))
(reg-sub :db-counter (fn [db _] (-> db :db-counter)))

(defn msg-types [put-fn]
  (let [msg-types (subscribe [:msg-types])
        active-type (subscribe [:active-type])
        sort-fns {:by-name first
                  :by-cnt  last}
        local (r/atom {:sort      :by-cnt
                       :search    ""
                       :ascending true})
        input-fn (fn [ev]
                   (let [s (-> ev .-nativeEvent .-target .-value)]
                     (swap! local assoc-in [:search] s)))]
    (fn msg-types-render [put-fn]
      (let [active-type @active-type
            sort-fn (get sort-fns (:sort @local))
            filtered (filter (fn [[k _]] (s/includes? (str k) (:search @local)))
                             @msg-types)
            sorted (sort-by sort-fn filtered)
            sorted (if (:ascending @local) sorted (reverse sorted))]
        [:div.msg-types
         [:input {:type      :text
                  :on-change input-fn
                  :value     (:search @local)}]
         [:table
          [:tbody
           [:tr
            [:th
             "message type"
             [:span.fa {:class    (str (if (:ascending @local)
                                         "fa-sort-alpha-asc"
                                         "fa-sort-alpha-desc")
                                       (when (= :by-name (:sort @local))
                                         " sort-active"))
                        :on-click #(do
                                     (swap! local assoc :sort :by-name)
                                     (swap! local update :ascending not))}]]
            [:th
             [:span.fa {:class    (str (if (:ascending @local)
                                         "fa-sort-amount-asc"
                                         "fa-sort-amount-desc")
                                       (when (= :by-cnt (:sort @local))
                                         " sort-active"))
                        :on-click #(do
                                     (swap! local assoc :sort :by-cnt)
                                     (swap! local update :ascending not))}]]]
           (for [[msg-type cnt] sorted]
             ^{:key (str msg-type)}
             [:tr {:on-click #(let [subscription {:msg-type msg-type}]
                                (if (= msg-type active-type)
                                  (put-fn [:observer/stop])
                                  (put-fn [:observer/subscribe subscription]))
                                (put-fn [:svg/set-active msg-type])
                                (put-fn [:cell/active msg-type]))}
              [:td.cmp-id {:class (when (= msg-type active-type) "active")}
               (str msg-type)]
              [:td.cnt cnt]])]]]))))

(defn re-frame-ui [put-fn]
  (let [cmp-ids (subscribe [:cmp-ids])
        count (subscribe [:cnt])
        spec-errors (subscribe [:spec-errors])
        kafka-status (subscribe [:kafka-status])
        known-hosts (subscribe [:known-hosts])
        local (r/atom {:kafka-host ""
                       :expanded   true})
        input-fn (fn [ev]
                   (let [address (-> ev .-nativeEvent .-target .-value)]
                     (swap! local assoc-in [:kafka-host] address)))
        subscribe #(let [kafka-host (:kafka-host @local)]
                     (info :start kafka-host)
                     (put-fn [:kafka/start kafka-host])
                     (swap! local assoc-in [:expanded] false))
        stop #(do (put-fn [:kafka/stop])
                  (swap! local assoc-in [:expanded] true)
                  (swap! local assoc-in [:kafka-host] ""))
        freeze #(put-fn [:state/freeze])
        clear #(put-fn [:state/clear])]
    (fn [_]
      (let [entered (:kafka-host @local)
            hosts (filter #(s/includes? % entered) @known-hosts)]
        [:div.grid.observer
         [:div.wrapper
          [:div.menu
           [:div.section
            [:div.header
             [:div.hosts
              [:div.host-input
               [:input {:type      :text
                        :on-change input-fn
                        :value     (:kafka-host @local)}]
               (if (contains? #{:starting :connected} (:status @kafka-status))
                 [:button.stop {:on-click stop}
                  [:i.fas.fa-stop] "stop"]
                 [:button {:on-click subscribe}
                  [:i.fas.fa-play] "subscribe"])]
              (when (and (:expanded @local) (not-empty @known-hosts))
                [:div.known-hosts
                 (for [host hosts]
                   ^{:key host}
                   [:div.known-host
                    {:on-click (fn [_]
                                 (swap! local assoc-in [:kafka-host] host)
                                 (subscribe))}
                    host])])]
             (let [cnt @count]
               (when (pos? cnt)
                 [:div.cnt [:strong cnt] " messages analyzed"]))]
            [:div.status {:class (when (= :error (:status @kafka-status)) "error")}
             (:text @kafka-status)]]]
          [:div.col-1
           [gv/wiring-view put-fn]]
          [:div.col-2
           (when @cmp-ids
             [:div.section.msg-cmp
              [msg-types put-fn]
              [:div
               (for [cmp-id @cmp-ids]
                 ^{:key (str cmp-id)}
                 [uc/component-table cmp-id put-fn])]])
           [:div
            [:button.freeze {:on-click freeze} [:i.fas.fa-bolt] "freeze"]
            [:button.clear {:on-click clear} [:i.fas.fa-trash] "clear"]]]
          [:div.col-3
           [um/matches put-fn]]
          [:div.col-4
           [gv/flow-graph put-fn]]
          [:div.col-5
           [uf/msg-flow put-fn]]
          [:div.col-6
           [ud/detailed-msg put-fn]]
          [:div.col-7.spec-errors
           [:h2 "Spec Validation Errors"]
           (for [err @spec-errors]
             (let [{:keys [firehose-id msg spec-error]} err
                   click #(put-fn [:sled/get {:k firehose-id}])]
               ^{:key firehose-id}
               [:div {:on-click click}
                [:div.header
                 [:time (u/format-time (:ts err))]
                 (str (first msg))]
                [:pre [:code spec-error]]]))]]]))))

(defn state-fn
  "Renders main view component and wires the central re-frame app-db as the
   observed component state, which will then be updated whenever the store-cmp
   changes."
  [put-fn]
  (r/render [re-frame-ui put-fn] (.getElementById js/document "app"))
  {:observed rdb/app-db})

(defn cmp-map [cmp-id]
  {:cmp-id   cmp-id
   :state-fn state-fn})
