(ns com.matthiasnehlsen.inspect.main
  (:gen-class)
  (:require
   [com.matthiasnehlsen.inspect :as inspect :refer [inspect]]
   [clojure.tools.namespace.repl :refer [refresh]]
   [clojure.tools.logging :as log]
   [com.stuartsierra.component :as component]
   [clojure.pprint :as pp]
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

(defn reload [] (inspect/stop) (refresh) (inspect/start))

(defn -main [& args]
  (log/info "Application started")
  (inspect/start))
