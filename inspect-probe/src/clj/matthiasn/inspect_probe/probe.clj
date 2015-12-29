(ns matthiasn.inspect-probe.probe
  (:gen-class)
  (:refer-clojure :exclude [defn defn-])
  (:require
    [clojure.core :as core]
    [clj-time.core :as t]
    [matthiasn.systems-toolbox.component :as comp]
    [matthiasn.systems-toolbox.kafka-producer :as kp]
    [net.cgrand.seqexp :as se]))

(def cfg {:msg-type-topic-mapping {:inspect/probe "inspect-probe-events"}})
(defonce kafka-producer (comp/make-component (kp/cmp-map :probe/kafka-prod-cmp cfg)))

(def index (get (System/getenv) "INSPECT_IDX" "inspect"))

(core/defn inspect-fn
  "Traces a single call to a function f with args. 'name' is the
  symbol name of the function."
  [fn-name args res namespace-name]
  (let [ts (System/currentTimeMillis)
        event {:namespace    namespace-name
               :index        index
               :fn-name      (str fn-name)
               :args         args
               :return-value res
               :ts           ts
               :datetime     (t/now)
               :duration     (- (System/currentTimeMillis) ts)}]
    (comp/send-msg kafka-producer [:inspect/probe event])))

(core/defn ^:private process-argvec-entry [argvec+bindings argvec-entry]
  (cond
    (symbol? argvec-entry)
    (update argvec+bindings :argvec conj argvec-entry)

    (and (map? argvec-entry)
         (contains? argvec-entry :as))
    (-> argvec+bindings
        (update :argvec conj (:as argvec-entry))
        (update :bindings conj argvec-entry (:as argvec-entry)))

    (and (vector? argvec-entry)
         (= :as (second (rseq argvec-entry))))
    (let [as-name (first (rseq argvec-entry))]
      (-> argvec+bindings
          (update :argvec conj as-name)
          (update :bindings conj argvec-entry as-name)))

    :else
    (let [gs (gensym "param__")]
      (-> argvec+bindings
          (update :argvec conj gs)
          (update :bindings conj argvec-entry gs)))))

(core/defn ^:private prepare-argvec+bindings [argvec]
  (update (reduce process-argvec-entry
                  {:argvec   []
                   :bindings []}
                  argvec)
          :argvec #(some->> %2 (vary-meta %1)) (constantly (meta argvec))))

(core/defn ^:private prepare-argvec+body [name-str {:keys [argvec bindings]} body]
  `(~argvec
     (let [args# ~(let [no-& (filterv #(not= '& %) argvec)]
                    (zipmap (map keyword no-&) no-&))
           ~@bindings
           res# (do ~@body)]
       (inspect-fn ~name-str args# res# '~(ns-name *ns*))
       res#)))

(defmacro defn
  "Same as defn, except for additionally sending args and result off to inspect."
  {:added "0.2.1"}
  [fn-name & decls]
  (let [{:keys [docstring+meta argvecs+bodies]}
        (se/exec (se/cat (se/as :docstring+meta (se/cat (se/? string?)
                                                        (se/? map?)))
                         (se/as :argvecs+bodies (se/| (se/cat vector? (se/* se/_))
                                                      (se/* #(and (list? %)
                                                                  (vector? (first %)))))))
                 decls)
        single-arity? (vector? (first argvecs+bodies))
        argvecs (if single-arity?
                  [(first argvecs+bodies)]
                  (mapv first argvecs+bodies))
        bodies (if single-arity?
                 [(next argvecs+bodies)]
                 (mapv next argvecs+bodies))
        new-argvecs+bindings (mapv prepare-argvec+bindings argvecs)
        new-argvecs+bodies (mapv (partial prepare-argvec+body (name fn-name))
                                 new-argvecs+bindings
                                 bodies)]
    `(core/defn ~fn-name ~@docstring+meta ~@new-argvecs+bodies)))

(defmacro defn-
  "same as defn, yielding non-public def"
  {:added "0.2.1"}
  [name & decls]
  `(defn ~(vary-meta name assoc :private true) ~@decls))
