(ns finance.domain.reports
  "Report generation functions.
   Pure functions that transform transaction data into report formats."
  (:require [finance.domain.transaction :as tx]))

(defn balance-report
  "Generates a complete balance report."
  [transactions]
  {:total-balance (tx/total-balance transactions)
   :total-income (tx/total-income transactions)
   :total-expenses (tx/total-expenses transactions)
   :transaction-count (count transactions)})

(defn category-breakdown
  "Generates category breakdown with percentages."
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

(defn monthly-report
  "Generates a monthly trend report."
  [transactions]
  (let [monthly (tx/monthly-summary transactions)]
    {:months monthly
     :averages (when (seq monthly)
                 {:avg-income (/ (reduce + (map :income monthly)) (count monthly))
                  :avg-expenses (/ (reduce + (map :expenses monthly)) (count monthly))
                  :avg-balance (/ (reduce + (map :balance monthly)) (count monthly))})}))

(defn dashboard-data
  "Generates all data needed for dashboard display."
  [transactions]
  {:balance (balance-report transactions)
   :recent-transactions (take 10 (tx/sort-by-date transactions))
   :category-breakdown (category-breakdown transactions)
   :monthly-trend (monthly-report transactions)})
