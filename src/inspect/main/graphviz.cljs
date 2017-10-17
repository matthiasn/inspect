(ns inspect.main.graphviz
  (:require [viz.js :as Viz]
            [fs :refer [writeFileSync]]))

(defn sanitize [cmp-id] (str "\"" cmp-id "\""))

(defn sub-graph [[label nodes]]
  (let [cluster-name (str "cluster_" label)]
    (str "subgraph " cluster-name " { label =< <B>" label " </B>>; "
         (apply str (map (fn [n] (str n "; ")) nodes))
         "} ")))

(defn gen-svg [cmp-ids edges active-type]
  (let [edge-mapper (fn edge-mapper [{:keys [source target msg-type]}]
                      (str (sanitize source) " -> " (sanitize target)
                           (if (= msg-type active-type)
                             " [color = red penwidth=4]"
                             (when active-type
                               " [color = lightgrey]"))
                           "; "))
        links (apply str (map edge-mapper edges))
        clusters
        (reduce
          (fn [acc in]
            (let [{:keys [source-ns source target-ns target]} in]
              (-> acc
                  (update-in [source-ns] #(set (conj % (sanitize source))))
                  (update-in [target-ns] #(set (conj % (sanitize target)))))))
          {}
          edges)
        sub-graphs (map sub-graph clusters)
        digraph (str "digraph { " links (apply str sub-graphs) "}")
        svg (Viz digraph)]
    (writeFileSync "/tmp/inspect.dot" digraph "utf-8")
    (writeFileSync "/tmp/inspect.svg" svg "utf-8")
    svg))

(defn generate-svg [{:keys [current-state msg-payload put-fn]}]
  (let [new-state (assoc-in current-state [:prev] msg-payload)]
    (when-not (= (:prev current-state) msg-payload)
      (let [{:keys [cmp-ids edges]} msg-payload
            active-type (:active-type current-state)
            svg (gen-svg cmp-ids edges active-type)]
        {:new-state new-state
         :emit-msg  [:svg/overview svg]}))))

(defn set-active [{:keys [current-state msg-payload]}]
  (let [{:keys [cmp-ids edges]} (:prev current-state)
        active-type (when-not (= msg-payload (:active-type current-state))
                      msg-payload)
        new-state (assoc-in current-state [:active-type] active-type)
        svg (gen-svg cmp-ids edges active-type)]
    {:new-state new-state
     :emit-msg  [:svg/overview svg]}))

(defn cmp-map [cmp-id]
  {:cmp-id      cmp-id
   :handler-map {:svg/gen-overview generate-svg
                 :svg/set-active   set-active}})
