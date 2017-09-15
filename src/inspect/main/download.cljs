(ns inspect.main.download
  (:require [taoensso.timbre :as timbre :refer-macros [info debug]]
            [electron :refer [BrowserWindow]]
            [electron-dl :refer [download]]
            [decompress]
            [inspect.main.runtime :as rt]))

(def urls
  {:kafka "http://apache.mirror.digionline.de/kafka/0.10.1.1/kafka_2.11-0.10.1.1.tgz"
   :jdk   "http://cdn.azul.com/zulu/bin/zulu8.23.0.3-jdk8.0.144-macosx_x64.zip"})

(defn download-bin [{:keys [msg-payload]}]
  (info "DOWNLOAD:" msg-payload)
  (let [window (BrowserWindow. (clj->js {:width 400 :height 300}))
        url (msg-payload urls)
        progress (fn [p] (info "DOWNLOAD: progress" p))
        {:keys [downloads]} rt/runtime-info
        opts (clj->js {:directory          downloads
                       :onProgress         progress
                       :openFolderWhenDone true})
        jdk-dl (download window url opts)
        on-complete (fn [dl-item]
                      (let [filename (.getFilename dl-item)]
                        (decompress (str downloads "/" filename) downloads)
                        (info "DOWNLOAD completed" filename)))]
    (.then jdk-dl on-complete)
    {}))

(defn cmp-map [cmp-id]
  {:cmp-id      cmp-id
   :handler-map {:download/bin download-bin}})

