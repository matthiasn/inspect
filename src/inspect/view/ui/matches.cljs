(ns inspect.view.ui.matches
  (:require-macros [reagent.ratom :refer [reaction]])
  (:require [re-frame.core :refer [subscribe]]
            [taoensso.timbre :as timbre :refer-macros [info debug]]
            [inspect.view.util :as u]
            [inspect.view.ui.detail :as ud]
            [clojure.pprint :as pp]
            [reagent.core :as r]
            [clojure.data.avl :as avl]
            [inspect.view.ui.timeline :as ut]))

(defn matches [put-fn]
  (let [local (r/atom {:ts 9999999999999})
        avl-map (subscribe [:avl-map])
        from-avl (reaction (->> @avl-map
                                (map second)
                                (apply concat)
                                (map second)))

        msg-flow (subscribe [:show-flow])
        flows (subscribe [:flows])
        active-type (subscribe [:active-type])
        older-than (reaction (->> (avl/subrange @avl-map < (:ts @local))
                                  (map second)
                                  (apply concat)
                                  (map second)))
        filtered (reaction
                   (take-last 15 (if @active-type
                                   (filter
                                     #(contains? (:max-per-type %)
                                                 @active-type)
                                     @older-than)
                                   @older-than)))]
    (fn [put-fn]
      (let [msg-flow @msg-flow
            active-type @active-type

            first-flow-ts (:first-seen-ts (first @from-avl))
            time-span (- (:first-seen-ts (last @from-avl)) first-flow-ts)
            slider-val (* (/ (- (:ts @local)
                                first-flow-ts)
                             time-span)
                          100)
            slider-val (if (= (:ts @local) 9999999999999)
                         100
                         slider-val)
            slider-input (fn [e]
                           (let [v (.-target.value e)
                                 new-ts (if (> v 99)
                                          9999999999999
                                          (.floor js/Math
                                                  (+ first-flow-ts
                                                     (* v (/ time-span 100)))))]
                             (swap! local assoc-in [:ts] new-ts)))]
        [:div.msg-flows.section
         [:h2 "Message Flows"]
         [:div
          "Recorded message flows starting between "
          [:span.time (:first-seen (first @from-avl))] " and "
          [:span.time (:first-seen (last @from-avl))]]
         [:div "Recorded time span: " [:span.time time-span "ms"]]
         (if (= slider-val 100)
           [:div "Showing " [:span.time "latest"] " message flows."]
           [:div
            "Showing message flows started before: "
            [:span.time (u/format-time (:ts @local))]])
         (when (> time-span 0)
           [:input {:type      "range"
                    :value     slider-val
                    :min       min
                    :max       max
                    :style     {:width "600px"}
                    :on-change slider-input}])
         ;[:div [ut/timeline-view first-flow-ts time-span (:ts @local) put-fn]]
         [:table
          [:tbody
           [:tr
            [:th "First seen"]
            [:th "Last seen"]
            [:th "Duration"]
            [:th "Processing time"]
            [:th "Msg type"]
            [:th "Max size"]
            [:th "Tag"]]
           (for [{:keys [tag first-seen max-per-type duration last-seen
                         selected msgs max-size processing-time]} @filtered]
             ^{:key (str tag first-seen)}
             [:tr {:class    (when (= tag (:tag msg-flow))
                               "active-flow")
                   :on-click #(put-fn [:flow/show {:tag tag :msgs msgs}])}
              [:td first-seen]
              [:td last-seen]
              [:td.number (if (> duration 10000)
                            (str (.floor js/Math (/ duration 1000)) "s")
                            (str duration "ms"))]
              [:td.number (str processing-time "ms")]
              [:td [:table.max-per-type
                    [:tbody
                     (for [[msg-type size] max-per-type]
                       ^{:key (str tag first-seen msg-type)}
                       [:tr
                        [:td (str msg-type)]
                        [:td size]])]]]
              [:td.number max-size]
              [:td (subs tag 0 8)]])]]]))))
