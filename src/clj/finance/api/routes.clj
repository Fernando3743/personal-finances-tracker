(ns finance.api.routes
  "API route definitions using Compojure."
  (:require [compojure.core :refer [GET POST PUT DELETE context routes wrap-routes]]
            [compojure.route :as route]
            [ring.util.response :as response]
            [clojure.string :as str]
            [finance.api.handlers :as handlers]
            [finance.api.auth-handlers :as auth]
            [finance.api.profile-handlers :as profile]
            [finance.api.recurring-handlers :as recurring]
            [finance.api.budget-handlers :as budget]
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
        (handlers/get-dashboard conn request))

      (context "/recurring" []
        (GET "/" request
          (recurring/list-recurring conn request))

        (POST "/" request
          (recurring/create-recurring conn request))

        (GET "/upcoming" request
          (recurring/upcoming-recurring conn request))

        (POST "/generate" request
          (recurring/generate-transactions conn request))

        (GET "/:id" [id :as request]
          (recurring/get-recurring conn id request))

        (PUT "/:id" [id :as request]
          (recurring/update-recurring conn id request))

        (DELETE "/:id" [id :as request]
          (recurring/delete-recurring conn id request))

        (POST "/:id/toggle" [id :as request]
          (recurring/toggle-active conn id request)))

      (context "/budgets" []
        (GET "/" request
          (budget/list-budgets conn request))

        (POST "/" request
          (budget/create-budget conn request))

        (GET "/status" request
          (budget/get-budget-status conn request))

        (POST "/copy" request
          (budget/copy-budgets-to-month conn request))

        (GET "/:id" [id :as request]
          (budget/get-budget conn id request))

        (PUT "/:id" [id :as request]
          (budget/update-budget conn id request))

        (DELETE "/:id" [id :as request]
          (budget/delete-budget conn id request)))

      (context "/profile" []
        (GET "/" request
          (profile/get-profile conn request))

        (PUT "/" request
          (profile/update-profile conn request))

        (PUT "/password" request
          (profile/change-password conn request))

        (PUT "/preferences" request
          (profile/update-preferences conn request))

        (GET "/export" request
          (profile/export-data conn request))

        (DELETE "/" request
          (profile/delete-account conn request))))
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
