(ns inspect.view.ui
  (:require-macros [reagent.ratom :refer [reaction]])
  (:require [reagent.core :as rc]
            [re-frame.core :refer [reg-sub subscribe]]
            [re-frame.db :as rdb]
            [taoensso.timbre :as timbre :refer-macros [info debug]]
            [inspect.view.force :as f]
            [inspect.view.force2 :as f2]
            [inspect.view.graphviz :as gv]
            [inspect.view.util :as u]
            [cljs.pprint :as pp]
            [cljs.spec.alpha :as s]
            [clojure.set :as set]
            [reagent.core :as r]))

(s/def :observer/cmps-msgs map?)

;; Subscription Handlers
(reg-sub :cmps-msgs (fn [db _] (:cmps-msgs db)))
(reg-sub :matches (fn [db _] (:matches db)))
(reg-sub :active-type (fn [db _] (:active-type db)))
(reg-sub :kafka-status (fn [db _] (:kafka-status db)))
(reg-sub :components (fn [db _] (:components (:cmps-msgs db))))
(reg-sub :prev-cmps (fn [db _] (:components (:cmps-msgs-prev db))))
(reg-sub :frozen (fn [db _] (:components (:frozen db))))
(reg-sub :cmp-ids (fn [db _] (:cmp-ids (:cmps-msgs db))))
(reg-sub :edges (fn [db _] (:edges (:cmps-msgs db))))
(reg-sub :cnt (fn [db _] (-> db :cmps-msgs :cnt)))

(defn cnt-cell
  [cmp-id msg-type dir]
  (let [components (subscribe [:components])
        cnt (reaction (-> @components cmp-id dir msg-type))
        prev-cmps (subscribe [:prev-cmps])
        prev-cnt (reaction (-> @prev-cmps cmp-id dir msg-type))]
    (fn [cmp-id msg-type dir]
      (let [changed (not= @cnt @prev-cnt)]
        [:td.cnt {:class (when changed "changed")}
         @cnt]))))

(defn delta-cell
  [cmp-id msg-type dir]
  (let [components (subscribe [:components])
        cnt (reaction (-> @components cmp-id dir msg-type))
        frozen-cmps (subscribe [:frozen])
        frozen-cnt (reaction (-> @frozen-cmps cmp-id dir msg-type))]
    (fn [cmp-id msg-type dir]
      (let [delta (- @cnt @frozen-cnt)]
        [:td.cnt (if (and @frozen-cnt (pos? delta))
                   [:strong (str "+" delta)]
                   " ")]))))

(defn msg-table
  [cmp-id dir put-fn]
  (let [components (subscribe [:components])
        active-type (subscribe [:active-type])
        cmp-map (reaction (cmp-id @components))]
    (fn [cmp-id dir put-fn]
      (let [active-type @active-type]
        [:table
         [:tbody
          (for [[msg-type cnt] (dir @cmp-map)]
            ^{:key (str msg-type)}
            [:tr
             [:td.dir [:strong (str dir)]]
             [:td.cmp-id {:on-click #(let [subscription {:msg-type msg-type
                                                         :cmp-id   cmp-id
                                                         :dir      dir}]
                                       (if (= msg-type active-type)
                                         (put-fn [:observer/stop])
                                         (put-fn [:observer/subscribe subscription]))
                                       (put-fn [:cell/active msg-type]))
                          :class    (when (= msg-type active-type) "active")}
              (str msg-type)]
             [cnt-cell cmp-id msg-type dir]
             [delta-cell cmp-id msg-type dir]])]]))))

(defn component [cmp-id put-fn]
  (let [components (subscribe [:components])]
    (fn [cmp-id put-fn]
      [:div
       [:h2
        ;[:span.color {:style {:background-color (u/random-color cmp-id)}}]
        (str cmp-id)]
       [:div.tables
        [msg-table cmp-id :in put-fn]
        [msg-table cmp-id :out put-fn]]])))

(defn matches [put-fn]
  (let [matches (subscribe [:matches])]
    (fn [put-fn]
      [:div
       [:h2 "Matches"]
       (for [match @matches]
         ^{:key (:firehose-id match)}
         [:div
          [:pre [:code (with-out-str (pp/pprint match))]]])])))

(defn re-frame-ui
  "Main view component"
  [put-fn]
  (let [cmp-ids (subscribe [:cmp-ids])
        count (subscribe [:cnt])
        kafka-status (subscribe [:kafka-status])
        local (r/atom {:kafka-host "localhost:9092"})
        input-fn (fn [ev]
                   (let [address (-> ev .-nativeEvent .-target .-value)]
                     (swap! local assoc-in [:kafka-host] address)))
        subscribe #(let [kafka-host (:kafka-host @local)]
                     (info :start kafka-host)
                     (put-fn [:kafka/start kafka-host]))
        stop #(put-fn [:kafka/stop])
        freeze #(put-fn [:state/freeze])]
    (fn [_]
      [:div.observer
       [gv/wiring put-fn]
       [:div
        [:div.header
         [:div
          [:input {:type      :text
                   :on-change input-fn
                   :value     (:kafka-host @local)}]
          (if (= :connected (:status @kafka-status))
            [:button.stop {:on-click stop}
             [:span.fa.fa-stop] "stop"]
            [:button {:on-click subscribe}
             [:span.fa.fa-play] "subscribe"])]
         [:div.cnt " Count: " [:strong @count]]]
        [:div.status {:class (when (= :error (:status @kafka-status)) "error")}
         (:text @kafka-status)]
        (for [cmp-id @cmp-ids]
          ^{:key (str cmp-id)}
          [component cmp-id put-fn])
        [:div [:button.freeze {:on-click freeze} [:span.fa.fa-bolt] "freeze"]]
        [matches put-fn]]])))

(defn state-fn
  "Renders main view component and wires the central re-frame app-db as the
   observed component state, which will then be updated whenever the store-cmp
   changes."
  [put-fn]
  (rc/render [re-frame-ui put-fn] (.getElementById js/document "app"))
  {:observed rdb/app-db})

(defn cmp-map
  [cmp-id]
  {:cmp-id   cmp-id
   :state-fn state-fn})
