(ns inspect.main.update-window
  (:require [clojure.string :as str]
            [taoensso.timbre :as timbre :refer-macros [info debug]]
            [electron :refer [app BrowserWindow ipcMain]]
            [matthiasn.systems-toolbox.switchboard :as sb]
            [matthiasn.systems-toolbox.component :as stc]
            [cljs.nodejs :as nodejs :refer [process]]
            [cljs.reader :refer [read-string]]
            [clojure.pprint :as pp]
            [inspect.main.runtime :as rt]))

(defn updater-window
  [{:keys [current-state cmp-state put-fn]}]
  (when-let [existing (:updater-window current-state)]
    (.close existing))
  (let [window (BrowserWindow. (clj->js {:width 600 :height 300}))
        url (str "file://" (:app-path rt/runtime-info) "/updater.html")
        new-state (-> current-state
                      (assoc-in [:updater-window] window)
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

(defn relay-msg
  [{:keys [current-state msg-type msg-meta msg-payload]}]
  (when-let [updater-window (:updater-window current-state)]
    (let [web-contents (.-webContents updater-window)
          serializable [msg-type {:msg-payload msg-payload :msg-meta msg-meta}]]
      ;(debug "Relaying" (str msg-type) (str msg-payload))
      (.send web-contents "relay" (pr-str serializable))))
  {})

(defn close-window
  [{:keys [current-state]}]
  (info "Closing Updater Window")
  (when-let [updater-window (:updater-window current-state)]
    (when (:active current-state)
      (.close updater-window)
      (info "Closed Updater Window")))
  {})

(defn state-fn
  [put-fn]
  (let [state (atom {})
        relay (fn [ev m]
                (let [parsed (read-string m)
                      msg-type (first parsed)
                      {:keys [msg-payload msg-meta]} (second parsed)
                      msg (with-meta [msg-type msg-payload] msg-meta)]
                  (info "Update IPC relay:" (with-out-str (pp/pprint msg)))
                  (if (= msg-type :window/close)
                    (close-window {:current-state @state})
                    (put-fn msg))))]
    (.on ipcMain "relay" relay)
    {:state state}))

(defn cmp-map
  [cmp-id]
  (let [relay-types #{:update/status}
        relay-map (zipmap relay-types (repeat relay-msg))]
    {:cmp-id      cmp-id
     :state-fn    state-fn
     :handler-map (merge relay-map
                         {:window/updater updater-window
                          :window/close   close-window})}))
