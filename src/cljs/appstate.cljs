(ns inspect.appstate
  (:require-macros [cljs.core.async.macros :refer [go-loop go alt!]])
  (:require [cljs.core.async :as async :refer [<!]]
            [reagent.core :as reagent :refer [atom]]))

(enable-console-print!)

;;; Application state in reagent's watched atoms

(def app (atom {:events [] :next-n 10}))

(def known-event-types (atom #{}))
(def selected-event-types (atom #{}))

