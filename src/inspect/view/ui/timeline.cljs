(ns inspect.view.ui.timeline
  (:require [moment]
            [re-frame.core :refer [subscribe]]
            [taoensso.timbre :as timbre :refer-macros [info debug]]
            [reagent.ratom :refer-macros [reaction]]
            [reagent.core :as r]
            [clojure.data.avl :as avl]
            [clojure.pprint :as pp]))

(defn tick
  "Renders individual timeline tick."
  [ts pos color w h base-y local]
  (let [half-h (/ h 2)]
    [:line
     {:x1             pos
      :y1             (- base-y half-h)
      :x2             pos
      :y2             (+ base-y half-h)
      :on-mouse-enter #(swap! local assoc-in [:mouse-over] ts)
      :on-mouse-leave (fn [_ev] (.setTimeout js/window #(swap! local dissoc :mouse-over) 5000))
      :stroke         color
      :stroke-width   w}]))

(def ymd-hms "YYYY-MM-DD HH:mm:ss")
(def hm "HH:mm:ss:SSS")
(defn df [ts format] (.format (moment ts) format))

(defn timeline-zoom
  "Renders timeline zoom slider."
  [local _tl-start _ts-range _flows]
  (let [mouse-up (fn [_ev]
                   (swap! local #(-> %
                                     (assoc-in [:in-slider-down] false)
                                     (assoc-in [:out-slider-down] false)
                                     (assoc-in [:rect-down] false))))
        in-slider-down #(swap! local assoc-in [:in-slider-down] true)
        out-slider-down #(swap! local assoc-in [:out-slider-down] true)
        slider-width 540
        calc-x-scale (fn []
                       (let [rng (- (:scale-out @local) (:scale-in @local))
                             x-scale (/ slider-width rng)
                             tl-width (* (:elem-width @local) x-scale)
                             offset (* (/ (- (:scale-in @local) 10) slider-width) tl-width)]
                         (swap! local assoc-in [:x-scale] x-scale)
                         (swap! local assoc-in [:x-offset] offset)))
        mouse-move (fn [ev]
                     (let [x (- (.-clientX ev) (:zoom-left @local))
                           x (min (+ slider-width 10) (max 10 x))]
                       (when (:in-slider-down @local)
                         (swap! local assoc-in [:scale-in] x)
                         (calc-x-scale))
                       (when (:in-slider-down @local)
                         (swap! local assoc-in [:scale-in] x)
                         (calc-x-scale))
                       (when (:out-slider-down @local)
                         (swap! local assoc-in [:scale-out] x)
                         (calc-x-scale))
                       (when (:rect-down @local)
                         (let [offset (- (:rect-down-x @local) x)
                               scale-in (- (:rect-down-in @local) offset)
                               scale-out (- (:rect-down-out @local) offset)]
                           (when (and (>= scale-in 10) (<= scale-out (+ slider-width 10)))
                             (swap! local assoc-in [:scale-in] scale-in)
                             (swap! local assoc-in [:scale-out] scale-out)
                             (calc-x-scale))))))
        rect-down (fn [ev]
                    (let [x (- (.-clientX ev) (:zoom-left @local))
                          x (min (+ slider-width 10) (max 10 x))]
                      (swap! local #(-> %
                                        (assoc-in [:rect-down] true)
                                        (assoc-in [:rect-down-in] (:scale-in @local))
                                        (assoc-in [:rect-down-out] (:scale-out @local))
                                        (assoc-in [:rect-down-x] x)))))
        render (fn zoom-render [local tl-start ts-range flows]
                 (let [calc-pos #(+ 10 (* slider-width
                                          (/ (- % tl-start) ts-range)))]
                   [:div.timeline-zoom
                    [:svg {:viewBox        (str "0 0 1000 40")
                           :on-mouse-up    mouse-up
                           :on-mouse-leave mouse-up
                           :on-mouse-move  mouse-move}
                     (let [scale-in (:scale-in @local)
                           scale-out (:scale-out @local)
                           w (- scale-out scale-in)]
                       [:g

                        (for [{:keys [first-seen-ts max-per-type]} @flows]
                          ^{:key (str first-seen-ts (keys max-per-type))}
                          [tick nil (calc-pos first-seen-ts) "green" 1 10 10 local])
                        [:line {:x1           10
                                :x2           (+ slider-width 10)
                                :y1           20
                                :y2           20
                                :stroke       "black"
                                :stroke-width 1}]
                        [:rect {:x             scale-in
                                :y             8
                                :width         w
                                :height        24
                                :stroke        "#333"
                                :stroke-width  1
                                :on-mouse-down rect-down
                                :fill          "#02c9d4"
                                :opacity       0.3}]
                        [:path {:transform     (str "translate(" scale-in ",0)")
                                :fill          "#555"
                                :on-mouse-down in-slider-down
                                :d             "M0,5 l0,30 l-4,0 l0,-20 l-7,0 l11,-10 L10,5 Z"}]
                        [:rect {:fill          "white"
                                :opacity       0.01
                                :x             (- scale-in 10)
                                :width         20
                                :height        40
                                :y             0
                                :on-mouse-down in-slider-down}]
                        [:path {:transform     (str "translate(" scale-out ",0) scale(-1,1)")
                                :fill          "#555"
                                :on-mouse-down out-slider-down
                                :d             "M0,5 l0,30 l-4,0 l0,-20 l-7,0 l11,-10 L10,5 Z"}]
                        [:rect {:fill          "white"
                                :opacity       0.01
                                :x             (- scale-out 10)
                                :width         20
                                :height        40
                                :y             0
                                :on-mouse-down out-slider-down}]])]]))]
    (r/create-class
      {:component-did-mount #(swap! local assoc-in [:zoom-node] (r/dom-node %))
       :reagent-render      render})))

(def default-timeline-state
  {:zoom-slider-pos   10
   :offset-slider-pos 10
   :left              0
   :zoom-left         0
   :x-scale           1
   :x-offset          0
   :elem-width        1000
   :scale-in          10
   :scale-out         550})

(defn timeline-view [first-flow-ts time-span ts put-fn]
  (let [avl-map (subscribe [:avl-map])
        flows (reaction (->> @avl-map
                             (map second)
                             (apply concat)
                             (map second)))
        local (r/atom default-timeline-state)
        hr (* 60 60 1000)
        timestamps (reaction (map :first-seen-ts @flows))
        hour-before-mn (reaction (let [mn (apply min @timestamps)] (- mn (mod mn hr))))
        hour-after-mx (reaction (let [mx (apply max @timestamps)] (+ (- mx (mod mx hr)) hr hr)))
        rng (reaction (- @hour-after-mx @hour-before-mn))
        hour-tick-timestamps (reaction (mapv #(+ @hour-before-mn (* % hr)) (range (/ @rng hr))))
        reset-fn (fn [_ev]
                   (put-fn [:timeline/set-ts nil])
                   (reset! local default-timeline-state))
        hr-label (fn [ts hrs-total]
                   (let [increment (condp < hrs-total
                                     168 24
                                     72 12
                                     36 6
                                     3)]
                     (zero? (mod (.hour (moment ts)) increment))))
        mouse-down #(swap! local assoc-in [:mouse-down] true)
        mouse-up #(swap! local assoc-in [:mouse-down] false)
        mouse-move (fn [ev]
                     (let [x (+ (- (.-clientX ev) (:left @local)) (:x-offset @local))
                           dom-node (:tl-node @local)
                           elem-w (if dom-node (.-offsetWidth dom-node) 1000)
                           x-scale (:x-scale @local)
                           tl-width (* elem-w x-scale)
                           ts (+ @hour-before-mn (* (/ x tl-width) @rng))]
                       (when (:mouse-down @local)
                         (swap! local assoc-in [:ts] ts)
                         (put-fn [:timeline/set-ts ts]))))
        render (fn timeline-render [first-flow-ts ts-range ts _put-fn]
                 (let [elem-w 1000
                       hrs-total (/ ts-range hr)
                       x-scale (:x-scale @local)
                       tl-width (* elem-w x-scale)
                       hrs-shown (/ hrs-total x-scale)
                       hrs-shown 6
                       x-offset (:x-offset @local)
                       tl-start @hour-before-mn
                       tl-start first-flow-ts
                       calc-pos #(* tl-width (/ (- % tl-start) ts-range))
                       hr-ticks (condp < hrs-shown
                                  144 (take-nth 6 @hour-tick-timestamps)
                                  72 (take-nth 3 @hour-tick-timestamps)
                                  @hour-tick-timestamps)]

                   [:div.timeline

                    ;[:pre [:code (with-out-str (pp/pprint (second @flows)))]]

                    [:div.widget-header
                     [:h2 "Timeline"]
                     ;[timeline-zoom local @hour-tick-timestamps]
                     [timeline-zoom local tl-start ts-range flows]
                     [:button {:on-click reset-fn
                               :class    (if (:ts @local) "shake active" "inactive")}
                      [:i.fa.fa-undo] "reset"]]
                    [:svg {                                 ;:viewBox       (str "0 0 " elem-w " 75")
                           :style         (if (:mouse-down @local) {:cursor :hand} {})
                           :on-mouse-down mouse-down
                           :on-mouse-up   mouse-up
                           :on-mouse-move mouse-move}
                     [:g
                      {:transform (str "translate(-" x-offset ",0)")}
                      [:line {:x1           0
                              :y1           35
                              :x2           tl-width
                              :y2           35
                              :stroke       "black"
                              :stroke-width 1}]
                      #_(for [ts (rest hr-ticks)]
                          ^{:key ts}
                          [tick ts (calc-pos ts) "black" 1 (if (hr-label ts hrs-total) 10 6) 35 local])
                      (for [{:keys [first-seen-ts max-per-type]} @flows]
                        ^{:key (str first-seen-ts (keys max-per-type))}
                        [tick ts (calc-pos first-seen-ts) "green" 3 10 10 local])
                      #_(for [ts @hour-tick-timestamps]
                          ^{:key ts}
                          [:text {:x           (calc-pos ts)
                                  :y           50
                                  :font-size   7
                                  :text-anchor "middle"}
                           (when (hr-label ts hrs-total)
                             (df ts hm))])
                      (for [ts @hour-tick-timestamps]
                        ^{:key ts}
                        [:text {:x           (calc-pos ts)
                                :y           61
                                :font-size   7
                                :text-anchor "middle"}
                         (when (zero? (mod (+ ts hr) (* hr 24)))
                           (df ts "DD.MM.YYYY"))])
                      (when ts
                        (let [playhead-pos (calc-pos ts)]
                          [:g
                           [:line.playhead
                            {:x1           playhead-pos
                             :y1           5
                             :x2           playhead-pos
                             :y2           60
                             :stroke       "#02c9d4"
                             :opacity      0.8
                             :stroke-width 3}]
                           [:text {:x           playhead-pos
                                   :y           72
                                   :font-size   9
                                   :font-weight "bold"
                                   :text-anchor "middle"
                                   :fill        "black"}
                            (df ts hm)]]))]]
                    ]))]
    (r/create-class
      {:component-did-mount #(swap! local assoc-in [:tl-node] (r/dom-node %))
       :reagent-render      render})))
