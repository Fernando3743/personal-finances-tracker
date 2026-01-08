(ns finance.rf-logic.dashboard
  "Dashboard page logic - summary, charts, category breakdown."
  (:require [re-frame.core :as rf]
            [day8.re-frame.http-fx]
            [ajax.core :as ajax]))

(def api-base "http://localhost:3000/api")

(rf/reg-event-fx
 :dashboard/fetch-summary
 (fn [{:keys [_db]} _]
   {:http-xhrio {:method :get
                 :uri (str api-base "/summary")
                 :response-format (ajax/json-response-format {:keywords? true})
                 :on-success [:dashboard/fetch-summary-success]
                 :on-failure [:app/api-error]}}))

(rf/reg-event-db
 :dashboard/fetch-summary-success
 (fn [db [_ response]]
   (assoc db :summary response)))

(rf/reg-event-fx
 :dashboard/fetch-dashboard
 (fn [{:keys [db]} _]
   {:db (assoc db :loading? true)
    :http-xhrio {:method :get
                 :uri (str api-base "/dashboard")
                 :response-format (ajax/json-response-format {:keywords? true})
                 :on-success [:dashboard/fetch-dashboard-success]
                 :on-failure [:app/api-error]}}))

(rf/reg-event-db
 :dashboard/fetch-dashboard-success
 (fn [db [_ response]]
   (-> db
       (assoc :loading? false)
       (assoc :summary (:balance response))
       (assoc :transactions (:recent-transactions response))
       (assoc :category-breakdown (:category-breakdown response))
       (assoc :monthly-report (:monthly-trend response)))))

(rf/reg-event-db
 :dashboard/set-chart-range
 (fn [db [_ range]]
   (assoc db :chart-time-range range)))

(rf/reg-sub
 :dashboard/summary
 (fn [db _]
   (:summary db)))

(rf/reg-sub
 :dashboard/total-balance
 :<- [:dashboard/summary]
 (fn [summary _]
   (or (:total-balance summary) 0)))

(rf/reg-sub
 :dashboard/total-income
 :<- [:dashboard/summary]
 (fn [summary _]
   (or (:total-income summary) 0)))

(rf/reg-sub
 :dashboard/total-expenses
 :<- [:dashboard/summary]
 (fn [summary _]
   (or (:total-expenses summary) 0)))

(rf/reg-sub
 :dashboard/category-breakdown
 (fn [db _]
   (:category-breakdown db)))

(rf/reg-sub
 :dashboard/categories-list
 :<- [:dashboard/category-breakdown]
 (fn [breakdown _]
   (:categories breakdown)))

(rf/reg-sub
 :dashboard/category-totals
 :<- [:dashboard/categories-list]
 (fn [categories _]
   (let [total-expense (reduce + 0 (map #(or (:total %) 0)
                                        (filter #(= (:type %) :expense) categories)))]
     {:categories categories
      :total-expense total-expense})))

(rf/reg-sub
 :dashboard/monthly-report
 (fn [db _]
   (:monthly-report db)))

(rf/reg-sub
 :dashboard/chart-time-range
 (fn [db _]
   (:chart-time-range db)))

(rf/reg-sub
 :dashboard/available-currencies
 (fn [_db _]
   [:COP :USD]))

(rf/reg-sub
 :dashboard/summary-by-currency
 :<- [:dashboard/summary]
 (fn [summary _]
   (or (:by-currency summary) {})))

(rf/reg-sub
 :dashboard/balance-for-currency
 :<- [:dashboard/summary-by-currency]
 (fn [by-currency [_ currency]]
   (get-in by-currency [currency :balance] 0)))

(rf/reg-sub
 :dashboard/income-for-currency
 :<- [:dashboard/summary-by-currency]
 (fn [by-currency [_ currency]]
   (get-in by-currency [currency :income] 0)))

(rf/reg-sub
 :dashboard/expenses-for-currency
 :<- [:dashboard/summary-by-currency]
 (fn [by-currency [_ currency]]
   (get-in by-currency [currency :expenses] 0)))

(rf/reg-sub
 :dashboard/all-currency-balances
 :<- [:dashboard/summary-by-currency]
 (fn [by-currency _]
   (mapv (fn [[currency data]]
           {:currency currency
            :balance (or (:balance data) 0)
            :income (or (:income data) 0)
            :expenses (or (:expenses data) 0)
            :count (or (:count data) 0)})
         by-currency)))
