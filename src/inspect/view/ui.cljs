(ns inspect.view.ui
  (:require-macros [reagent.ratom :refer [reaction]])
  (:require [reagent.core :as r]
            [re-frame.core :refer [reg-sub subscribe]]
            [re-frame.db :as rdb]
            [taoensso.timbre :as timbre :refer-macros [info debug]]
            [inspect.view.graphviz :as gv]
            [inspect.view.ui.matches :as um]
            [inspect.view.ui.timeline :as ut]
            [inspect.view.ui.detail :as ud]
            [inspect.view.ui.flow :as uf]
            [inspect.view.ui.cmp :as uc]
            [inspect.view.util :as u]))

;; Subscription Handlers
(reg-sub :cmps-msgs (fn [db _] (:cmps-msgs db)))
(reg-sub :flows (fn [db _] (:flows db)))

(reg-sub :res1 (fn [db _] []))
(reg-sub :res2 (fn [db _] []))

(reg-sub :detailed-msg (fn [db _] (:detailed-msg db)))
(reg-sub :avl-map (fn [db _] (:avl-map db)))
(reg-sub :show-flow (fn [db _] (:show-flow db)))
(reg-sub :ordered-msgs (fn [db _] (:ordered-msgs db)))
(reg-sub :active-type (fn [db _] (:active-type db)))
(reg-sub :kafka-status (fn [db _] (:kafka-status db)))
(reg-sub :components (fn [db _] (:components (:cmps-msgs db))))
(reg-sub :prev-cmps (fn [db _] (:components (:cmps-msgs-prev db))))
(reg-sub :frozen (fn [db _] (:components (:frozen db))))
(reg-sub :cmp-ids (fn [db _] (:cmp-ids (:cmps-msgs db))))
(reg-sub :edges (fn [db _] (:edges (:cmps-msgs db))))
(reg-sub :cnt (fn [db _] (-> db :cmps-msgs :cnt)))

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
        freeze #(put-fn [:state/freeze])
        clear #(put-fn [:state/clear])]
    (fn [_]
      [:div.observer
       [:div.section
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
         (:text @kafka-status)]]
       [gv/wiring-view put-fn]
       (when @cmp-ids
         [:div.section
          (for [cmp-id @cmp-ids]
            ^{:key (str cmp-id)}
            [uc/component-table cmp-id put-fn])])
       [:div
        [:button.freeze {:on-click freeze} [:span.fa.fa-bolt] "freeze"]
        [:button.clear {:on-click clear} [:span.fa.fa-trash] "clear"]]
       [um/matches put-fn]
       [uf/msg-flow put-fn]
       [ud/detailed-msg put-fn]])))

(defn state-fn
  "Renders main view component and wires the central re-frame app-db as the
   observed component state, which will then be updated whenever the store-cmp
   changes."
  [put-fn]
  (r/render [re-frame-ui put-fn] (.getElementById js/document "app"))
  {:observed rdb/app-db})

(defn cmp-map
  [cmp-id]
  {:cmp-id   cmp-id
   :state-fn state-fn})
