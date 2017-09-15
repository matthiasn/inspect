(ns inspect.view.core
  (:require [inspect.specs.specs]
            [inspect.view.log]
            [matthiasn.systems-toolbox-electron.ipc-renderer :as ipc]
            [inspect.view.store :as st]
            [inspect.view.ui :as ui]
            [matthiasn.systems-toolbox.switchboard :as sb]
            [taoensso.timbre :as timbre :refer-macros [info debug]]))

(defonce switchboard (sb/component :updater/switchboard))

(def relay-types #{:update/check
                   :update/check-beta
                   :update/download
                   :update/install
                   :kafka/start
                   :kafka/stop
                   :observer/subscribe
                   :observer/stop
                   :window/close})

(defn start []
  (info "Starting OBSERVER")
  (sb/send-mult-cmd switchboard
    [[:cmd/init-comp #{(ipc/cmp-map :observer/ipc-cmp relay-types)
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

(.addEventListener js/window "load" #(start))
