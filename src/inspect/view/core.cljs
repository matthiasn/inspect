(ns inspect.view.core
  (:require [inspect.specs.specs]
            [inspect.view.log]
            [matthiasn.systems-toolbox-electron.ipc-renderer :as ipc]
            [inspect.view.store :as st]
            [inspect.view.ui :as ui]
            [matthiasn.systems-toolbox.switchboard :as sb]
            [matthiasn.systems-toolbox.scheduler :as sched]
            [taoensso.timbre :as timbre :refer-macros [info debug]]))

(defonce switchboard (sb/component :updater/switchboard))

(def relay-types #{:update/check
                   :update/check-beta
                   :update/download
                   :update/install
                   :sled/get
                   :sled/put
                   :sled/bench
                   :svg/set-active
                   :kafka/start
                   :kafka/stop
                   :kafka/get-hosts
                   :observer/subscribe
                   :observer/stop
                   :window/close})

(defn start []
  (info "Starting OBSERVER")
  (sb/send-mult-cmd
    switchboard
    [[:cmd/init-comp #{(ipc/cmp-map :observer/ipc relay-types)
                       (st/cmp-map :observer/store)
                       (sched/cmp-map :observer/scheduler)
                       (ui/cmp-map :observer/ui)}]

     [:cmd/route {:from :observer/ipc
                  :to   #{:observer/ui
                          :observer/store}}]

     [:cmd/route {:from :observer/ui
                  :to   #{:observer/ipc
                          :observer/store}}]

     [:cmd/route {:from :observer/scheduler
                  :to   :observer/ipc}]

     [:cmd/observe-state {:from :observer/store
                          :to   :observer/ui}]

     [:cmd/send {:to  :observer/ipc
                 :msg [:kafka/get-hosts]}]]))

(.addEventListener js/window "load" #(start))
