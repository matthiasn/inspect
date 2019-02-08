(ns inspect.view.force
  (:require [d3 :as d3]
            [d3-force :refer [forceSimulation forceLink forceManyBody
                              forceCenter forceCollide]]
            [re-frame.core :refer [subscribe]]
            [taoensso.timbre :as timbre :refer-macros [info debug]]
            [randomcolor]
            [reagent.core :as r]
            [inspect.view.util :as u]
            [clojure.set :as set]))

(defn by-id [id] (.getElementById js/document id))

(defn components-force
  [canvas nodes links put-fn]
  (let [context (.getContext canvas "2d")
        width (.-width canvas)
        height (.-height canvas)
        sim (-> (forceSimulation)
                (.force "link" (-> (forceLink)
                                   (.id (fn [d] (.-id d)))
                                   (.strength 0.1)))
                (.force "charge" (forceManyBody))
                (.force "collide" (-> (forceCollide)
                                      (.radius (fn [d]
                                                 (+ (.-r d) 25)
                                                 35))
                                      (.iterations 20)))
                (.force "center" (forceCenter (/ width 2) (/ height 2))))
        draw-node (fn [d]
                    (let [cmp-id (.-id d)
                          color (u/random-color cmp-id)]
                      (.beginPath context)
                      (.moveTo context (.-x d) (.-y d))
                      (.arc context (.-x d) (.-y d) 15 0 (* 2 3.1415))
                      (.stroke context)
                      (aset context "fillStyle" color)
                      (.fill context)))
        draw-link (fn [d]
                    (.beginPath context)
                    (.moveTo context (.-x (.-source d)) (.-y (.-source d)))
                    (.lineTo context (.-x (.-target d)) (.-y (.-target d)))
                    (.stroke context))
        ticked (fn [d]
                 (.clearRect context 0 0 width height)
                 (doseq [link links]
                   (draw-link link))
                 (doseq [node nodes]
                   (draw-node node)))
        run (fn [nodes links]
              (-> sim
                  (.nodes nodes)
                  (.on "tick" ticked)
                  (.force "link")
                  (.links links)))]
    (run nodes links)
    {:run run :sim sim}))

(defn components-force-layout [_ put-fn]
  (let [render-fn (fn [] [:canvas#force {:width 600 :height 300}])
        did-mount (fn [this])
        did-update (fn [this]
                     (let [elem (by-id "force")
                           [_ nodes links] (r/argv this)]
                       ;(.redraw (:word-cloud @local) (clj->js words))
                       (components-force elem nodes links put-fn)))]
    (r/create-class
      {:reagent-render       render-fn
       :component-did-mount  did-mount
       :component-did-update did-update
       :display-name         "components-force-layout"})))

(defn wiring [put-fn]
  (let [cmp-ids (subscribe [:cmp-ids])
        edges (subscribe [:edges])]
    (fn [put-fn]
      (let [links (clj->js (vec @edges))
            sources (set (mapv :source @edges))
            targets (set (mapv :target @edges))
            nodes-set (set/union sources targets)
            nodes (clj->js (mapv (fn [id] {:id (str id)}) nodes-set))]
        [:div.force-wrapper
         [components-force-layout nodes links put-fn]
         [:ul
          (for [node nodes-set]
            ^{:key (str node)}
            [:li
             [:span.color {:style {:background-color (u/random-color node)}}]
             node])]]))))
