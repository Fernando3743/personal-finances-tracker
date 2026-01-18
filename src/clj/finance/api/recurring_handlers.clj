(ns finance.api.recurring-handlers
  "API handlers for recurring transactions."
  (:require [finance.domain.recurring :as recurring]
            [finance.storage.datomic :as db]
            [ring.util.response :as response])
  (:import [java.util Date]))

(defn- json-response
  ([body] (json-response body 200))
  ([body status]
   (-> (response/response body)
       (response/status status)
       (response/content-type "application/json"))))

(defn- error-response
  [message status]
  (json-response {:error message} status))

(defn list-recurring
  "GET /api/recurring - Returns current user's recurring transactions."
  [conn request]
  (let [user-id (:user-id request)
        items (db/load-recurring-for-user conn user-id)]
    (json-response {:recurring (recurring/sort-by-next-occurrence items)
                    :count (count items)})))

(defn get-recurring
  "GET /api/recurring/:id - Returns a single recurring transaction if owned by user."
  [conn id request]
  (let [user-id (:user-id request)
        rec-id (parse-uuid id)]
    (if (db/user-owns-recurring? conn user-id rec-id)
      (if-let [item (db/load-recurring-by-id conn rec-id)]
        (json-response item)
        (error-response "Recurring transaction not found" 404))
      (error-response "Recurring transaction not found" 404))))

(defn create-recurring
  "POST /api/recurring - Creates a new recurring transaction for current user."
  [conn request]
  (let [user-id (:user-id request)
        {:keys [amount type category frequency description currency start-date end-date active]} (:body request)]
    (if (and amount type category frequency)
      (let [rec (recurring/create-recurring
                 (bigdec amount)
                 (keyword type)
                 (keyword category)
                 (keyword frequency)
                 {:description description
                  :currency (keyword (or currency "COP"))
                  :start-date (when start-date (Date. (long start-date)))
                  :end-date (when end-date (Date. (long end-date)))
                  :active? (if (nil? active) true active)})
            rec-with-user (assoc rec :recurring/user-id user-id)]
        (if (recurring/valid? rec-with-user)
          (do
            (db/save-recurring! conn rec-with-user)
            (json-response rec-with-user 201))
          (error-response (recurring/explain-invalid rec-with-user) 400)))
      (error-response "Missing required fields: amount, type, category, frequency" 400))))

(defn update-recurring
  "PUT /api/recurring/:id - Updates a recurring transaction if owned by user."
  [conn id request]
  (let [user-id (:user-id request)
        uuid (parse-uuid id)
        updates (:body request)]
    (if (db/user-owns-recurring? conn user-id uuid)
      (if-let [_existing (db/load-recurring-by-id conn uuid)]
        (let [converted-updates
              (reduce-kv
               (fn [m k v]
                 (let [key-name (name k)
                       new-key (keyword "recurring" key-name)]
                   (assoc m new-key
                          (case key-name
                            "type" (keyword v)
                            "category" (keyword v)
                            "currency" (keyword v)
                            "frequency" (keyword v)
                            "amount" (bigdec v)
                            "active" v
                            "start-date" (when v (Date. (long v)))
                            "end-date" (when v (Date. (long v)))
                            "next-occurrence" (when v (Date. (long v)))
                            v))))
               {}
               updates)]
          (if-let [updated (db/update-recurring! conn uuid converted-updates)]
            (json-response updated)
            (error-response "Update failed" 500)))
        (error-response "Recurring transaction not found" 404))
      (error-response "Recurring transaction not found" 404))))

(defn delete-recurring
  "DELETE /api/recurring/:id - Deletes a recurring transaction if owned by user."
  [conn id request]
  (let [user-id (:user-id request)
        uuid (parse-uuid id)]
    (if (db/user-owns-recurring? conn user-id uuid)
      (if (db/delete-recurring! conn uuid)
        (json-response {:deleted true})
        (error-response "Recurring transaction not found" 404))
      (error-response "Recurring transaction not found" 404))))

(defn toggle-active
  "POST /api/recurring/:id/toggle - Toggles active status."
  [conn id request]
  (let [user-id (:user-id request)
        uuid (parse-uuid id)]
    (if (db/user-owns-recurring? conn user-id uuid)
      (if-let [existing (db/load-recurring-by-id conn uuid)]
        (let [new-active (not (:recurring/active? existing))]
          (if-let [updated (db/update-recurring! conn uuid {:recurring/active? new-active})]
            (json-response updated)
            (error-response "Update failed" 500)))
        (error-response "Recurring transaction not found" 404))
      (error-response "Recurring transaction not found" 404))))

(defn generate-transactions
  "POST /api/recurring/generate - Generates pending transactions from recurring templates."
  [conn request]
  (let [user-id (:user-id request)
        now (Date.)
        user-recurring (db/load-recurring-for-user conn user-id)
        pending (filter #(recurring/should-generate? % now) user-recurring)
        generated (doall
                   (for [rec pending]
                     (let [tx (-> (recurring/generate-transaction rec)
                                  (assoc :transaction/user-id user-id))
                           updated-rec (recurring/advance-next-occurrence rec)]
                       (db/save-transaction-for-user! conn user-id tx)
                       (db/update-recurring! conn (:recurring/id rec)
                                             {:recurring/next-occurrence (:recurring/next-occurrence updated-rec)
                                              :recurring/last-generated (:recurring/last-generated updated-rec)})
                       tx)))]
    (json-response {:generated (vec generated)
                    :count (count generated)})))

(defn upcoming-recurring
  "GET /api/recurring/upcoming - Returns recurring transactions due within n days."
  [conn request]
  (let [user-id (:user-id request)
        days (or (some-> (get-in request [:params :days]) Integer/parseInt) 7)
        items (db/load-recurring-for-user conn user-id)
        upcoming (recurring/upcoming items days)]
    (json-response {:upcoming upcoming
                    :count (count upcoming)})))
