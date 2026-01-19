(ns finance.views.components.charts.bar-chart
  "Pure SVG bar chart component for daily spending visualization.")

(defn bar-chart
  "Renders a pure SVG bar chart for daily spending.

   Props:
   - :data - Vector of maps with :day :amount :label keys
             e.g. [{:day \"Mon\" :amount 1200 :label \"M\"} ...]
   - :width - Chart width (default 300)
   - :height - Chart height (default 150)
   - :bar-color - Bar fill color (default violet-500)
   - :max-value - Optional max value for y-axis scaling"
  [{:keys [data width height bar-color max-value]}]
  (let [width (or width 300)
        height (or height 150)
        bar-color (or bar-color "#8b5cf6")
        bar-count (count data)
        bar-width (/ width (* bar-count 1.5))
        spacing (* bar-width 0.5)
        max-val (or max-value (if (seq data)
                                (let [m (apply max (map :amount data))]
                                  (if (pos? m) m 1))
                                1))
        scale-y (fn [amount] (if (pos? max-val)
                               (* (/ amount max-val) height)
                               0))]

    [:svg {:viewBox (str "0 0 " width " " height)
           :class "w-full h-full overflow-visible"
           :preserveAspectRatio "none"}

     ;; Grid lines (horizontal)
     (for [i (range 5)]
       ^{:key (str "grid-" i)}
       [:line {:x1 0
               :y1 (* i (/ height 4))
               :x2 width
               :y2 (* i (/ height 4))
               :stroke "currentColor"
               :stroke-width "1"
               :stroke-dasharray "4 4"
               :class "text-neutral-200 dark:text-neutral-700 opacity-50"}])

     ;; Bars
     (map-indexed
      (fn [idx {:keys [amount label]}]
        (let [bar-height (scale-y amount)
              x (+ (* idx (+ bar-width spacing)) spacing)
              is-highest? (= amount max-val)]
          ^{:key (str "bar-" idx)}
          [:g
           ;; Background bar (track)
           [:rect {:x x
                   :y 0
                   :width bar-width
                   :height height
                   :rx "4"
                   :class "fill-neutral-100 dark:fill-neutral-800"}]

           ;; Actual bar
           [:rect {:x x
                   :y (- height bar-height)
                   :width bar-width
                   :height bar-height
                   :rx "4"
                   :class (str "transition-all duration-300 hover:opacity-80 cursor-pointer "
                               (if is-highest?
                                 "fill-violet-600 dark:fill-violet-500"
                                 "fill-violet-400 dark:fill-violet-500 opacity-60"))}]

           ;; Label
           [:text {:x (+ x (/ bar-width 2))
                   :y (- height 8)
                   :text-anchor "middle"
                   :class (str "text-[10px] font-medium pointer-events-none "
                               (if is-highest?
                                 "fill-violet-600 dark:fill-violet-400"
                                 "fill-neutral-500 dark:fill-neutral-400"))}
            label]]))
      data)]))
