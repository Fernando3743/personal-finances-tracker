(ns finance.api.wallet-handlers
  "API handlers for wallets/accounts."
  (:require [finance.domain.wallet :as wallet]
            [finance.storage.datomic :as db]
            [finance.api.common :as common]
            [clojure.tools.logging :as log]))

(defn list-wallets
  "GET /api/wallets - Returns current user's wallets.
   Optional query param: type (bank, card, crypto, investment)"
  [conn request]
  (let [user-id (:user-id request)
        type-filter (get-in request [:params :type])]
    (let [wallets (if type-filter
                    (db/load-wallets-by-type conn user-id (keyword type-filter))
                    (db/load-wallets-for-user conn user-id))]
      (log/info "Listed wallets for user" user-id "type" type-filter "count" (count wallets))
      (common/json-response {:wallets wallets
                             :count (count wallets)}))))

(defn get-wallet
  "GET /api/wallets/:id - Returns a single wallet if owned by user."
  [conn id request]
  (let [user-id (:user-id request)
        wallet-id (common/safe-parse-uuid id)]
    (if-not wallet-id
      (common/error-response "Invalid wallet ID format" 400)
      (if (db/user-owns-wallet? conn user-id wallet-id)
        (if-let [item (db/load-wallet-by-id conn wallet-id)]
          (do
            (log/info "Retrieved wallet" wallet-id "for user" user-id)
            (common/json-response item))
          (common/error-response "Wallet not found" 404))
        (do
          (log/warn "Unauthorized wallet access attempt: user" user-id "wallet" wallet-id)
          (common/error-response "Wallet not found" 404))))))

(defn create-wallet
  "POST /api/wallets - Creates a new wallet for current user."
  [conn request]
  (let [user-id (:user-id request)
        {:keys [name type institution account-number balance currency status color icon-url balance-label]} (:body request)]
    (if (and name type institution)
      (let [w (wallet/create-wallet
               name
               type
               institution
               {:account-number account-number
                :balance (when balance (bigdec balance))
                :currency (when currency (keyword currency))
                :status (when status (keyword status))
                :color color
                :icon-url icon-url
                :balance-label balance-label})
            w-with-user (assoc w :wallet/user-id user-id)]
        (if (wallet/valid? w-with-user)
          (do
            (db/save-wallet! conn w-with-user)
            (log/info "Created wallet" (:wallet/id w-with-user) "for user" user-id
                      "type" type "institution" institution)
            (common/json-response w-with-user 201))
          (do
            (log/warn "Invalid wallet creation attempt:" (wallet/explain-invalid w-with-user))
            (common/error-response (wallet/explain-invalid w-with-user) 400))))
      (common/error-response "Missing required fields: name, type, institution" 400))))

(defn update-wallet
  "PUT /api/wallets/:id - Updates a wallet if owned by user."
  [conn id request]
  (let [user-id (:user-id request)
        uuid (common/safe-parse-uuid id)
        updates (:body request)]
    (if-not uuid
      (common/error-response "Invalid wallet ID format" 400)
      (if (db/user-owns-wallet? conn user-id uuid)
        (if-let [_existing (db/load-wallet-by-id conn uuid)]
          (let [converted-updates (common/convert-updates "wallet" updates)]
            (if-let [updated (db/update-wallet! conn uuid converted-updates)]
              (do
                (log/info "Updated wallet" uuid "for user" user-id)
                (common/json-response updated))
              (do
                (log/error "Failed to update wallet" uuid)
                (common/error-response "Update failed" 500))))
          (common/error-response "Wallet not found" 404))
        (do
          (log/warn "Unauthorized wallet update attempt: user" user-id "wallet" uuid)
          (common/error-response "Wallet not found" 404))))))

(defn delete-wallet
  "DELETE /api/wallets/:id - Deletes a wallet if owned by user."
  [conn id request]
  (let [user-id (:user-id request)
        uuid (common/safe-parse-uuid id)]
    (if-not uuid
      (common/error-response "Invalid wallet ID format" 400)
      (if (db/user-owns-wallet? conn user-id uuid)
        (if (db/delete-wallet! conn uuid)
          (do
            (log/info "Deleted wallet" uuid "for user" user-id)
            (common/json-response {:deleted true}))
          (common/error-response "Wallet not found" 404))
        (do
          (log/warn "Unauthorized wallet deletion attempt: user" user-id "wallet" uuid)
          (common/error-response "Wallet not found" 404))))))

(defn get-wallet-summary
  "GET /api/wallets/summary - Returns aggregated wallet summary for user."
  [conn request]
  (let [user-id (:user-id request)]
    (let [summary (db/get-wallet-summary conn user-id)]
      (log/info "Retrieved wallet summary for user" user-id)
      (common/json-response summary))))

(defn sync-wallet
  "POST /api/wallets/:id/sync - Triggers a sync for a wallet.
   For now, this just updates the last-updated timestamp and sets status to synced."
  [conn id request]
  (let [user-id (:user-id request)
        uuid (common/safe-parse-uuid id)]
    (if-not uuid
      (common/error-response "Invalid wallet ID format" 400)
      (if (db/user-owns-wallet? conn user-id uuid)
        (if-let [_existing (db/load-wallet-by-id conn uuid)]
          (if-let [updated (db/update-wallet! conn uuid {:wallet/status :synced})]
            (do
              (log/info "Synced wallet" uuid "for user" user-id)
              (common/json-response updated))
            (common/error-response "Sync failed" 500))
          (common/error-response "Wallet not found" 404))
        (do
          (log/warn "Unauthorized wallet sync attempt: user" user-id "wallet" uuid)
          (common/error-response "Wallet not found" 404))))))

(defn refresh-all-wallets
  "POST /api/wallets/refresh - Triggers a refresh for all user's wallets."
  [conn request]
  (let [user-id (:user-id request)
        wallets (db/load-wallets-for-user conn user-id)]
    (doseq [wallet wallets]
      (db/update-wallet! conn (:wallet/id wallet) {:wallet/status :synced}))
    (log/info "Refreshed all wallets for user" user-id "count" (count wallets))
    (common/json-response {:refreshed (count wallets)})))
