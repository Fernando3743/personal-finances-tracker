(ns finance.api.auth-handlers
  "Authentication request handlers."
  (:require [finance.domain.user :as user]
            [finance.storage.datomic :as db]
            [ring.util.response :as response]))

(defn- json-response
  ([body] (json-response body 200))
  ([body status]
   (-> (response/response body)
       (response/status status)
       (response/content-type "application/json"))))

(defn register
  "POST /api/auth/register - Creates a new user."
  [conn request]
  (let [{:keys [email password name]} (:body request)]
    (cond
      (not (and email password name))
      (json-response {:error "Missing required fields: email, password, name"} 400)

      (not (user/valid-email? email))
      (json-response {:error "Invalid email format"} 400)

      (not (user/valid-password? password))
      (json-response {:error "Password must be at least 8 characters"} 400)

      (db/email-exists? conn email)
      (json-response {:error "Email already registered"} 409)

      :else
      (let [new-user (user/create-user email password name)
            _ (db/save-user! conn new-user)]
        (-> (json-response {:user (user/sanitize-user new-user)} 201)
            (assoc :session {:user-id (:user/id new-user)}))))))

(defn login
  "POST /api/auth/login - Authenticates user and creates session."
  [conn request]
  (let [{:keys [email password]} (:body request)]
    (if-let [db-user (db/find-user-by-email conn email)]
      (if (user/verify-password password (:user/password-hash db-user))
        (-> (json-response {:user (user/sanitize-user db-user)})
            (assoc :session {:user-id (:user/id db-user)}))
        (json-response {:error "Invalid credentials"} 401))
      (json-response {:error "Invalid credentials"} 401))))

(defn logout
  "POST /api/auth/logout - Destroys session."
  [_conn _request]
  (-> (json-response {:message "Logged out"})
      (assoc :session nil)))

(defn me
  "GET /api/auth/me - Returns current user info."
  [conn request]
  (if-let [user-id (:user-id request)]
    (if-let [db-user (db/find-user-by-id conn user-id)]
      (json-response {:user (user/sanitize-user db-user)})
      (json-response {:error "User not found"} 404))
    (json-response {:error "Not authenticated"} 401)))
