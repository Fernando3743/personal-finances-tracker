(ns finance.utils.filters
  "Shared filtering utilities for transactions, incomes, and expenses."
  (:require [clojure.string :as str]))

(defn filter-by-currency
  "Filters transactions by currency.

   Parameters:
   - transactions: Collection of transaction maps
   - currency: Keyword (:COP, :USD, etc.) or nil for all

   Returns: Filtered collection"
  [transactions currency]
  (if (some? currency)
    (filter #(= (:transaction/currency %) currency) transactions)
    transactions))

(defn filter-by-search
  "Filters transactions by search term (searches description and category).

   Parameters:
   - transactions: Collection of transaction maps
   - search: String search term

   Returns: Filtered collection"
  [transactions search]
  (if (or (nil? search) (str/blank? (str search)))
    transactions
    (let [search-lower (str/lower-case (str search))]
      (filter (fn [tx]
                (or (str/includes? (str/lower-case (or (:transaction/description tx) ""))
                                  search-lower)
                    (str/includes? (str/lower-case (name (or (:transaction/category tx) :other)))
                                  search-lower)))
              transactions))))

(defn filter-by-type
  "Filters transactions by type (:income or :expense).

   Parameters:
   - transactions: Collection of transaction maps
   - type: Keyword (:income or :expense) or nil for all

   Returns: Filtered collection"
  [transactions type]
  (if (some? type)
    (filter #(= (:transaction/type %) type) transactions)
    transactions))

(defn filter-by-category
  "Filters transactions by category.

   Parameters:
   - transactions: Collection of transaction maps
   - category: Keyword (category name) or nil for all

   Returns: Filtered collection"
  [transactions category]
  (if (some? category)
    (filter #(= (:transaction/category %) category) transactions)
    transactions))

(defn sort-transactions
  "Sorts transactions by specified field and direction.

   Parameters:
   - transactions: Collection of transaction maps
   - sort-field: Keyword (:date, :amount, :category)
   - sort-dir: Keyword (:asc or :desc)

   Returns: Sorted collection"
  [transactions sort-field sort-dir]
  (let [sort-key (case sort-field
                   :date :transaction/date
                   :amount :transaction/amount
                   :category :transaction/category
                   :transaction/date)
        sorted (sort-by sort-key transactions)]
    (if (= sort-dir :desc)
      (reverse sorted)
      sorted)))

(defn apply-filters
  "Applies all filters to a collection of transactions.

   Parameters:
   - transactions: Collection of transaction maps
   - filters: Map with optional keys:
     - :search - String search term
     - :type - :income or :expense
     - :category - Category keyword
     - :currency - Currency keyword
     - :sort-field - :date, :amount, or :category
     - :sort-dir - :asc or :desc

   Returns: Filtered and sorted collection"
  [transactions {:keys [search type category currency sort-field sort-dir]
                 :or {sort-field :date sort-dir :desc}}]
  (-> transactions
      (filter-by-currency currency)
      (filter-by-search search)
      (filter-by-type type)
      (filter-by-category category)
      (sort-transactions sort-field sort-dir)))

(defn has-active-filters?
  "Checks if any filters are currently active.

   Parameters:
   - filters: Map with optional keys :search, :type, :category, :currency

   Returns: Boolean indicating if any filters are active"
  [{:keys [search type category currency]}]
  (or (not (str/blank? (str search)))
      (some? type)
      (some? category)
      (some? currency)))
