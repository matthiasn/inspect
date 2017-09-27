(ns inspect.view.ui.flow
  (:require-macros [reagent.ratom :refer [reaction]])
  (:require [re-frame.core :refer [subscribe]]
            [taoensso.timbre :as timbre :refer-macros [info debug]]
            [inspect.view.util :as u]
            [cljs.pprint :as pp]
            [inspect.view.graphviz :as gv]))

(defn msg-mapper [[firehose-id firehose-msg]]
  (let [{:keys [cmp-id msg duration msg-meta firehose-type]} firehose-msg
        [msg-type msg-size] msg]
    (-> firehose-msg
        (assoc-in [:msg-type] msg-type)
        (assoc-in [:msg-size] msg-size))))

(defn msg-compare [x y]
  (let [c (compare (:ts x) (:ts y))]
    (if (not= c 0)
      c
      (compare (-> x :msg-meta :cmp-seq count)
               (-> y :msg-meta :cmp-seq count)))))

(defn msg-flow [put-fn]
  (let [msg-flow (subscribe [:show-flow])
        detailed-msg (subscribe [:detailed-msg])
        active-detail (reaction (:firehose-id @detailed-msg))]
    (fn [put-fn]
      (let [{:keys [tag msgs]} @msg-flow
            sorted-by-ts (sort msg-compare (map msg-mapper msgs))
            active-detail @active-detail]
        (when tag
          [:div.msg-flow.section
           [:h3 "Tag: " tag]
           [gv/flow-graph put-fn]
           [:table
            [:tbody
             [:tr
              [:th "Time"]
              [:th "Cmp ID"]
              [:th "Msg type"]
              [:th "Duration"]
              [:th "Size"]
              [:th "Direction"]]
             (for [firehose-msg sorted-by-ts]
               (let [{:keys [cmp-id duration msg-type msg-meta firehose-type
                             firehose-id ts msg-size]} firehose-msg
                     color (u/chf msg-type u/colors)
                     click #(put-fn [:msg/get firehose-id])]
                 ^{:key firehose-id}
                 [:tr {:class    (when (= firehose-id active-detail) "active")
                       :on-click click}
                  [:td (u/format-time ts)]
                  [:td (str cmp-id)]
                  [:td
                   [:span {:style {:background-color color
                                   :padding-right    "10px"}}]
                   (str msg-type)]
                  [:td.number (when duration (str duration "ms"))]
                  [:td.number msg-size]
                  [:td.number (if (= firehose-type :firehose/cmp-recv) "IN" "OUT")]]))]]])))))
