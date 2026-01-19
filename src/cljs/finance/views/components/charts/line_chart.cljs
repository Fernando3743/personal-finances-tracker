(ns finance.views.components.charts.line-chart
  "Pure SVG line/area chart component for cash flow trend visualization.")

(defn smooth-path
  "Generate smooth Bezier curve path for line chart.
   Points are scaled to fit within the chart dimensions."
  [points width height max-val]
  (when (seq points)
    (let [n (count points)
          x-step (if (> n 1) (/ width (dec n)) 0)
          safe-max (if (pos? max-val) max-val 1)
          scale-y (fn [v] (- height (* (/ v safe-max) height)))
          coords (map-indexed (fn [i v] [(* i x-step) (scale-y v)]) points)]
      (if (= n 1)
        ;; Single point - just move to it
        (let [[x y] (first coords)]
          (str "M " x " " y))
        ;; Multiple points - smooth curve
        (str "M " (ffirst coords) " " (second (first coords))
             (apply str
                    (map (fn [[[x1 y1] [x2 y2]]]
                           (let [cp1x (+ x1 (* 0.3 (- x2 x1)))
                                 cp2x (- x2 (* 0.3 (- x2 x1)))]
                             (str " C " cp1x " " y1 " " cp2x " " y2 " " x2 " " y2)))
                         (partition 2 1 coords))))))))

(defn line-chart
  "Renders area + line chart for cash flow trend.

   Props:
   - :data - Vector of numeric values [1200 1500 1800 2100 ...]
   - :width - Chart width (default 300)
   - :height - Chart height (default 150)
   - :color - Line/area color (default violet)
   - :labels - Optional vector of labels for x-axis
   - :show-points - Show data points (default true)"
  [{:keys [data width height color labels show-points]}]
  (let [width (or width 300)
        height (or height 150)
        color (or color "#8b5cf6")
        show-points? (if (nil? show-points) true show-points)
        max-val (if (seq data)
                  (let [m (apply max (map #(js/Math.abs %) data))]
                    (if (pos? m) m 1))
                  1)
        rounded-max (let [r (* 1000 (js/Math.ceil (/ max-val 1000)))]
                      (if (pos? r) r 1))
        path-d (smooth-path data width height rounded-max)
        gradient-id (str "gradient-" (random-uuid))]

    [:svg {:viewBox (str "0 0 " width " " height)
           :preserveAspectRatio "none"
           :class "w-full h-full overflow-visible"}
     [:defs
      ;; Gradient for area fill
      [:linearGradient {:id gradient-id
                        :x1 "0%"
                        :y1 "0%"
                        :x2 "0%"
                        :y2 "100%"}
       [:stop {:offset "0%"
               :stop-color color
               :stop-opacity "0.3"}]
       [:stop {:offset "100%"
               :stop-color color
               :stop-opacity "0"}]]]

     ;; Grid lines (horizontal)
     (for [i (range 5)]
       ^{:key (str "grid-" i)}
       [:line {:x1 0
               :y1 (* i (/ height 4))
               :x2 width
               :y2 (* i (/ height 4))
               :stroke "currentColor"
               :stroke-dasharray "4 4"
               :stroke-width "1"
               :class "text-neutral-200 dark:text-neutral-700 opacity-50"}])

     ;; Area fill
     (when (seq data)
       [:path {:d (str path-d " L " width " " height " L 0 " height " Z")
               :fill (str "url(#" gradient-id ")")}])

     ;; Line stroke
     (when (seq data)
       [:path {:d path-d
               :fill "none"
               :stroke color
               :stroke-width "2.5"
               :stroke-linecap "round"
               :stroke-linejoin "round"
               :class "transition-all"}])

     ;; Data points
     (when (and show-points? (seq data))
       (let [n (count data)
             x-step (if (> n 1) (/ width (dec n)) 0)
             safe-max (if (pos? rounded-max) rounded-max 1)
             scale-y (fn [v] (- height (* (/ v safe-max) height)))]
         (map-indexed
          (fn [idx val]
            (let [x (* idx x-step)
                  y (scale-y val)]
              ^{:key (str "point-" idx)}
              [:circle {:cx x
                        :cy y
                        :r "3"
                        :fill "#ffffff"
                        :stroke color
                        :stroke-width "2"
                        :class "hover:r-5 transition-all cursor-pointer drop-shadow-md"}]))
          data)))

     ;; X-axis labels
     (when (seq labels)
       (let [n (count labels)
             x-step (if (> n 1) (/ width (dec n)) 0)]
         (map-indexed
          (fn [idx label]
            ^{:key (str "label-" idx)}
            [:text {:x (* idx x-step)
                    :y (- height 5)
                    :text-anchor (cond
                                   (= idx 0) "start"
                                   (= idx (dec n)) "end"
                                   :else "middle")
                    :class "text-[10px] fill-neutral-500 dark:fill-neutral-400 font-medium"}
             label])
          labels)))]))
