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
  (comm/chsk-send! [:cmd/get-next {:next-n 10}]))

(defn lister [items]
  [:div
   (for [item items]
     [:pre [:code (:pp item)]])])

(defn lister-user []
  [:div
   [:button#get-next {:on-click get-next} "Next"]
   [lister (reverse (:events @appstate/app))]])

(defn run []
  (reagent/render-component (fn [] [lister-user])
                            (by-id "code")))

(run)
