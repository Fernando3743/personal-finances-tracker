(ns finance.api.auth-handlers
  "Authentication request handlers."
  (:require [finance.domain.user :as user]
            [finance.storage.datomic :as db]
            [finance.api.common :as common]
            [clojure.tools.logging :as log]))

(defn register
  "POST /api/auth/register - Creates a new user."
  [conn request]
  (let [{:keys [email password name]} (:body request)]
    (cond
      (not (and email password name))
      (common/error-response "Missing required fields: email, password, name" 400)

      (not (user/valid-email? email))
      (common/error-response "Invalid email format" 400)

      (not (user/valid-password? password))
      (common/error-response "Password must be at least 8 characters" 400)

      (db/email-exists? conn email)
      (do
        (log/warn "Registration attempt with existing email:" email)
        (common/error-response "Email already registered" 409))

      :else
      (let [new-user (user/create-user email password name)
            _ (db/save-user! conn new-user)]
        (log/info "New user registered:" email)
        (-> (common/json-response {:user (user/sanitize-user new-user)} 201)
            (assoc :session {:user-id (:user/id new-user)}))))))

(defn login
  "POST /api/auth/login - Authenticates user and creates session."
  [conn request]
  (let [{:keys [email password]} (:body request)]
    (if-let [db-user (db/find-user-by-email conn email)]
      (if (user/verify-password password (:user/password-hash db-user))
        (do
          (log/info "User logged in:" email)
          (-> (common/json-response {:user (user/sanitize-user db-user)})
              (assoc :session {:user-id (:user/id db-user)})))
        (do
          (log/warn "Failed login attempt for:" email)
          (common/error-response "Invalid credentials" 401)))
      (do
        (log/warn "Login attempt with non-existent email:" email)
        (common/error-response "Invalid credentials" 401)))))

(defn logout
  "POST /api/auth/logout - Destroys session."
  [_conn request]
  (when-let [user-id (:user-id request)]
    (log/info "User logged out:" user-id))
  (-> (common/json-response {:message "Logged out"})
      (assoc :session nil)))

(defn me
  "GET /api/auth/me - Returns current user info."
  [conn request]
  (if-let [user-id (:user-id request)]
    (if-let [db-user (db/find-user-by-id conn user-id)]
      (common/json-response {:user (user/sanitize-user db-user)})
      (common/error-response "User not found" 404))
    (common/error-response "Not authenticated" 401)))
