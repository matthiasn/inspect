(ns matthiasn.inspect.probe
  (:gen-class)
  (:require
    [matthiasn.systems-toolbox.switchboard :as sb]
    [matthiasn.inspect.kafka-producer :as kp]
    [matthiasn.inspect.kafka-consumer :as kc]))

(defonce switchboard (sb/component :probe/switchboard))

(defn init
  []
  (sb/send-mult-cmd
    switchboard
    [[:cmd/init-comp [(kc/cmp-map :probe/kafka-consumer-cmp)]]
     [:cmd/init-comp [(kp/cmp-map :probe/kafka-prod-cmp)]]]))

(defn send-to-producer
  [msg]
  (sb/send-cmd
    switchboard
    [:cmd/send {:to  :probe/kafka-prod-cmp
                :msg [:inspect/args msg]}]))

(defn inspect-fn-call
  "Traces a single call to a function f with args. 'name' is the
symbol name of the function."
  [fn-name f args]
  (do
    (send-to-producer [(keyword (name fn-name) "args") (into [] args)])
    (let [value (apply f args)]
      (send-to-producer [(keyword (name fn-name) "return-value") value])
      value)))

(defmacro defn
  "Use in place of defn; hands each call's arguments and the return value
  of this fn over to inspect for further inspection in a UI.
  The first argument of the form definition can be a doc string.
  Borrowed from https://github.com/clojure/tools.trace"
  [fn-name & definition]
  (let [doc-string (if (string? (first definition)) (first definition) "")
        fn-form  (if (string? (first definition)) (rest definition) definition)]
    `(do
       (declare ~fn-name)
       (let [f# (fn ~@fn-form)]
         (defn ~fn-name ~doc-string [& args#]
           (inspect-fn-call '~fn-name f# args#))))))
