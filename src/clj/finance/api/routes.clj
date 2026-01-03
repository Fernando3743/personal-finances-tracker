(ns finance.api.routes
  "API route definitions using Compojure."
  (:require [compojure.core :refer [defroutes GET POST PUT DELETE context]]
            [compojure.route :as route]
            [ring.util.response :as response]
            [finance.api.handlers :as handlers]))

(defn api-routes
  "Creates API routes with the given Datomic connection."
  [conn]
  (context "/api" []
    ;; Transactions CRUD
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

    ;; Summary & Reports
    (GET "/summary" request
      (handlers/get-summary conn request))

    (GET "/summary/categories" request
      (handlers/get-category-breakdown conn request))

    (GET "/summary/monthly" request
      (handlers/get-monthly-report conn request))

    (GET "/dashboard" request
      (handlers/get-dashboard conn request))))

(defn app-routes
  "Creates the full application routes."
  [conn]
  (compojure.core/routes
   (api-routes conn)
   (GET "/" [] (-> (response/resource-response "index.html" {:root "public"})
                    (response/content-type "text/html")))
   (route/resources "/")
   (route/not-found {:error "Not found"})))
