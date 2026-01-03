(ns finance.api.handlers
  "API request handlers.
   Thin layer that coordinates between HTTP and domain logic."
  (:require [finance.domain.transaction :as tx]
            [finance.domain.reports :as reports]
            [finance.storage.protocol :as store]
            [ring.util.response :as response]))

(defn- json-response
  "Creates a JSON response with the given body and status."
  ([body] (json-response body 200))
  ([body status]
   (-> (response/response body)
       (response/status status)
       (response/content-type "application/json"))))

(defn- error-response
  "Creates an error response."
  [message status]
  (json-response {:error message} status))

;; =============================================================================
;; Transaction Handlers
;; =============================================================================

(defn list-transactions
  "GET /api/transactions - Returns all transactions."
  [store _request]
  (let [transactions (store/load-transactions store)]
    (json-response {:transactions (tx/sort-by-date transactions)
                    :count (count transactions)})))

(defn get-transaction
  "GET /api/transactions/:id - Returns a single transaction."
  [store id _request]
  (if-let [transaction (store/load-transaction store (parse-uuid id))]
    (json-response transaction)
    (error-response "Transaction not found" 404)))

(defn create-transaction
  "POST /api/transactions - Creates a new transaction."
  [store request]
  (let [{:keys [amount type category description tags]} (:body request)]
    (if (and amount type category)
      (let [transaction (tx/create-transaction
                         (bigdec amount)
                         (keyword type)
                         (keyword category)
                         {:description (or description "")
                          :tags (set (map keyword (or tags [])))})]
        (if (tx/valid? transaction)
          (do
            (store/save-transaction! store transaction)
            (json-response transaction 201))
          (error-response (tx/explain-invalid transaction) 400)))
      (error-response "Missing required fields: amount, type, category" 400))))

(defn update-transaction
  "PUT /api/transactions/:id - Updates a transaction."
  [store id request]
  (let [uuid (parse-uuid id)
        updates (:body request)]
    (if-let [_existing (store/load-transaction store uuid)]
      (let [;; Convert string keys to namespaced keywords
            converted-updates
            (reduce-kv
             (fn [m k v]
               (let [key-name (name k)
                     new-key (keyword "transaction" key-name)]
                 (assoc m new-key
                        (case key-name
                          "type" (keyword v)
                          "category" (keyword v)
                          "tags" (set (map keyword v))
                          "amount" (bigdec v)
                          v))))
             {}
             updates)]
        (if-let [updated (store/update-transaction! store uuid converted-updates)]
          (json-response updated)
          (error-response "Update failed" 500)))
      (error-response "Transaction not found" 404))))

(defn delete-transaction
  "DELETE /api/transactions/:id - Deletes a transaction."
  [store id _request]
  (if (store/delete-transaction! store (parse-uuid id))
    (json-response {:deleted true})
    (error-response "Transaction not found" 404)))

;; =============================================================================
;; Summary Handlers
;; =============================================================================

(defn get-summary
  "GET /api/summary - Returns balance and category breakdown."
  [store _request]
  (let [transactions (store/load-transactions store)]
    (json-response (reports/balance-report transactions))))

(defn get-category-breakdown
  "GET /api/summary/categories - Returns category breakdown."
  [store _request]
  (let [transactions (store/load-transactions store)]
    (json-response (reports/category-breakdown transactions))))

(defn get-monthly-report
  "GET /api/summary/monthly - Returns monthly trend data."
  [store _request]
  (let [transactions (store/load-transactions store)]
    (json-response (reports/monthly-report transactions))))

(defn get-dashboard
  "GET /api/dashboard - Returns all dashboard data."
  [store _request]
  (let [transactions (store/load-transactions store)]
    (json-response (reports/dashboard-data transactions))))
