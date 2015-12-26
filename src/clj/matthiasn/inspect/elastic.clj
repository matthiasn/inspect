(ns matthiasn.inspect.elastic
  (:gen-class)
  (:require
    [clojure.core.match :refer [match]]
    [clojurewerkz.elastisch.rest :as esr]
    [clojurewerkz.elastisch.rest.document :as esd]
    [clojurewerkz.elastisch.rest.index :as esi]
    [clj-uuid :as uuid]
    [clojure.tools.logging :as log]))

(def es-address (get (System/getenv) "ES_ADDRESS" "http://127.0.0.1:9200"))

(def mapping-types
  {"event" {:properties {:namespace    {:type "string" :store "yes"}
                         :fn-name      {:type "string" :store "yes"}
                         :args         {:type "string" :store "yes"}
                         :return-value {:type "string" :store "yes"}
                         :ts           {:type "long" :store "yes"}
                         :datetime     {:type "string" :store "yes"}
                         :duration     {:type "long" :store "yes"}}}})

(defn es-state-fn
  "Returns function for making state while using provided configuration."
  [put-fn]
  (let [conn (esr/connect es-address)]
    (log/info "ElasticSearch connection started to" es-address)
    {:state (atom {:conn conn})}))

(defn persist-fn
  "Persist event into configured ElasticSearch index."
  [{:keys [cmp-state msg-payload]}]
  (try
    (when msg-payload
      (let [conn (:conn @cmp-state)
            es-index (:index msg-payload)]
        (when-not (esi/exists? conn es-index)
          (esi/create conn es-index :mappings mapping-types))
        (esd/put conn es-index "event" (str (uuid/v1)) (dissoc msg-payload :index))))
    (catch Exception ex (log/error ex "Exception when trying to persist to ElasticSearch"))))

(defn cmp-map
  [cmp-id]
  {:cmp-id           cmp-id
   :state-fn         es-state-fn
   :all-msgs-handler persist-fn})
