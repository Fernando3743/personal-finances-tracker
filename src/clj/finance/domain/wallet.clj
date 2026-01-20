(ns finance.domain.wallet
  "Pure domain logic for wallets/accounts."
  (:require [clojure.spec.alpha :as s])
  (:import [java.util Date]))

;; =============================================================================
;; Specs
;; =============================================================================

(s/def :wallet/id uuid?)
(s/def :wallet/name (s/and string? #(>= (count %) 1)))
(s/def :wallet/type #{:bank :card :crypto :investment})
(s/def :wallet/institution (s/and string? #(>= (count %) 1)))
(s/def :wallet/account-number (s/and string? #(<= (count %) 4)))
(s/def :wallet/balance number?)
(s/def :wallet/currency #{:USD :COP :EUR :GBP :BTC :ETH :SOL})
(s/def :wallet/status #{:active :synced :updating :error})
(s/def :wallet/color (s/nilable string?))
(s/def :wallet/icon-url (s/nilable string?))
(s/def :wallet/balance-label (s/nilable string?))
(s/def :wallet/user-id uuid?)

(s/def ::wallet
  (s/keys :req [:wallet/id
                :wallet/name
                :wallet/type
                :wallet/institution
                :wallet/balance
                :wallet/currency
                :wallet/status
                :wallet/user-id]
          :opt [:wallet/account-number
                :wallet/color
                :wallet/icon-url
                :wallet/balance-label
                :wallet/created-at
                :wallet/last-updated]))

;; =============================================================================
;; Wallet Construction
;; =============================================================================

(defn create-wallet
  "Creates a new wallet with required fields."
  ([name type institution]
   (create-wallet name type institution {}))
  ([name type institution {:keys [account-number balance currency status color icon-url balance-label]}]
   {:wallet/id (random-uuid)
    :wallet/name name
    :wallet/type (keyword type)
    :wallet/institution institution
    :wallet/account-number (or account-number "")
    :wallet/balance (bigdec (or balance 0))
    :wallet/currency (keyword (or currency :USD))
    :wallet/status (keyword (or status :active))
    :wallet/color color
    :wallet/icon-url icon-url
    :wallet/balance-label (or balance-label "Current Balance")
    :wallet/created-at (Date.)
    :wallet/last-updated (Date.)}))

;; =============================================================================
;; Validation
;; =============================================================================

(defn valid?
  "Validates a wallet against spec."
  [wallet]
  (s/valid? ::wallet wallet))

(defn explain-invalid
  "Returns explanation if wallet is invalid, nil otherwise."
  [wallet]
  (when-not (valid? wallet)
    (s/explain-str ::wallet wallet)))

;; =============================================================================
;; Wallet Type Predicates
;; =============================================================================

(defn bank?
  "Returns true if wallet is a bank account."
  [wallet]
  (= :bank (:wallet/type wallet)))

(defn card?
  "Returns true if wallet is a card (credit/debit)."
  [wallet]
  (= :card (:wallet/type wallet)))

(defn crypto?
  "Returns true if wallet is a crypto wallet."
  [wallet]
  (= :crypto (:wallet/type wallet)))

(defn investment?
  "Returns true if wallet is an investment account."
  [wallet]
  (= :investment (:wallet/type wallet)))

(defn fiat-wallet?
  "Returns true if wallet holds fiat currency."
  [wallet]
  (#{:USD :COP :EUR :GBP} (:wallet/currency wallet)))

(defn crypto-wallet?
  "Returns true if wallet holds cryptocurrency."
  [wallet]
  (#{:BTC :ETH :SOL} (:wallet/currency wallet)))

;; =============================================================================
;; Filtering & Grouping
;; =============================================================================

(defn by-type
  "Filters wallets by type."
  [wallets wallet-type]
  (filter #(= wallet-type (:wallet/type %)) wallets))

(defn by-currency
  "Filters wallets by currency."
  [wallets currency]
  (filter #(= currency (:wallet/currency %)) wallets))

(defn by-status
  "Filters wallets by status."
  [wallets status]
  (filter #(= status (:wallet/status %)) wallets))

(defn group-by-type
  "Groups wallets by their type."
  [wallets]
  (group-by :wallet/type wallets))

(defn group-by-currency
  "Groups wallets by their currency."
  [wallets]
  (group-by :wallet/currency wallets))

(defn active-wallets
  "Returns only active wallets."
  [wallets]
  (by-status wallets :active))

(defn fiat-wallets
  "Returns only fiat currency wallets."
  [wallets]
  (filter fiat-wallet? wallets))

(defn crypto-wallets
  "Returns only crypto wallets."
  [wallets]
  (filter crypto-wallet? wallets))

;; =============================================================================
;; Aggregations
;; =============================================================================

(defn total-balance
  "Calculates total balance of wallets (single currency only)."
  [wallets]
  (reduce + 0M (map :wallet/balance wallets)))

(defn total-balance-by-currency
  "Calculates total balance grouped by currency."
  [wallets]
  (reduce (fn [acc wallet]
            (let [curr (:wallet/currency wallet)
                  bal (:wallet/balance wallet)]
              (update acc curr (fnil + 0M) bal)))
          {}
          wallets))

(defn net-worth
  "Calculates net worth (sum of all positive balances minus debts).
   Cards typically have negative balances representing debt."
  [wallets]
  (reduce + 0M (map :wallet/balance wallets)))

(defn total-assets
  "Returns sum of positive balances (assets)."
  [wallets]
  (reduce + 0M (filter pos? (map :wallet/balance wallets))))

(defn total-liabilities
  "Returns sum of negative balances (debts/liabilities)."
  [wallets]
  (abs (reduce + 0M (filter neg? (map :wallet/balance wallets)))))

;; =============================================================================
;; Status Helpers
;; =============================================================================

(defn needs-sync?
  "Returns true if wallet needs synchronization."
  [wallet]
  (#{:error :updating} (:wallet/status wallet)))

(defn is-syncing?
  "Returns true if wallet is currently syncing."
  [wallet]
  (= :updating (:wallet/status wallet)))

(defn has-error?
  "Returns true if wallet has sync error."
  [wallet]
  (= :error (:wallet/status wallet)))

;; =============================================================================
;; Display Helpers
;; =============================================================================

(defn masked-account-number
  "Returns masked account number for display (e.g., '•••• 4422')."
  [wallet]
  (let [last-four (:wallet/account-number wallet "")]
    (if (seq last-four)
      (str "•••• •••• •••• " last-four)
      "•••• •••• •••• ••••")))

(defn institution-initials
  "Returns 2-letter initials from institution name."
  [wallet]
  (let [institution (:wallet/institution wallet "")]
    (->> (clojure.string/split institution #"\s+")
         (take 2)
         (map #(subs % 0 1))
         (apply str)
         clojure.string/upper-case)))

(defn balance-display-label
  "Returns the appropriate balance label for display."
  [wallet]
  (or (:wallet/balance-label wallet)
      (case (:wallet/type wallet)
        :card "Available Credit"
        :crypto "Total Portfolio"
        :investment "Total Value"
        "Current Balance")))

;; =============================================================================
;; Sorting
;; =============================================================================

(defn sort-by-balance
  "Sorts wallets by balance (highest first by default)."
  ([wallets] (sort-by-balance wallets :desc))
  ([wallets direction]
   (let [comparator (if (= direction :asc)
                      #(compare %1 %2)
                      #(compare %2 %1))]
     (sort-by :wallet/balance comparator wallets))))

(defn sort-by-name
  "Sorts wallets alphabetically by name."
  [wallets]
  (sort-by :wallet/name wallets))

(defn sort-by-last-updated
  "Sorts wallets by last updated (most recent first)."
  [wallets]
  (sort-by :wallet/last-updated #(compare %2 %1) wallets))
