(ns inspect.core
  (:require [inspect.communicator :as comm]
            [inspect.appstate :as appstate]
            [reagent.core :as reagent :refer [atom]]
            [cljs.core.async :as async :refer [<! >! chan put! alts! timeout]]))

(enable-console-print!)

;;;; Main file of the inspect tool written in ClojureScript

(defn by-id [id] (.getElementById js/document id))

(defn get-next
  []
  (comm/chsk-send! [:cmd/get-next {:next-n (:next-n @appstate/app)}]))

(defn lister [items]
  [:div
   (for [item items]
     [:div
      [:h4 (str (:origin item))]
      [:p (:received item)]
      [:pre [:code (:payload item)]]
      [:br]])])

(defn lister-user []
  [:div
   [:button#get-next {:on-click get-next} "Next"]
     [:input {:type "number"
           :value (:next-n @appstate/app)
           :on-change #(let [value (js/parseInt (-> % .-target .-value))]
                        (when-not (or (js/isNaN value) (neg? value)) (swap! appstate/app assoc :next-n value)))}]
   [lister (reverse (:events @appstate/app))]])

(defn run []
  (reagent/render-component (fn [] [lister-user])
                            (by-id "code")))

(run)
