(ns finance.views.analytics
  "Analytics page view - reports and visualizations."
  (:require [re-frame.core :as rf]
            [finance.utils.currency :as currency]
            [finance.components.icons :refer [icon]]
            [finance.components.category-icon :refer [category-icon]]))

(defn stat-card [{:keys [title value subtitle icon-name variant]}]
  (let [variant-classes (case variant
                          :income "border-l-4 border-l-green-500"
                          :expense "border-l-4 border-l-red-500"
                          "")]
    [:div {:class (str "bg-white dark:bg-neutral-800 rounded-xl border border-neutral-200 dark:border-neutral-700 p-4 " variant-classes)}
     [:div {:class "flex items-start gap-3"}
      [:div {:class (str "p-2 rounded-lg "
                         (case variant
                           :income "bg-green-100 dark:bg-green-900/20 text-green-600 dark:text-green-500"
                           :expense "bg-red-100 dark:bg-red-900/20 text-red-600 dark:text-red-500"
                           "bg-purple-100 dark:bg-purple-900/20 text-purple-700 dark:text-purple-400"))}
       [icon icon-name {:width 24 :height 24}]]
      [:div
       [:div {:class "text-xl font-bold text-neutral-900 dark:text-neutral-50"} value]
       [:div {:class "text-sm text-neutral-600 dark:text-neutral-400"} title]
       (when subtitle
         [:div {:class "text-xs text-neutral-400 dark:text-neutral-500 mt-0.5"} subtitle])]]]))

(defn overview-section []
  (let [stats @(rf/subscribe [:analytics/overview-stats])]
    [:div {:class "space-y-4"}
     [:h3 {:class "text-lg font-semibold text-neutral-900 dark:text-neutral-50"} "Overview"]
     [:div {:class "grid grid-cols-1 sm:grid-cols-2 lg:grid-cols-3 xl:grid-cols-5 gap-4"}
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
    [:div {:class "bg-white dark:bg-neutral-800 rounded-xl border border-neutral-200 dark:border-neutral-700 p-5"}
     [:h4 {:class "text-lg font-semibold text-neutral-900 dark:text-neutral-50 mb-4"} "Income vs Expenses (Monthly)"]
     (if (empty? data)
       [:div {:class "text-center py-8"}
        [:p {:class "text-neutral-600 dark:text-neutral-400"} "No data available yet"]]
       [:div {:class "overflow-x-auto"}
        [:table {:class "w-full"}
         [:thead {:class "bg-neutral-100 dark:bg-neutral-800"}
          [:tr
           [:th {:class "py-3 px-4 text-left text-xs font-medium text-neutral-600 dark:text-neutral-400 uppercase"} "Month"]
           [:th {:class "py-3 px-4 text-right text-xs font-medium text-neutral-600 dark:text-neutral-400 uppercase"} "Income"]
           [:th {:class "py-3 px-4 text-right text-xs font-medium text-neutral-600 dark:text-neutral-400 uppercase"} "Expenses"]
           [:th {:class "py-3 px-4 text-right text-xs font-medium text-neutral-600 dark:text-neutral-400 uppercase"} "Net"]]]
         [:tbody
          (for [{:keys [month income expenses net]} data]
            ^{:key month}
            [:tr {:class "border-b border-neutral-200 dark:border-neutral-700"}
             [:td {:class "py-3 px-4 text-sm text-neutral-900 dark:text-neutral-50"} month]
             [:td {:class "py-3 px-4 text-right text-sm font-mono text-green-600 dark:text-green-500"} (currency/format-currency income :COP)]
             [:td {:class "py-3 px-4 text-right text-sm font-mono text-red-600 dark:text-red-500"} (currency/format-currency expenses :COP)]
             [:td {:class (str "py-3 px-4 text-right text-sm font-mono font-semibold "
                               (if (pos? net) "text-green-600 dark:text-green-500" "text-red-600 dark:text-red-500"))}
              (currency/format-currency net :COP)]])]]])]))

(defn top-categories-section []
  (let [expense-cats @(rf/subscribe [:analytics/top-expense-categories])
        income-sources @(rf/subscribe [:analytics/income-sources])]
    [:div {:class "grid grid-cols-1 lg:grid-cols-2 gap-6"}
     [:div {:class "bg-white dark:bg-neutral-800 rounded-xl border border-neutral-200 dark:border-neutral-700 p-5"}
      [:h4 {:class "text-lg font-semibold text-neutral-900 dark:text-neutral-50 mb-4"} "Top Expense Categories"]
      (if (empty? expense-cats)
        [:div {:class "text-center py-8"}
         [:p {:class "text-neutral-600 dark:text-neutral-400"} "No expenses recorded"]]
        [:div {:class "space-y-3"}
         (for [{:keys [category total count]} expense-cats]
           ^{:key category}
           [:div {:class "flex items-center gap-3 p-3 bg-neutral-100 dark:bg-neutral-800 rounded-lg"}
            [category-icon category {:size :sm}]
            [:div {:class "flex-1 min-w-0"}
             [:div {:class "text-sm font-medium text-neutral-900 dark:text-neutral-50"} (name category)]
             [:div {:class "text-xs text-neutral-400 dark:text-neutral-500"} (str count " transactions")]]
            [:div {:class "font-mono font-semibold text-red-600 dark:text-red-500"} (currency/format-currency total :COP)]])])]

     [:div {:class "bg-white dark:bg-neutral-800 rounded-xl border border-neutral-200 dark:border-neutral-700 p-5"}
      [:h4 {:class "text-lg font-semibold text-neutral-900 dark:text-neutral-50 mb-4"} "Income Sources"]
      (if (empty? income-sources)
        [:div {:class "text-center py-8"}
         [:p {:class "text-neutral-600 dark:text-neutral-400"} "No income recorded"]]
        [:div {:class "space-y-3"}
         (for [{:keys [category total count]} income-sources]
           ^{:key category}
           [:div {:class "flex items-center gap-3 p-3 bg-neutral-100 dark:bg-neutral-800 rounded-lg"}
            [category-icon category {:size :sm}]
            [:div {:class "flex-1 min-w-0"}
             [:div {:class "text-sm font-medium text-neutral-900 dark:text-neutral-50"} (name category)]
             [:div {:class "text-xs text-neutral-400 dark:text-neutral-500"} (str count " transactions")]]
            [:div {:class "font-mono font-semibold text-green-600 dark:text-green-500"} (currency/format-currency total :COP)]])])]]))

(defn budget-comparison-section []
  (let [data @(rf/subscribe [:analytics/budget-vs-actual])]
    [:div {:class "bg-white dark:bg-neutral-800 rounded-xl border border-neutral-200 dark:border-neutral-700 p-5"}
     [:h4 {:class "text-lg font-semibold text-neutral-900 dark:text-neutral-50 mb-4"} "Budget vs Actual"]
     (if (empty? data)
       [:div {:class "text-center py-8"}
        [:p {:class "text-neutral-600 dark:text-neutral-400"} "No budgets set"]]
       [:div {:class "space-y-4"}
        (for [{:keys [category budgeted spent percentage status]} data]
          ^{:key category}
          [:div {:class (str "p-3 rounded-lg border "
                             (case status :exceeded "border-red-500 bg-red-100/30 dark:bg-red-900/20" :warning "border-amber-500 bg-amber-100/30 dark:bg-amber-900/20" "border-neutral-200 dark:border-neutral-700 bg-neutral-100 dark:bg-neutral-800"))}
           [:div {:class "flex items-center justify-between mb-2"}
            [:div {:class "flex items-center gap-2"}
             [category-icon category {:size :sm}]
             [:span {:class "text-sm font-medium text-neutral-900 dark:text-neutral-50"} (name category)]]
            [:span {:class "text-sm font-medium text-neutral-600 dark:text-neutral-400"} (str (.toFixed percentage 0) "%")]]
           [:div {:class "h-2 bg-neutral-200 dark:bg-neutral-700 rounded-full overflow-hidden mb-2"}
            [:div {:class (str "h-full rounded-full transition-all "
                               (case status :exceeded "bg-red-500" :warning "bg-amber-500" "bg-green-500"))
                   :style {:width (str (min 100 percentage) "%")}}]]
           [:div {:class "flex items-center justify-between text-xs"}
            [:span {:class "text-neutral-600 dark:text-neutral-400"} (str "Spent: " (currency/format-currency spent :COP))]
            [:span {:class "text-neutral-400 dark:text-neutral-500"} (str "Budget: " (currency/format-currency budgeted :COP))]]])])]))

(defn currency-breakdown-section []
  (let [by-currency @(rf/subscribe [:analytics/currency-breakdown])]
    [:div {:class "bg-white dark:bg-neutral-800 rounded-xl border border-neutral-200 dark:border-neutral-700 p-5"}
     [:h4 {:class "text-lg font-semibold text-neutral-900 dark:text-neutral-50 mb-4"} "By Currency"]
     [:div {:class "grid grid-cols-1 md:grid-cols-2 gap-4"}
      (for [[curr data] by-currency]
        ^{:key curr}
        [:div {:class "p-4 bg-neutral-100 dark:bg-neutral-800 rounded-lg"}
         [:div {:class "flex items-center justify-between mb-3"}
          [:span {:class "text-lg font-semibold text-neutral-900 dark:text-neutral-50"} (name curr)]
          [:span {:class "text-xs text-neutral-400 dark:text-neutral-500"} (str (:count data) " transactions")]]
         [:div {:class "grid grid-cols-3 gap-4 text-sm"}
          [:div
           [:span {:class "text-xs text-neutral-400 dark:text-neutral-500"} "Income"]
           [:div {:class "font-mono font-semibold text-green-600 dark:text-green-500"} (currency/format-currency (:income data) curr)]]
          [:div
           [:span {:class "text-xs text-neutral-400 dark:text-neutral-500"} "Expenses"]
           [:div {:class "font-mono font-semibold text-red-600 dark:text-red-500"} (currency/format-currency (:expenses data) curr)]]
          [:div
           [:span {:class "text-xs text-neutral-400 dark:text-neutral-500"} "Balance"]
           [:div {:class (str "font-mono font-semibold "
                              (if (pos? (:balance data)) "text-green-600 dark:text-green-500" "text-red-600 dark:text-red-500"))}
            (currency/format-currency (:balance data) curr)]]]])]]))

(defn view-tabs []
  (let [selected @(rf/subscribe [:analytics/selected-view])]
    [:div {:class "flex gap-1 p-1 bg-neutral-100 dark:bg-neutral-800 rounded-lg mb-6 overflow-x-auto"}
     (for [[key label] [[:overview "Overview"]
                        [:income-expense "Income/Expense"]
                        [:categories "Categories"]
                        [:budgets "Budgets"]]]
       ^{:key key}
       [:button {:class (str "px-4 py-2 rounded-md text-sm font-medium whitespace-nowrap transition-colors "
                             (if (= selected key)
                               "bg-white dark:bg-neutral-800 text-neutral-900 dark:text-neutral-50 shadow-sm"
                               "text-neutral-600 dark:text-neutral-400 hover:text-neutral-900 dark:hover:text-neutral-50"))
                 :on-click #(rf/dispatch [:analytics/set-view key])}
        label])]))

(defn analytics-view []
  (let [selected-view @(rf/subscribe [:analytics/selected-view])
        loading? @(rf/subscribe [:analytics/loading?])]
    [:div {:class "space-y-6"}
     [:div {:class "mb-6"}
      [:h1 {:class "text-2xl font-bold text-neutral-900 dark:text-neutral-50"} "Analytics"]
      [:p {:class "text-neutral-600 dark:text-neutral-400 text-sm mt-1"} "Insights into your financial health"]]

     [view-tabs]

     (if loading?
       [:div {:class "space-y-4"}
        (for [i (range 4)]
          ^{:key i}
          [:div {:class "flex items-center gap-4 p-4 bg-neutral-100 dark:bg-neutral-800 rounded-lg animate-pulse"}
           [:div {:class "w-10 h-10 rounded-full bg-neutral-200 dark:bg-neutral-700"}]
           [:div {:class "flex-1 space-y-2"}
            [:div {:class "h-4 bg-neutral-200 dark:bg-neutral-700 rounded w-3/5"}]
            [:div {:class "h-3 bg-neutral-200 dark:bg-neutral-700 rounded w-2/5"}]]])]

       [:div {:class "space-y-6"}
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
