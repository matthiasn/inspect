(ns inspect.view.ui.matches
  (:require-macros [reagent.ratom :refer [reaction]])
  (:require [re-frame.core :refer [subscribe]]
            [taoensso.timbre :as timbre :refer-macros [info debug]]
            [inspect.view.util :as u]))

(defn matches [put-fn]
  (let [matches (subscribe [:matches])
        msg-flow (subscribe [:show-flow])
        active-type (subscribe [:active-type])
        ordered-msgs (subscribe [:ordered-msgs])]
    (fn [put-fn]
      (let [active-type @active-type
            ordered-msgs @ordered-msgs
            msg-flow @msg-flow]
        [:div.msg-flows
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
           (for [[tag msgs] (take-last 100 ordered-msgs)]
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
               ^{:key (str tag first-ts)}
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
                         ^{:key (str tag first-ts msg-type)}
                         [:tr
                          [:td (str msg-type)]
                          [:td size]])]]]
                [:td.number max-size]
                [:td (subs tag 0 8)]
                [:td {:on-click #(prn :click)}
                 [:span.fa.fa-eye-slash]]]))]]]))))
