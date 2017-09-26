(ns inspect.view.graphviz
  (:require-macros [reagent.ratom :refer [reaction]])
  (:require [viz.js :as Viz]
            [re-frame.core :refer [subscribe]]
            [taoensso.timbre :as timbre :refer-macros [info debug]]
            [randomcolor]
            [clojure.string :as str]
            [reagent.core :as r]))

(defn by-id [id] (.getElementById js/document id))

(defn sanitize [cmp-id] (str "\"" cmp-id "\""))

(defn sub-graph [[label nodes]]
  (let [cluster-name (str "cluster_" label)]
    (str "subgraph " cluster-name " { label =< <B>" label " </B>>; "
         (apply str (map (fn [n] (str n "; ")) nodes))
         "} ")))

(defn inner-wiring-view [_ put-fn]
  (info :inner-wiring-view)
  (let [render-fn (fn [] (info :reagent-render) [:div#graphviz1])
        did-mount (fn [] (info :component-did-mount))
        did-update (fn [this]
                     (let [[_ digraph] (r/argv this)
                           svg-elem (by-id "graphviz1")
                           svg (Viz digraph)]
                       (aset svg-elem "innerHTML" svg)))]
    (r/create-class
      {:reagent-render       render-fn
       :component-did-mount  did-mount
       :component-did-update did-update
       :display-name         "wiring-view"})))

(defn wiring-view [_put-fn]
  (let [cmp-ids (subscribe [:cmp-ids])
        edges (subscribe [:edges])
        active-type (subscribe [:active-type])
        edge-mapper (fn edge-mapper [{:keys [source target msg-type]}]
                      (str (sanitize source) " -> " (sanitize target)
                           (if (= msg-type @active-type)
                             " [color = red penwidth=4]"
                             (when @active-type
                               " [color = lightgrey]"))
                           "; "))
        links (reaction (apply str (map edge-mapper @edges)))
        clusters (reaction
                   (reduce
                     (fn [acc in]
                       (let [{:keys [source-ns source target-ns target]} in]
                         (-> acc
                             (update-in [source-ns] #(set (conj % (sanitize source))))
                             (update-in [target-ns] #(set (conj % (sanitize target)))))))
                     {}
                     @edges))
        sub-graphs (reaction (map sub-graph @clusters))
        digraph (reaction (str "digraph { " @links (apply str @sub-graphs) "}"))]
    (fn [put-fn]
      [:div
       [inner-wiring-view @digraph put-fn]])))
