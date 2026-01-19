(ns finance.core
  "Application entry point.
   Sets up middleware, storage, and starts the server."
  (:require [ring.adapter.jetty :as jetty]
            [ring.middleware.json :refer [wrap-json-body wrap-json-response]]
            [ring.middleware.resource :refer [wrap-resource]]
            [ring.middleware.content-type :refer [wrap-content-type]]
            [ring.middleware.session :refer [wrap-session]]
            [ring.middleware.multipart-params :refer [wrap-multipart-params]]
            [finance.session.redis :as redis]
            [finance.config :as config]
            [finance.api.routes :as routes]
            [finance.storage.datomic :as db]
            [datomic.api :as d]
            [finance.auth.middleware :refer [wrap-current-user]]
            [nrepl.server :as nrepl]
            [nrepl.cmdline :as nrepl-cmd]
            [cider.nrepl :refer [cider-nrepl-handler]]
            [clojure.tools.logging :as log])
  (:gen-class))

(defn- get-db-uri
  "Gets database URI from environment variables."
  []
  (config/get-env "DATOMIC_URI"))

(def session-store (redis/create-session-store))

(def ^:private allowed-origins
  #{"http://localhost:8280" "http://localhost:3000"})

(defn- wrap-cors
  "Simple CORS middleware that properly handles credentials."
  [handler]
  (fn [request]
    (let [origin (get-in request [:headers "origin"])]
      (if (and origin (contains? allowed-origins origin))
        (if (= :options (:request-method request))
          {:status 200
           :headers {"Access-Control-Allow-Origin" origin
                     "Access-Control-Allow-Methods" "GET, POST, PUT, DELETE, OPTIONS"
                     "Access-Control-Allow-Headers" "Content-Type, Accept, Authorization"
                     "Access-Control-Allow-Credentials" "true"
                     "Access-Control-Max-Age" "86400"}
           :body ""}
          (let [response (handler request)]
            (-> response
                (assoc-in [:headers "Access-Control-Allow-Origin"] origin)
                (assoc-in [:headers "Access-Control-Allow-Credentials"] "true"))))
        (handler request)))))

(defn create-app
  "Creates the Ring application with all middleware."
  [conn]
  (-> (routes/app-routes conn)
      wrap-current-user
      (wrap-session {:store session-store
                     :cookie-name "finance-session"
                     :cookie-attrs {:http-only true
                                    :same-site :lax
                                    :max-age config/session-ttl-seconds}})
      wrap-multipart-params
      (wrap-json-body {:keywords? true})
      wrap-json-response
      wrap-cors
      (wrap-resource "public")
      wrap-content-type))

(defn start-server
  "Starts the Jetty server."
  [port]
  (let [db-uri (get-db-uri)
        conn (db/create-conn db-uri)
        app (create-app conn)]
    (log/info (str "Starting server on http://localhost:" port))
    (log/info "Database connected")
    (jetty/run-jetty app {:port port :join? false})))

(defn start-nrepl
  "Starts the nREPL server with CIDER middleware (development only)."
  [port]
  (let [server (nrepl/start-server :port port :handler cider-nrepl-handler)]
    (nrepl-cmd/save-port-file server {})
    (log/info (str "nREPL server started on port " port))
    server))

(defn -main
  "Main entry point."
  [& args]
  (let [port (parse-long (or (first args) "3000"))
        env (config/get-env-optional "ENV")]
    (when (= "development" env)
      (log/info "Starting nREPL in development mode")
      (start-nrepl 7888))
    (start-server port)
    (log/info "Server running. Press Ctrl+C to stop.")))

(comment
  ;; Development helpers
  (def server (start-server 3000))
  (.stop server)
  (def conn (db/create-conn (get-db-uri)))

  (d/q '[:find (pull ?e [*])
         :where
         [?e :user/id ?id]]
       (d/db conn))

  ;; Seed recurring test data
  ;; First, require the seed namespace
  (require '[finance.dev.seed :as seed])

  ;; Get a user ID
  (def user-id (seed/get-first-user-id conn))

  ;; Clear existing and seed new recurring transactions
  (seed/clear-recurring! conn user-id)
  (seed/seed-recurring! conn user-id)

  ;; Verify the data
  (db/load-recurring-for-user conn user-id)
  )
