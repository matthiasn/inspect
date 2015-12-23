(ns matthiasn.inspect-probe.probe
  (:gen-class)
  (:require
    [matthiasn.systems-toolbox.component :as comp]
    [clj-time.core :as t]
    [matthiasn.inspect-probe.kafka-producer :as kp]))

(defonce kafka-producer (comp/make-component (kp/cmp-map :probe/kafka-prod-cmp)))

(defn inspect-wrapper-fn
  "Traces a single call to a function f with args. 'name' is the
  symbol name of the function."
  [f fn-name namespace-name]
  (let [fn-name# fn-name]
    (fn
      [& args]
      (let [ts# (System/currentTimeMillis)
            res# (apply f args)]
        (comp/send-msg kafka-producer
                       [:inspect/probe
                        {:namespace    namespace-name
                         :fn-name      fn-name#
                         :args         args
                         :return-value res#
                         :ts           ts#
                         :datetime     (t/now)
                         :duration     (- (System/currentTimeMillis) ts#)}])
        res#))))

(def ^{:private true :dynamic true}
assert-valid-fdecl (fn [fdecl]))

(def
  ^{:private true}
  sigs
  (fn [fdecl]
    (assert-valid-fdecl fdecl)
    (let [asig
          (fn [fdecl]
            (let [arglist (first fdecl)
                  ;elide implicit macro args
                  arglist (if (clojure.lang.Util/equals '&form (first arglist))
                            (clojure.lang.RT/subvec arglist 2 (clojure.lang.RT/count arglist))
                            arglist)
                  body (next fdecl)]
              (if (map? (first body))
                (if (next body)
                  (with-meta arglist (conj (if (meta arglist) (meta arglist) {}) (first body)))
                  arglist)
                arglist)))]
      (if (seq? (first fdecl))
        (loop [ret [] fdecls fdecl]
          (if fdecls
            (recur (conj ret (asig (first fdecls))) (next fdecls))
            (seq ret)))
        (list (asig fdecl))))))


(def
  ^{:doc      "Same as (def name (fn [params* ] exprs*)) or (def
    name (fn ([params* ] exprs*)+)) with any doc-string or attrs added
    to the var metadata. prepost-map defines a map with optional keys
    :pre and :post that contain collections of pre or post conditions."
    :arglists '([name doc-string? attr-map? [params*] prepost-map? body]
                 [name doc-string? attr-map? ([params*] prepost-map? body) + attr-map?])
    :added    "1.0"}
  defn (fn defn [&form &env name & fdecl]
         ;; Note: Cannot delegate this check to def because of the call to (with-meta name ..)
         (if (instance? clojure.lang.Symbol name)
           nil
           (throw (IllegalArgumentException. "First argument to defn must be a symbol")))
         (let [m (if (string? (first fdecl))
                   {:doc (first fdecl)}
                   {})
               fdecl (if (string? (first fdecl))
                       (next fdecl)
                       fdecl)
               m (if (map? (first fdecl))
                   (conj m (first fdecl))
                   m)
               fdecl (if (map? (first fdecl))
                       (next fdecl)
                       fdecl)
               fdecl (if (vector? (first fdecl))
                       (list fdecl)
                       fdecl)
               m (if (map? (last fdecl))
                   (conj m (last fdecl))
                   m)
               fdecl (if (map? (last fdecl))
                       (butlast fdecl)
                       fdecl)
               m (conj {:arglists (list 'quote (sigs fdecl))} m)
               m (let [inline (:inline m)
                       ifn (first inline)
                       iname (second inline)]
                   ;; same as: (if (and (= 'fn ifn) (not (symbol? iname))) ...)
                   (if (if (clojure.lang.Util/equiv 'fn ifn)
                         (if (instance? clojure.lang.Symbol iname) false true))
                     ;; inserts the same fn name to the inline fn if it does not have one
                     (assoc m :inline (cons ifn (cons (clojure.lang.Symbol/intern (.concat (.getName ^clojure.lang.Symbol name) "__inliner"))
                                                      (next inline))))
                     m))
               m (conj (if (meta name) (meta name) {}) m)
               inner-fn (eval `(fn ~name ~@fdecl))]
           (list 'def (with-meta name m)
                 (list `inspect-wrapper-fn inner-fn (str name) (str (ns-name *ns*)))))))

(. (var defn) (setMacro))

(defmacro defn-
  "same as defn, yielding non-public def"
  {:added "0.2.1"}
  [name & decls]
  (list* `defn (with-meta name (assoc (meta name) :private true)) decls))
