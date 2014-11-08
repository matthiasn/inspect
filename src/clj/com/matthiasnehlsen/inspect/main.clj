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
  [interval chan msg]
  (go-loop [] (<! (timeout interval)) (put! chan msg) (recur)))

(interval-put-loop  1000 inspect/in-chan {:origin :every-second :payload {:msg "every second"}})
(interval-put-loop  5000 inspect/in-chan {:origin :every-five-seconds :payload {:msg "every five seconds"}})
(interval-put-loop 10000 inspect/in-chan {:origin :every-ten-seconds :payload {:msg "every ten seconds"}})
(interval-put-loop 60000 inspect/in-chan {:origin :every-minute :payload {:msg "every minute"}})

(defn reload [] (inspect/stop) (refresh) (inspect/start))

(defn -main [& args]
  (log/info "Application started")
  (inspect/start))
