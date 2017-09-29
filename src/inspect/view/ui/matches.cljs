(ns inspect.view.ui.matches
  (:require-macros [reagent.ratom :refer [reaction]])
  (:require [re-frame.core :refer [subscribe]]
            [taoensso.timbre :as timbre :refer-macros [info debug]]
            [inspect.view.util :as u]
            [inspect.view.ui.detail :as ud]
            [clojure.pprint :as pp]
            [reagent.core :as r]))

(defn matches [put-fn]
  (let [local (r/atom {:ts 0})
        avl-map (subscribe [:avl-map])
        msg-flow (subscribe [:show-flow])
        flows (subscribe [:flows])
        active-type (subscribe [:active-type])]
    (fn [put-fn]
      (let [flows (vals @flows)
            flows (take-last 12 (if @active-type
                                  (filter
                                    #(contains? (:max-per-type %) @active-type)
                                    flows)
                                  flows))
            msg-flow @msg-flow
            active-type @active-type
            from-avl (->> @avl-map
                          (map second)
                          (apply concat)
                          (map second))
            from-avl (take-last 12 (if active-type
                                     (filter
                                       #(contains? (:max-per-type %) active-type)
                                       from-avl)
                                     from-avl))]
        [:div.msg-flows.section
         [:h2 "Message Flows"]
         #_
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
              [:td (subs tag 0 8)]])]]

         [:input {:type      "range"
                  :value     (:ts @local)
                  :min       min
                  :max       max
                  :style     {:width "100%"}
                  :on-change (fn [e]
                               (let [v (.-target.value e)]
                                 (info v)
                                 (swap! local assoc-in [:ts] v)))}]

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
                         selected msgs max-size processing-time]} from-avl]
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
              [:td (subs tag 0 8)]])]]
         ;[:pre [:code (with-out-str (pp/pprint (last @avl-map)))]]
         ]))))
