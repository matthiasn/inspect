(ns matthiasn.inspect-probe.probe
  (:gen-class)
  (:require
    [matthiasn.systems-toolbox.switchboard :as sb]
    [fipp.clojure :as fipp]
    [matthiasn.inspect-probe.kafka-producer :as kp]
    [clojure.pprint :as pp]))

(defonce switchboard (sb/component :probe/switchboard))

(defn init
  []
  (sb/send-mult-cmd
    switchboard
    [[:cmd/init-comp [(kp/cmp-map :probe/kafka-prod-cmp)]]]))

(defonce started (init))

(defn send-to-producer
  [msg]
  (sb/send-cmd
    switchboard
    [:cmd/send {:to  :probe/kafka-prod-cmp
                :msg [:inspect/probe msg]}]))

(defn inspect-fn
  "Traces a single call to a function f with args. 'name' is the
  symbol name of the function."
  [fn-name args res namespace-name]
  (let [ts (System/currentTimeMillis)
        event {:namespace    namespace-name
               :fn-name      (str fn-name)
               :args         (with-out-str (fipp/pprint (into [] args)))
               :return-value (with-out-str (fipp/pprint res))
               :ts           ts
               :duration     (- (System/currentTimeMillis) ts)}]
    (send-to-producer event)))

(defmacro defn-wrapped
  "same as defn, yielding non-public def"
  {:added "1.0"}
  [fn-name & decls]
  (let [[pre-argsvec decls] (split-with #(not (vector? %)) decls)
        [args-vec decls] (split-at 1 decls)
        pre-post (when (and (next decls) (map? (first decls))) (first decls))
        body (list (last decls))
        name-str (name fn-name)
        body (list `(let [args# ~@args-vec
                          res# ~@body
                          ns-name# (ns-name ~*ns*)]
                      (inspect-fn ~name-str args# res# ns-name#)
                      res#))
        decls2 (concat pre-argsvec args-vec pre-post body)]
    (list* `defn fn-name decls2)))
