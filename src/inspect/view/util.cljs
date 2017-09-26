(ns inspect.view.util
  (:require [randomcolor]))

(defn random-color [seed]
  (randomcolor (clj->js {:seed (str (hash (str seed)))})))


;; from https://github.com/bluemont/con-hash/blob/master/src/con_hash/core.clj
(defn clockwise-node
  "Returns the next clockwise node starting from hashed-item. The
  term 'clockwise' means 'greater than with wrap-around'."
  [hashed-item hashed-nodes]
  {:pre [(map? hashed-nodes) (sorted? hashed-nodes)]}
  (let [kv (->> hashed-nodes
                (filter (fn [[k v]] (< hashed-item k)))
                first)]
    (second (or kv (first hashed-nodes)))))

(defn hash-nodes
  "Returns a sorted map (where the key is a hash and the value is the
  original node) from a list of nodes and node hashing function."
  [nodes hash-fn]
  (->> nodes
       (map (fn [n] [(hash-fn n) n]))
       (into (sorted-map))))

(defn consistent-hash-fn
  "Returns a consistent hash function for a given item hashing
  function and node hashing function. The returned function accepts
  an item and sequence of nodes and returns one of the nodes."
  [item-hash-fn node-hash-fn]
  {:pre [(fn? item-hash-fn) (fn? node-hash-fn)]}
  (fn [item nodes]
    (let [hashed-nodes (hash-nodes nodes node-hash-fn)]
      (clockwise-node (item-hash-fn item) hashed-nodes))))

(def colors
  #{"aliceblue" "aqua" "aquamarine" "bisque" "black" "blue"
    "blueviolet" "brown" "burlywood" "cadetblue" "chocolate" "coral"
    "crimson" "cyan" "darkblue" "darkcyan"
    "darkgoldenrod" "darkgreen" "darkgrey" "darkkhaki" "darkmagenta"
    "darkolivegreen " "darkorange" "darkorchid" "darkred" "darksalmon"
    "darkseagreen" "darkslateblue" "darkturquoise" "darkviolet"
    "deeppink" "deepskyblue" "dimgray" "dimgrey" "dodgerblue"
    "firebrick" "forestgreen" "fuchsia" "gold "
    "goldenrod" "green" "hotpink" "indianred"
    "indigo" "khaki" "lavender" "lavenderblush" "lawngreen" "limegreen"
    "magenta" "maroon" "mediumaquamarine" "mediumblue"
    "mediumorchid " "mediumpurple" "mediumseagreen" "mediumslateblue"
    "mediumspringgreen" "mediumturquoise " "mediumvioletred" "midnightblue"
    "navy" "orange" "orangered" "orchid" "plum" "powderblue" "purple"
    "red" "rosybrown" "royalblue" "salmon" "seagreen" "sienna" "skyblue"
    "slateblue" "springgreen" "steelblue" "teal" "tomato" "turquoise"})
