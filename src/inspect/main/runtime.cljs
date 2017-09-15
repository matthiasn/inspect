(ns inspect.main.runtime
  (:require [path :refer [normalize]]
            [electron :refer [app]]
            [cljs.nodejs :as nodejs :refer [process]]
            [clojure.string :as s]))

(def runtime-info
  (let [user-data (.getPath app "userData")
        cwd (.cwd process)
        rp (.-resourcesPath process)
        app-path (if (s/includes? rp "Electron.app")
                   cwd
                   (str rp "/app"))
        platform (.-platform process)
        info {:platform       (.-platform process)
              :user-data      user-data
              :dl-path        (str user-data "/downloads")
              :bin-path       (str user-data "/bin")
              :cwd            cwd
              :pid-file       (str user-data "/inspect.pid")
              :resources-path rp
              :app-path       app-path}]
    (into {} (map (fn [[k v]] [k (normalize v)]) info))))
