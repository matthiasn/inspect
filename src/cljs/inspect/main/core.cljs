(ns inspect.main.core
  (:require [inspect.specs.specs]
            [inspect.main.log]
            [taoensso.timbre :as timbre :refer-macros [info error]]
            [matthiasn.systems-toolbox-electron.ipc-main :as ipc]
            [matthiasn.systems-toolbox-electron.window-manager :as wm]
            [matthiasn.systems-toolbox.scheduler :as sched]
            [matthiasn.systems-toolbox.switchboard :as sb]
            [inspect.main.menu :as menu]
            [inspect.main.update :as upd]
            [inspect.main.graphviz :as gv]
            [inspect.main.reader :as kafka]
            [inspect.main.download :as dl]
            [inspect.main.store :as st]
            [inspect.main.sled :as db]
            [inspect.main.startup :as startup]
            [electron :refer [app]]
            [cljs.nodejs :as nodejs :refer [process]]
            [inspect.main.runtime :as rt]))

(when-not (aget js/goog "global" "setTimeout")
  (info "goog.global.setTimeout not defined - let's change that")
  (aset js/goog "global" "setTimeout" js/setTimeout))

(defonce switchboard (sb/component :electron/switchboard))

(def wm-relay #{:exec/js
                :import/listen
                :reader/status
                :reader/files
                :subscription/match
                :spec/error
                :update/status
                :sled/res
                :svg/overview
                :observer/cmps-msgs})

(def app-path (:app-path rt/runtime-info))

(defn start []
  (info "Starting CORE:" (.-resourcesPath process))
  (sb/send-mult-cmd
    switchboard
    [[:cmd/init-comp #{(wm/cmp-map :electron/window-manager wm-relay app-path)
                       (kafka/cmp-map :electron/kafka-cmp)
                       (st/cmp-map :electron/store-cmp)
                       (db/cmp-map :electron/db-cmp)
                       (gv/cmp-map :electron/graphviz)
                       (dl/cmp-map :electron/download-cmp)
                       (ipc/cmp-map :electron/ipc-cmp)
                       (startup/cmp-map :electron/startup-cmp)
                       (upd/cmp-map :electron/update-cmp)
                       (sched/cmp-map :electron/scheduler-cmp)
                       (menu/cmp-map :electron/menu-cmp)}]

     [:cmd/route {:from :electron/menu-cmp
                  :to   #{:electron/window-manager
                          :electron/kafka-cmp
                          :electron/download-cmp
                          :electron/startup-cmp
                          :electron/update-cmp}}]

     [:cmd/route {:from #{:electron/scheduler-cmp}
                  :to   #{:electron/update-cmp
                          :electron/store-cmp
                          :electron/kafka-cmp}}]

     [:cmd/route {:from :electron/update-cmp
                  :to   #{:electron/window-manager
                          :electron/scheduler-cmp}}]

     [:cmd/route {:from :electron/ipc-cmp
                  :to   #{:electron/store-cmp
                          :electron/kafka-cmp
                          :electron/db-cmp
                          :electron/window-manager
                          :electron/graphviz
                          :electron/update-cmp}}]

     [:cmd/route {:from :electron/scheduler-cmp
                  :to   :electron/kafka-cmp}]

     [:cmd/route {:from :electron/graphviz
                  :to   :electron/window-manager}]

     [:cmd/route {:from #{:electron/kafka-cmp
                          :electron/db-cmp
                          :electron/scheduler-cmp}
                  :to   #{:electron/store-cmp
                          :electron/window-manager}}]

     [:cmd/route {:from :electron/store-cmp
                  :to   #{:electron/window-manager
                          :electron/graphviz
                          :electron/db-cmp}}]

     [:cmd/send {:to  :electron/window-manager
                 :msg [:window/new {:url (:index-page rt/runtime-info)}]}]

     [:cmd/send {:to  :electron/scheduler-cmp
                 :msg [:cmd/schedule-new {:timeout 2000
                                          :message [:state/publish]
                                          :repeat  true}]}]

     [:cmd/send {:to  :electron/scheduler-cmp
                 :msg [:cmd/schedule-new {:timeout (* 24 60 60 1000)
                                          :message [:update/auto-check]
                                          :repeat  true
                                          :initial true}]}]]))

(.on app "ready" start)
(.on app "uncaughtException" #(error "uncaughtException" %))
