(ns inspect.main.startup
  (:require [taoensso.timbre :as timbre :refer-macros [info debug]]
            [electron :refer [app session]]
            [inspect.main.runtime :as rt]
            [cljs.nodejs :as nodejs :refer [process]]))

(defn shutdown
  [{:keys []}]
  (info "Shutting down")
  (.quit app)
  {})

(defn clear-cache
  [{:keys []}]
  (info "Clearing Electron Cache")
  (let [session (.-defaultSession session)]
    (.clearCache session #(info "Electron Cache Cleared")))
  {})

(defn cmp-map
  [cmp-id]
  {:cmp-id      cmp-id
   :handler-map {:app/shutdown    shutdown
                 :app/clear-cache clear-cache}})

(.on app "window-all-closed"
     (fn [ev]
       (info "window-all-closed")
       (when-not (= (:platform rt/runtime-info) "darwin")
         (.quit app))))
