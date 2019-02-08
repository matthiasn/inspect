(ns inspect.main.runtime
  (:require [path :refer [normalize join]]
            [electron :refer [app]]
            [cljs.nodejs :as nodejs :refer [process]]
            [clojure.string :as s]))

(def runtime-info
  (let [user-data (.getPath app "userData")
        cwd (.cwd process)
        rp (.-resourcesPath process)
        app-path (if (s/includes? (s/lower-case rp) "electron")
                   cwd
                   (str rp "/app"))
        repo-dir (s/includes? rp "Electron.app")
        platform (.-platform process)
        info {:platform       (.-platform process)
              :downloads      (.getPath app "downloads")
              :user-data      user-data
              :bin-path       (join user-data "bin")
              :cwd            cwd
              :pid-file       (str user-data "/inspect.pid")
              :resources-path rp
              :app-path       app-path}]
    (into {:index-page (if repo-dir
                         "./resources/view-dev.html"
                         "/resources/view.html")}
          (map (fn [[k v]] [k (normalize v)]) info))))
