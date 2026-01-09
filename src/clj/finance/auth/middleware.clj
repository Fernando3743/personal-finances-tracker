(ns finance.auth.middleware
  "Authentication middleware for Ring."
  (:require [ring.util.response :as response]))

(defn wrap-auth-required
  "Middleware that requires authentication.
   Returns 401 if :user-id is not in session."
  [handler]
  (fn [request]
    (if-let [user-id (get-in request [:session :user-id])]
      (handler (assoc request :user-id user-id))
      (-> (response/response {:error "Unauthorized"})
          (response/status 401)
          (response/content-type "application/json")))))

(defn wrap-current-user
  "Adds :user-id to request from session (nil if not logged in).
   Does NOT enforce authentication - use wrap-auth-required for that."
  [handler]
  (fn [request]
    (let [user-id (get-in request [:session :user-id])]
      (handler (assoc request :user-id user-id)))))
