(ns finance.utils.date-test
  "Tests for date formatting utilities."
  (:require [cljs.test :refer-macros [deftest testing is]]
            [finance.utils.date :as date]))

(deftest format-date-test
  (testing "Formats ISO string date"
    (let [result (date/format-date "2023-12-25")]
      (is (string? result))
      (is (re-find #"Dec" result))
      (is (re-find #"25" result))
      (is (re-find #"2023" result))))

  (testing "Formats js/Date object"
    (let [d (js/Date. 2023 11 25)
          result (date/format-date d)]
      (is (string? result))
      (is (re-find #"Dec" result))
      (is (re-find #"25" result))))

  (testing "Returns nil for nil input"
    (is (nil? (date/format-date nil))))

  (testing "Returns nil for invalid date string"
    (is (nil? (date/format-date "not-a-date")))))

(deftest format-date-short-test
  (testing "Formats to short format without year"
    (let [result (date/format-date-short "2023-12-25")]
      (is (string? result))
      (is (re-find #"Dec" result))
      (is (re-find #"25" result))))

  (testing "Returns nil for nil input"
    (is (nil? (date/format-date-short nil)))))

(deftest format-month-year-test
  (testing "Formats to month and year only"
    (let [result (date/format-month-year "2023-12-25")]
      (is (string? result))
      (is (re-find #"December" result))
      (is (re-find #"2023" result))))

  (testing "Returns nil for nil input"
    (is (nil? (date/format-month-year nil)))))

(deftest format-day-test
  (testing "Formats to day only"
    (let [result (date/format-day "2023-12-25")]
      (is (string? result))
      (is (re-find #"25" result))))

  (testing "Returns nil for nil input"
    (is (nil? (date/format-day nil)))))

(deftest today-iso-test
  (testing "Returns ISO format string"
    (let [result (date/today-iso)]
      (is (string? result))
      (is (re-matches #"\d{4}-\d{2}-\d{2}" result))))

  (testing "Returns correct length"
    (is (= 10 (count (date/today-iso)))))

  (testing "Contains current year"
    (let [current-year (str (.getFullYear (js/Date.)))]
      (is (clojure.string/starts-with? (date/today-iso) current-year)))))
