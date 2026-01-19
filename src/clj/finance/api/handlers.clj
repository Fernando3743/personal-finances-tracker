(ns finance.api.handlers
  "API request handlers.
   Thin layer that coordinates between HTTP and domain logic."
  (:require [finance.domain.transaction :as tx]
            [finance.domain.reports :as reports]
            [finance.storage.datomic :as db]
            [finance.api.common :as common]
            [clojure.tools.logging :as log]))

(defn list-transactions
  "GET /api/transactions - Returns current user's transactions."
  [conn request]
  (let [user-id (:user-id request)
        transactions (db/load-transactions-for-user conn user-id)]
    (log/debug (str "Fetched " (count transactions) " transactions for user " user-id))
    (common/json-response {:transactions (tx/sort-by-date transactions)
                           :count (count transactions)})))

(defn get-transaction
  "GET /api/transactions/:id - Returns a single transaction if owned by user."
  [conn id request]
  (if-let [tx-id (common/safe-parse-uuid id)]
    (let [user-id (:user-id request)]
      (common/with-owned-resource
        (partial db/user-owns-transaction? conn)
        (partial db/load-transaction conn)
        tx-id
        user-id
        common/json-response))
    (common/error-response "Invalid transaction ID format" 400)))

(defn create-transaction
  "POST /api/transactions - Creates a new transaction for current user."
  [conn request]
  (let [user-id (:user-id request)
        {:keys [amount type category description tags currency exchange-rate]} (:body request)]
    (if (and amount type category)
      (if-let [parsed-amount (common/safe-parse-bigdec (str amount))]
        (let [transaction (tx/create-transaction
                           parsed-amount
                           (keyword type)
                           (keyword category)
                           {:description (or description "")
                            :tags (when tags (set (map keyword tags)))
                            :currency (keyword (or currency "COP"))
                            :exchange-rate (when exchange-rate (common/safe-parse-bigdec (str exchange-rate)))})]
          (if (tx/valid? transaction)
            (do
              (log/info (str "Creating transaction for user " user-id " - type: " type " category: " category " amount: " parsed-amount))
              (db/save-transaction-for-user! conn user-id transaction)
              (common/json-response transaction 201))
            (common/error-response (tx/explain-invalid transaction) 400)))
        (common/error-response "Invalid amount format" 400))
      (common/error-response "Missing required fields: amount, type, category" 400))))

(defn update-transaction
  "PUT /api/transactions/:id - Updates a transaction if owned by user."
  [conn id request]
  (if-let [uuid (common/safe-parse-uuid id)]
    (let [user-id (:user-id request)
          updates (:body request)]
      (if (db/user-owns-transaction? conn user-id uuid)
        (if-let [_existing (db/load-transaction conn uuid)]
          (let [converted-updates (common/convert-updates "transaction" updates)]
            (log/info (str "Updating transaction " uuid " for user " user-id))
            (if-let [updated (db/update-transaction! conn uuid converted-updates)]
              (common/json-response updated)
              (common/error-response "Update failed" 500)))
          (common/error-response "Transaction not found" 404))
        (common/error-response "Transaction not found" 404)))
    (common/error-response "Invalid transaction ID format" 400)))

(defn delete-transaction
  "DELETE /api/transactions/:id - Deletes a transaction if owned by user."
  [conn id request]
  (if-let [uuid (common/safe-parse-uuid id)]
    (let [user-id (:user-id request)]
      (if (db/user-owns-transaction? conn user-id uuid)
        (do
          (log/info (str "Deleting transaction " uuid " for user " user-id))
          (if (db/delete-transaction! conn uuid)
            (common/json-response {:deleted true})
            (common/error-response "Transaction not found" 404)))
        (common/error-response "Transaction not found" 404)))
    (common/error-response "Invalid transaction ID format" 400)))

(defn get-summary
  "GET /api/summary - Returns balance and category breakdown for current user."
  [conn request]
  (let [user-id (:user-id request)
        transactions (db/load-transactions-for-user conn user-id)]
    (common/json-response (reports/balance-report transactions))))

(defn get-category-breakdown
  "GET /api/summary/categories - Returns category breakdown for current user."
  [conn request]
  (let [user-id (:user-id request)
        transactions (db/load-transactions-for-user conn user-id)]
    (common/json-response (reports/category-breakdown transactions))))

(defn get-monthly-report
  "GET /api/summary/monthly - Returns monthly trend data for current user."
  [conn request]
  (let [user-id (:user-id request)
        transactions (db/load-transactions-for-user conn user-id)]
    (common/json-response (reports/monthly-report transactions))))

(defn get-dashboard
  "GET /api/dashboard - Returns all dashboard data for current user."
  [conn request]
  (let [user-id (:user-id request)
        transactions (db/load-transactions-for-user conn user-id)]
    (log/debug (str "Fetching dashboard for user " user-id))
    (common/json-response (reports/dashboard-data transactions))))
