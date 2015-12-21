(ns matthiasn.inspect-probe.probe
  (:gen-class)
  (:require
    [matthiasn.systems-toolbox.component :as comp]
    [fipp.clojure :as fipp]
    [matthiasn.inspect-probe.kafka-producer :as kp]))

(defonce kafka-producer (comp/make-component (kp/cmp-map :probe/kafka-prod-cmp)))

(defn inspect-fn
  "Traces a single call to a function f with args. 'name' is the
  symbol name of the function."
  [fn-name args res namespace-name]
  (let [ts (System/currentTimeMillis)
        event {:namespace    namespace-name
               :fn-name      (str fn-name)
               :args         (with-out-str (fipp/pprint args))
               :return-value (with-out-str (fipp/pprint res))
               :ts           ts
               :duration     (- (System/currentTimeMillis) ts)}]
    (comp/send-msg kafka-producer [:inspect/probe event])))

(defmacro get-env [] `[~@(keys &env)])

(defmacro defn
  "Same as defn, except for additionally sending args and result off to inspect."
  {:added "0.2.1"}
  [fn-name & decls]
  (let [[pre-argsvec decls2] (split-with #(not (vector? %)) decls)]
    (if (empty? decls2)
      (list* `defn fn-name decls)
      (let [[args-vec decls] (split-at 1 decls2)
            pre-pos? (and (next decls) (map? (first decls)))
            pre-post (when pre-pos? (take 1 decls))
            body (if pre-pos? (drop 1 decls) decls)
            name-str (name fn-name)
            body (list `(let [args# (get-env)
                              res# (do ~@body)
                              ns-name# (ns-name ~*ns*)]
                          (inspect-fn ~name-str args# res# ns-name#)
                          res#))
            decls2 (concat pre-argsvec args-vec pre-post body)]
        (list* `defn fn-name decls2)))))

(defmacro defn-
  "same as defn, yielding non-public def"
  {:added "0.2.1"}
  [name & decls]
  (list* `defn (with-meta name (assoc (meta name) :private true)) decls))
