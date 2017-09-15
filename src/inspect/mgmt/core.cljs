(ns inspect.update.core
  (:require [inspect.specs.specs]
            [inspect.update.log :as log]
            [taoensso.timbre :as timbre :refer-macros [info]]
            [inspect.update.ipc :as ipc]
            [inspect.update.ui :as ui]
            [electron :refer [ipcRenderer]]
            [matthiasn.systems-toolbox.switchboard :as sb]))

(defonce switchboard (sb/component :mgmt/switchboard))

(defn start []
  (info "Starting UPDATER")
  (sb/send-mult-cmd
    switchboard
    [[:cmd/init-comp #{(ipc/cmp-map :mgmt/ipc-cmp #{:update/check
                                                    :update/check-beta
                                                    :update/download
                                                    :update/install
                                                    :window/close})
                       (ui/cmp-map :mgmt/ui-cmp)}]

     [:cmd/route {:from :mgmt/ipc-cmp
                  :to   #{:mgmt/ui-cmp}}]

     [:cmd/route {:from :mgmt/ui-cmp
                  :to   #{:mgmt/ipc-cmp}}]]))

(.addEventListener js/window "load" #(start))
