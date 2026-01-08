(ns finance.subs
  "Re-frame subscriptions."
  (:require [re-frame.core :as rf]
            [clojure.string :as str]))

;; =============================================================================
;; Basic Subscriptions
;; =============================================================================

(rf/reg-sub
 ::db
 (fn [db _]
   db))

(rf/reg-sub
 ::loading?
 (fn [db _]
   (:loading? db)))

(rf/reg-sub
 ::error
 (fn [db _]
   (:error db)))

(rf/reg-sub
 ::active-view
 (fn [db _]
   (:active-view db)))

;; =============================================================================
;; Theme & UI Subscriptions
;; =============================================================================

(rf/reg-sub
 ::theme
 (fn [db _]
   (:theme db)))

(rf/reg-sub
 ::panel
 (fn [db _]
   (:panel db)))

(rf/reg-sub
 ::panel-open?
 :<- [::panel]
 (fn [panel _]
   (:open? panel)))

(rf/reg-sub
 ::panel-mode
 :<- [::panel]
 (fn [panel _]
   (:mode panel)))

(rf/reg-sub
 ::toast-queue
 (fn [db _]
   (:toast-queue db)))

;; =============================================================================
;; Filter Subscriptions
;; =============================================================================

(rf/reg-sub
 ::filter
 (fn [db _]
   (:filter db)))

(rf/reg-sub
 ::filter-search
 :<- [::filter]
 (fn [filter _]
   (:search filter)))

(rf/reg-sub
 ::filter-type
 :<- [::filter]
 (fn [filter _]
   (:type filter)))

(rf/reg-sub
 ::filter-category
 :<- [::filter]
 (fn [filter _]
   (:category filter)))

(rf/reg-sub
 ::filter-currency
 :<- [::filter]
 (fn [filter _]
   (:currency filter)))

(rf/reg-sub
 ::has-active-filters?
 :<- [::filter]
 (fn [filter _]
   (or (not (str/blank? (:search filter)))
       (some? (:type filter))
       (some? (:category filter))
       (some? (:currency filter)))))

;; =============================================================================
;; Transaction Subscriptions
;; =============================================================================

(rf/reg-sub
 ::transactions
 (fn [db _]
   (:transactions db)))

(rf/reg-sub
 ::transaction-count
 :<- [::transactions]
 (fn [transactions _]
   (count transactions)))

(rf/reg-sub
 ::recent-transactions
 :<- [::transactions]
 (fn [transactions _]
   (take 5 transactions)))

;; Filtered transactions based on current filter state
(rf/reg-sub
 ::filtered-transactions
 :<- [::transactions]
 :<- [::filter]
 (fn [[transactions fltr] _]
   (let [{:keys [search type category currency sort-by sort-dir]} fltr]
     (cond->> transactions
       ;; Filter by currency
       (some? currency)
       (filter #(= (:transaction/currency %) currency))

       ;; Filter by search term
       (not (str/blank? search))
       (filter #(or (str/includes? (str/lower-case (or (:transaction/description %) ""))
                                   (str/lower-case search))
                    (str/includes? (str/lower-case (name (or (:transaction/category %) :other)))
                                   (str/lower-case search))))

       ;; Filter by type
       (some? type)
       (filter #(= (:transaction/type %) type))

       ;; Filter by category
       (some? category)
       (filter #(= (:transaction/category %) category))

       ;; Sort
       true
       (sort-by (case sort-by
                  :date :transaction/date
                  :amount :transaction/amount
                  :category :transaction/category
                  :transaction/date))

       ;; Sort direction
       (= sort-dir :desc)
       reverse))))

;; Group transactions by date
(rf/reg-sub
 ::transactions-by-date
 :<- [::filtered-transactions]
 (fn [transactions _]
   (group-by (fn [tx]
               (when-let [date (:transaction/date tx)]
                 (let [d (js/Date. date)]
                   (.toLocaleDateString d "en-US" #js {:year "numeric"
                                                       :month "short"
                                                       :day "numeric"}))))
             transactions)))

;; =============================================================================
;; Summary Subscriptions
;; =============================================================================

(rf/reg-sub
 ::summary
 (fn [db _]
   (:summary db)))

(rf/reg-sub
 ::total-balance
 :<- [::summary]
 (fn [summary _]
   (or (:total-balance summary) 0)))

(rf/reg-sub
 ::total-income
 :<- [::summary]
 (fn [summary _]
   (or (:total-income summary) 0)))

(rf/reg-sub
 ::total-expenses
 :<- [::summary]
 (fn [summary _]
   (or (:total-expenses summary) 0)))

;; =============================================================================
;; Category Breakdown
;; =============================================================================

(rf/reg-sub
 ::category-breakdown
 (fn [db _]
   (:category-breakdown db)))

(rf/reg-sub
 ::categories-list
 :<- [::category-breakdown]
 (fn [breakdown _]
   (:categories breakdown)))

;; Calculate total expenses for percentage calculations
(rf/reg-sub
 ::category-totals
 :<- [::categories-list]
 (fn [categories _]
   (let [total-expense (reduce + 0 (map #(or (:total %) 0)
                                        (filter #(= (:type %) :expense) categories)))]
     {:categories categories
      :total-expense total-expense})))

;; =============================================================================
;; Monthly Report & Charts
;; =============================================================================

(rf/reg-sub
 ::monthly-report
 (fn [db _]
   (:monthly-report db)))

(rf/reg-sub
 ::chart-time-range
 (fn [db _]
   (:chart-time-range db)))

;; =============================================================================
;; Form Subscriptions
;; =============================================================================

(rf/reg-sub
 ::transaction-form
 (fn [db _]
   (:transaction-form db)))

(rf/reg-sub
 ::form-field
 :<- [::transaction-form]
 (fn [form [_ field]]
   (get form field)))

(rf/reg-sub
 ::categories
 (fn [db _]
   (:categories db)))

(rf/reg-sub
 ::form-valid?
 :<- [::transaction-form]
 (fn [form _]
   (let [{:keys [amount type category]} form]
     (and (not (empty? (str amount)))
          (some? type)
          (some? category)
          (pos? (js/parseFloat amount))))))

(rf/reg-sub
 ::form-amount-display
 :<- [::transaction-form]
 (fn [form _]
   (let [amount (:amount form)]
     (if (str/blank? (str amount))
       "0.00"
       (let [parsed (js/parseFloat amount)]
         (if (js/isNaN parsed)
           "0.00"
           (.toFixed parsed 2)))))))

;; =============================================================================
;; Currency Subscriptions
;; =============================================================================

(rf/reg-sub
 ::available-currencies
 (fn [_db _]
   [:COP :USD]))

(rf/reg-sub
 ::summary-by-currency
 :<- [::summary]
 (fn [summary _]
   (or (:by-currency summary) {})))

(rf/reg-sub
 ::balance-for-currency
 :<- [::summary-by-currency]
 (fn [by-currency [_ currency]]
   (get-in by-currency [currency :balance] 0)))

(rf/reg-sub
 ::income-for-currency
 :<- [::summary-by-currency]
 (fn [by-currency [_ currency]]
   (get-in by-currency [currency :income] 0)))

(rf/reg-sub
 ::expenses-for-currency
 :<- [::summary-by-currency]
 (fn [by-currency [_ currency]]
   (get-in by-currency [currency :expenses] 0)))

;; Get all currency balances as a vector for display
(rf/reg-sub
 ::all-currency-balances
 :<- [::summary-by-currency]
 (fn [by-currency _]
   (mapv (fn [[currency data]]
           {:currency currency
            :balance (or (:balance data) 0)
            :income (or (:income data) 0)
            :expenses (or (:expenses data) 0)
            :count (or (:count data) 0)})
         by-currency)))

(rf/reg-sub
 ::form-currency
 :<- [::transaction-form]
 (fn [form _]
   (or (:currency form) :COP)))
