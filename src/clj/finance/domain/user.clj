(ns finance.domain.user
  "Pure domain logic for users.
   Demonstrates: SRP, Pure Functions, Spec-based validation."
  (:require [clojure.spec.alpha :as s]
            [clojure.string :as str])
  (:import [at.favre.lib.crypto.bcrypt BCrypt]))

(s/def :user/id uuid?)
(s/def :user/email (s/and string? #(re-matches #".+@.+\..+" %)))
(s/def :user/password-hash string?)
(s/def :user/name (s/and string? #(<= 1 (count %) 100)))
(s/def :user/created-at inst?)

;; Password validation (plain text before hashing)
(s/def ::password (s/and string? #(>= (count %) 8)))

(s/def ::user
  (s/keys :req [:user/id
                :user/email
                :user/password-hash
                :user/name
                :user/created-at]))

(defn hash-password
  "Hashes a plain-text password using BCrypt."
  [password]
  (.hashToString (BCrypt/withDefaults) 12 (.toCharArray password)))

(defn verify-password
  "Verifies a plain-text password against a BCrypt hash."
  [password hash]
  (-> (BCrypt/verifyer)
      (.verify (.toCharArray password) hash)
      (.verified)))

(defn create-user
  "Creates a new user map with hashed password."
  [email password name]
  {:user/id (random-uuid)
   :user/email (str/lower-case email)
   :user/password-hash (hash-password password)
   :user/name name
   :user/created-at (java.util.Date.)})

(defn valid?
  "Validates a user against spec."
  [user]
  (s/valid? ::user user))

(defn valid-password?
  "Validates password meets requirements (min 8 chars)."
  [password]
  (s/valid? ::password password))

(defn valid-email?
  "Validates email format."
  [email]
  (s/valid? :user/email email))

(defn sanitize-user
  "Removes sensitive fields (password-hash) for API responses."
  [user]
  (dissoc user :user/password-hash))
