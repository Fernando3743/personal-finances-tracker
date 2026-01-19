(ns finance.rf-logic.expenses
  "Expenses page logic - filtering and display of expense transactions."
  (:require [re-frame.core :as rf]
            [clojure.string :as str]
            [finance.db :as db]
            [finance.utils.filters :as filters]
            [finance.utils.date :as date-utils]))

(rf/reg-event-fx
 :expenses/init
 (fn [_ _]
   {:dispatch [:tx/fetch-transactions]}))

(rf/reg-event-db
 :expenses/update-filter
 (fn [db [_ field value]]
   (assoc-in db [:expenses-filter field] value)))

(rf/reg-event-db
 :expenses/clear-filters
 (fn [db _]
   (assoc db :expenses-filter (:expenses-filter db/default-db))))

(rf/reg-sub
 :expenses/filter
 (fn [db _]
   (:expenses-filter db)))

(rf/reg-sub
 :expenses/filter-search
 :<- [:expenses/filter]
 (fn [fltr _]
   (:search fltr)))

(rf/reg-sub
 :expenses/filter-category
 :<- [:expenses/filter]
 (fn [fltr _]
   (:category fltr)))

(rf/reg-sub
 :expenses/filter-currency
 :<- [:expenses/filter]
 (fn [fltr _]
   (:currency fltr)))

(rf/reg-sub
 :expenses/has-active-filters?
 :<- [:expenses/filter]
 (fn [fltr _]
   (filters/has-active-filters? fltr)))

(rf/reg-sub
 :expenses/all-expenses
 :<- [:tx/transactions]
 (fn [transactions _]
   (filter #(= :expense (:transaction/type %)) transactions)))

(rf/reg-sub
 :expenses/filtered-list
 :<- [:expenses/all-expenses]
 :<- [:expenses/filter]
 (fn [[expenses fltr] _]
   (filters/apply-filters expenses fltr)))

(rf/reg-sub
 :expenses/count
 :<- [:expenses/all-expenses]
 (fn [expenses _]
   (count expenses)))

(rf/reg-sub
 :expenses/filtered-count
 :<- [:expenses/filtered-list]
 (fn [expenses _]
   (count expenses)))

(rf/reg-sub
 :expenses/total
 :<- [:expenses/filtered-list]
 (fn [expenses _]
   (reduce + 0 (map :transaction/amount expenses))))

(rf/reg-sub
 :expenses/total-by-currency
 :<- [:expenses/filtered-list]
 (fn [expenses _]
   (reduce (fn [acc tx]
             (let [curr (:transaction/currency tx)
                   amt (:transaction/amount tx)]
               (update acc curr (fnil + 0) amt)))
           {}
           expenses)))

(rf/reg-sub
 :expenses/by-category
 :<- [:expenses/filtered-list]
 (fn [expenses _]
   (reduce (fn [acc tx]
             (let [cat (:transaction/category tx)
                   amt (:transaction/amount tx)]
               (update acc cat (fnil + 0) amt)))
           {}
           expenses)))

(rf/reg-sub
 :expenses/top-category
 :<- [:expenses/by-category]
 (fn [by-cat _]
   (when (seq by-cat)
     (key (apply max-key val by-cat)))))

(rf/reg-sub
 :expenses/avg-expense
 :<- [:expenses/filtered-list]
 (fn [expenses _]
   (if (empty? expenses)
     0
     (/ (reduce + 0 (map :transaction/amount expenses))
        (count expenses)))))

(rf/reg-sub
 :expenses/grouped-by-date
 :<- [:expenses/filtered-list]
 (fn [expenses _]
   (group-by (fn [tx]
               (when-let [date (:transaction/date tx)]
                 (let [d (js/Date. date)]
                   (.toLocaleDateString d "en-US" #js {:year "numeric"
                                                       :month "short"
                                                       :day "numeric"}))))
             expenses)))

(rf/reg-sub
 :expenses/expense-categories
 :<- [:expenses/items]
 (fn [expenses _]
   (->> expenses
        (map :transaction/category)
        (remove nil?)
        distinct
        sort)))

(rf/reg-sub
 :expenses/category-breakdown
 :<- [:expenses/by-category]
 :<- [:expenses/total]
 (fn [[by-cat total] _]
   (when (and (seq by-cat) (pos? total))
     (->> by-cat
          (map (fn [[cat amt]]
                 {:category cat
                  :amount amt
                  :percentage (* 100 (/ amt total))}))
          (sort-by :amount >)))))
