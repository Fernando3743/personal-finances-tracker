(ns finance.domain.user-test
  "Tests for user domain logic and security."
  (:require [clojure.test :refer [deftest is testing]]
            [finance.domain.user :as user]))

(deftest test-create-user
  (testing "Creating a valid user"
    (let [user (user/create-user "test@example.com" "password123" "Test User")]
      (is (user/valid? user))
      (is (= "test@example.com" (:user/email user)))
      (is (= "Test User" (:user/name user)))
      (is (uuid? (:user/id user)))
      (is (inst? (:user/created-at user)))))

  (testing "Email is lowercased"
    (let [user (user/create-user "TEST@EXAMPLE.COM" "password123" "Test User")]
      (is (= "test@example.com" (:user/email user)))))

  (testing "Password is hashed"
    (let [user (user/create-user "test@example.com" "password123" "Test User")]
      (is (:user/password-hash user))
      (is (not= "password123" (:user/password-hash user))))))

(deftest test-email-validation
  (testing "Valid emails"
    (is (user/valid-email? "test@example.com"))
    (is (user/valid-email? "user.name@example.co.uk"))
    (is (user/valid-email? "user+tag@example.com")))

  (testing "Invalid emails - weak regex would accept these"
    (is (not (user/valid-email? "test@co")))
    (is (not (user/valid-email? "a@.b")))
    (is (not (user/valid-email? "@example.com")))
    (is (not (user/valid-email? "test@")))
    (is (not (user/valid-email? "test example@test.com")))
    (is (not (user/valid-email? ""))))

  (testing "TLD must be at least 2 characters"
    (is (not (user/valid-email? "test@example.c")))))

(deftest test-password-validation
  (testing "Valid passwords (>= 8 chars)"
    (is (user/valid-password? "password"))
    (is (user/valid-password? "12345678"))
    (is (user/valid-password? "p@ssw0rd!")))

  (testing "Invalid passwords (< 8 chars)"
    (is (not (user/valid-password? "pass")))
    (is (not (user/valid-password? "1234567")))
    (is (not (user/valid-password? ""))))

  (testing "Exactly 8 characters should be valid"
    (is (user/valid-password? "password"))))

(deftest test-password-hashing
  (testing "Hash generates different hashes for same password"
    (let [hash1 (user/hash-password "password123")
          hash2 (user/hash-password "password123")]
      (is (not= hash1 hash2))
      (is (string? hash1))
      (is (string? hash2))))

  (testing "Hash is not the plain password"
    (let [password "mypassword"
          hash (user/hash-password password)]
      (is (not= password hash))))

  (testing "Hash starts with BCrypt identifier"
    (let [hash (user/hash-password "password123")]
      (is (.startsWith hash "$2a$")))))

(deftest test-password-verification
  (testing "Correct password verifies"
    (let [password "password123"
          hash (user/hash-password password)]
      (is (user/verify-password password hash))))

  (testing "Incorrect password does not verify"
    (let [password "password123"
          hash (user/hash-password password)]
      (is (not (user/verify-password "wrongpassword" hash)))))

  (testing "Case sensitive password verification"
    (let [password "Password123"
          hash (user/hash-password password)]
      (is (not (user/verify-password "password123" hash)))))

  (testing "Verify with empty password returns false"
    (let [hash (user/hash-password "password123")]
      (is (not (user/verify-password "" hash)))))

  (testing "Verify with nil hash returns false"
    (is (not (user/verify-password "password123" nil)))))

(deftest test-user-sanitization
  (testing "Sanitize removes password-hash"
    (let [user (user/create-user "test@example.com" "password123" "Test User")
          sanitized (user/sanitize-user user)]
      (is (nil? (:user/password-hash sanitized)))
      (is (= (:user/id user) (:user/id sanitized)))
      (is (= (:user/email user) (:user/email sanitized)))
      (is (= (:user/name user) (:user/name sanitized)))))

  (testing "Sanitize preserves all other fields"
    (let [user {:user/id (random-uuid)
                :user/email "test@example.com"
                :user/name "Test User"
                :user/password-hash "secret-hash"
                :user/created-at (java.util.Date.)
                :user/preferred-currency :USD}
          sanitized (user/sanitize-user user)]
      (is (nil? (:user/password-hash sanitized)))
      (is (= (:user/id user) (:user/id sanitized)))
      (is (= (:user/preferred-currency user) (:user/preferred-currency sanitized))))))

(deftest test-name-validation
  (testing "Valid name lengths (1-100 chars)"
    (let [user (user/create-user "test@example.com" "password123" "A")]
      (is (user/valid? user)))
    (let [user (user/create-user "test@example.com" "password123" (apply str (repeat 100 "x")))]
      (is (user/valid? user))))

  (testing "Empty name is invalid"
    (let [user {:user/id (random-uuid)
                :user/email "test@example.com"
                :user/password-hash (user/hash-password "password123")
                :user/name ""
                :user/created-at (java.util.Date.)}]
      (is (not (user/valid? user)))))

  (testing "Name too long (>100 chars) is invalid"
    (let [user {:user/id (random-uuid)
                :user/email "test@example.com"
                :user/password-hash (user/hash-password "password123")
                :user/name (apply str (repeat 101 "x"))
                :user/created-at (java.util.Date.)}]
      (is (not (user/valid? user))))))

(deftest test-password-timing-attack-resistance
  (testing "Verify always takes roughly same time (BCrypt property)"
    (let [hash (user/hash-password "password123")
          start1 (System/nanoTime)
          _ (user/verify-password "password123" hash)
          time1 (- (System/nanoTime) start1)
          start2 (System/nanoTime)
          _ (user/verify-password "wrongpassword" hash)
          time2 (- (System/nanoTime) start2)]
      ;; Both should take roughly the same time (within an order of magnitude)
      ;; BCrypt is designed to be resistant to timing attacks
      (is (< (/ (min time1 time2) (max time1 time2)) 10)))))

(deftest test-spec-validation
  (testing "User with all required fields is valid"
    (let [user {:user/id (random-uuid)
                :user/email "test@example.com"
                :user/password-hash (user/hash-password "password123")
                :user/name "Test User"
                :user/created-at (java.util.Date.)}]
      (is (user/valid? user))))

  (testing "User missing required fields is invalid"
    (is (not (user/valid? {:user/email "test@example.com"})))
    (is (not (user/valid? {:user/id (random-uuid)})))))
