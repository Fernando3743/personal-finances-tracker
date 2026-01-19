(ns finance.utils.currency-test
  "Tests for currency formatting utilities."
  (:require [cljs.test :refer-macros [deftest testing is are]]
            [finance.utils.currency :as currency]))

(deftest currency-symbol-test
  (testing "Returns correct symbol for COP"
    (is (= "$" (currency/currency-symbol :COP))))

  (testing "Returns correct symbol for USD"
    (is (= "US$" (currency/currency-symbol :USD))))

  (testing "Returns default symbol for nil"
    (is (= "$" (currency/currency-symbol nil))))

  (testing "Returns default symbol for unknown currency"
    (is (= "$" (currency/currency-symbol :UNKNOWN)))))

(deftest format-currency-test
  (testing "Formats COP amounts without decimals"
    (is (= "$1,000" (currency/format-currency 1000 :COP)))
    (is (= "$0" (currency/format-currency 0 :COP)))
    (is (= "$1,234,567" (currency/format-currency 1234567 :COP))))

  (testing "Formats USD amounts with 2 decimals"
    (is (= "US$1,000.00" (currency/format-currency 1000 :USD)))
    (is (= "US$0.00" (currency/format-currency 0 :USD)))
    (is (= "US$99.99" (currency/format-currency 99.99 :USD))))

  (testing "Handles negative amounts"
    (is (= "-$1,000" (currency/format-currency -1000 :COP)))
    (is (= "-US$50.00" (currency/format-currency -50 :USD))))

  (testing "Shows positive sign when requested"
    (is (= "+$1,000" (currency/format-currency 1000 :COP {:show-sign? true})))
    (is (= "+US$50.00" (currency/format-currency 50 :USD {:show-sign? true}))))

  (testing "Handles nil amount"
    (is (= "$0" (currency/format-currency nil :COP))))

  (testing "Default currency is COP when called with one arg"
    (is (= "$1,000" (currency/format-currency 1000)))))

(deftest currency-name-test
  (testing "Returns correct name for COP"
    (is (= "Colombian Peso" (currency/currency-name :COP))))

  (testing "Returns correct name for USD"
    (is (= "US Dollar" (currency/currency-name :USD))))

  (testing "Returns Unknown for invalid currency"
    (is (= "Unknown" (currency/currency-name :INVALID)))))

(deftest available-currencies-test
  (testing "Contains expected currencies"
    (is (= [:COP :USD] currency/available-currencies)))

  (testing "COP is first (default)"
    (is (= :COP (first currency/available-currencies)))))
