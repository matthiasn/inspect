(ns inspect.main.window-manager
  (:require [clojure.string :as str]
            [taoensso.timbre :as timbre :refer-macros [info debug]]
            [inspect.main.runtime :as rt]
            [electron :refer [app BrowserWindow ipcMain]]
            [matthiasn.systems-toolbox.switchboard :as sb]
            [matthiasn.systems-toolbox.component :as stc]
            [cljs.nodejs :as nodejs :refer [process]]
            [cljs.reader :refer [read-string]]
            [clojure.pprint :as pp]))

(defn new-window [{:keys [current-state cmp-state msg-payload]}]
  (let [{:keys [url]} msg-payload
        window (BrowserWindow. (clj->js {:width  1200
                                         :height 800
                                         :show   false}))
        window-id (stc/make-uuid)
        show #(.show window)
        url (str "file://" (:app-path rt/runtime-info) "/" url)
        new-state (-> current-state
                      (assoc-in [:main-window] window)
                      (assoc-in [:windows window-id] window)
                      (assoc-in [:active] window-id))
        new-state (if-let [loading (:loading new-state)]
                    (do (.close loading)
                        (dissoc new-state :loading))
                    new-state)
        focus (fn [_]
                (info "Focused" window-id)
                (swap! cmp-state assoc-in [:active] window-id))
        blur (fn [_]
               (info "Blurred" window-id)
               (swap! cmp-state assoc-in [:active] nil))
        close (fn [_]
                (info "Closed" window-id)
                (swap! cmp-state assoc-in [:active] nil)
                (swap! cmp-state update-in [:windows] dissoc window-id))
        ready (fn [_]
                (info "ready" window-id)
                (show)
                (.send (.-webContents window) "window-id" window-id))]
    (info "Opening new window" url)
    (.on window "focus" #(js/setTimeout focus 10))
    (.once window "ready-to-show" ready)
    (.on window "blur" blur)
    (.on window "close" close)
    (.loadURL window url)
    {:new-state new-state}))

(defn loading [{:keys [current-state cmp-state]}]
  (when-not (:loading current-state)
    (let [window (BrowserWindow. (clj->js {:width 400 :height 300}))
          window-id (stc/make-uuid)
          url (str "file://" (:app-path rt/runtime-info) "/loading.html")
          new-state (assoc-in current-state [:loading] window)]
      (info "Opening new load window" url)
      (.loadURL window url)
      {:new-state new-state})))

(defn active-window [current-state]
  (let [active (:active current-state)]
    (get-in current-state [:windows active])))

(defn web-contents [current-state]
  (when-let [active-window (active-window current-state)]
    (.-webContents active-window)))

(defn relay-msg [{:keys [current-state msg-type msg-meta msg-payload]}]
  (let [window-id (:window-id msg-meta)
        window-ids (if (= window-id :broadcast)
                     (keys (:windows current-state))
                     [window-id])]
    (doseq [window-id window-ids]
      (let [window (get-in current-state [:windows window-id])
            web-contents (when window (.-webContents window))
            serializable [msg-type {:msg-payload msg-payload :msg-meta msg-meta}]]
        (when web-contents
          (info "Relaying" msg-type window-id)
          (.send web-contents "relay" (pr-str serializable))))))
  {})

(defn dev-tools [{:keys [current-state]}]
  (when-let [web-contents (web-contents current-state)]
    (info "Open dev-tools")
    (.openDevTools web-contents))
  {})

(defn close-window [{:keys [current-state]}]
  (if-let [active-window (active-window current-state)]
    (let [active (:active current-state)
          new-state (update-in current-state [:windows] dissoc active)]
      (info "Closing:" (:active current-state))
      (.close active-window)
      {:new-state new-state})
    {}))

(defn activate [{:keys [current-state]}]
  (info "Activate APP")
  (when (empty? (:windows current-state))
    {:send-to-self [:window/new {:url "view.html"}]}))

(defn cmp-map
  [cmp-id relay-types]
  (let [relay-map (zipmap relay-types (repeat relay-msg))]
    {:cmp-id      cmp-id
     :handler-map (merge relay-map
                         {:window/new       new-window
                          :window/loading   loading
                          :window/activate  activate
                          :window/close     close-window
                          :window/dev-tools dev-tools})}))
