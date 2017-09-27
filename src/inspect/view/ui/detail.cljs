(ns inspect.view.ui.detail
  (:require-macros [reagent.ratom :refer [reaction]])
  (:require [re-frame.core :refer [subscribe]]
            [taoensso.timbre :as timbre :refer-macros [info debug]]
            [inspect.view.util :as u]
            [cljs.pprint :as pp]))

(defn detailed-msg [put-fn]
  (let [detailed-msg (subscribe [:detailed-msg])]
    (fn [put-fn]
      (when @detailed-msg
        [:div [:pre [:code (with-out-str (pp/pprint @detailed-msg))]]]))))
