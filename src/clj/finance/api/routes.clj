(ns finance.api.routes
  "API route definitions using Compojure."
  (:require [compojure.core :refer [defroutes GET POST PUT DELETE context]]
            [compojure.route :as route]
            [ring.util.response :as response]
            [finance.api.handlers :as handlers]))

(defn api-routes
  "Creates API routes with the given store dependency."
  [store]
  (context "/api" []
    ;; Transactions CRUD
    (GET "/transactions" request
      (handlers/list-transactions store request))

    (POST "/transactions" request
      (handlers/create-transaction store request))

    (GET "/transactions/:id" [id :as request]
      (handlers/get-transaction store id request))

    (PUT "/transactions/:id" [id :as request]
      (handlers/update-transaction store id request))

    (DELETE "/transactions/:id" [id :as request]
      (handlers/delete-transaction store id request))

    ;; Summary & Reports
    (GET "/summary" request
      (handlers/get-summary store request))

    (GET "/summary/categories" request
      (handlers/get-category-breakdown store request))

    (GET "/summary/monthly" request
      (handlers/get-monthly-report store request))

    (GET "/dashboard" request
      (handlers/get-dashboard store request))))

(defn app-routes
  "Creates the full application routes."
  [store]
  (compojure.core/routes
   (api-routes store)
   (GET "/" [] (-> (response/resource-response "index.html" {:root "public"})
                    (response/content-type "text/html")))
   (route/resources "/")
   (route/not-found {:error "Not found"})))
