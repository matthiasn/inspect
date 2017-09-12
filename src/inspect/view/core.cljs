(ns inspect.view.core
  (:require [inspect.view.log]
            [inspect.view.ipc :as ipc]
            [inspect.view.store :as st]
            [inspect.view.ui :as ui]
            [electron :refer [ipcRenderer]]
            [matthiasn.systems-toolbox.switchboard :as sb]
            [taoensso.timbre :as timbre :refer-macros [info debug]]))

(defonce switchboard (sb/component :updater/switchboard))

(defn start []
  (info "Starting OBSERVER")
  (debug "Starting OBSERVER")
  (sb/send-mult-cmd
    switchboard
    [[:cmd/init-comp #{(ipc/cmp-map :observer/ipc-cmp #{:update/check
                                                        :update/check-beta
                                                        :update/download
                                                        :update/install
                                                        :kafka/start
                                                        :kafka/stop
                                                        :window/close})
                       (st/cmp-map :observer/store)
                       (ui/cmp-map :observer/ui-cmp)}]

     [:cmd/route {:from :observer/ipc-cmp
                  :to   #{:observer/ui-cmp
                          :observer/store}}]

     [:cmd/route {:from :observer/ui-cmp
                  :to   #{:observer/ipc-cmp
                          :observer/store}}]

     [:cmd/observe-state {:from :observer/store
                          :to   :observer/ui-cmp}]]))


(defn load-handler [ev]
  (start))


(.addEventListener js/window "load" load-handler)
