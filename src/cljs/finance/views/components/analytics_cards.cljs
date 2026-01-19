(ns finance.views.components.analytics-cards
  "Analytics card components for transactions dashboard."
  (:require [re-frame.core :as rf]
            [finance.views.components.charts.bar-chart :refer [bar-chart]]
            [finance.views.components.charts.donut-chart :refer [donut-chart]]
            [finance.views.components.charts.line-chart :refer [line-chart]]
            [finance.utils.currency :as currency]
            [finance.components.icons :refer [icon]]))

(defn daily-spending-card []
  (let [data @(rf/subscribe [:tx/daily-spending-data])
        total-spending (reduce + 0 (map :amount data))
        max-day (apply max-key :amount data)
        avg-spending (if (seq data) (/ total-spending (count data)) 0)]
    [:div {:class "bg-white dark:bg-neutral-800 p-5 rounded-xl shadow-sm border border-neutral-200 dark:border-neutral-700"}
     [:div {:class "flex justify-between items-start mb-4"}
      [:div
       [:h3 {:class "text-xs font-semibold text-neutral-500 dark:text-neutral-400 uppercase tracking-wider"} "Daily Spending"]
       [:div {:class "flex items-baseline gap-2 mt-1"}
        [:span {:class "text-2xl font-bold text-neutral-900 dark:text-neutral-50"}
         (currency/format-currency (:amount max-day) :COP)]
        (when (> total-spending 0)
          [:span {:class "text-xs font-medium text-green-600 dark:text-green-500 bg-green-100 dark:bg-green-900/20 px-1.5 py-0.5 rounded flex items-center gap-0.5"}
           [icon :trending-up {:width 12 :height 12}]
           (str (:label max-day))])]]
      [:button {:class "text-neutral-400 dark:text-neutral-500 hover:text-violet-600 dark:hover:text-violet-400 transition-colors"
                :title "More options"}
       [icon :more-horizontal {:width 20 :height 20}]]]

     ;; Bar chart
     [:div {:class "h-32 w-full"}
      [bar-chart {:data data
                  :width 300
                  :height 120}]]

     ;; Average info
     [:div {:class "mt-3 pt-3 border-t border-neutral-100 dark:border-neutral-700 text-xs text-neutral-600 dark:text-neutral-400"}
      (str "Avg: " (currency/format-currency avg-spending :COP) "/day")]]))

(defn top-categories-card []
  (let [cat-data @(rf/subscribe [:tx/top-categories-data])
        segments (:segments cat-data)
        total (:total cat-data)
        has-data? (and (seq segments) (pos? total))]
    [:div {:class "bg-white dark:bg-neutral-800 p-5 rounded-xl shadow-sm border border-neutral-200 dark:border-neutral-700"}
     [:div {:class "flex justify-between items-start mb-4"}
      [:div
       [:h3 {:class "text-xs font-semibold text-neutral-500 dark:text-neutral-400 uppercase tracking-wider"} "Top Categories"]
       [:div {:class "flex items-baseline gap-2 mt-1"}
        [:span {:class "text-2xl font-bold text-neutral-900 dark:text-neutral-50"}
         (currency/format-currency total :COP)]
        [:span {:class "text-xs font-medium text-neutral-500 dark:text-neutral-400"} "total"]]]
      [:button {:class "text-neutral-400 dark:text-neutral-500 hover:text-violet-600 dark:hover:text-violet-400 transition-colors"
                :title "Filter"}
       [icon :filter {:width 20 :height 20}]]]

     ;; Content: Show either data or empty state
     (if has-data?
       ;; Donut chart + Legend
       [:div {:class "flex items-center gap-4 h-32"}
        [:div {:class "flex-shrink-0"}
         [donut-chart {:segments segments
                       :size 96
                       :stroke-width 20
                       :center-icon "🛒"
                       :total-amount total
                       :currency :COP}]]
        [:div {:class "flex flex-col gap-2 flex-1 min-w-0"}
         (for [{:keys [category amount percent color]} (take 3 segments)]
           ^{:key category}
           [:div {:class "flex items-center justify-between text-xs"}
            [:div {:class "flex items-center gap-2 min-w-0"}
             [:span {:class "w-2 h-2 rounded-full flex-shrink-0"
                     :style {:background-color color}}]
             [:span {:class "font-medium text-neutral-900 dark:text-neutral-50 truncate"}
              (name category)]]
            [:span {:class "font-bold text-neutral-900 dark:text-neutral-50 ml-2"}
             (str (js/Math.round percent) "%")]])]]

       ;; Empty state
       [:div {:class "flex flex-col items-center justify-center h-32 text-center"}
        [:div {:class "text-3xl mb-2"} "📊"]
        [:p {:class "text-sm text-neutral-500 dark:text-neutral-400"}
         "No expenses yet"]
        [:p {:class "text-xs text-neutral-400 dark:text-neutral-500"}
         "Add transactions to see category breakdown"]])]))

(defn net-cash-flow-card []
  (let [flow-data @(rf/subscribe [:tx/net-cash-flow-data])
        data (:data flow-data)
        labels (:labels flow-data)
        current-week (last data)
        previous-week (if (> (count data) 1) (nth data (- (count data) 2)) 0)
        change-percent (if (zero? previous-week)
                         0
                         (js/Math.round (* (/ (- current-week previous-week) (js/Math.abs previous-week)) 100)))
        is-positive? (>= current-week 0)
        is-increasing? (> current-week previous-week)]
    [:div {:class "bg-white dark:bg-neutral-800 p-5 rounded-xl shadow-sm border border-neutral-200 dark:border-neutral-700"}
     [:div {:class "flex justify-between items-start mb-4"}
      [:div
       [:h3 {:class "text-xs font-semibold text-neutral-500 dark:text-neutral-400 uppercase tracking-wider"} "Net Cash Flow"]
       [:div {:class "flex items-baseline gap-2 mt-1"}
        [:span {:class (str "text-2xl font-bold "
                            (if is-positive?
                              "text-green-600 dark:text-green-500"
                              "text-red-600 dark:text-red-500"))}
         (currency/format-currency current-week :COP {:show-sign? true})]
        (when (not (zero? change-percent))
          [:span {:class (str "text-xs font-medium px-1.5 py-0.5 rounded flex items-center gap-0.5 "
                              (if is-increasing?
                                "text-green-600 dark:text-green-500 bg-green-100 dark:bg-green-900/20"
                                "text-red-600 dark:text-red-500 bg-red-100 dark:bg-red-900/20"))}
           [icon (if is-increasing? :arrow-up :arrow-down) {:width 12 :height 12}]
           (str (js/Math.abs change-percent) "%")])]]
      [:button {:class "text-neutral-400 dark:text-neutral-500 hover:text-violet-600 dark:hover:text-violet-400 transition-colors"
                :title "Expand"}
       [icon :maximize {:width 20 :height 20}]]]

     ;; Line chart
     [:div {:class "h-32 w-full"}
      [line-chart {:data data
                   :width 300
                   :height 120
                   :color "#8b5cf6"
                   :labels (take-last 2 labels)
                   :show-points true}]]]))

(defn analytics-cards-row []
  "Three analytics cards in a responsive grid with staggered animation."
  [:div {:class "grid grid-cols-1 lg:grid-cols-3 gap-6 mb-6 animate-stagger"}
   [daily-spending-card]
   [top-categories-card]
   [net-cash-flow-card]])
