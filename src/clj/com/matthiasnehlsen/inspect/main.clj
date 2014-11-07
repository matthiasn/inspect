(ns com.matthiasnehlsen.inspect.main
  (:gen-class)
  (:require
   [clojure.tools.logging :as log]
   [com.stuartsierra.component :as component]
   [clojure.pprint :as pp]
   [clojure.core.async :as async :refer [<! chan put! timeout go-loop]]))

(def inspect-chan (chan))

(go-loop [] (<! (timeout 1000))
         (put! inspect-chan {:origin :every-second :payload "every second"})
         (recur))

(go-loop [] (<! (timeout 5000))
         (put! inspect-chan {:origin :every-five-seconds :payload "every five seconds"})
         (recur))

(go-loop [] (<! (timeout 10000))
         (put! inspect-chan {:origin :every-ten-seconds :payload "every ten seconds"})
         (recur))

(go-loop [] (<! (timeout 60000))
         (put! inspect-chan {:origin :every-minute :payload "every minute"})
         (recur))

(go-loop []
         (let [msg (<! inspect-chan)]
           (log/info (pp/pprint msg)))
         (recur))

(defn -main [& args]
  (log/info "Application started")
  (Thread/sleep Long/MAX_VALUE))
