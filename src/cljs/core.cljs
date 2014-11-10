(ns inspect.core
  (:require [inspect.communicator :as comm]
            [inspect.appstate :as appstate]
            [reagent.core :as reagent :refer [atom]]
            [cljs.core.async :as async :refer [<! >! chan put! alts! timeout]]))

(enable-console-print!)

;;;; Main file of the inspect tool written in ClojureScript

(defn by-id [id] (.getElementById js/document id))

(defn get-next
  ""
  []
  (comm/chsk-send! [:cmd/get-next {:next-n (:next-n @appstate/app)}]))

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

(defn select-btn [t selected]
  (let [active (if (active? selected t) "btn-primary" "")]
    [:button.btn {:class active :on-click #(toggle-selected selected t)} (str t)]))

(defn known-types [types selected]
  [:div.btn-group
   (for [t types]
     [select-btn t selected])
   [:br]])

(defn event-div
  ""
  [item selected]
  (let [origin (:origin item)]
    (when (active? selected origin)
      [:div
       [:h4 (str origin)]
       [:p (:received item)]
       [:pre [:code (:payload item)]]
       [:br]]))  )

(defn lister [items selected]
  [:div
   (for [item items]
     [event-div item selected])])

(defn inspect-view []
  [:div
   [known-types @appstate/known-event-types appstate/selected-event-types]
   [:button#get-next {:on-click get-next} "Next"]
   [:input {:type "number"
            :value (:next-n @appstate/app)
            :on-change #(let [value (js/parseInt (-> % .-target .-value))]
                          (when-not (or (js/isNaN value) (neg? value)) (swap! appstate/app assoc :next-n value)))}]
   [lister (reverse (:events @appstate/app)) appstate/selected-event-types]])

(defn run []
  (reagent/render-component (fn [] [inspect-view])
                            (by-id "code")))

(run)
