(ns finance.views.analytics
  "Analytics page view - reports and visualizations."
  (:require [re-frame.core :as rf]
            [finance.db :as db]
            [finance.utils.currency :as currency]
            [finance.components.icons :refer [icon]]))

(defn stat-card [{:keys [title value subtitle icon-name variant]}]
  [:div.flow-stat-card
   {:class (when variant (str "flow-stat-card--" (name variant)))}
   [:div.flow-stat-card__icon
    [icon icon-name {:width 24 :height 24}]]
   [:div.flow-stat-card__content
    [:span.flow-stat-card__value value]
    [:span.flow-stat-card__title title]
    (when subtitle
      [:span.flow-stat-card__subtitle subtitle])]])

(defn overview-section []
  (let [stats @(rf/subscribe [:analytics/overview-stats])]
    [:div.flow-analytics-overview
     [:h3.flow-analytics-section__title "Overview"]
     [:div.flow-stats-grid
      [stat-card {:title "Total Income"
                  :value (currency/format-currency (:total-income stats) :COP)
                  :icon-name :trending-up
                  :variant :income}]
      [stat-card {:title "Total Expenses"
                  :value (currency/format-currency (:total-expenses stats) :COP)
                  :icon-name :trending-down
                  :variant :expense}]
      [stat-card {:title "Net Balance"
                  :value (currency/format-currency (:net-balance stats) :COP)
                  :icon-name :dollar
                  :variant (if (pos? (:net-balance stats)) :income :expense)}]
      [stat-card {:title "Savings Rate"
                  :value (str (.toFixed (or (:savings-rate stats) 0) 1) "%")
                  :subtitle "of income saved"
                  :icon-name :piggy-bank
                  :variant :neutral}]
      [stat-card {:title "Transactions"
                  :value (:transaction-count stats)
                  :subtitle "total recorded"
                  :icon-name :list
                  :variant :neutral}]]]))

(defn income-expense-chart []
  (let [data @(rf/subscribe [:analytics/income-expense-comparison])]
    [:div.flow-analytics-chart
     [:h4.flow-analytics-chart__title "Income vs Expenses (Monthly)"]
     (if (empty? data)
       [:div.flow-empty.flow-empty--sm
        [:p "No data available yet"]]
       [:div.flow-chart-container
        [:table.flow-data-table
         [:thead
          [:tr
           [:th "Month"]
           [:th "Income"]
           [:th "Expenses"]
           [:th "Net"]]]
         [:tbody
          (for [{:keys [month income expenses net]} data]
            ^{:key month}
            [:tr
             [:td month]
             [:td.flow-data-table__income (currency/format-currency income :COP)]
             [:td.flow-data-table__expense (currency/format-currency expenses :COP)]
             [:td {:class (if (pos? net) "flow-data-table__income" "flow-data-table__expense")}
              (currency/format-currency net :COP)]])]]])]))

(defn top-categories-section []
  (let [expense-cats @(rf/subscribe [:analytics/top-expense-categories])
        income-sources @(rf/subscribe [:analytics/income-sources])]
    [:div.flow-analytics-categories
     [:div.flow-analytics-category-list
      [:h4.flow-analytics-chart__title "Top Expense Categories"]
      (if (empty? expense-cats)
        [:div.flow-empty.flow-empty--sm [:p "No expenses recorded"]]
        [:div.flow-category-list
         (for [{:keys [category total count]} expense-cats]
           ^{:key category}
           [:div.flow-category-item
            [:span.flow-category-item__icon (get db/category-icons category "📦")]
            [:span.flow-category-item__name (name category)]
            [:span.flow-category-item__count (str count " transactions")]
            [:span.flow-category-item__total (currency/format-currency total :COP)]])])]

     [:div.flow-analytics-category-list
      [:h4.flow-analytics-chart__title "Income Sources"]
      (if (empty? income-sources)
        [:div.flow-empty.flow-empty--sm [:p "No income recorded"]]
        [:div.flow-category-list
         (for [{:keys [category total count]} income-sources]
           ^{:key category}
           [:div.flow-category-item.flow-category-item--income
            [:span.flow-category-item__icon (get db/category-icons category "💰")]
            [:span.flow-category-item__name (name category)]
            [:span.flow-category-item__count (str count " transactions")]
            [:span.flow-category-item__total (currency/format-currency total :COP)]])])]]))

(defn budget-comparison-section []
  (let [data @(rf/subscribe [:analytics/budget-vs-actual])]
    [:div.flow-analytics-budgets
     [:h4.flow-analytics-chart__title "Budget vs Actual"]
     (if (empty? data)
       [:div.flow-empty.flow-empty--sm [:p "No budgets set"]]
       [:div.flow-budget-comparison
        (for [{:keys [category budgeted spent percentage status]} data]
          ^{:key category}
          [:div.flow-budget-row
           {:class (case status :exceeded "flow-budget-row--exceeded" :warning "flow-budget-row--warning" "")}
           [:div.flow-budget-row__category
            [:span.flow-budget-row__icon (get db/category-icons category "📦")]
            [:span.flow-budget-row__name (name category)]]
           [:div.flow-budget-row__bar
            [:div.flow-budget-row__progress
             {:style {:width (str (min 100 percentage) "%")}}]]
           [:div.flow-budget-row__values
            [:span.flow-budget-row__spent (currency/format-currency spent :COP)]
            [:span.flow-budget-row__separator "/"]
            [:span.flow-budget-row__budget (currency/format-currency budgeted :COP)]]
           [:span.flow-budget-row__percent (str (.toFixed percentage 0) "%")]])])]))

(defn currency-breakdown-section []
  (let [by-currency @(rf/subscribe [:analytics/currency-breakdown])]
    [:div.flow-analytics-currency
     [:h4.flow-analytics-chart__title "By Currency"]
     [:div.flow-currency-cards
      (for [[curr data] by-currency]
        ^{:key curr}
        [:div.flow-currency-card
         [:div.flow-currency-card__header
          [:span.flow-currency-card__name (name curr)]
          [:span.flow-currency-card__count (str (:count data) " transactions")]]
         [:div.flow-currency-card__stats
          [:div.flow-currency-card__stat
           [:span.flow-currency-card__label "Income"]
           [:span.flow-currency-card__value.flow-currency-card__value--income
            (currency/format-currency (:income data) curr)]]
          [:div.flow-currency-card__stat
           [:span.flow-currency-card__label "Expenses"]
           [:span.flow-currency-card__value.flow-currency-card__value--expense
            (currency/format-currency (:expenses data) curr)]]
          [:div.flow-currency-card__stat
           [:span.flow-currency-card__label "Balance"]
           [:span.flow-currency-card__value
            {:class (if (pos? (:balance data)) "flow-currency-card__value--income" "flow-currency-card__value--expense")}
            (currency/format-currency (:balance data) curr)]]]])]]))

(defn view-tabs []
  (let [selected @(rf/subscribe [:analytics/selected-view])]
    [:div.flow-tabs
     (for [[key label] [[:overview "Overview"]
                        [:income-expense "Income/Expense"]
                        [:categories "Categories"]
                        [:budgets "Budgets"]]]
       ^{:key key}
       [:button.flow-tabs__tab
        {:class (when (= selected key) "flow-tabs__tab--active")
         :on-click #(rf/dispatch [:analytics/set-view key])}
        label])]))

(defn analytics-view []
  (let [selected-view @(rf/subscribe [:analytics/selected-view])
        loading? @(rf/subscribe [:analytics/loading?])]
    [:div.flow-analytics-page
     [:div.flow-page-header
      [:h1.flow-page-title "Analytics"]
      [:p.flow-page-subtitle "Insights into your financial health"]]

     [view-tabs]

     (if loading?
       [:div.flow-tx-loading
        [:div.flow-skeleton.flow-skeleton--animated
         (for [i (range 4)]
           ^{:key i}
           [:div.flow-skeleton__row
            [:div.flow-skeleton__circle]
            [:div.flow-skeleton__lines
             [:div.flow-skeleton__line {:style {:width "60%"}}]
             [:div.flow-skeleton__line.flow-skeleton__line--sm {:style {:width "40%"}}]]])]]

       [:div.flow-analytics-content
        (case selected-view
          :overview
          [:<>
           [overview-section]
           [currency-breakdown-section]]

          :income-expense
          [income-expense-chart]

          :categories
          [top-categories-section]

          :budgets
          [budget-comparison-section]

          [overview-section])])]))
