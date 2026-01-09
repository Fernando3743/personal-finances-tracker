(ns finance.api.routes
  "API route definitions using Compojure."
  (:require [compojure.core :refer [GET POST PUT DELETE context routes wrap-routes]]
            [compojure.route :as route]
            [ring.util.response :as response]
            [clojure.string :as str]
            [finance.api.handlers :as handlers]
            [finance.api.auth-handlers :as auth]
            [finance.auth.middleware :refer [wrap-auth-required]]))

(defn auth-routes
  "Public authentication routes (no auth required)."
  [conn]
  (context "/api/auth" []
    (POST "/register" request
      (auth/register conn request))
    (POST "/login" request
      (auth/login conn request))
    (POST "/logout" request
      (auth/logout conn request))
    (GET "/me" request
      (auth/me conn request))))

(defn protected-api-routes
  "Protected API routes (require authentication)."
  [conn]
  (wrap-routes
    (context "/api" []
      (GET "/transactions" request
        (handlers/list-transactions conn request))

      (POST "/transactions" request
        (handlers/create-transaction conn request))

      (GET "/transactions/:id" [id :as request]
        (handlers/get-transaction conn id request))

      (PUT "/transactions/:id" [id :as request]
        (handlers/update-transaction conn id request))

      (DELETE "/transactions/:id" [id :as request]
        (handlers/delete-transaction conn id request))

      (GET "/summary" request
        (handlers/get-summary conn request))

      (GET "/summary/categories" request
        (handlers/get-category-breakdown conn request))

      (GET "/summary/monthly" request
        (handlers/get-monthly-report conn request))

      (GET "/dashboard" request
        (handlers/get-dashboard conn request)))
    wrap-auth-required))

(defn- spa-fallback
  "Fallback handler for SPA routes. Serves index.html for non-API routes."
  [request]
  (if (str/starts-with? (:uri request) "/api")
    {:status 404
     :headers {"Content-Type" "application/json"}
     :body {:error "Not found"}}
    (-> (response/resource-response "index.html" {:root "public"})
        (response/content-type "text/html"))))

(defn app-routes
  "Creates the full application routes."
  [conn]
  (routes
   (auth-routes conn)           ; Public routes (must come first)
   (protected-api-routes conn)  ; Protected routes
   (route/resources "/")
   spa-fallback))
