(ns finance.domain.transaction
  "Pure domain logic for transactions.
   All functions are pure and side-effect free.
   Demonstrates: SRP, Pure Functions, Spec-based validation."
  (:require [clojure.spec.alpha :as s]))

(s/def :transaction/id uuid?)
(s/def :transaction/amount number?)
(s/def :transaction/type #{:income :expense})
(s/def :transaction/category keyword?)
(s/def :transaction/date inst?)
(s/def :transaction/description (s/and string? #(<= (count %) 500)))
(s/def :transaction/tags (s/coll-of keyword? :kind set?))
(s/def :transaction/currency #{:COP :USD})
(s/def :transaction/exchange-rate (s/and number? pos?))

(s/def ::transaction
  (s/keys :req [:transaction/id
                :transaction/amount
                :transaction/type
                :transaction/category
                :transaction/date
                :transaction/currency]
          :opt [:transaction/description
                :transaction/tags
                :transaction/exchange-rate]))

;; Default categories
(def default-categories
  #{:groceries :restaurants :transportation :utilities
    :entertainment :healthcare :shopping :salary
    :freelance :investments :gifts :other})

(defn create-transaction
  "Creates a new transaction map.
   amount: positive number (sign determined by type)
   type: :income or :expense
   category: keyword
   opts: optional map with :description, :tags, :date, :currency, :exchange-rate"
  ([amount type category]
   (create-transaction amount type category {}))
  ([amount type category {:keys [description tags date currency exchange-rate]}]
   (cond-> {:transaction/id (random-uuid)
            :transaction/amount (abs amount)
            :transaction/type type
            :transaction/category category
            :transaction/date (or date (java.util.Date.))
            :transaction/currency (or currency :COP)
            :transaction/description (or description "")
            :transaction/tags (or tags #{})}
     exchange-rate (assoc :transaction/exchange-rate exchange-rate))))

(defn valid?
  "Validates a transaction against spec."
  [transaction]
  (s/valid? ::transaction transaction))

(defn explain-invalid
  "Returns explanation if transaction is invalid, nil otherwise."
  [transaction]
  (when-not (valid? transaction)
    (s/explain-str ::transaction transaction)))

(defn signed-amount
  "Returns the signed amount based on transaction type.
   Income is positive, expense is negative."
  [{:transaction/keys [amount type]}]
  (if (= type :income)
    amount
    (- amount)))

(defn total-balance
  "Calculates total balance from a collection of transactions."
  [transactions]
  (reduce + 0 (map signed-amount transactions)))

(defn total-income
  "Calculates total income from transactions."
  [transactions]
  (->> transactions
       (filter #(= :income (:transaction/type %)))
       (map :transaction/amount)
       (reduce + 0)))

(defn total-expenses
  "Calculates total expenses from transactions."
  [transactions]
  (->> transactions
       (filter #(= :expense (:transaction/type %)))
       (map :transaction/amount)
       (reduce + 0)))

(defn by-type
  "Filters transactions by type (:income or :expense)."
  [transactions type]
  (filter #(= type (:transaction/type %)) transactions))

(defn by-category
  "Filters transactions by category."
  [transactions category]
  (filter #(= category (:transaction/category %)) transactions))

(defn by-tag
  "Filters transactions that have a specific tag."
  [transactions tag]
  (filter #(contains? (:transaction/tags %) tag) transactions))

(defn in-date-range
  "Filters transactions within a date range (inclusive)."
  [transactions from to]
  (filter #(let [date (:transaction/date %)]
             (and (not (.before date from))
                  (not (.after date to))))
          transactions))

(defn by-month
  "Filters transactions for a specific year and month."
  [transactions year month]
  (filter #(let [cal (doto (java.util.Calendar/getInstance)
                       (.setTime (:transaction/date %)))]
             (and (= year (.get cal java.util.Calendar/YEAR))
                  (= (dec month) (.get cal java.util.Calendar/MONTH))))
          transactions))

(defn by-currency
  "Filters transactions by currency (:COP or :USD)."
  [transactions currency]
  (filter #(= currency (:transaction/currency %)) transactions))

(defn group-by-category
  "Groups transactions by category."
  [transactions]
  (group-by :transaction/category transactions))

(defn group-by-type
  "Groups transactions by type."
  [transactions]
  (group-by :transaction/type transactions))

(defn group-by-month
  "Groups transactions by year-month."
  [transactions]
  (group-by
   (fn [tx]
     (let [cal (doto (java.util.Calendar/getInstance)
                 (.setTime (:transaction/date tx)))]
       {:year (.get cal java.util.Calendar/YEAR)
        :month (inc (.get cal java.util.Calendar/MONTH))}))
   transactions))

(defn group-by-currency
  "Groups transactions by currency."
  [transactions]
  (group-by :transaction/currency transactions))

(defn totals-by-currency
  "Returns totals grouped by currency.
   Result: {:COP {:balance x :income y :expenses z :count n}
            :USD {:balance x :income y :expenses z :count n}}"
  [transactions]
  (let [grouped (group-by-currency transactions)]
    (reduce-kv
     (fn [acc currency txs]
       (assoc acc currency
              {:balance (total-balance txs)
               :income (total-income txs)
               :expenses (total-expenses txs)
               :count (count txs)}))
     {}
     grouped)))

(defn category-summary
  "Returns summary statistics grouped by category.
   Each entry has :category, :total, :count."
  [transactions]
  (->> transactions
       group-by-category
       (map (fn [[cat txs]]
              {:category cat
               :total (total-balance txs)
               :count (count txs)
               :income (total-income txs)
               :expenses (total-expenses txs)}))
       (sort-by :total)))

(defn monthly-summary
  "Returns summary statistics grouped by month.
   Each entry has :year, :month, :income, :expenses, :balance."
  [transactions]
  (->> transactions
       group-by-month
       (map (fn [[{:keys [year month]} txs]]
              {:year year
               :month month
               :income (total-income txs)
               :expenses (total-expenses txs)
               :balance (total-balance txs)
               :count (count txs)}))
       (sort-by (juxt :year :month))))

(defn sort-by-date
  "Sorts transactions by date (newest first by default)."
  ([transactions]
   (sort-by-date transactions :desc))
  ([transactions order]
   (let [comparator (if (= order :asc) compare #(compare %2 %1))]
     (sort-by :transaction/date comparator transactions))))

(defn sort-by-amount
  "Sorts transactions by absolute amount."
  ([transactions]
   (sort-by-amount transactions :desc))
  ([transactions order]
   (let [comparator (if (= order :asc) compare #(compare %2 %1))]
     (sort-by #(abs (:transaction/amount %)) comparator transactions))))
