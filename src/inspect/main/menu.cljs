(ns inspect.main.menu
  (:require [taoensso.timbre :as timbre :refer-macros [info debug]]
            [electron :refer [app Menu]]
            [cljs.nodejs :as nodejs :refer [process]]))

(defn state-fn
  [put-fn]
  (let [menu-tpl
        [{:label   "Application"
          :submenu [{:label    "About iWasWhere"
                     :selector "orderFrontStandardAboutPanel:"}
                    {:label "Check for Updates..."
                     :click #(put-fn [:window/updater])}
                    {:label   "Clear Caches"
                     :submenu [{:label "Clear Electron Cache"
                                :click #(put-fn [:app/clear-cache])}
                               {:label "Clear iWasWhere Snapshot"
                                :click #(put-fn [:app/clear-iww-cache])}]}
                    {:label       "Close Window"
                     :accelerator "Cmd+W"
                     :click       #(put-fn [:window/close])}
                    {:label "Quit Background Service"
                     :click #(do (put-fn [:app/shutdown-jvm])
                                 (put-fn [:app/shutdown]))}
                    {:label       "Quit"
                     :accelerator "Cmd+Q"
                     :click       #(put-fn [:app/shutdown])}]}
         {:label   "File"
          :submenu [{:label       "New Entry"
                     :accelerator "Cmd+N"
                     :click       #(put-fn [:exec/js "iwaswhere_web.ui.menu.new_entry()"])}
                    {:label "New Story"
                     :click #(put-fn
                               [:window/send
                                {:cmd      "iwaswhere_web.ui.menu.new_story()"
                                 :cmd-type "cmd"}])}
                    {:label "New Saga"
                     :click #(put-fn
                               [:window/send
                                {:cmd      "iwaswhere_web.ui.menu.new_saga()"
                                 :cmd-type "cmd"}])}
                    {:label       "Upload"
                     :accelerator "Cmd+U"
                     :click       #(put-fn
                                     [:window/send
                                      {:cmd      "iwaswhere_web.ui.menu.upload()"
                                       :cmd-type "cmd"}])}]}
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
                     :selector    "selectAll:"}
                    {:label   "Spelling"
                     :submenu [{:label "English"
                                :click #(put-fn
                                          [:window/send
                                           {:cmd      "window.spellCheckHandler.switchLanguage('en-US');"
                                            :cmd-type "cmd"}])}
                               {:label "French"
                                :click #(put-fn
                                          [:window/send
                                           {:cmd      "window.spellCheckHandler.switchLanguage('fr-FR');"
                                            :cmd-type "cmd"}])}
                               {:label "German"
                                :click #(put-fn
                                          [:window/send
                                           {:cmd      "window.spellCheckHandler.switchLanguage('de-DE');"
                                            :cmd-type "cmd"}])}
                               {:label "Italian"
                                :click #(put-fn
                                          [:window/send
                                           {:cmd      "window.spellCheckHandler.switchLanguage('it-IT');"
                                            :cmd-type "cmd"}])}
                               {:label "Spanish"
                                :click #(put-fn
                                          [:window/send
                                           {:cmd      "window.spellCheckHandler.switchLanguage('es-ES');"
                                            :cmd-type "cmd"}])}
                               {:type "separator"}
                               {:label "none"
                                :click #(put-fn
                                          [:window/send
                                           {:cmd      "window.spellCheckHandler.currentSpellchecker=null;;"
                                            :cmd-type "cmd"}])}]}]}
         {:label   "View"
          :submenu [{:label       "New Window"
                     :accelerator "Option+Cmd+N"
                     :click       #(put-fn [:window/new "main"])}
                    {:label "Open Dev Tools"
                     :click #(put-fn [:window/dev-tools])}
                    {:label "Main View"
                     :click #(put-fn
                               [:window/send
                                {:cmd      "window.location = '/#/'"
                                 :cmd-type "cmd"}])}
                    {:label "Charts"
                     :click #(put-fn
                               [:window/send
                                {:cmd      "window.location = '/#/charts1'"
                                 :cmd-type "cmd"}])}
                    {:label "Dashboards"
                     :click #(put-fn
                               [:window/send
                                {:cmd      "window.location = '/#/dashboards/dashboard-1'"
                                 :cmd-type "cmd"}])}
                    {:label "Hide Menu"
                     :click #(put-fn [:window/send
                                      {:cmd      "iwaswhere_web.ui.menu.hide()"
                                       :cmd-type "cmd"}])}]}]
        menu (.buildFromTemplate Menu (clj->js menu-tpl))
        activate #(put-fn [:window/activate])]
    (info "Starting Menu Component")
    (.on app "activate" activate)
    (.setApplicationMenu Menu menu))
  {:state (atom {})})

(defn cmp-map
  [cmp-id]
  {:cmp-id   cmp-id
   :state-fn state-fn})
