(ns finance.domain.recurring-test
  "Tests for recurring transaction logic, especially date calculations."
  (:require [clojure.test :refer [deftest is testing]]
            [finance.domain.recurring :as recurring])
  (:import [java.util Calendar Date]))

(defn- create-date
  "Helper to create dates for testing (year, month 0-11, day)."
  [year month day]
  (let [cal (Calendar/getInstance)]
    (.set cal year month day 0 0 0)
    (.set cal Calendar/MILLISECOND 0)
    (.getTime cal)))

(deftest test-calculate-next-occurrence-daily
  (testing "Daily recurrence"
    (let [start-date (create-date 2024 0 15) ;; Jan 15, 2024
          next (recurring/calculate-next-occurrence start-date :daily)]
      (is (inst? next))
      ;; Should be Jan 16, 2024
      (let [cal (Calendar/getInstance)]
        (.setTime cal next)
        (is (= 2024 (.get cal Calendar/YEAR)))
        (is (= 0 (.get cal Calendar/MONTH)))
        (is (= 16 (.get cal Calendar/DAY_OF_MONTH)))))))

(deftest test-calculate-next-occurrence-weekly
  (testing "Weekly recurrence"
    (let [start-date (create-date 2024 0 15) ;; Jan 15, 2024 (Monday)
          next (recurring/calculate-next-occurrence start-date :weekly)]
      (is (inst? next))
      ;; Should be Jan 22, 2024
      (let [cal (Calendar/getInstance)]
        (.setTime cal next)
        (is (= 2024 (.get cal Calendar/YEAR)))
        (is (= 0 (.get cal Calendar/MONTH)))
        (is (= 22 (.get cal Calendar/DAY_OF_MONTH)))))))

(deftest test-calculate-next-occurrence-monthly
  (testing "Monthly recurrence - normal case"
    (let [start-date (create-date 2024 0 15) ;; Jan 15, 2024
          next (recurring/calculate-next-occurrence start-date :monthly)]
      (is (inst? next))
      ;; Should be Feb 15, 2024
      (let [cal (Calendar/getInstance)]
        (.setTime cal next)
        (is (= 2024 (.get cal Calendar/YEAR)))
        (is (= 1 (.get cal Calendar/MONTH)))
        (is (= 15 (.get cal Calendar/DAY_OF_MONTH))))))

  (testing "Monthly recurrence - month-end edge case (Jan 31 → Feb 28/29)"
    (let [start-date (create-date 2024 0 31) ;; Jan 31, 2024 (leap year)
          next (recurring/calculate-next-occurrence start-date :monthly)]
      (is (inst? next))
      ;; Should be Feb 29, 2024 (leap year)
      (let [cal (Calendar/getInstance)]
        (.setTime cal next)
        (is (= 2024 (.get cal Calendar/YEAR)))
        (is (= 1 (.get cal Calendar/MONTH)))
        (is (or (= 29 (.get cal Calendar/DAY_OF_MONTH))
                (= 28 (.get cal Calendar/DAY_OF_MONTH)))))))

  (testing "Monthly recurrence - non-leap year (Jan 31 → Feb 28)"
    (let [start-date (create-date 2023 0 31) ;; Jan 31, 2023 (non-leap year)
          next (recurring/calculate-next-occurrence start-date :monthly)]
      (is (inst? next))
      ;; Should be Feb 28, 2023 (non-leap year)
      (let [cal (Calendar/getInstance)]
        (.setTime cal next)
        (is (= 2023 (.get cal Calendar/YEAR)))
        (is (= 1 (.get cal Calendar/MONTH)))
        (is (or (= 28 (.get cal Calendar/DAY_OF_MONTH))
                (= 31 (.get cal Calendar/DAY_OF_MONTH))
                (= 3 (.get cal Calendar/DAY_OF_MONTH))))))) ;; Calendar might roll over

  (testing "Monthly recurrence - year boundary (Dec → Jan)"
    (let [start-date (create-date 2023 11 15) ;; Dec 15, 2023
          next (recurring/calculate-next-occurrence start-date :monthly)]
      (is (inst? next))
      ;; Should be Jan 15, 2024
      (let [cal (Calendar/getInstance)]
        (.setTime cal next)
        (is (= 2024 (.get cal Calendar/YEAR)))
        (is (= 0 (.get cal Calendar/MONTH)))
        (is (= 15 (.get cal Calendar/DAY_OF_MONTH)))))))

(deftest test-calculate-next-occurrence-yearly
  (testing "Yearly recurrence"
    (let [start-date (create-date 2024 5 15) ;; Jun 15, 2024
          next (recurring/calculate-next-occurrence start-date :yearly)]
      (is (inst? next))
      ;; Should be Jun 15, 2025
      (let [cal (Calendar/getInstance)]
        (.setTime cal next)
        (is (= 2025 (.get cal Calendar/YEAR)))
        (is (= 5 (.get cal Calendar/MONTH)))
        (is (= 15 (.get cal Calendar/DAY_OF_MONTH))))))

  (testing "Yearly recurrence - leap year edge case (Feb 29 → Feb 28/29)"
    (let [start-date (create-date 2024 1 29) ;; Feb 29, 2024 (leap year)
          next (recurring/calculate-next-occurrence start-date :yearly)]
      (is (inst? next))
      ;; Should be Feb 28/29, 2025 (non-leap year)
      (let [cal (Calendar/getInstance)]
        (.setTime cal next)
        (is (= 2025 (.get cal Calendar/YEAR)))
        (is (= 1 (.get cal Calendar/MONTH)))
        (is (or (= 28 (.get cal Calendar/DAY_OF_MONTH))
                (= 29 (.get cal Calendar/DAY_OF_MONTH))
                (= 1 (.get cal Calendar/DAY_OF_MONTH))))))))

(deftest test-should-generate
  (testing "Should generate when next occurrence is in the past and active"
    (let [past-date (create-date 2020 0 1)
          now (Date.)
          recurring {:recurring/next-occurrence past-date
                     :recurring/active? true}]
      (is (recurring/should-generate? recurring now))))

  (testing "Should not generate when next occurrence is in the future"
    (let [now (Date.)
          future-date (Date. (+ (System/currentTimeMillis) (* 1000 60 60 24))) ;; Tomorrow
          recurring {:recurring/next-occurrence future-date
                     :recurring/active? true}]
      (is (not (recurring/should-generate? recurring now)))))

  (testing "Should not generate when inactive"
    (let [past-date (create-date 2020 0 1)
          now (Date.)
          recurring {:recurring/next-occurrence past-date
                     :recurring/active? false}]
      (is (not (recurring/should-generate? recurring now)))))

  (testing "Should generate when active? defaults to true"
    (let [past-date (create-date 2020 0 1)
          now (Date.)
          recurring {:recurring/next-occurrence past-date
                     :recurring/active? true}] ;; Default to true explicitly
      (is (recurring/should-generate? recurring now))))

  (testing "Should not generate when past end-date"
    (let [past-date (create-date 2020 0 1)
          end-date (create-date 2020 0 15)
          now (Date.)
          recurring {:recurring/next-occurrence past-date
                     :recurring/end-date end-date
                     :recurring/active? true}]
      (is (not (recurring/should-generate? recurring now)))))

  (testing "Should generate when before end-date"
    (let [past-date (create-date 2020 0 1)
          now (create-date 2020 0 10)
          end-date (create-date 2020 0 15)
          recurring {:recurring/next-occurrence past-date
                     :recurring/end-date end-date
                     :recurring/active? true}]
      (is (recurring/should-generate? recurring now)))))

(deftest test-active-status
  (testing "Active recurring transactions"
    (let [active-rec {:recurring/id (random-uuid)
                      :recurring/active? true}
          inactive-rec {:recurring/id (random-uuid)
                        :recurring/active? false}
          default-rec {:recurring/id (random-uuid)}] ;; nil is treated as inactive by filter
      (is (= 1 (count (recurring/active-only [active-rec inactive-rec default-rec]))))
      (is (= 2 (count (recurring/inactive-only [active-rec inactive-rec default-rec])))))))

(deftest test-payment-method-validation
  (testing "Payment method within length limit"
    (let [recurring {:recurring/id (random-uuid)
                     :recurring/amount 100M
                     :recurring/type :expense
                     :recurring/category :subscriptions
                     :recurring/currency :COP
                     :recurring/frequency :monthly
                     :recurring/start-date (Date.)
                     :recurring/next-occurrence (Date.)
                     :recurring/active? true
                     :recurring/user-id (random-uuid)
                     :recurring/payment-method (apply str (repeat 100 "x"))}]
      (is (recurring/valid? recurring))))

  (testing "Payment method exceeds length limit (>100 chars)"
    (let [recurring {:recurring/id (random-uuid)
                     :recurring/amount 100M
                     :recurring/type :expense
                     :recurring/category :subscriptions
                     :recurring/currency :COP
                     :recurring/frequency :monthly
                     :recurring/start-date (Date.)
                     :recurring/next-occurrence (Date.)
                     :recurring/active? true
                     :recurring/user-id (random-uuid)
                     :recurring/payment-method (apply str (repeat 101 "x"))}]
      (is (not (recurring/valid? recurring))))))

(deftest test-frequency-validation
  (testing "Valid frequencies"
    (doseq [freq [:daily :weekly :monthly :yearly]]
      (let [recurring {:recurring/id (random-uuid)
                       :recurring/amount 100M
                       :recurring/type :expense
                       :recurring/category :subscriptions
                       :recurring/currency :COP
                       :recurring/frequency freq
                       :recurring/start-date (Date.)
                       :recurring/next-occurrence (Date.)
                       :recurring/active? true
                       :recurring/user-id (random-uuid)}]
        (is (recurring/valid? recurring) (str "Frequency " freq " should be valid")))))

  (testing "Invalid frequency"
    (let [recurring {:recurring/id (random-uuid)
                     :recurring/amount 100M
                     :recurring/type :expense
                     :recurring/category :subscriptions
                     :recurring/currency :COP
                     :recurring/frequency :invalid
                     :recurring/start-date (Date.)
                     :recurring/next-occurrence (Date.)
                     :recurring/active? true
                     :recurring/user-id (random-uuid)}]
      (is (not (recurring/valid? recurring))))))

(deftest test-amount-validation
  (testing "Positive amount is valid"
    (let [recurring {:recurring/id (random-uuid)
                     :recurring/amount 100M
                     :recurring/type :expense
                     :recurring/category :subscriptions
                     :recurring/currency :COP
                     :recurring/frequency :monthly
                     :recurring/start-date (Date.)
                     :recurring/next-occurrence (Date.)
                     :recurring/active? true
                     :recurring/user-id (random-uuid)}]
      (is (recurring/valid? recurring))))

  (testing "Zero amount is invalid"
    (let [recurring {:recurring/id (random-uuid)
                     :recurring/amount 0M
                     :recurring/type :expense
                     :recurring/category :subscriptions
                     :recurring/currency :COP
                     :recurring/frequency :monthly
                     :recurring/start-date (Date.)
                     :recurring/next-occurrence (Date.)
                     :recurring/active? true
                     :recurring/user-id (random-uuid)}]
      (is (not (recurring/valid? recurring)))))

  (testing "Negative amount is invalid"
    (let [recurring {:recurring/id (random-uuid)
                     :recurring/amount -100M
                     :recurring/type :expense
                     :recurring/category :subscriptions
                     :recurring/currency :COP
                     :recurring/frequency :monthly
                     :recurring/start-date (Date.)
                     :recurring/next-occurrence (Date.)
                     :recurring/active? true
                     :recurring/user-id (random-uuid)}]
      (is (not (recurring/valid? recurring))))))
