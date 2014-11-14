(ns example.core
  (:gen-class)
  (:require
   [com.matthiasnehlsen.inspect :as inspect :refer [inspect]]
   [clojure.tools.namespace.repl :refer [refresh]]
   [clojure.tools.logging :as log]
   [clojure.pprint :as pp]
   [clj-pid.core :as pid]
   [clojure.core.async :as async :refer [<! chan put! timeout go-loop]]))

(defn interval-put-loop
  "put msg on chan every interval milliseonds"
  [interval msg-type msg]
  (go-loop [] (<! (timeout interval)) (inspect msg-type msg) (recur)))

(interval-put-loop     1 :interval-put/every-millisecond {:msg "every millisecond"})
(interval-put-loop  1000 :interval-put/every-second {:msg "every second"})
(interval-put-loop  5000 :interval-put/every-five-seconds {:msg "every five seconds"})
(interval-put-loop 10000 :interval-put/every-ten-seconds {:msg "every ten seconds"})
(interval-put-loop 60000 :interval-put/every-minute {:msg "every minute"})

;; optional, only necessary when default port 8000 is not desired
;(inspect/configure {:port 8001})

(defn reload [] (inspect/stop) (refresh) (inspect/start))

(defn -main [& args]
  (pid/save "example.pid")
  (pid/delete-on-shutdown! "example.pid")
  (log/info "Application started, PID" (pid/current))
  (inspect/start))
