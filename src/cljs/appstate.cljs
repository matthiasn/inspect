(ns inspect.appstate
  (:require-macros [cljs.core.async.macros :refer [go-loop go alt!]])
  (:require [cljs.core.async :as async :refer [<!]]))

(enable-console-print!)

;;; Application state in a single atom
;;; Will be initialized with the map returned by util/initial-state.
;;; Reset to a new clean slate when a new search is started.
(def app (atom {}))
