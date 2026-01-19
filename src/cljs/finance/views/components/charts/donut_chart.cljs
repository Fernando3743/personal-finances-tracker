(ns finance.views.components.charts.donut-chart
  "Pure SVG donut chart component for category breakdown visualization."
  (:require [finance.utils.currency :as currency]))

(defn donut-chart
  "Renders a pure SVG donut chart with segments.

   Props:
   - :segments - Vector of maps with :category :amount :percent :color keys
                 e.g. [{:category :food :amount 450 :percent 35 :color \"#F59E0B\"} ...]
   - :size - Chart size (default 160)
   - :stroke-width - Ring thickness (default 24)
   - :center-icon - Optional icon/emoji for center
   - :total-amount - Total amount for center display
   - :currency - Currency for formatting (default :COP)"
  [{:keys [segments size stroke-width center-icon total-amount currency]}]
  (let [size (or size 160)
        stroke-w (or stroke-width 24)
        radius (/ (- size stroke-w) 2)
        circumference (* 2 js/Math.PI radius)
        center (/ size 2)
        curr (or currency :COP)]

    [:div {:class "relative inline-flex items-center justify-center"}
     [:svg {:width size
            :height size
            :viewBox (str "0 0 " size " " size)
            :class "transform -rotate-90"}

      ;; Background circle
      [:circle {:cx center
                :cy center
                :r radius
                :fill "none"
                :stroke "currentColor"
                :stroke-width stroke-w
                :class "text-neutral-200 dark:text-neutral-700"}]

      ;; Segment arcs
      (let [offset (atom 0)]
        (for [{:keys [percent color category]} segments]
          (let [dash (* (/ percent 100) circumference)
                gap (- circumference dash)
                current-offset @offset]
            (swap! offset + dash)
            ^{:key (str "segment-" category)}
            [:circle {:cx center
                      :cy center
                      :r radius
                      :fill "none"
                      :stroke color
                      :stroke-width stroke-w
                      :stroke-dasharray (str dash " " gap)
                      :stroke-dashoffset (- current-offset)
                      :class "hover:opacity-80 transition-opacity cursor-pointer"}])))]

     ;; Center content
     [:div {:class "absolute inset-0 flex flex-col items-center justify-center pointer-events-none"}
      (when center-icon
        [:span {:class "text-2xl mb-1"} center-icon])
      (when total-amount
        [:<>
         [:span {:class "text-xs text-neutral-500 dark:text-neutral-400 font-medium"} "TOTAL"]
         [:span {:class "text-base font-bold text-neutral-900 dark:text-neutral-50"}
          (currency/format-currency total-amount curr)]])]]))
