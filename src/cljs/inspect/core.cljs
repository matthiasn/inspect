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
  (let [next-items (into {} (map (fn [i] [i (:next-n @appstate/app)]) @appstate/selected))]
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
  [stats selected]
  [:div
   [:div.btn-group
    (for [t (map (fn [[t _]] t) (vec @stats)) ] [select-btn t selected])
    [:br]]
   [:br]
   [:br]
   [:button#get-next.pure-button.pure-button-primary.button-xsmall {:on-click get-next-items} "Next"]
   [:input {:type "number"
            :value (:next-n @appstate/app)
            :on-change #(let [value (js/parseInt (-> % .-target .-value))]
                          (when-not (or (js/isNaN value) (neg? value) (> value 100))
                            (swap! appstate/app assoc :next-n value)))}]])

(defn selection
  "creates table with the current subscriptions"
  [client-map selected-event-types stats]
  (let [selected @selected-event-types
        stats @stats]
    [:div.pure-u-md-1-4
     [:table.pure-table.table-small.pure-table-striped
      [:thead [:tr [:th "origin"] [:th "left"] [:th "msg/10s"]]]
      [:tbody
       (for [[origin n] @client-map]
         [:tr {:class (if (contains? selected origin) "active" "")}
          [:td (str origin)] [:td n] [:td (get stats origin)]])]]]))

(defn event-div
  "creates div with the event (header, timestamp, pre code) when type currently selected"
  [item selected]
  (let [origin (:origin item)]
    (when (active? selected origin)
      [:div.event
       [:span (:received item)]
       [:h4 (str origin)]
       [:pre [:code (str (:hiccup item))]]
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
   [lister (reverse (:events @appstate/app)) appstate/selected]])

(defn run []
  (reagent/render-component (fn [] [inspect-view]) (by-id "code"))
  (reagent/render-component (fn [] [known-types appstate/stats appstate/selected]) (by-id "selection"))
  (reagent/render-component (fn [] [selection appstate/client-map appstate/selected appstate/stats]) (by-id "types")))

(run)
