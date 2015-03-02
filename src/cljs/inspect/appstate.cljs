(ns inspect.appstate
  (:require [cljs.core.async :refer [<!]]
            [reagent.core :refer [atom]]))

;;; Application state in reagent's watched atoms

(def app (atom {:events [] :next-n 10}))

(def stats (atom {}))
(def selected (atom #{}))
(def client-map (atom {}))

