(ns inspect.main.menu
  (:require [taoensso.timbre :as timbre :refer-macros [info debug]]
            [electron :refer [app Menu]]
            [cljs.nodejs :as nodejs :refer [process]]
            [inspect.main.runtime :as rt]))

(defn state-fn [put-fn]
  (let [index-page (:index-page rt/runtime-info)
        menu-tpl
        [{:label   "Application"
          :submenu [{:label    "About"
                     :selector "orderFrontStandardAboutPanel:"}
                    {:label "Check for Updates..."
                     :click #(put-fn [:window/new {:url    "updater.html"
                                                   :width  600
                                                   :height 300}])}
                    {:label   "Clear Caches"
                     :submenu [{:label "Clear Electron Cache"
                                :click #(put-fn [:app/clear-cache])}]}
                    {:label "Process Management"
                     :click #(put-fn [:mgmt/open])}
                    {:label "Download Kafka"
                     :click #(put-fn [:download/bin :kafka])}
                    {:label "Download JDK"
                     :click #(put-fn [:download/bin :jdk])}
                    {:label "Run Zookeeper"
                     :click #(put-fn [:run/zookeeper])}
                    {:label "Run Kafka"
                     :click #(put-fn [:run/kafka])}
                    {:label       "Close Window"
                     :accelerator "Cmd+W"
                     :click       #(put-fn [:window/close])}
                    {:label       "Quit"
                     :accelerator "Cmd+Q"
                     :click       #(put-fn [:app/shutdown])}]}
         {:label   "File"
          :submenu []}
         {:label   "Edit"
          :submenu [{:label       "Undo"
                     :accelerator "CmdOrCtrl+Z"
                     :selector    "undo:"}
                    {:label       "Redo"
                     :accelerator "Shift+CmdOrCtrl+Z"
                     :selector    "redo:"}
                    {:label       "Cut"
                     :accelerator "CmdOrCtrl+X"
                     :selector    "cut:"}
                    {:label       "Copy"
                     :accelerator "CmdOrCtrl+C"
                     :selector    "copy:"}
                    {:label       "Paste"
                     :accelerator "CmdOrCtrl+V"
                     :selector    "paste:"}
                    {:label       "Select All"
                     :accelerator "CmdOrCtrl+A"
                     :selector    "selectAll:"}]}
         {:label   "View"
          :submenu [{:label       "New Window"
                     :accelerator "Option+Cmd+N"
                     :click       #(put-fn [:window/new {:url index-page}])}
                    {:type "separator"}
                    {:role "zoomin"}
                    {:role "zoomout"}
                    {:type "separator"}
                    {:label "Open Dev Tools"
                     :click #(put-fn [:window/dev-tools])}]}]
        menu (.buildFromTemplate Menu (clj->js menu-tpl))
        activate #(put-fn [:window/activate])]
    (info "Starting Menu Component")
    (.on app "activate" activate)
    (.setApplicationMenu Menu menu))
  {:state (atom {})})

(defn cmp-map [cmp-id]
  {:cmp-id   cmp-id
   :state-fn state-fn})
