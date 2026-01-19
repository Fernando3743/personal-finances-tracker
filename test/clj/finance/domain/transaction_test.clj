(ns finance.domain.transaction-test
  "Tests for transaction domain logic."
  (:require [clojure.test :refer [deftest is testing]]
            [finance.domain.transaction :as tx]))

(deftest test-create-transaction
  (testing "Creating a valid transaction"
    (let [transaction (tx/create-transaction 100M :income :salary {})]
      (is (tx/valid? transaction))
      (is (= 100M (:transaction/amount transaction)))
      (is (= :income (:transaction/type transaction)))
      (is (= :salary (:transaction/category transaction)))))

  (testing "Transaction with optional fields"
    (let [transaction (tx/create-transaction
                       50M :expense :food
                       {:description "Lunch"
                        :tags #{:dining :work}
                        :currency :USD})]
      (is (tx/valid? transaction))
      (is (= "Lunch" (:transaction/description transaction)))
      (is (= #{:dining :work} (:transaction/tags transaction)))
      (is (= :USD (:transaction/currency transaction)))))

  (testing "Default currency is COP"
    (let [transaction (tx/create-transaction 100M :income :salary {})]
      (is (= :COP (:transaction/currency transaction)))))

  (testing "Negative amounts are converted to positive via abs"
    (let [transaction (tx/create-transaction -100M :income :salary {})]
      (is (tx/valid? transaction))
      (is (= 100M (:transaction/amount transaction))))))

(deftest test-signed-amount
  (testing "Income has positive signed amount"
    (let [transaction (tx/create-transaction 100M :income :salary {})]
      (is (= 100M (tx/signed-amount transaction)))))

  (testing "Expense has negative signed amount"
    (let [transaction (tx/create-transaction 100M :expense :food {})]
      (is (= -100M (tx/signed-amount transaction))))))

(deftest test-total-balance
  (testing "Balance calculation with mixed transactions"
    (let [income (tx/create-transaction 1000M :income :salary {})
          expense1 (tx/create-transaction 300M :expense :food {})
          expense2 (tx/create-transaction 200M :expense :transport {})]
      (is (= 500M (tx/total-balance [income expense1 expense2])))))

  (testing "Empty transactions list"
    (is (= 0M (tx/total-balance []))))

  (testing "Only income"
    (let [income1 (tx/create-transaction 1000M :income :salary {})
          income2 (tx/create-transaction 500M :income :freelance {})]
      (is (= 1500M (tx/total-balance [income1 income2])))))

  (testing "Only expenses"
    (let [expense1 (tx/create-transaction 300M :expense :food {})
          expense2 (tx/create-transaction 200M :expense :transport {})]
      (is (= -500M (tx/total-balance [expense1 expense2]))))))

(deftest test-filtering
  (testing "Filter by type"
    (let [income (tx/create-transaction 1000M :income :salary {})
          expense (tx/create-transaction 300M :expense :food {})
          all-txs [income expense]]
      (is (= 1 (count (tx/by-type all-txs :income))))
      (is (= 1 (count (tx/by-type all-txs :expense))))))

  (testing "Filter by category"
    (let [food1 (tx/create-transaction 100M :expense :food {})
          food2 (tx/create-transaction 200M :expense :food {})
          transport (tx/create-transaction 50M :expense :transport {})
          all-txs [food1 food2 transport]]
      (is (= 2 (count (tx/by-category all-txs :food))))
      (is (= 1 (count (tx/by-category all-txs :transport))))))

  (testing "Filter by currency"
    (let [cop-tx (tx/create-transaction 100M :income :salary {:currency :COP})
          usd-tx (tx/create-transaction 100M :income :salary {:currency :USD})
          all-txs [cop-tx usd-tx]]
      (is (= 1 (count (tx/by-currency all-txs :COP))))
      (is (= 1 (count (tx/by-currency all-txs :USD)))))))

(deftest test-zero-amount
  (testing "Zero amount should be invalid"
    (let [transaction {:transaction/id (random-uuid)
                       :transaction/amount 0M
                       :transaction/type :income
                       :transaction/category :salary
                       :transaction/date (java.util.Date.)
                       :transaction/currency :COP}]
      (is (not (tx/valid? transaction))))))

(deftest test-totals-by-currency
  (testing "Multiple currencies totals"
    (let [cop-income (tx/create-transaction 1000M :income :salary {:currency :COP})
          cop-expense (tx/create-transaction 300M :expense :food {:currency :COP})
          usd-income (tx/create-transaction 100M :income :salary {:currency :USD})
          usd-expense (tx/create-transaction 30M :expense :food {:currency :USD})
          all-txs [cop-income cop-expense usd-income usd-expense]
          totals (tx/totals-by-currency all-txs)]
      (is (= 1000M (get-in totals [:COP :income])))
      (is (= 300M (get-in totals [:COP :expenses])))
      (is (= 100M (get-in totals [:USD :income])))
      (is (= 30M (get-in totals [:USD :expenses]))))))

(deftest test-group-by-category
  (testing "Grouping transactions by category"
    (let [food1 (tx/create-transaction 100M :expense :food {})
          food2 (tx/create-transaction 200M :expense :food {})
          transport (tx/create-transaction 50M :expense :transport {})
          all-txs [food1 food2 transport]
          grouped (tx/group-by-category all-txs)]
      (is (= 2 (count (:food grouped))))
      (is (= 1 (count (:transport grouped)))))))

(deftest test-description-length-validation
  (testing "Description too long (>500 chars) should be invalid"
    (let [long-desc (apply str (repeat 501 "x"))
          transaction {:transaction/id (random-uuid)
                       :transaction/amount 100M
                       :transaction/type :income
                       :transaction/category :salary
                       :transaction/date (java.util.Date.)
                       :transaction/description long-desc
                       :transaction/currency :COP}]
      (is (not (tx/valid? transaction)))))

  (testing "Description exactly 500 chars should be valid"
    (let [exact-desc (apply str (repeat 500 "x"))
          transaction {:transaction/id (random-uuid)
                       :transaction/amount 100M
                       :transaction/type :income
                       :transaction/category :salary
                       :transaction/date (java.util.Date.)
                       :transaction/description exact-desc
                       :transaction/currency :COP}]
      (is (tx/valid? transaction)))))

(deftest test-sorting
  (testing "Sort by date"
    (let [tx1 (assoc (tx/create-transaction 100M :income :salary {})
                     :transaction/date (java.util.Date. 120 0 1))
          tx2 (assoc (tx/create-transaction 200M :income :salary {})
                     :transaction/date (java.util.Date. 120 0 2))
          tx3 (assoc (tx/create-transaction 300M :income :salary {})
                     :transaction/date (java.util.Date. 120 0 3))
          sorted (tx/sort-by-date [tx3 tx1 tx2])]
      (is (= 300M (:transaction/amount (first sorted))))
      (is (= 100M (:transaction/amount (last sorted))))))

  (testing "Sort by amount"
    (let [tx1 (tx/create-transaction 100M :income :salary {})
          tx2 (tx/create-transaction 300M :income :salary {})
          tx3 (tx/create-transaction 200M :income :salary {})
          sorted (tx/sort-by-amount [tx1 tx2 tx3])]
      (is (= 300M (:transaction/amount (first sorted))))
      (is (= 100M (:transaction/amount (last sorted)))))))
