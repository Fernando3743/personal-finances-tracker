(ns finance.views.dashboard
  "Dashboard view with financial overview and metrics."
  (:require [re-frame.core :as rf]
            [clojure.string :as str]
            [finance.utils.currency :as currency]
            [finance.components.icons :refer [icon]]))

(defn format-day [date]
  (when date
    (let [d (js/Date. date)]
      (.getDate d))))

(defn format-month-year [date]
  (when date
    (let [d (js/Date. date)]
      (.toLocaleDateString d "en-US" #js {:month "short" :year "numeric"}))))

(defn currency-card [{:keys [currency balance income expenses]}]
  (let [currency-name (case currency
                        :COP "Colombian Peso"
                        :USD "US Dollar"
                        (name currency))
        trend-percent (if (pos? balance) 2.5 -0.4)
        trend-positive? (pos? trend-percent)]
    [:div.currency-card
     {:class (str "currency-card--" (name currency))}
     [:div.currency-card__header
      [:div.currency-card__info
       [:div.currency-card__badge (name currency)]
       [:div.currency-card__details
        [:span.currency-card__name currency-name]
        [:span.currency-card__balance (currency/format-currency balance currency)]]]
      [:div.currency-card__trend
       {:class (if trend-positive? "currency-card__trend--up" "currency-card__trend--down")}
       [icon (if trend-positive? :trending-up :trending-down) {:width 14 :height 14}]
       [:span (str (when trend-positive? "+") trend-percent "%")]]]
     [:div.currency-card__footer
      [:div.currency-card__stat
       [:div.currency-card__stat-header
        [:span.currency-card__stat-dot.currency-card__stat-dot--income]
        [:span.currency-card__stat-label "Income"]]
       [:span.currency-card__stat-value (currency/format-currency income currency)]]
      [:div.currency-card__stat
       [:div.currency-card__stat-header
        [:span.currency-card__stat-dot.currency-card__stat-dot--expense]
        [:span.currency-card__stat-label "Expenses"]]
       [:span.currency-card__stat-value (currency/format-currency expenses currency)]]]]))

(defn balances-section []
  (let [currency-balances @(rf/subscribe [:dashboard/all-currency-balances])
        currencies @(rf/subscribe [:dashboard/available-currencies])]
    [:<>
     [:div.dashboard-header
      [:h1.dashboard-header__title "Dashboard"]
      [:p.dashboard-header__subtitle "Your finances across all currencies"]]
     [:div.currency-cards-grid
      (if (seq currency-balances)
        (for [{:keys [currency balance income expenses]} currency-balances]
          ^{:key currency}
          [currency-card {:currency currency :balance balance :income income :expenses expenses}])
        (for [curr currencies]
          ^{:key curr}
          [currency-card {:currency curr :balance 0 :income 0 :expenses 0}]))]]))

(defn transaction-row [{:keys [transaction/id transaction/date transaction/description
                               transaction/amount transaction/type transaction/category
                               transaction/currency]}]
  (let [day (format-day date)
        month-year (format-month-year date)
        is-income? (= type :income)
        curr (or currency :COP)
        display-text (or description (str (name (or category :other)) " transaction"))]
    [:div.transaction-row
     {:on-click #(rf/dispatch [:app/navigate :transactions])}
     [:div.transaction-row__date
      [:span.date-day day]
      [:span.date-month-year month-year]]
     [:div.transaction-row__description display-text]
     [:div.transaction-row__currency
      [:span.currency-badge (name curr)]]
     [:div.transaction-row__amount
      {:class (if is-income? "amount--positive" "amount--negative")}
      (if is-income?
        (currency/format-currency amount curr)
        (str "-" (currency/format-currency amount curr)))]
     [:span.transaction-row__chevron ">"]]))

(defn transactions-panel []
  (let [transactions @(rf/subscribe [:tx/recent-transactions])]
    [:div.transactions-panel
     [:div.transactions-panel__header
      [:div.transactions-panel__header-content
       [:h2.transactions-panel__title "Transactions"]
       [:p.transactions-panel__subtitle "Recent activity"]]
      [:a.view-all-link
       {:on-click #(rf/dispatch [:app/navigate :transactions])}
       "View all >"]]

     (if (empty? transactions)
       [:div.transactions-panel__empty
        [:p "No transactions yet"]
        [:button.flow-btn.flow-btn--primary
         {:on-click #(rf/dispatch [:app/navigate :add-transaction])}
         "Add Transaction"]]

       [:div.transactions-table
        (for [t transactions]
          ^{:key (or (:transaction/id t) (random-uuid))}
          [transaction-row t])])]))

(def category-colors
  {:housing "#7c3aed"
   :groceries "#22C55E"
   :food "#F59E0B"
   :restaurants "#F97316"
   :transportation "#10B981"
   :transport "#10B981"
   :utilities "#EAB308"
   :entertainment "#EC4899"
   :healthcare "#14B8A6"
   :shopping "#8B5CF6"
   :salary "#22C55E"
   :freelance "#06B6D4"
   :investments "#10B981"
   :gifts "#D946EF"
   :other "#EF4444"})

(defn generate-smooth-path [points width height max-val]
  (when (seq points)
    (let [n (count points)
          x-step (/ width (max 1 (dec n)))
          scale-y #(- height (* (/ % max-val) height))
          coords (map-indexed (fn [i v] [(* i x-step) (scale-y v)]) points)]
      (if (= n 1)
        (let [[x y] (first coords)]
          (str "M " x " " y))
        (str "M " (first (first coords)) " " (second (first coords))
             (apply str
                    (map (fn [[[x1 y1] [x2 y2]]]
                           (let [cp1x (+ x1 (* 0.3 (- x2 x1)))
                                 cp2x (- x2 (* 0.3 (- x2 x1)))]
                             (str " C " cp1x " " y1 " " cp2x " " y2 " " x2 " " y2)))
                         (partition 2 1 coords))))))))

(defn cash-flow-chart []
  (let [monthly-report @(rf/subscribe [:dashboard/monthly-report])
        months (or (:months monthly-report) [])
        income-data (if (seq months)
                      (mapv #(or (:income %) 0) months)
                      [800 1200 1800 2400 2800 3200])
        expense-data (if (seq months)
                       (mapv #(or (:expenses %) 0) months)
                       [600 800 1200 1600 1800 2000])
        max-val (max 1 (apply max (concat income-data expense-data)))
        rounded-max (* 1000 (js/Math.ceil (/ max-val 1000)))
        width 500
        height 200
        y-labels [4 3 2 1 0]]
    [:div.cash-flow-chart
     [:div.cash-flow-chart__y-axis
      (for [label y-labels]
        ^{:key label}
        [:span.cash-flow-chart__y-label (str label "k")])]
     [:div.cash-flow-chart__graph
      [:svg {:viewBox (str "0 0 " width " " height)
             :preserveAspectRatio "none"
             :class "cash-flow-chart__svg"}
       [:defs
        [:linearGradient {:id "incomeGradient" :x1 "0%" :y1 "0%" :x2 "0%" :y2 "100%"}
         [:stop {:offset "0%" :stop-color "#10b981" :stop-opacity "0.3"}]
         [:stop {:offset "100%" :stop-color "#10b981" :stop-opacity "0"}]]
        [:linearGradient {:id "expenseGradient" :x1 "0%" :y1 "0%" :x2 "0%" :y2 "100%"}
         [:stop {:offset "0%" :stop-color "#ef4444" :stop-opacity "0.3"}]
         [:stop {:offset "100%" :stop-color "#ef4444" :stop-opacity "0"}]]]
       (for [i (range 5)]
         ^{:key i}
         [:line {:x1 0 :y1 (* i (/ height 4)) :x2 width :y2 (* i (/ height 4))
                 :stroke "var(--color-border-subtle)" :stroke-dasharray "4 4"}])
       [:path {:d (str (generate-smooth-path income-data width height rounded-max)
                       " L " width " " height " L 0 " height " Z")
               :fill "url(#incomeGradient)"}]
       [:path {:d (generate-smooth-path income-data width height rounded-max)
               :fill "none" :stroke "#10b981" :stroke-width "2.5"}]
       [:path {:d (str (generate-smooth-path expense-data width height rounded-max)
                       " L " width " " height " L 0 " height " Z")
               :fill "url(#expenseGradient)"}]
       [:path {:d (generate-smooth-path expense-data width height rounded-max)
               :fill "none" :stroke "#ef4444" :stroke-width "2.5"}]]]]))

(defn cash-flow-widget []
  (let [time-range @(rf/subscribe [:dashboard/chart-time-range])]
    [:div.chart-panel
     [:div.chart-panel__header
      [:div.chart-panel__titles
       [:h3.chart-panel__title "Monthly Cash Flow"]
       [:p.chart-panel__subtitle "Income vs Expenses Analysis"]]
      [:div.time-range-selector
       (for [[key label] [[:week "W"] [:month "M"] [:year "Y"]]]
         ^{:key key}
         [:button.range-btn
          {:class (when (= time-range key) "range-btn--active")
           :on-click #(rf/dispatch [:dashboard/set-chart-range key])}
          label])]]
     [cash-flow-chart]]))

(defn donut-chart [{:keys [segments total]}]
  (let [size 160
        stroke-width 24
        radius (/ (- size stroke-width) 2)
        circumference (* 2 js/Math.PI radius)
        center (/ size 2)]
    [:div.donut-chart
     [:svg {:width size :height size :viewBox (str "0 0 " size " " size)}
      [:circle {:cx center :cy center :r radius
                :fill "none" :stroke "var(--color-border-subtle)" :stroke-width stroke-width}]
      (let [offset (atom 0)]
        (for [{:keys [percent color]} segments]
          (let [dash (* (/ percent 100) circumference)
                gap (- circumference dash)
                current-offset @offset]
            (swap! offset + dash)
            ^{:key color}
            [:circle {:cx center :cy center :r radius
                      :fill "none" :stroke color :stroke-width stroke-width
                      :stroke-dasharray (str dash " " gap)
                      :stroke-dashoffset (- (* 0.25 circumference) current-offset)
                      :style {:transform "rotate(-90deg)" :transform-origin "center"}}])))]
     [:div.donut-chart__center
      [:span.donut-chart__label "TOTAL"]
      [:span.donut-chart__value (currency/format-currency total :USD)]]]))

(defn category-donut-widget []
  (let [{:keys [categories]} @(rf/subscribe [:dashboard/category-totals])
        expense-cats (->> categories
                          (filter #(= (:type %) :expense))
                          (sort-by :total >)
                          (take 4))
        total-expenses (reduce + 0 (map :total expense-cats))
        segments (if (seq expense-cats)
                   (map (fn [{:keys [category total]}]
                          {:name (name category)
                           :percent (if (pos? total-expenses)
                                      (* 100 (/ total total-expenses))
                                      0)
                           :color (get category-colors category "#6B7280")})
                        expense-cats)
                   [{:name "Housing" :percent 45 :color "#7c3aed"}
                    {:name "Food" :percent 25 :color "#F59E0B"}
                    {:name "Transport" :percent 15 :color "#10B981"}
                    {:name "Others" :percent 15 :color "#EF4444"}])
        display-total (if (pos? total-expenses) total-expenses 2690)]
    [:div.chart-panel.chart-panel--category
     [:div.chart-panel__header
      [:h3.chart-panel__title "Spending by Category"]]
     [:div.category-chart-content
      [donut-chart {:segments segments :total display-total}]
      [:div.category-legend
       (for [{:keys [name percent color]} segments]
         ^{:key name}
         [:div.category-legend__item
          [:div.category-legend__info
           [:span.category-legend__dot {:style {:background-color color}}]
           [:span.category-legend__name (str/capitalize name)]]
          [:span.category-legend__percent (str (js/Math.round percent) "%")]])]]]))

(defn trend-chart-widget []
  [cash-flow-widget])

(defn category-summary-widget []
  [category-donut-widget])

(defn dashboard-skeleton []
  [:div.dashboard-skeleton
   [:div.skeleton-balances
    (for [i (range 3)]
      ^{:key i}
      [:div.skeleton-card])]
   [:div.skeleton-content
    [:div.skeleton-panel]
    [:div.skeleton-sidebar
     [:div.skeleton-widget]
     [:div.skeleton-widget]]]])

(defn dashboard-view []
  (let [loading? @(rf/subscribe [:app/loading?])]
    [:div.dashboard
     (if loading?
       [dashboard-skeleton]
       [:<>
        [balances-section]
        [:div.charts-section
         [cash-flow-widget]
         [category-donut-widget]]
        [transactions-panel]])]))
