(ns inspect.specs.specs
  (:require [cljs.spec.alpha :as s]
            [matthiasn.systems-toolbox.spec]))

(s/def :kafka/start string?)
(s/def :kafka/status map?)

(s/def :observer/cmps-msgs map?)
(s/def :observer/subscribe map?)

(s/def :subscription/match :firehose/cmp-recv)

(s/def :inspect.update/status #{:update/available
                               :update/not-available
                               :update/checking
                               :update/downloading
                               :update/downloaded
                               :update/error})

(s/def :update/status (s/keys :req-un [:inspect.update/status]))
