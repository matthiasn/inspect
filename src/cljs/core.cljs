(ns inspect.core
  (:require [inspect.communicator :as comm]
            [inspect.appstate :as appstate]
            [reagent.core :as reagent :refer [atom]]
            [cljs.core.async :as async :refer [<! >! chan put! alts! timeout]]))

(enable-console-print!)

;;;; Main file of the inspect tool written in ClojureScript

(defn by-id
  "helper function for selecting DOM elements"
  [id]
  (.getElementById js/document id))

(defn get-next-items
  "send command to server to subscribe to the next events"
  []
  (let [next-items (into {} (map (fn [i] [i (:next-n @appstate/app)]) @appstate/selected-event-types))]
    (print next-items)
    (comm/chsk-send! [:cmd/get-next-items next-items])))

(defn active?
  "check if message type is active"
  [selected t]
  (contains? @selected t))

(defn toggle-selected
  "toggle selected status of event type"
  [selected t]
  (if (active? selected t)
    (swap! selected disj t)
    (swap! selected conj t)))

(defn select-btn
  "creates toggle button in group for selecting shown event types"
  [t selected]
  (let [active (if (active? selected t) "pure-button-primary" "")]
    [:button.pure-button.button-xsmall {:class active :on-click #(toggle-selected selected t)} (str t)]))

(defn known-types
  "creates button group for all known event types"
  [types selected]
  [:div.btn-group
   (for [t types] [select-btn t selected])
   [:br]])

(defn subscribed-selected
  "creates table with the current subscriptions"
  [client-map]
  [:div
   [:table.pure-table.table-small.pure-table-striped
    [:thead [:tr [:th "origin"] [:th "remaining"]]]
    [:tbody
     (for [[origin n] @client-map]
       [:tr [:td (str origin)] [:td n]])]]])

(defn event-div
  "creates div with the event (header, timestamp, pre code) when type currently selected"
  [item selected]
  (let [origin (:origin item)]
    (when (active? selected origin)
      [:div.event
       [:span (:received item)]
       [:h4 (str origin)]
       [:pre [:code (:payload item)]]
       [:br]])))

(defn lister
  "list view for events"
  [items selected]
  [:div
   (for [item items]
     [event-div item selected])])

(defn inspect-view
  "creates main view of the application"
  []
  [:div
   [known-types @appstate/known-event-types appstate/selected-event-types]
   [:br]
   [:button#get-next.pure-button.pure-button-primary.button-xsmall {:on-click get-next-items} "Next"]
   [:input {:type "number"
            :value (:next-n @appstate/app)
            :on-change #(let [value (js/parseInt (-> % .-target .-value))]
                          (when-not (or (js/isNaN value) (neg? value)) (swap! appstate/app assoc :next-n value)))}]
   [:br]
   [:br]
   [subscribed-selected appstate/client-map]
   [:br]
   [lister (reverse (:events @appstate/app)) appstate/selected-event-types]])

(defn run []
  (reagent/render-component (fn [] [inspect-view])
                            (by-id "code")))

(run)
