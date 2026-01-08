(ns finance.domain.reports
  "Report generation functions.
   Pure functions that transform transaction data into report formats."
  (:require [finance.domain.transaction :as tx]))

(defn balance-report
  "Generates a complete balance report with per-currency breakdown."
  [transactions]
  {:by-currency (tx/totals-by-currency transactions)
   :total-balance (tx/total-balance transactions)
   :total-income (tx/total-income transactions)
   :total-expenses (tx/total-expenses transactions)
   :transaction-count (count transactions)})

(defn- category-breakdown-for-transactions
  "Helper to generate category breakdown for a set of transactions."
  [transactions]
  (let [summary (tx/category-summary transactions)
        total-expenses (tx/total-expenses transactions)
        total-income (tx/total-income transactions)]
    {:categories
     (map (fn [{:keys [category total income expenses count]}]
            {:category category
             :total total
             :income income
             :expenses expenses
             :count count
             :expense-percentage (if (zero? total-expenses)
                                   0
                                   (* 100 (/ expenses total-expenses)))
             :income-percentage (if (zero? total-income)
                                  0
                                  (* 100 (/ income total-income)))})
          summary)
     :totals {:income total-income
              :expenses total-expenses
              :balance (tx/total-balance transactions)}}))

(defn category-breakdown
  "Generates category breakdown with percentages, grouped by currency."
  [transactions]
  (let [by-currency (tx/group-by-currency transactions)]
    {:by-currency
     (reduce-kv
      (fn [acc currency txs]
        (assoc acc currency (category-breakdown-for-transactions txs)))
      {}
      by-currency)
     ;; Legacy fields for backwards compatibility
     :categories (:categories (category-breakdown-for-transactions transactions))
     :totals (:totals (category-breakdown-for-transactions transactions))}))

(defn- monthly-report-for-transactions
  "Helper to generate monthly report for a set of transactions."
  [transactions]
  (let [monthly (tx/monthly-summary transactions)]
    {:months monthly
     :averages (when (seq monthly)
                 {:avg-income (/ (reduce + (map :income monthly)) (count monthly))
                  :avg-expenses (/ (reduce + (map :expenses monthly)) (count monthly))
                  :avg-balance (/ (reduce + (map :balance monthly)) (count monthly))})}))

(defn monthly-report
  "Generates a monthly trend report, grouped by currency."
  [transactions]
  (let [by-currency (tx/group-by-currency transactions)
        overall (monthly-report-for-transactions transactions)]
    {:by-currency
     (reduce-kv
      (fn [acc currency txs]
        (assoc acc currency (monthly-report-for-transactions txs)))
      {}
      by-currency)
     ;; Legacy fields for backwards compatibility
     :months (:months overall)
     :averages (:averages overall)}))

(defn dashboard-data
  "Generates all data needed for dashboard display."
  [transactions]
  {:balance (balance-report transactions)
   :recent-transactions (take 10 (tx/sort-by-date transactions))
   :category-breakdown (category-breakdown transactions)
   :monthly-trend (monthly-report transactions)})
