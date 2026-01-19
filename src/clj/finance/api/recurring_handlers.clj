(ns finance.api.recurring-handlers
  "API handlers for recurring transactions."
  (:require [finance.domain.recurring :as recurring]
            [finance.storage.datomic :as db]
            [finance.api.common :as common]
            [clojure.tools.logging :as log])
  (:import [java.util Date]))

(defn list-recurring
  "GET /api/recurring - Returns current user's recurring transactions."
  [conn request]
  (let [user-id (:user-id request)
        items (db/load-recurring-for-user conn user-id)]
    (log/info "Listing recurring transactions for user" user-id)
    (common/json-response {:recurring (recurring/sort-by-next-occurrence items)
                           :count (count items)})))

(defn get-recurring
  "GET /api/recurring/:id - Returns a single recurring transaction if owned by user."
  [conn id request]
  (let [user-id (:user-id request)
        rec-id (common/safe-parse-uuid id)]
    (if-not rec-id
      (common/error-response "Invalid recurring transaction ID" 400)
      (if (db/user-owns-recurring? conn user-id rec-id)
        (if-let [item (db/load-recurring-by-id conn rec-id)]
          (do
            (log/info "Retrieved recurring transaction" rec-id "for user" user-id)
            (common/json-response item))
          (common/error-response "Recurring transaction not found" 404))
        (do
          (log/warn "User" user-id "attempted to access recurring transaction" rec-id "they don't own")
          (common/error-response "Recurring transaction not found" 404))))))

(defn create-recurring
  "POST /api/recurring - Creates a new recurring transaction for current user."
  [conn request]
  (let [user-id (:user-id request)
        {:keys [amount type category frequency description currency start-date end-date active payment-method]} (:body request)]
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
                  :active? (if (nil? active) true active)
                  :payment-method payment-method})
            rec-with-user (assoc rec :recurring/user-id user-id)]
        (if (recurring/valid? rec-with-user)
          (do
            (log/info "Creating recurring transaction for user" user-id ":" (select-keys rec-with-user [:recurring/type :recurring/category :recurring/frequency]))
            (db/save-recurring! conn rec-with-user)
            (common/json-response rec-with-user 201))
          (do
            (log/warn "Invalid recurring transaction data for user" user-id)
            (common/error-response (recurring/explain-invalid rec-with-user) 400))))
      (common/error-response "Missing required fields: amount, type, category, frequency" 400))))

(defn update-recurring
  "PUT /api/recurring/:id - Updates a recurring transaction if owned by user."
  [conn id request]
  (let [user-id (:user-id request)
        uuid (common/safe-parse-uuid id)
        updates (:body request)]
    (if-not uuid
      (common/error-response "Invalid recurring transaction ID" 400)
      (if (db/user-owns-recurring? conn user-id uuid)
        (if-let [_existing (db/load-recurring-by-id conn uuid)]
          (let [converted-updates (-> (common/convert-updates "recurring" updates)
                                      (update :recurring/start-date #(when % (Date. (long %))))
                                      (update :recurring/end-date #(when % (Date. (long %))))
                                      (update :recurring/next-occurrence #(when % (Date. (long %)))))]
            (log/info "Updating recurring transaction" uuid "for user" user-id)
            (if-let [updated (db/update-recurring! conn uuid converted-updates)]
              (common/json-response updated)
              (common/error-response "Update failed" 500)))
          (common/error-response "Recurring transaction not found" 404))
        (do
          (log/warn "User" user-id "attempted to update recurring transaction" uuid "they don't own")
          (common/error-response "Recurring transaction not found" 404))))))

(defn delete-recurring
  "DELETE /api/recurring/:id - Deletes a recurring transaction if owned by user."
  [conn id request]
  (let [user-id (:user-id request)
        uuid (common/safe-parse-uuid id)]
    (if-not uuid
      (common/error-response "Invalid recurring transaction ID" 400)
      (if (db/user-owns-recurring? conn user-id uuid)
        (do
          (log/info "Deleting recurring transaction" uuid "for user" user-id)
          (if (db/delete-recurring! conn uuid)
            (common/json-response {:deleted true})
            (common/error-response "Recurring transaction not found" 404)))
        (do
          (log/warn "User" user-id "attempted to delete recurring transaction" uuid "they don't own")
          (common/error-response "Recurring transaction not found" 404))))))

(defn toggle-active
  "POST /api/recurring/:id/toggle - Toggles active status."
  [conn id request]
  (let [user-id (:user-id request)
        uuid (common/safe-parse-uuid id)]
    (if-not uuid
      (common/error-response "Invalid recurring transaction ID" 400)
      (if (db/user-owns-recurring? conn user-id uuid)
        (if-let [existing (db/load-recurring-by-id conn uuid)]
          (let [new-active (not (:recurring/active? existing))]
            (log/info "Toggling active status for recurring transaction" uuid "to" new-active)
            (if-let [updated (db/update-recurring! conn uuid {:recurring/active? new-active})]
              (common/json-response updated)
              (common/error-response "Update failed" 500)))
          (common/error-response "Recurring transaction not found" 404))
        (do
          (log/warn "User" user-id "attempted to toggle recurring transaction" uuid "they don't own")
          (common/error-response "Recurring transaction not found" 404))))))

(defn generate-transactions
  "POST /api/recurring/generate - Generates pending transactions from recurring templates."
  [conn request]
  (let [user-id (:user-id request)
        now (Date.)
        user-recurring (db/load-recurring-for-user conn user-id)
        pending (filter #(recurring/should-generate? % now) user-recurring)
        _ (log/info "Generating transactions from" (count pending) "pending recurring transactions for user" user-id)
        generated (doall
                   (for [rec pending]
                     (let [tx (-> (recurring/generate-transaction rec)
                                  (assoc :transaction/user-id user-id))
                           updated-rec (recurring/advance-next-occurrence rec)]
                       (log/debug "Generated transaction from recurring" (:recurring/id rec))
                       (db/save-transaction-for-user! conn user-id tx)
                       (db/update-recurring! conn (:recurring/id rec)
                                             {:recurring/next-occurrence (:recurring/next-occurrence updated-rec)
                                              :recurring/last-generated (:recurring/last-generated updated-rec)})
                       tx)))]
    (log/info "Successfully generated" (count generated) "transactions for user" user-id)
    (common/json-response {:generated (vec generated)
                           :count (count generated)})))

(defn upcoming-recurring
  "GET /api/recurring/upcoming - Returns recurring transactions due within n days."
  [conn request]
  (let [user-id (:user-id request)
        days (or (common/safe-parse-int (get-in request [:params :days])) 7)
        items (db/load-recurring-for-user conn user-id)
        upcoming (recurring/upcoming items days)]
    (log/info "Retrieved" (count upcoming) "upcoming recurring transactions for user" user-id "within" days "days")
    (common/json-response {:upcoming upcoming
                           :count (count upcoming)})))

(defn pay-now
  "POST /api/recurring/:id/pay - Mark payment as paid (creates transaction and advances date)."
  [conn id request]
  (let [user-id (:user-id request)
        uuid (common/safe-parse-uuid id)]
    (if-not uuid
      (common/error-response "Invalid recurring transaction ID" 400)
      (if (db/user-owns-recurring? conn user-id uuid)
        (if-let [rec (db/load-recurring-by-id conn uuid)]
          (let [tx (-> (recurring/generate-transaction rec)
                       (assoc :transaction/user-id user-id))
                updated-rec (recurring/advance-next-occurrence rec)]
            (log/info "Paying now for recurring transaction" uuid "for user" user-id)
            (db/save-transaction-for-user! conn user-id tx)
            (db/update-recurring! conn uuid
                                  {:recurring/next-occurrence (:recurring/next-occurrence updated-rec)
                                   :recurring/last-generated (:recurring/last-generated updated-rec)})
            (common/json-response {:transaction tx
                                   :recurring (db/load-recurring-by-id conn uuid)}))
          (common/error-response "Recurring transaction not found" 404))
        (do
          (log/warn "User" user-id "attempted to pay recurring transaction" uuid "they don't own")
          (common/error-response "Recurring transaction not found" 404))))))

(defn skip-payment
  "POST /api/recurring/:id/skip - Skip this occurrence (advances date without creating transaction)."
  [conn id request]
  (let [user-id (:user-id request)
        uuid (common/safe-parse-uuid id)]
    (if-not uuid
      (common/error-response "Invalid recurring transaction ID" 400)
      (if (db/user-owns-recurring? conn user-id uuid)
        (if-let [rec (db/load-recurring-by-id conn uuid)]
          (let [updated-rec (recurring/advance-next-occurrence rec)]
            (log/info "Skipping payment for recurring transaction" uuid "for user" user-id)
            (if-let [saved (db/update-recurring! conn uuid
                                                 {:recurring/next-occurrence (:recurring/next-occurrence updated-rec)})]
              (common/json-response saved)
              (common/error-response "Update failed" 500)))
          (common/error-response "Recurring transaction not found" 404))
        (do
          (log/warn "User" user-id "attempted to skip recurring transaction" uuid "they don't own")
          (common/error-response "Recurring transaction not found" 404))))))
