(ns finance.domain.budget
  "Pure domain logic for budgets."
  (:require [clojure.spec.alpha :as s])
  (:import [java.util Calendar Date]))

(s/def :budget/id uuid?)
(s/def :budget/category keyword?)
(s/def :budget/amount (s/and number? pos?))
(s/def :budget/currency #{:COP :USD})
(s/def :budget/year pos-int?)
(s/def :budget/month (s/and pos-int? #(<= 1 % 12)))
(s/def :budget/alert-threshold (s/and number? #(<= 0 % 1)))
(s/def :budget/user-id uuid?)

(s/def ::budget
  (s/keys :req [:budget/id
                :budget/category
                :budget/amount
                :budget/currency
                :budget/year
                :budget/month
                :budget/user-id]
          :opt [:budget/alert-threshold
                :budget/created-at]))

(defn current-year-month
  "Returns the current year and month as a map."
  []
  (let [cal (Calendar/getInstance)]
    {:year (.get cal Calendar/YEAR)
     :month (inc (.get cal Calendar/MONTH))}))

(defn create-budget
  "Creates a new budget."
  ([category amount]
   (create-budget category amount {}))
  ([category amount {:keys [currency year month alert-threshold]}]
   (let [{:keys [year month] :as current} (current-year-month)]
     {:budget/id (random-uuid)
      :budget/category category
      :budget/amount (abs amount)
      :budget/currency (or currency :COP)
      :budget/year (or year (:year current))
      :budget/month (or month (:month current))
      :budget/alert-threshold (or alert-threshold 0.8M)
      :budget/created-at (Date.)})))

(defn valid?
  "Validates a budget against spec."
  [budget]
  (s/valid? ::budget budget))

(defn explain-invalid
  "Returns explanation if budget is invalid, nil otherwise."
  [budget]
  (when-not (valid? budget)
    (s/explain-str ::budget budget)))

(defn calculate-spent
  "Calculates total spent for a budget's category from transactions.
   Expects expenses only (type = :expense)."
  [transactions budget]
  (let [category (:budget/category budget)
        currency (:budget/currency budget)]
    (->> transactions
         (filter #(and (= :expense (:transaction/type %))
                       (= category (:transaction/category %))
                       (= currency (:transaction/currency %))))
         (map :transaction/amount)
         (reduce + 0M))))

(defn calculate-remaining
  "Calculates remaining budget amount."
  [budget spent]
  (- (:budget/amount budget) spent))

(defn calculate-percentage
  "Calculates percentage of budget used."
  [budget spent]
  (if (zero? (:budget/amount budget))
    0
    (/ spent (:budget/amount budget))))

(defn budget-status
  "Returns the status of a budget: :ok, :warning, or :exceeded."
  [budget spent]
  (let [percentage (calculate-percentage budget spent)
        threshold (:budget/alert-threshold budget)]
    (cond
      (>= percentage 1) :exceeded
      (>= percentage threshold) :warning
      :else :ok)))

(defn alert-needed?
  "Checks if an alert should be shown for this budget."
  [budget spent]
  (let [status (budget-status budget spent)]
    (#{:warning :exceeded} status)))

(defn budget-summary
  "Creates a summary for a budget with spending data."
  [budget spent]
  {:budget budget
   :spent spent
   :remaining (calculate-remaining budget spent)
   :percentage (calculate-percentage budget spent)
   :status (budget-status budget spent)})

(defn budgets-status
  "Creates summaries for multiple budgets given their spending.
   spending-by-category is a map of {category -> spent-amount}."
  [budgets spending-by-category]
  (map (fn [budget]
         (let [spent (get spending-by-category (:budget/category budget) 0M)]
           (budget-summary budget spent)))
       budgets))

(defn exceeded-budgets
  "Returns budgets that have been exceeded."
  [budget-summaries]
  (filter #(= :exceeded (:status %)) budget-summaries))

(defn warning-budgets
  "Returns budgets that are at warning level."
  [budget-summaries]
  (filter #(= :warning (:status %)) budget-summaries))

(defn by-category
  "Filters budgets by category."
  [budgets category]
  (filter #(= category (:budget/category %)) budgets))

(defn by-currency
  "Filters budgets by currency."
  [budgets currency]
  (filter #(= currency (:budget/currency %)) budgets))

(defn for-month
  "Filters budgets for a specific year and month."
  [budgets year month]
  (filter #(and (= year (:budget/year %))
                (= month (:budget/month %)))
          budgets))

(defn total-budgeted
  "Returns total amount budgeted."
  [budgets]
  (reduce + 0M (map :budget/amount budgets)))

(defn total-spent
  "Returns total spent from budget summaries."
  [budget-summaries]
  (reduce + 0M (map :spent budget-summaries)))

(defn group-by-category
  "Groups budgets by category."
  [budgets]
  (group-by :budget/category budgets))

(defn sort-by-percentage
  "Sorts budget summaries by percentage used (highest first)."
  [budget-summaries]
  (sort-by :percentage #(compare %2 %1) budget-summaries))
