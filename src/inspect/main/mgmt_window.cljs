(ns inspect.main.mgmt-window
  (:require [clojure.string :as str]
            [taoensso.timbre :as timbre :refer-macros [info debug]]
            [electron :refer [app BrowserWindow ipcMain]]
            [matthiasn.systems-toolbox.switchboard :as sb]
            [matthiasn.systems-toolbox.component :as stc]
            [cljs.nodejs :as nodejs :refer [process]]
            [cljs.reader :refer [read-string]]
            [clojure.pprint :as pp]
            [inspect.main.runtime :as rt]))

(defn mgmt-window [{:keys [current-state cmp-state]}]
  (when-let [existing (:updater-window current-state)]
    (.close existing))
  (let [window (BrowserWindow. (clj->js {:width 600 :height 300}))
        url (str "file://" (:app-path rt/runtime-info) "/mgmt.html")
        new-state (-> current-state
                      (assoc-in [:mgmt-window] window)
                      (assoc-in [:active] true))
        close (fn [_]
                (info "Closed updater-window")
                (swap! cmp-state assoc-in [:updater-window] nil))
        focus (fn [_]
                (info "Focused updater-window")
                (swap! cmp-state assoc-in [:active] true))
        blur (fn [_]
               (info "Blurred updater-window")
               (swap! cmp-state assoc-in [:active] false))]
    (info "Opening new updater window" url)
    (.loadURL window url)
    (.on window "focus" focus)
    (.on window "blur" blur)
    (.on window "close" close)
    {:new-state new-state}))

(defn relay-msg [{:keys [current-state msg-type msg-meta msg-payload]}]
  (when-let [updater-window (:updater-window current-state)]
    (let [web-contents (.-webContents updater-window)
          serializable [msg-type {:msg-payload msg-payload :msg-meta msg-meta}]]
      (debug "Relaying" (str msg-type) (str msg-payload))
      (.send web-contents "relay" (pr-str serializable))))
  {})

(defn close-window [{:keys [current-state]}]
  (info "Closing Updater Window")
  (when-let [mgmt-window (:mgmt-window current-state)]
    (when (:active current-state)
      (.close mgmt-window)
      (info "Closed Updater Window")))
  {})

(defn cmp-map [cmp-id]
  (let [relay-types #{:update/status}
        relay-map (zipmap relay-types (repeat relay-msg))]
    {:cmp-id      cmp-id
     :handler-map (merge relay-map
                         {:mgmt/open  mgmt-window
                          :mgmt/close close-window})}))
