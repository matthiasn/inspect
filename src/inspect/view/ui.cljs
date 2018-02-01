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
            [clojure.pprint :as pp]))

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
(reg-sub :components (fn [db _] (:components (:cmps-msgs db))))
(reg-sub :prev-cmps (fn [db _] (:components (:cmps-msgs-prev db))))
(reg-sub :frozen (fn [db _] (:components (:frozen db))))
(reg-sub :cmp-ids (fn [db _] (:cmp-ids (:cmps-msgs db))))
(reg-sub :edges (fn [db _] (:edges (:cmps-msgs db))))
(reg-sub :cnt (fn [db _] (-> db :cmps-msgs :cnt)))
(reg-sub :db-counter (fn [db _] (-> db :db-counter)))

(defn re-frame-ui [put-fn]
  (let [cmp-ids (subscribe [:cmp-ids])
        count (subscribe [:cnt])
        spec-errors (subscribe [:spec-errors])
        kafka-status (subscribe [:kafka-status])
        local (r/atom {:kafka-hosts ["localhost:9092"]})
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
      [:div.grid.observer
       [:div.wrapper
        [:div.menu
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
           (let [cnt @count]
             (when (pos? cnt)
               [:div.cnt [:strong cnt] " messages analyzed"]))]
          [:div.status {:class (when (= :error (:status @kafka-status)) "error")}
           (:text @kafka-status)]]]
        [:div.col-1
         [gv/wiring-view put-fn]]
        [:div.col-2
         (when @cmp-ids
           [:div.section
            (for [cmp-id @cmp-ids]
              ^{:key (str cmp-id)}
              [uc/component-table cmp-id put-fn])])
         [:div
          [:button.freeze {:on-click freeze} [:span.fa.fa-bolt] "freeze"]
          [:button.clear {:on-click clear} [:span.fa.fa-trash] "clear"]]]
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
              [:pre [:code spec-error]]]))]]])))

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
