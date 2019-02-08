(ns inspect.main.startup
  (:require [taoensso.timbre :as timbre :refer-macros [info debug error]]
            [electron :refer [app session]]
            [path :refer [normalize join]]
            [inspect.main.runtime :as rt]
            [child_process :refer [spawn]]
            [clojure.string :as str]))

(defn shutdown [{:keys []}]
  (info "Shutting down")
  (.quit app)
  {})

(defn clear-cache [{:keys []}]
  (info "Clearing Electron Cache")
  (let [session (.-defaultSession session)]
    (.clearCache session #(info "Electron Cache Cleared")))
  {})

(defn spawn-process [cmd args opts]
  (info "STARTUP: spawning" cmd args opts)
  (spawn cmd (clj->js args) (clj->js opts)))

(defn run-zookeeper [{:keys [current-state]}]
  (info "STARTUP: run Zookeeper")
  (let [{:keys [user-data app-path cwd repo-dir downloads]} rt/runtime-info
        kafka-path (join downloads "kafka_2.11-0.10.1.1")
        zk-bin (join kafka-path "bin/zookeeper-server-start.sh")
        zk-conf (str kafka-path "/config/zookeeper.properties")
        java-home (join downloads "zulu8.23.0.3-jdk8.0.144-macosx_x64")
        service (spawn-process zk-bin
                               [zk-conf]
                               {:detached false
                                :cwd      kafka-path
                                :env      {:JAVA_HOME java-home}})
        std-out (.-stdout service)
        std-err (.-stderr service)]
    (.on std-out "data" #(info "ZOOKEEPER " (.toString % "utf8")))
    (.on std-err "data" #(error "ZOOKEEPER " (.toString % "utf8")))
    {:new-state (assoc-in current-state [:zookeeper] service)}))

(defn run-kafka [{:keys [current-state]}]
  (info "STARTUP: run Kafka")
  (let [{:keys [user-data app-path bin-path cwd downloads]} rt/runtime-info
        kafka-path (join downloads "kafka_2.11-0.10.1.1")
        bin (join kafka-path "bin/kafka-server-start.sh")
        conf (str kafka-path "/config/server.properties")
        java-home (join downloads "zulu8.23.0.3-jdk8.0.144-macosx_x64")
        service (spawn-process bin
                               [conf]
                               {:detached false
                                :cwd      kafka-path
                                :env      {:JAVA_HOME java-home}})
        std-out (.-stdout service)
        std-out-handler #(info "KAFKA " (.toString % "utf8"))
        std-err (.-stderr service)]
    (.on std-out "data" std-out-handler)
    (.on std-err "data" #(error "KAFKA " (.toString % "utf8")))
    {:new-state (assoc-in current-state [:zookeeper] service)}))

(defn cmp-map [cmp-id]
  {:cmp-id      cmp-id
   :handler-map {:app/shutdown    shutdown
                 :app/clear-cache clear-cache
                 :run/zookeeper   run-zookeeper
                 :run/kafka       run-kafka}})

(.on app "window-all-closed"
     (fn [ev]
       (info "window-all-closed")
       (when-not (= (:platform rt/runtime-info) "darwin")
         (shutdown {}))))
