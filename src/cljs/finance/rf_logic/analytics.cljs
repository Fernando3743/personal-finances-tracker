(ns finance.rf-logic.analytics
  "Analytics page logic - reports and visualizations."
  (:require [re-frame.core :as rf]
            [finance.db :as db]))

(rf/reg-event-fx
 :analytics/init
 (fn [_ _]
   {:dispatch-n [[:tx/fetch-transactions]
                 [:dashboard/fetch-summary]
                 [:budgets/fetch-status]]}))

(rf/reg-event-db
 :analytics/set-date-range
 (fn [db [_ start end]]
   (assoc-in db [:analytics :date-range] {:start start :end end})))

(rf/reg-event-db
 :analytics/set-view
 (fn [db [_ view]]
   (assoc-in db [:analytics :selected-view] view)))

(rf/reg-sub
 :analytics/loading?
 (fn [db _]
   (get-in db [:analytics :loading?] false)))

(rf/reg-sub
 :analytics/date-range
 (fn [db _]
   (get-in db [:analytics :date-range])))

(rf/reg-sub
 :analytics/selected-view
 (fn [db _]
   (get-in db [:analytics :selected-view] :overview)))

(rf/reg-sub
 :analytics/monthly-data
 (fn [db _]
   (get-in db [:monthly-report :months] [])))

(rf/reg-sub
 :analytics/income-expense-comparison
 :<- [:analytics/monthly-data]
 (fn [months _]
   (map (fn [m]
          {:month (str (:year m) "-" (when (< (:month m) 10) "0") (:month m))
           :income (:income m)
           :expenses (:expenses m)
           :net (- (:income m) (:expenses m))})
        months)))

(rf/reg-sub
 :analytics/savings-rate
 :<- [:tx/transactions]
 (fn [transactions _]
   (let [income (reduce + 0 (map :transaction/amount
                                  (filter #(= :income (:transaction/type %)) transactions)))
         expenses (reduce + 0 (map :transaction/amount
                                    (filter #(= :expense (:transaction/type %)) transactions)))]
     (if (zero? income)
       0
       (* 100 (/ (- income expenses) income))))))

(rf/reg-sub
 :analytics/category-trends
 :<- [:tx/transactions]
 (fn [transactions _]
   (let [expenses (filter #(= :expense (:transaction/type %)) transactions)
         by-category (group-by :transaction/category expenses)]
     (->> by-category
          (map (fn [[cat txs]]
                 {:category cat
                  :total (reduce + 0 (map :transaction/amount txs))
                  :count (count txs)
                  :avg (if (empty? txs) 0 (/ (reduce + 0 (map :transaction/amount txs)) (count txs)))}))
          (sort-by :total >)))))

(rf/reg-sub
 :analytics/top-expense-categories
 :<- [:analytics/category-trends]
 (fn [trends _]
   (take 5 trends)))

(rf/reg-sub
 :analytics/income-sources
 :<- [:tx/transactions]
 (fn [transactions _]
   (let [incomes (filter #(= :income (:transaction/type %)) transactions)
         by-category (group-by :transaction/category incomes)]
     (->> by-category
          (map (fn [[cat txs]]
                 {:category cat
                  :total (reduce + 0 (map :transaction/amount txs))
                  :count (count txs)}))
          (sort-by :total >)))))

(rf/reg-sub
 :analytics/budget-vs-actual
 :<- [:budgets/budget-summaries]
 (fn [summaries _]
   (map (fn [s]
          {:category (get-in s [:budget :budget/category])
           :budgeted (get-in s [:budget :budget/amount])
           :spent (:spent s)
           :remaining (:remaining s)
           :percentage (* 100 (:percentage s))
           :status (:status s)})
        summaries)))

(rf/reg-sub
 :analytics/total-income
 :<- [:tx/transactions]
 (fn [transactions _]
   (reduce + 0 (map :transaction/amount
                    (filter #(= :income (:transaction/type %)) transactions)))))

(rf/reg-sub
 :analytics/total-expenses
 :<- [:tx/transactions]
 (fn [transactions _]
   (reduce + 0 (map :transaction/amount
                    (filter #(= :expense (:transaction/type %)) transactions)))))

(rf/reg-sub
 :analytics/net-balance
 :<- [:analytics/total-income]
 :<- [:analytics/total-expenses]
 (fn [[income expenses] _]
   (- income expenses)))

(rf/reg-sub
 :analytics/transaction-count
 :<- [:tx/transactions]
 (fn [transactions _]
   (count transactions)))

(rf/reg-sub
 :analytics/avg-transaction
 :<- [:tx/transactions]
 (fn [transactions _]
   (if (empty? transactions)
     0
     (/ (reduce + 0 (map :transaction/amount transactions))
        (count transactions)))))

(rf/reg-sub
 :analytics/income-by-month
 :<- [:analytics/monthly-data]
 (fn [months _]
   (map (fn [m]
          {:month (str (:month m) "/" (:year m))
           :amount (:income m)})
        months)))

(rf/reg-sub
 :analytics/expenses-by-month
 :<- [:analytics/monthly-data]
 (fn [months _]
   (map (fn [m]
          {:month (str (:month m) "/" (:year m))
           :amount (:expenses m)})
        months)))

(rf/reg-sub
 :analytics/currency-breakdown
 (fn [db _]
   (get-in db [:summary :by-currency] {})))

(rf/reg-sub
 :analytics/overview-stats
 :<- [:analytics/total-income]
 :<- [:analytics/total-expenses]
 :<- [:analytics/net-balance]
 :<- [:analytics/savings-rate]
 :<- [:analytics/transaction-count]
 (fn [[income expenses balance savings-rate tx-count] _]
   {:total-income income
    :total-expenses expenses
    :net-balance balance
    :savings-rate savings-rate
    :transaction-count tx-count}))
