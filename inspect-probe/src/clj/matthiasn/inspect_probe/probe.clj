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

(defn inspect-fn-call
  "Traces a single call to a function f with args. 'name' is the
  symbol name of the function."
  [fn-name f args namespace-name]
  (let [ts (System/currentTimeMillis)
        value (apply f args)
        fn-name (name fn-name)]
    (send-to-producer {:namespace    namespace-name
                       :fn-name      fn-name
                       ;:args         (into [] (map #(with-out-str (pp/pprint %)) args))
                       :args         (with-out-str (fipp/pprint (into [] args)))
                       :return-value (with-out-str (fipp/pprint value))
                       ;                       :args         (into [] args)
                       ;                       :return-value value
                       :ts           ts
                       :duration     (- (System/currentTimeMillis) ts)})
    value))

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
    (prn event)
    (send-to-producer event)))

(defmacro defn
  "Use in place of defn; hands each call's arguments and the return value
  of this fn over to inspect for further inspection in a UI.
  The first argument of the form definition can be a doc string.
  Borrowed from https://github.com/clojure/tools.trace"
  [fn-name & fdecl]
  (let [m (merge {} (meta fn-name))
        m (if (string? (first fdecl)) (merge m {:doc (first fdecl)}) m)
        fdecl (if (string? (first fdecl)) (next fdecl) fdecl)
        m (if (map? (first fdecl)) (merge m (first fdecl)) m)
        fdecl (if (map? (first fdecl)) (next fdecl) fdecl)
        params (first fdecl)
        body (next fdecl)
        conds (when (and (next body) (map? (first body)))
                (first body))
        body (if conds (next body) body)
        conds (merge {} conds)
        args# params]
    (prn params)
    (prn body)
    `(defn ~fn-name ~m [~@params]
           ~conds
           (let [res# (do ~@body)
                 ns# (ns-name ~*ns*)]
             (inspect-fn ~fn-name [~@params] res# ns#)
             res#))))

#_(defmacro defn
    "same as defn, yielding non-public def"
    {:added "1.0"}
    [name & decls]
    (list* `defn name decls))
