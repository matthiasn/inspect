(ns com.matthiasnehlsen.inspect
  (:gen-class)
  (:require
   [com.matthiasnehlsen.inspect.matcher :as matcher]
   [com.matthiasnehlsen.inspect.http :as http]
   [clojure.tools.logging :as log]
   [com.stuartsierra.component :as component]
   [clojure.pprint :as pp]
   [clj-time.core :as t]
   [clj-time.format :as f]
   [clojure.core.async :as async :refer [<! chan put! timeout go-loop]]))

(def in-chan (chan))

(def built-in-formatter (f/formatters :date-time))

(defn inspect
  "Send message to inspect sub-system with msg-type"
  [msg-type msg] (put! in-chan {:origin msg-type :received (f/unparse built-in-formatter (t/now)) :payload msg}))

(def conf {:port 8000})

(defn get-system
  "Create system by wiring individual components so that component/start
  will bring up the individual components in the correct order."
  [conf]
  (component/system-map
   :matcher (matcher/new-matcher in-chan inspect)
   :http    (component/using (http/new-http-server conf) {:matcher :matcher})))

(def system (get-system conf))

(defn start [] (alter-var-root #'system component/start))
(defn stop [] (alter-var-root #'system
                              (fn [s] (when s (component/stop s)))))
