(ns inspect.view.force2
  (:require [d3 :as d3]
            [d3-ellipse-force :refer [ellipseForce]]
            [d3-force :refer [forceSimulation forceLink forceCenter]]
            [re-frame.core :refer [subscribe]]
            [taoensso.timbre :as timbre :refer-macros [info debug]]
            [randomcolor]
            [reagent.core :as r]
            [inspect.view.util :as u]
            [clojure.set :as set]
            [reagent.core :as rc]))

(defn components-force
  [nodes links put-fn]
  (let [svg (d3/select "#force2")
        width (js/parseInt (.attr svg "width"))
        height (js/parseInt (.attr svg "height"))
        color (d3/scaleOrdinal d3/schemeCategory20)
        sim (-> (forceSimulation)
                (.force "link" (-> (forceLink)
                                   (.id (fn [d] (.-id d)))
                                   (.strength 0.15)))
                (.force "charge" (ellipseForce 6 1.5 6))
                (.force "collide" (ellipseForce 6 1.5 6))
                (.force "center" (forceCenter (/ width 2) (/ height 2))))
        link (-> svg
                 (.append "g")
                 (.attr "class" "link")
                 (.selectAll "line")
                 (.data links)
                 (.enter)
                 (.append "line")
                 (.attr "stroke-width" (fn [d] 2))
                 (.attr "stroke" (fn [d] "#999")))

        drag-started (fn [d]
                       (let [active (.-active d3/event)
                             x (.-x d3/event)
                             y (.-y d3/event)]
                         ;(when-not)
                         (-> sim
                             (.alphaTarget 0.3)
                             (.restart))
                         (info "drag-started" x y active)
                         (aset d "fx" (.-x d))
                         (aset d "fy" (.-y d)))
                       )
        dragged (fn [d]
                  (let [active (.-active d3/event)
                        x (.-x d3/event)
                        y (.-y d3/event)]
                    ;(info "drag" x y active)
                    (aset d "fx" x)
                    (aset d "x" x)
                    (aset d "fy" y)))
        drag-end (fn [d]
                       (info "drag-end" d)
                   (-> sim
                       (.alphaTarget 0))
                       (aset d "fx" nil)
                       (aset d "fy" nil))
        node (-> svg
                 (.append "g")
                 (.attr "class" "node")
                 (.selectAll "ellipse")
                 (.data nodes (fn [d] (.-id d)))
                 (.enter)
                 (.append "ellipse")
                 (.attr "rx" (fn [d] (.-rx d)))
                 (.attr "ry" (fn [d] (.-ry d)))
                 (.attr "fill" (fn [d] (u/random-color (.-id d))))
                 (.call (-> (d3/drag)
                            (.on "start" drag-started)
                            (.on "drag" dragged)
                            (.on "end" drag-end))))
        text (-> svg
                 (.append "g")
                 (.attr "class" "labels")
                 (.selectAll "text")
                 (.data nodes)
                 (.enter)
                 (.append "text")
                 (.attr "dy" 2)
                 (.attr "text-anchor" "middle")
                 (.attr "stroke" (fn [d] "#444"))
                 (.attr "fill" "white")
                 (.text (fn [d] (.-id d))))
        ticked (fn [d]
                 (-> link
                     (.attr "x1" (fn [d] (aget d "source" "x")))
                     (.attr "y1" (fn [d] (aget d "source" "y")))
                     (.attr "x2" (fn [d] (aget d "target" "x")))
                     (.attr "y2" (fn [d] (aget d "target" "y"))))
                 (-> node
                     (.attr "cx" (fn [d] (.-x d)))
                     (.attr "cy" (fn [d] (.-y d))))
                 (-> text
                     (.attr "x" (fn [d] (.-x d)))
                     (.attr "y" (fn [d] (.-y d)))))
        run (fn [nodes links]
              (-> sim
                  (.nodes nodes)
                  (.on "tick" ticked)
                  (.force "link")
                  (.links links)))]
    (run nodes links)
    {:run run :sim sim}))

(defn components-force-layout [_ put-fn]
  (let [local (rc/atom {})
        render-fn (fn [] [:svg#force2 {:width 900 :height 500}])
        did-mount (fn [this]
                    (let [[_ nodes links] (r/argv this)
                          force-layout (components-force nodes links put-fn)]
                      (info "did-mount")
                      (reset! local force-layout)))
        did-update (fn [this]
                     (let [run (:run @local)
                           [_ nodes links] (r/argv this)]
                       (info "did-update")
                       (components-force nodes links put-fn)
                       (run nodes links)))]
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
            nodes (clj->js (mapv (fn [id]
                                   {:id    (str id)
                                    :rx    (* 5 (count (str id)))
                                    :ry    12
                                    :group (namespace (keyword id))})
                                 nodes-set))]
        [:div                                               ;.force-wrapper
         [components-force-layout nodes links put-fn]
         [:ul
          (for [node nodes-set]
            ^{:key (str node)}
            [:li
             [:span.color {:style {:background-color (u/random-color node)}}]
             node])]]))))
