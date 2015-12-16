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
                       ;:args         (with-out-str (fipp/pprint (into [] args)))
                       ;:return-value (with-out-str (fipp/pprint value))
                       :args         (into [] args)
                       :return-value value
                       :ts           ts
                       :duration     (- (System/currentTimeMillis) ts)})
    value))

(defmacro defn
  "Use in place of defn; hands each call's arguments and the return value
  of this fn over to inspect for further inspection in a UI.
  The first argument of the form definition can be a doc string.
  Borrowed from https://github.com/clojure/tools.trace"
  [fn-name & definition]
  (let [doc-string (if (string? (first definition)) (first definition) "")
        fn-form (if (string? (first definition)) (rest definition) definition)]
    `(do
       (declare ~fn-name)
       (let [f# (fn ~@fn-form)
             ns# (ns-name ~*ns*)]
         (defn ~fn-name ~doc-string [& args#]
               (inspect-fn-call '~fn-name f# args# ns#))))))
