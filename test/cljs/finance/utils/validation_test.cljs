(ns finance.utils.validation-test
  "Tests for validation utilities."
  (:require [cljs.test :refer-macros [deftest is testing run-tests]]
            [finance.utils.validation :as validation]))

(deftest valid-email?-test
  (testing "Valid email addresses"
    (is (true? (validation/valid-email? "user@example.com")))
    (is (true? (validation/valid-email? "test.user@domain.co.uk")))
    (is (true? (validation/valid-email? "user+tag@example.com"))))

  (testing "Invalid email addresses"
    (is (false? (validation/valid-email? "invalid")))
    (is (false? (validation/valid-email? "@example.com")))
    (is (false? (validation/valid-email? "user@")))
    (is (false? (validation/valid-email? "user @example.com")))
    (is (false? (validation/valid-email? nil)))
    (is (false? (validation/valid-email? "")))))

(deftest valid-url?-test
  (testing "Valid safe URLs"
    (is (true? (validation/valid-url? "https://example.com")))
    (is (true? (validation/valid-url? "http://example.com")))
    (is (true? (validation/valid-url? "/relative/path")))
    (is (true? (validation/valid-url? "//cdn.example.com/image.png"))))

  (testing "Dangerous URLs blocked"
    (is (false? (validation/valid-url? "javascript:alert('xss')")))
    (is (false? (validation/valid-url? "data:text/html,<script>alert('xss')</script>")))
    (is (false? (validation/valid-url? "vbscript:msgbox")))
    (is (false? (validation/valid-url? "JAVASCRIPT:alert(1)")))))

(deftest clean-amount-test
  (testing "Valid amount cleaning"
    (is (= "123.45" (validation/clean-amount "123.45")))
    (is (= "123" (validation/clean-amount "123")))
    (is (= "0.99" (validation/clean-amount "0.99"))))

  (testing "Remove invalid characters"
    (is (= "12345" (validation/clean-amount "$123.45" )))
    (is (= "123.45" (validation/clean-amount "abc123.45xyz"))))

  (testing "Fix multiple decimal points"
    (is (= "123.45" (validation/clean-amount "123.4.5")))
    (is (= "1.23" (validation/clean-amount "1...2.3"))))

  (testing "Empty or nil input"
    (is (= "" (validation/clean-amount "")))
    (is (= "" (validation/clean-amount nil)))))

(deftest valid-amount?-test
  (testing "Valid amounts"
    (is (true? (validation/valid-amount? "123.45")))
    (is (true? (validation/valid-amount? "123")))
    (is (true? (validation/valid-amount? "0.01"))))

  (testing "Invalid amounts"
    (is (false? (validation/valid-amount? "0")))
    (is (false? (validation/valid-amount? "-123")))
    (is (false? (validation/valid-amount? "abc")))
    (is (false? (validation/valid-amount? "123.45.67")))
    (is (false? (validation/valid-amount? "")))
    (is (false? (validation/valid-amount? nil)))))

(deftest valid-password?-test
  (testing "Valid passwords"
    (let [result (validation/valid-password? "password123")]
      (is (:valid? result))
      (is (nil? (:message result)))))

  (testing "Password too short"
    (let [result (validation/valid-password? "short")]
      (is (false? (:valid? result)))
      (is (= "Password must be at least 8 characters" (:message result)))))

  (testing "Empty password"
    (let [result (validation/valid-password? "")]
      (is (false? (:valid? result)))
      (is (= "Password is required" (:message result))))))

(deftest passwords-match?-test
  (testing "Matching passwords"
    (is (true? (validation/passwords-match? "password123" "password123"))))

  (testing "Non-matching passwords"
    (is (false? (validation/passwords-match? "password123" "different")))
    (is (false? (validation/passwords-match? "" "")))
    (is (false? (validation/passwords-match? "password" "")))))

(deftest valid-theme?-test
  (testing "Valid themes"
    (is (= :light (validation/valid-theme? :light)))
    (is (= :dark (validation/valid-theme? :dark))))

  (testing "Invalid themes default to light"
    (is (= :light (validation/valid-theme? :invalid)))
    (is (= :light (validation/valid-theme? nil)))
    (is (= :light (validation/valid-theme? :blue)))))

(deftest sanitize-theme-string-test
  (testing "Valid theme strings"
    (is (= :light (validation/sanitize-theme-string "light")))
    (is (= :dark (validation/sanitize-theme-string "dark"))))

  (testing "Invalid inputs default to light"
    (is (= :light (validation/sanitize-theme-string "invalid")))
    (is (= :light (validation/sanitize-theme-string nil)))
    (is (= :light (validation/sanitize-theme-string "")))
    (is (= :light (validation/sanitize-theme-string "  ")))))

;; Run tests when this namespace is loaded
(run-tests)
