(ns com.matthiasnehlsen.inspect.main
  (:gen-class)
  (:require
   [com.matthiasnehlsen.inspect.matcher.component :as matcher]
   [clojure.tools.namespace.repl :refer (refresh)]
   [com.matthiasnehlsen.inspect.http.component :as http]
   [clojure.tools.logging :as log]
   [com.stuartsierra.component :as component]
   [clojure.pprint :as pp]
   [clojure.core.async :as async :refer [<! chan put! timeout go-loop]]))

(def inspect-chan (chan))

(defn interval-put-loop
  "put msg on chan every interval milliseonds"
  [interval chan msg]
  (go-loop [] (<! (timeout interval)) (put! chan msg) (recur)))

(interval-put-loop  1000 inspect-chan {:origin :every-second :payload {:msg "every second"}})
(interval-put-loop  5000 inspect-chan {:origin :every-five-seconds :payload {:msg "every five seconds"}})
(interval-put-loop 10000 inspect-chan {:origin :every-ten-seconds :payload {:msg "every ten seconds"}})
(interval-put-loop 60000 inspect-chan {:origin :every-minute :payload {:msg "every minute"}})

(def conf {:port 8000})

(defn get-system
  "Create system by wiring individual components so that component/start
  will bring up the individual components in the correct order."
  [conf]
  (component/system-map
   :matcher (matcher/new-matcher inspect-chan)
   :http    (component/using (http/new-http-server conf) {:matcher :matcher})))

(def system (get-system conf))

(defn start [] (alter-var-root #'system component/start))
(defn stop [] (alter-var-root #'system component/stop))
(defn reload [] (stop) (refresh) (start))

(defn -main [& args]
  (log/info "Application started")
  (start))
