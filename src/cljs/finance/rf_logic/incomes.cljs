(ns finance.rf-logic.incomes
  "Incomes page logic - filtering and display of income transactions."
  (:require [re-frame.core :as rf]
            [clojure.string :as str]
            [finance.db :as db]))

(rf/reg-event-fx
 :incomes/init
 (fn [_ _]
   {:dispatch [:tx/fetch-transactions]}))

(rf/reg-event-db
 :incomes/update-filter
 (fn [db [_ field value]]
   (assoc-in db [:incomes-filter field] value)))

(rf/reg-event-db
 :incomes/clear-filters
 (fn [db _]
   (assoc db :incomes-filter (:incomes-filter db/default-db))))

(rf/reg-sub
 :incomes/filter
 (fn [db _]
   (:incomes-filter db)))

(rf/reg-sub
 :incomes/filter-search
 :<- [:incomes/filter]
 (fn [fltr _]
   (:search fltr)))

(rf/reg-sub
 :incomes/filter-category
 :<- [:incomes/filter]
 (fn [fltr _]
   (:category fltr)))

(rf/reg-sub
 :incomes/filter-currency
 :<- [:incomes/filter]
 (fn [fltr _]
   (:currency fltr)))

(rf/reg-sub
 :incomes/has-active-filters?
 :<- [:incomes/filter]
 (fn [fltr _]
   (or (not (str/blank? (:search fltr)))
       (some? (:category fltr))
       (some? (:currency fltr)))))

(rf/reg-sub
 :incomes/all-incomes
 :<- [:tx/transactions]
 (fn [transactions _]
   (filter #(= :income (:transaction/type %)) transactions)))

(rf/reg-sub
 :incomes/filtered-list
 :<- [:incomes/all-incomes]
 :<- [:incomes/filter]
 (fn [[incomes fltr] _]
   (let [{:keys [search category currency sort-by sort-dir]} fltr]
     (cond->> incomes
       (some? currency)
       (filter #(= (:transaction/currency %) currency))

       (not (str/blank? search))
       (filter #(or (str/includes? (str/lower-case (or (:transaction/description %) ""))
                                   (str/lower-case search))
                    (str/includes? (str/lower-case (name (or (:transaction/category %) :other)))
                                   (str/lower-case search))))

       (some? category)
       (filter #(= (:transaction/category %) category))

       true
       (sort-by (case sort-by
                  :date :transaction/date
                  :amount :transaction/amount
                  :category :transaction/category
                  :transaction/date))

       (= sort-dir :desc)
       reverse))))

(rf/reg-sub
 :incomes/count
 :<- [:incomes/all-incomes]
 (fn [incomes _]
   (count incomes)))

(rf/reg-sub
 :incomes/filtered-count
 :<- [:incomes/filtered-list]
 (fn [incomes _]
   (count incomes)))

(rf/reg-sub
 :incomes/total
 :<- [:incomes/filtered-list]
 (fn [incomes _]
   (reduce + 0 (map :transaction/amount incomes))))

(rf/reg-sub
 :incomes/total-by-currency
 :<- [:incomes/filtered-list]
 (fn [incomes _]
   (reduce (fn [acc tx]
             (let [curr (:transaction/currency tx)
                   amt (:transaction/amount tx)]
               (update acc curr (fnil + 0) amt)))
           {}
           incomes)))

(rf/reg-sub
 :incomes/by-category
 :<- [:incomes/filtered-list]
 (fn [incomes _]
   (reduce (fn [acc tx]
             (let [cat (:transaction/category tx)
                   amt (:transaction/amount tx)]
               (update acc cat (fnil + 0) amt)))
           {}
           incomes)))

(rf/reg-sub
 :incomes/top-category
 :<- [:incomes/by-category]
 (fn [by-cat _]
   (when (seq by-cat)
     (key (apply max-key val by-cat)))))

(rf/reg-sub
 :incomes/avg-income
 :<- [:incomes/filtered-list]
 (fn [incomes _]
   (if (empty? incomes)
     0
     (/ (reduce + 0 (map :transaction/amount incomes))
        (count incomes)))))

(rf/reg-sub
 :incomes/grouped-by-date
 :<- [:incomes/filtered-list]
 (fn [incomes _]
   (group-by (fn [tx]
               (when-let [date (:transaction/date tx)]
                 (let [d (js/Date. date)]
                   (.toLocaleDateString d "en-US" #js {:year "numeric"
                                                       :month "short"
                                                       :day "numeric"}))))
             incomes)))

(rf/reg-sub
 :incomes/income-categories
 (fn [db _]
   (filter #{:salary :freelance :investments :gifts :other} (:categories db))))
