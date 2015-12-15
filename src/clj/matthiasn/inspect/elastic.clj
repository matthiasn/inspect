(ns matthiasn.inspect.elastic
  (:gen-class)
  (:require
    [clojure.core.match :refer [match]]
    [clojurewerkz.elastisch.rest :as esr]
    [clojurewerkz.elastisch.rest.document :as esd]
    [clojurewerkz.elastisch.rest.index :as esi]
    [clj-uuid :as uuid]
    [clojure.tools.logging :as log]
    [clojure.pprint :as pp]))

(def es-address (get (System/getenv) "ES_ADDRESS" "http://127.0.0.1:9200"))
(def es-index (get (System/getenv) "ES_INDEX" "inspect"))

(defn es-state-fn
  "Returns function for making state while using provided configuration."
  [put-fn]
  (let [conn (esr/connect es-address)]
    (log/info "ElasticSearch connection started to" es-address)
    (when-not (esi/exists? conn es-index)
      (esi/create conn es-index))
    {:state (atom {:conn conn})}))

(defn persist-fn
  "Persist event into configured ElasticSearch index."
  [{:keys [cmp-state msg-type msg-payload]}]
  (try
    (let [es-doc {:event-type     msg-type
                  :event-pprinted msg-payload}]
      (esd/put (:conn @cmp-state) es-index "event" (str (uuid/v1)) es-doc))
    (catch Exception ex (log/error ex "Exception when trying to persist to ElasticSearch"))))

(defn cmp-map
  [cmp-id]
  {:cmp-id           cmp-id
   :state-fn         es-state-fn
   :all-msgs-handler persist-fn})
