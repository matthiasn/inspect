(ns inspect.view.util
  (:require [randomcolor]))

(defn random-color [seed]
  (randomcolor (clj->js {:seed (str (hash (str seed)))})))