(ns finance.views.transactions.analytics-table
  "Analytics-first table view with analytics cards and enhanced table."
  (:require [re-frame.core :as rf]
            [finance.views.components.analytics-cards :refer [analytics-cards-row]]
            [finance.views.transactions.pagination :refer [pagination]]
            [finance.utils.currency :as currency]
            [finance.components.icons :refer [icon]]
            [finance.components.category-icon :refer [category-icon category-badge]]))

(defn format-date-short [date-str]
  (when date-str
    (let [date (js/Date. date-str)]
      (.toLocaleDateString date "en-US" #js {:month "short"
                                             :day "numeric"}))))

(defn table-controls []
  "Month selector and Add Transaction button."
  [:div {:class "flex flex-col sm:flex-row sm:items-center justify-between gap-4 mb-4"}
   [:div {:class "flex items-center gap-3"}
     ;; Month selector
    [:button {:class (str "flex items-center gap-2 h-10 px-4 rounded-lg border border-neutral-200 dark:border-neutral-700 "
                          "bg-white dark:bg-neutral-800 text-sm font-semibold "
                          "hover:bg-neutral-50 dark:hover:bg-neutral-700 transition-colors shadow-sm")
              :title "Select month"}
     [icon :calendar {:width 18 :height 18}]
     [:span "This Month"]
     [icon :chevron-down {:width 18 :height 18}]]]

   ;; Add Transaction button
   [:button {:class (str "h-10 px-5 rounded-lg bg-violet-600 hover:bg-violet-700 text-white text-sm font-bold "
                         "flex items-center gap-2 shadow-lg shadow-violet-600/20 transition-all")
             :on-click #(rf/dispatch [:app/open-panel :add-transaction])}
    [icon :plus {:width 18 :height 18}]
    [:span "Add Transaction"]]])

(defn enhanced-transaction-table-row
  [{:keys [transaction/id transaction/amount transaction/type
           transaction/category transaction/description
           transaction/date transaction/currency]}]
  (let [is-income? (= type :income)
        curr (or currency :COP)
        display-amount (if is-income? amount (- amount))]
    [:tr {:class "group border-b border-neutral-200 dark:border-neutral-700 last:border-0 hover:bg-neutral-50 dark:hover:bg-neutral-800 transition-colors"}
     ;; Date
     [:td {:class "py-4 px-6 text-sm text-neutral-600 dark:text-neutral-400 whitespace-nowrap"}
      (format-date-short date)]

     ;; Description with icon
     [:td {:class "py-4 px-6"}
      [:div {:class "flex items-center gap-3"}
       [category-icon category {:size :sm}]
       [:span {:class "text-sm font-semibold text-neutral-900 dark:text-neutral-50"}
        (or description "No description")]]]

     ;; Category badge
     [:td {:class "py-4 px-6"}
      [category-badge category]]

     ;; Payment method (placeholder)
     [:td {:class "py-4 px-6 text-neutral-600 dark:text-neutral-400 flex items-center gap-2 text-sm"}
      [icon :credit-card {:width 18 :height 18}]
      (case curr
        :USD "Visa ••8890"
        :COP "Chase ••4242"
        "Cash")]

     ;; Amount
     [:td {:class (str "py-4 px-6 text-right font-mono font-bold text-sm "
                       (if is-income? "text-green-600 dark:text-green-500" "text-red-600 dark:text-red-500"))}
      (currency/format-currency display-amount curr {:show-sign? true})]

     ;; Actions (show on hover)
     [:td {:class "py-4 px-6 text-right opacity-0 group-hover:opacity-100 transition-opacity"}
      [:button {:class "p-2 rounded-lg text-neutral-400 dark:text-neutral-500 hover:text-red-500 hover:bg-red-100 dark:hover:bg-red-900/20 transition-colors"
                :on-click #(when (js/confirm "Delete this transaction?")
                             (rf/dispatch [:tx/delete-transaction (str id)]))
                :title "Delete transaction"}
       [icon :trash {:width 16 :height 16}]]]]))

(defn enhanced-transaction-table []
  (let [transactions @(rf/subscribe [:tx/paginated-transactions])
        search @(rf/subscribe [:tx/filter-search])]
    [:div {:class "bg-white dark:bg-neutral-800 rounded-xl border border-neutral-200 dark:border-neutral-700 overflow-hidden shadow-sm"}
     ;; Search and action buttons
     [:div {:class "flex items-center justify-between gap-4 px-6 py-4 border-b border-neutral-200 dark:border-neutral-700 bg-neutral-50 dark:bg-neutral-900/50"}
      [:div {:class "relative flex-1 max-w-sm"}
       [:span {:class "absolute left-3 top-1/2 -translate-y-1/2 text-neutral-400 dark:text-neutral-500"}
        [icon :search {:width 18 :height 18}]]
       [:input {:class (str "w-full pl-10 pr-4 py-2.5 bg-white dark:bg-neutral-800 border border-neutral-200 dark:border-neutral-700 "
                            "rounded-lg text-sm focus:outline-none focus:ring-2 focus:ring-violet-600/20 focus:border-violet-600 "
                            "transition-all text-neutral-900 dark:text-neutral-50 placeholder:text-neutral-400")
                :type "text"
                :placeholder "Search transactions..."
                :value (or search "")
                :on-change #(rf/dispatch [:tx/update-filter :search (-> % .-target .-value)])}]]

      [:div {:class "flex items-center gap-2"}
       [:button {:class "p-2 text-neutral-600 dark:text-neutral-400 hover:bg-neutral-100 dark:hover:bg-neutral-800 rounded-lg transition-colors"
                 :title "Filter"}
        [icon :filter {:width 20 :height 20}]]
       [:button {:class "p-2 text-neutral-600 dark:text-neutral-400 hover:bg-neutral-100 dark:hover:bg-neutral-800 rounded-lg transition-colors"
                 :title "Download"}
        [icon :download {:width 20 :height 20}]]
       [:button {:class "p-2 text-neutral-600 dark:text-neutral-400 hover:bg-neutral-100 dark:hover:bg-neutral-800 rounded-lg transition-colors"
                 :title "Settings"}
        [icon :settings {:width 20 :height 20}]]]]

     ;; Table
     [:div {:class "overflow-x-auto"}
      [:table {:class "w-full text-left border-collapse"}
       [:thead {:class "bg-neutral-50 dark:bg-neutral-900/50 border-b border-neutral-200 dark:border-neutral-700"}
        [:tr
         [:th {:class "py-4 px-6 text-xs font-bold text-neutral-500 dark:text-neutral-400 uppercase tracking-wider w-[180px]"} "Date"]
         [:th {:class "py-4 px-6 text-xs font-bold text-neutral-500 dark:text-neutral-400 uppercase tracking-wider"} "Description"]
         [:th {:class "py-4 px-6 text-xs font-bold text-neutral-500 dark:text-neutral-400 uppercase tracking-wider w-[180px]"} "Category"]
         [:th {:class "py-4 px-6 text-xs font-bold text-neutral-500 dark:text-neutral-400 uppercase tracking-wider w-[200px]"} "Method"]
         [:th {:class "py-4 px-6 text-xs font-bold text-neutral-500 dark:text-neutral-400 uppercase tracking-wider w-[150px] text-right"} "Amount"]
         [:th {:class "py-4 px-6 w-[60px]"}]]]

       [:tbody {:class "text-sm"}
        (if (empty? transactions)
          [:tr
           [:td {:class "py-12 px-6 text-center text-neutral-500 dark:text-neutral-400" :col-span 6}
            [:div {:class "flex flex-col items-center gap-2"}
             [:div {:class "text-3xl"} "📋"]
             [:p {:class "font-medium"} "No transactions found"]
             [:p {:class "text-sm"} "Try adjusting your search or filters"]]]]
          (for [t transactions]
            ^{:key (or (:transaction/id t) (random-uuid))}
            [enhanced-transaction-table-row t]))]]]]))

(defn analytics-table-view []
  "Complete analytics table view with cards, controls, table, and pagination."
  [:div {:class "space-y-6"}
   ;; Analytics cards row
   [analytics-cards-row]

   ;; Table controls
   [table-controls]

   ;; Enhanced table
   [enhanced-transaction-table]

   ;; Pagination
   [pagination]])
