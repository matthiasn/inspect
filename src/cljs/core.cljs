(ns inspect.core
  (:require-macros [cljs.core.async.macros :refer [go-loop go alt!]])
  (:require [inspect.communicator :as comm]
            [inspect.appstate :as appstate]
            [cljs.core.async :as async :refer [<! >! chan put! alts! timeout]]))

(enable-console-print!)

;;;; Main file of the inspect tool written in ClojureScript

