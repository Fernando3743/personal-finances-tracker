(ns finance.views.dashboard
  "Dashboard view with financial overview and metrics."
  (:require [re-frame.core :as rf]
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

(defn balance-card [{:keys [label amount icon-name curr]}]
  [:div.balance-card
   [:div.balance-card__icon
    [icon icon-name]]
   [:div.balance-card__content
    [:span.balance-card__label label]
    [:span.balance-card__amount (currency/format-currency amount (or curr :COP))]]])

(defn balances-section []
  (let [currency-balances @(rf/subscribe [:dashboard/all-currency-balances])
        currencies @(rf/subscribe [:dashboard/available-currencies])]
    [:section.dashboard-balances
     [:h2.dashboard-section-title "Balances"]
     [:p.dashboard-section-subtitle "Your finances across all currencies"]

     (if (seq currency-balances)
       [:div.currency-balances-grid
        (for [{:keys [currency balance income expenses]} currency-balances]
          ^{:key currency}
          [:div.currency-balance-group
           [:h3.currency-label (name currency)]
           [:div.balance-cards-row
            [balance-card {:label "Balance" :amount balance :curr currency :icon-name :wallet}]
            [balance-card {:label "Income" :amount income :curr currency :icon-name :arrow-up}]
            [balance-card {:label "Expenses" :amount expenses :curr currency :icon-name :arrow-down}]]])]

       [:div.currency-balances-grid
        (for [curr currencies]
          ^{:key curr}
          [:div.currency-balance-group
           [:h3.currency-label (name curr)]
           [:div.balance-cards-row
            [balance-card {:label "Balance" :amount 0 :curr curr :icon-name :wallet}]
            [balance-card {:label "Income" :amount 0 :curr curr :icon-name :arrow-up}]
            [balance-card {:label "Expenses" :amount 0 :curr curr :icon-name :arrow-down}]]])])]))

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
  {:groceries "#22C55E"
   :restaurants "#F97316"
   :transportation "#3B82F6"
   :utilities "#EAB308"
   :entertainment "#EC4899"
   :healthcare "#14B8A6"
   :shopping "#8B5CF6"
   :salary "#22C55E"
   :freelance "#06B6D4"
   :investments "#10B981"
   :gifts "#F43F5E"
   :other "#6B7280"})

(defn generate-path-d [points width height max-val]
  (when (seq points)
    (let [n (count points)
          x-step (/ width (max 1 (dec n)))
          scale-y #(- height (* (/ % max-val) height))]
      (str "M " 0 " " (scale-y (first points))
           (apply str
                  (map-indexed
                   (fn [i val]
                     (str " L " (* i x-step) " " (scale-y val)))
                   (rest points)))))))

(defn mini-trend-chart []
  (let [monthly-report @(rf/subscribe [:dashboard/monthly-report])
        months (or (:months monthly-report) [])
        income-data (if (seq months)
                      (mapv #(or (:income %) 0) months)
                      [100 150 120 200 180])
        expense-data (if (seq months)
                       (mapv #(or (:expenses %) 0) months)
                       [80 100 90 150 120])
        max-val (max 1 (apply max (concat income-data expense-data)))
        width 280
        height 130]
    [:svg.mini-trend-chart
     {:viewBox (str "0 0 " width " " height)
      :preserveAspectRatio "none"}
     [:path.mini-trend-chart__area--income
      {:d (str (generate-path-d income-data width height max-val)
               " L " width " " height " L 0 " height " Z")}]
     [:path.mini-trend-chart__area--expense
      {:d (str (generate-path-d expense-data width height max-val)
               " L " width " " height " L 0 " height " Z")}]
     [:path.mini-trend-chart__line.mini-trend-chart__line--income
      {:d (generate-path-d income-data width height max-val)}]
     [:path.mini-trend-chart__line.mini-trend-chart__line--expense
      {:d (generate-path-d expense-data width height max-val)}]]))

(defn trend-chart-widget []
  (let [time-range @(rf/subscribe [:dashboard/chart-time-range])]
    [:div.sidebar-widget
     [:div.sidebar-widget__header
      [:h3.sidebar-widget__title "Monthly Trends"]
      [:div.time-range-selector
       (for [[key label] [[:week "W"] [:month "M"] [:year "Y"]]]
         ^{:key key}
         [:button.range-btn
          {:class (when (= time-range key) "range-btn--active")
           :on-click #(rf/dispatch [:dashboard/set-chart-range key])}
          label])]]
     [:div.sidebar-widget__chart
      [mini-trend-chart]]]))

(defn category-summary-widget []
  (let [{:keys [categories]} @(rf/subscribe [:dashboard/category-totals])
        expense-cats (->> categories
                          (filter #(= (:type %) :expense))
                          (sort-by :total >)
                          (take 5))]
    [:div.sidebar-widget
     [:div.sidebar-widget__header
      [:h3.sidebar-widget__title "Top Categories"]]

     (if (seq expense-cats)
       [:div.category-list
        (for [{:keys [category total]} expense-cats]
          ^{:key category}
          [:div.category-item
           [:span.category-dot {:style {:background-color (get category-colors category "#6B7280")}}]
           [:span.category-item__name (name category)]
           [:span.category-item__amount (currency/format-currency total :COP)]])]
       [:div {:style {:text-align "center" :padding "1rem" :color "var(--color-text-tertiary)"}}
        [:p "No spending data yet"]])]))

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
     [:h1.dashboard__title "Dashboard"]
     (if loading?
       [dashboard-skeleton]
       [:<>
        [balances-section]
        [:div.dashboard__content
         [transactions-panel]
         [:div.dashboard__sidebar
          [trend-chart-widget]
          [category-summary-widget]]]])]))
