(ns inspect.view.ui.matches
  (:require-macros [reagent.ratom :refer [reaction]])
  (:require [re-frame.core :refer [subscribe]]
            [taoensso.timbre :as timbre :refer-macros [info debug]]
            [inspect.view.util :as u]))

(defn matches [put-fn]
  (let [matches (subscribe [:matches])
        msg-flow (subscribe [:show-flow])
        active-type (subscribe [:active-type])
        ordered-msgs (subscribe [:ordered-msgs])
        msg-mapper (fn [[tag msgs]]
                     (let [first-ts (apply min (map #(-> % second :ts) msgs))
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
                           duration (- last-ts first-ts)
                           selected (contains? max-per-type active-type)
                           active (= tag (:tag msg-flow))]
                       {:duration        duration
                        :active          active
                        :tag             tag
                        :msgs            msgs
                        :first-seen      first-seen
                        :last-seen       last-seen
                        :processing-time processing-time
                        :max-per-type    max-per-type
                        :max-size        max-size}))
        mapped-msgs (reaction (map msg-mapper @ordered-msgs))]
    (fn [put-fn]
      (let [msgs (take-last 100
                            (if @active-type
                              (filter #(contains? (:max-per-type %) @active-type)
                                      @mapped-msgs)
                              @mapped-msgs))]
        [:div.msg-flows.section
         [:h2 "Message Flows"]
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
           (for [{:keys [tag first-seen active max-per-type duration last-seen
                         selected msgs max-size processing-time]} msgs]
             ^{:key (str tag first-seen)}
             [:tr {:class    (if active "active-flow" (when selected "selected"))
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
