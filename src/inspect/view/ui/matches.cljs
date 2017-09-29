(ns inspect.view.ui.matches
  (:require-macros [reagent.ratom :refer [reaction]])
  (:require [re-frame.core :refer [subscribe]]
            [taoensso.timbre :as timbre :refer-macros [info debug]]
            [inspect.view.util :as u]
            [clojure.pprint :as pp]))

(defn matches [put-fn]
  (let [avl-map (subscribe [:avl-map])
        msg-flow (subscribe [:show-flow])
        flows (subscribe [:flows])
        active-type (subscribe [:active-type])]
    (fn [put-fn]
      (let [flows (vals @flows)
            flows (take-last 50 (if @active-type
                                  (filter
                                    #(contains? (:max-per-type %) @active-type)
                                    flows)
                                  flows))
            msg-flow @msg-flow
            active-type @active-type]
        [:div.msg-flows.section
         ;[:pre [:code (with-out-str (pp/pprint (take-last 2 @flows)))]]
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
           (for [{:keys [tag first-seen max-per-type duration last-seen
                         selected msgs max-size processing-time]} flows]
             ^{:key (str tag first-seen)}
             [:tr {:class    (if (= tag (:tag msg-flow))
                               "active-flow"
                               (when (contains? max-per-type active-type)
                                 "selected"))
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
