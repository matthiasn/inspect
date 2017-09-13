(ns inspect.view.graphviz
  (:require [viz.js :as Viz]
            [re-frame.core :refer [subscribe]]
            [taoensso.timbre :as timbre :refer-macros [info debug]]
            [randomcolor]
            [clojure.string :as str]))

(defn by-id [id] (.getElementById js/document id))

(defn sanitize [cmp-id] (str "\"" cmp-id "\""))

(defn sub-graph [[label nodes]]
  (let [cluster-name (str "cluster_" label)]
    (str "subgraph " cluster-name " { label =< <B>" label " </B>>; "
         (apply str (map (fn [n] (str n "; ")) nodes))
         "} ")))

(defn wiring [put-fn]
  (let [cmp-ids (subscribe [:cmp-ids])
        edges (subscribe [:edges])
        active-type (subscribe [:active-type])
        edge-mapper (fn edge-mapper [{:keys [source target msg-type]}]
                      (str (sanitize source) " -> " (sanitize target)
                           (if (= msg-type @active-type)
                             " [color = red penwidth=4]"
                             (when @active-type
                               " [color = lightgrey]"))
                           "; "))]
    (fn [put-fn]
      (let [links (map edge-mapper @edges)
            ;links (set links)
            links (apply str links)
            clusters (reduce
                       (fn [acc in]
                         (let [{:keys [source-ns source target-ns target]} in]
                           (-> acc
                               (update-in [source-ns] #(set (conj % (sanitize source))))
                               (update-in [target-ns] #(set (conj % (sanitize target)))))))
                       {}
                       @edges)
            sub-graphs (map sub-graph clusters)
            digraph (str "digraph { " links (apply str sub-graphs) "}")
            svg-elem (by-id "graphviz")
            svg (Viz digraph)]
        ;(info "clusters" clusters)
        ;(info "sub-graphs" sub-graphs)
        (debug digraph)
        (aset svg-elem "innerHTML" svg)
        [:div]))))
