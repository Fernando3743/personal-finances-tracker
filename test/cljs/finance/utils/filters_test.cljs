(ns finance.utils.filters-test
  "Tests for filter utilities."
  (:require [cljs.test :refer-macros [deftest is testing run-tests]]
            [finance.utils.filters :as filters]))

(def sample-transactions
  [{:transaction/id 1
    :transaction/currency :COP
    :transaction/description "Grocery shopping"
    :transaction/category :groceries
    :transaction/type :expense
    :transaction/amount 50000
    :transaction/date "2026-01-15"}
   {:transaction/id 2
    :transaction/currency :USD
    :transaction/description "Salary payment"
    :transaction/category :salary
    :transaction/type :income
    :transaction/amount 3000
    :transaction/date "2026-01-01"}
   {:transaction/id 3
    :transaction/currency :COP
    :transaction/description "Restaurant dinner"
    :transaction/category :restaurants
    :transaction/type :expense
    :transaction/amount 75000
    :transaction/date "2026-01-10"}])

(deftest filter-by-currency-test
  (testing "Filter by specific currency"
    (let [result (filters/filter-by-currency sample-transactions :COP)]
      (is (= 2 (count result)))
      (is (every? #(= :COP (:transaction/currency %)) result))))

  (testing "No filter when nil"
    (let [result (filters/filter-by-currency sample-transactions nil)]
      (is (= 3 (count result))))))

(deftest filter-by-search-test
  (testing "Search in description"
    (let [result (filters/filter-by-search sample-transactions "grocery")]
      (is (= 1 (count result)))
      (is (= "Grocery shopping" (:transaction/description (first result))))))

  (testing "Search in category"
    (let [result (filters/filter-by-search sample-transactions "salary")]
      (is (= 1 (count result)))))

  (testing "Case insensitive search"
    (let [result (filters/filter-by-search sample-transactions "GROCERY")]
      (is (= 1 (count result)))))

  (testing "Blank search returns all"
    (is (= 3 (count (filters/filter-by-search sample-transactions ""))))))

(deftest filter-by-type-test
  (testing "Filter by expense"
    (let [result (filters/filter-by-type sample-transactions :expense)]
      (is (= 2 (count result)))
      (is (every? #(= :expense (:transaction/type %)) result))))

  (testing "Filter by income"
    (let [result (filters/filter-by-type sample-transactions :income)]
      (is (= 1 (count result))))))

(deftest filter-by-category-test
  (testing "Filter by specific category"
    (let [result (filters/filter-by-category sample-transactions :groceries)]
      (is (= 1 (count result)))))

  (testing "No filter when nil"
    (is (= 3 (count (filters/filter-by-category sample-transactions nil))))))

(deftest sort-transactions-test
  (testing "Sort by date ascending"
    (let [result (filters/sort-transactions sample-transactions :date :asc)]
      (is (= "2026-01-01" (:transaction/date (first result))))))

  (testing "Sort by date descending"
    (let [result (filters/sort-transactions sample-transactions :date :desc)]
      (is (= "2026-01-15" (:transaction/date (first result))))))

  (testing "Sort by amount"
    (let [result (filters/sort-transactions sample-transactions :amount :asc)]
      (is (= 50000 (:transaction/amount (first result)))))))

(deftest has-active-filters?-test
  (testing "No active filters"
    (is (false? (filters/has-active-filters? {})))
    (is (false? (filters/has-active-filters? {:search ""}))))

  (testing "Active filters detected"
    (is (true? (filters/has-active-filters? {:search "test"})))
    (is (true? (filters/has-active-filters? {:type :expense})))
    (is (true? (filters/has-active-filters? {:category :groceries})))
    (is (true? (filters/has-active-filters? {:currency :COP})))))

(deftest apply-filters-test
  (testing "Apply multiple filters together"
    (let [filters {:currency :COP
                   :type :expense
                   :sort-by :date
                   :sort-dir :desc}
          result (filters/apply-filters sample-transactions filters)]
      (is (= 2 (count result)))
      (is (every? #(= :COP (:transaction/currency %)) result))
      (is (every? #(= :expense (:transaction/type %)) result))
      ;; Most recent first
      (is (= "2026-01-15" (:transaction/date (first result))))))

  (testing "Search with type filter"
    (let [filters {:search "restaurant"
                   :type :expense}
          result (filters/apply-filters sample-transactions filters)]
      (is (= 1 (count result)))
      (is (= :restaurants (:transaction/category (first result)))))))

;; Run tests when this namespace is loaded
(run-tests)
