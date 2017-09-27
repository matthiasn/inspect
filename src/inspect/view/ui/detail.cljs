(ns inspect.view.ui.detail
  "Slightly modified from https://github.com/kamituel/systems-toolbox-chrome"
  (:require-macros [reagent.ratom :refer [reaction]])
  (:require [re-frame.core :refer [subscribe]]
            [taoensso.timbre :as timbre :refer-macros [info debug]]
            [inspect.view.util :as u]
            [cljs.pprint :as pp]
            [reagent.core :as r]))

(defn data->hiccup
  "Converts an arbitrary EDN data structure to the HTML where each element (i.e. map, vector,
  sequence, number, string) are wrapped in DOM elements such as DIV's or SPAN's so they are
  easy to style using CSS."
  ([data expanded-path on-expand-fn]
   (data->hiccup data expanded-path on-expand-fn []))
  ([data expanded-path on-expand-fn current-path]
   (let [key-to-expand (first expanded-path)
         handle-coll (fn [v expand-key]
                       (if (or (not (coll? v)) (= key-to-expand expand-key))
                         [:div (data->hiccup v (rest expanded-path) on-expand-fn (conj current-path expand-key))]
                         [:div.collapsed
                          {:on-click (on-expand-fn (conj current-path expand-key))}
                          (data->hiccup (empty v) expanded-path on-expand-fn [])]))]
     (cond
       (map? data)
       [:div.map (for [[k v] data]
                   ^{:key (hash (conj current-path k))}
                   [:div.key-val
                    [:div (data->hiccup k expanded-path on-expand-fn (conj current-path k))]
                    (handle-coll v k)])]

       (vector? data)
       [:div.vector (for [[idx v]
                          (map-indexed (fn [idx v] [idx v]) data)]
                      ^{:key (hash (conj current-path idx))}
                      [:div (handle-coll v idx)])]

       (seq? data)
       [:div.seq (for [[idx v] (map-indexed (fn [idx v] [idx v]) data)]
                   ^{:key (hash (conj current-path idx))}
                   [:div (handle-coll v idx)])]

       (string? data)
       [:span.string data]

       (number? data)
       [:span.number data]

       (keyword? data)
       [:span.keyword (str data)]

       (nil? data)
       [:span.nil "nil"]

       (or (true? data) (false? data))
       [:span.boolean (str data)]

       :else
       (str data)))))

(defn edn-tree [data local k]
  [:div.edn-tree.light
   (data->hiccup data (k @local)
                 (fn [path]
                   (fn [_]
                     (swap! local assoc k path))))])

(defn detailed-msg [put-fn]
  (let [detailed-msg (subscribe [:detailed-msg])
        local (r/atom {})]
    (fn [put-fn]
      (when-let [detailed-msg @detailed-msg]
        (let [{:keys [msg msg-meta]} detailed-msg]
          [:div.section
           [:div.value.block [:h3 "Message payload"]]
           [edn-tree (second msg) local :expanded-body]
           [:div.value.block [:h3 "Message meta"]]
           [edn-tree msg-meta local :expanded-meta]])))))
