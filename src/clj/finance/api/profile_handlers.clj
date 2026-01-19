(ns finance.api.profile-handlers
  "Profile management request handlers."
  (:require [finance.domain.user :as user]
            [finance.storage.datomic :as db]
            [ring.util.response :as response]
            [cheshire.core :as json]
            [clojure.string :as str]
            [finance.api.common :as common]
            [clojure.tools.logging :as log]))

(defn get-profile
  "GET /api/profile - Returns full profile with statistics."
  [conn request]
  (let [user-id (:user-id request)
        db-user (db/find-user-by-id conn user-id)
        stats (db/get-user-statistics conn user-id)]
    (if db-user
      (common/json-response {:user (user/sanitize-user db-user)
                             :statistics stats})
      (common/error-response "User not found" 404))))

(defn update-profile
  "PUT /api/profile - Updates name and/or email."
  [conn request]
  (let [user-id (:user-id request)
        {:keys [name email]} (:body request)
        db-user (db/find-user-by-id conn user-id)]
    (cond
      (not db-user)
      (common/error-response "User not found" 404)

      (and name (str/blank? name))
      (common/error-response "Name cannot be empty" 400)

      (and email (not (user/valid-email? email)))
      (common/error-response "Invalid email format" 400)

      (and email
           (not= (str/lower-case email) (:user/email db-user))
           (db/email-exists? conn email))
      (common/error-response "Email already in use" 409)

      :else
      (let [updates (cond-> {}
                      name (assoc :user/name name)
                      email (assoc :user/email (str/lower-case email)))
            updated-user (db/update-user! conn user-id updates)]
        (if updated-user
          (do
            (log/info "Profile updated successfully for user" user-id "- updates:" updates)
            (common/json-response {:user (user/sanitize-user updated-user)}))
          (do
            (log/error "Profile update failed for user" user-id)
            (common/error-response "Update failed" 500)))))))

(defn change-password
  "PUT /api/profile/password - Changes password."
  [conn request]
  (let [user-id (:user-id request)
        {:keys [current_password new_password]} (:body request)
        db-user (db/find-user-by-id conn user-id)]
    (cond
      (not db-user)
      (common/error-response "User not found" 404)

      (not (and current_password new_password))
      (common/error-response "Both current_password and new_password are required" 400)

      (not (user/verify-password current_password (:user/password-hash db-user)))
      (do
        (log/warn "Failed password change attempt for user" user-id "- incorrect current password")
        (common/error-response "Current password is incorrect" 401))

      (not (user/valid-password? new_password))
      (common/error-response "New password must be at least 8 characters" 400)

      :else
      (let [new-hash (user/hash-password new_password)
            updated-user (db/update-user! conn user-id {:user/password-hash new-hash})]
        (if updated-user
          (do
            (log/info "Password changed successfully for user" user-id)
            (common/json-response {:message "Password changed successfully"}))
          (do
            (log/error "Password change failed for user" user-id)
            (common/error-response "Password change failed" 500)))))))

(defn update-preferences
  "PUT /api/profile/preferences - Updates currency/theme preferences."
  [conn request]
  (let [user-id (:user-id request)
        {:keys [currency]} (:body request)]
    (cond
      (and currency (not (#{:COP :USD "COP" "USD"} currency)))
      (common/error-response "Invalid currency. Must be COP or USD" 400)

      :else
      (let [updates (cond-> {}
                      currency (assoc :user/preferred-currency (keyword currency)))
            updated-user (db/update-user! conn user-id updates)]
        (if updated-user
          (do
            (log/info "Preferences updated successfully for user" user-id "- updates:" updates)
            (common/json-response {:user (user/sanitize-user updated-user)}))
          (do
            (log/error "Preferences update failed for user" user-id)
            (common/error-response "Update failed" 500)))))))

(defn export-data
  "GET /api/profile/export - Exports all user data as JSON."
  [conn request]
  (let [user-id (:user-id request)
        db-user (db/find-user-by-id conn user-id)
        transactions (db/load-transactions-for-user conn user-id)
        export-data {:user (user/sanitize-user db-user)
                     :transactions transactions
                     :exported_at (java.util.Date.)}]
    (-> (response/response (json/generate-string export-data {:pretty true}))
        (response/status 200)
        (response/content-type "application/json")
        (response/header "Content-Disposition"
                         (str "attachment; filename=\"finance-export-"
                              (.format (java.text.SimpleDateFormat. "yyyy-MM-dd") (java.util.Date.))
                              ".json\"")))))

(defn delete-account
  "DELETE /api/profile - Deletes account after password confirmation."
  [conn request]
  (let [user-id (:user-id request)
        {:keys [password]} (:body request)
        db-user (db/find-user-by-id conn user-id)]
    (cond
      (not db-user)
      (common/error-response "User not found" 404)

      (not password)
      (common/error-response "Password required to delete account" 400)

      (not (user/verify-password password (:user/password-hash db-user)))
      (do
        (log/warn "Failed account deletion attempt for user" user-id "- incorrect password")
        (common/error-response "Incorrect password" 401))

      :else
      (do
        (db/delete-user! conn user-id)
        (log/info "Account deleted for user" user-id)
        (-> (common/json-response {:message "Account deleted"})
            (assoc :session nil))))))