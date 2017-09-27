(ns inspect.view.ui.cmp
  (:require-macros [reagent.ratom :refer [reaction]])
  (:require [re-frame.core :refer [subscribe]]
            [taoensso.timbre :as timbre :refer-macros [info debug]]
            [inspect.view.util :as u]
            [cljs.pprint :as pp]))

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

(defn component-table [cmp-id put-fn]
  (let [components (subscribe [:components])]
    (fn [cmp-id put-fn]
      [:div
       [:h2
        ;[:span.color {:style {:background-color (u/random-color cmp-id)}}]
        (str cmp-id)]
       [:div.tables
        [msg-table cmp-id :in put-fn]
        [msg-table cmp-id :out put-fn]]])))