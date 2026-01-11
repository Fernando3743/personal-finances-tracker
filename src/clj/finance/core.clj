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
            [cider.nrepl :refer [cider-nrepl-handler]])
  (:gen-class))

(def ^:private db-uri "datomic:dev://localhost:4334/finance?password=admin")

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
  (let [conn (db/create-conn db-uri)
        app (create-app conn)]
    (println (str "Starting server on http://localhost:" port))
    (println (str "Database: " db-uri))
    (jetty/run-jetty app {:port port :join? false})))

(defn start-nrepl
  "Starts the nREPL server with CIDER middleware."
  [port]
  (let [server (nrepl/start-server :port port :handler cider-nrepl-handler)]
    (nrepl-cmd/save-port-file server {})
    (println (str "nREPL server started on port " port))
    server))

(defn -main
  "Main entry point."
  [& args]
  (let [port (Integer/parseInt (or (first args) "3000"))]
    (start-nrepl 7888)
    (start-server port)
    (println "Server running. Press Ctrl+C to stop.")))

(comment
  ;; Development helpers
  (def server (start-server 3000))
  (.stop server)
  (def conn (db/create-conn db-uri))

  (d/q '[:find (pull ?e [*])
         :where
         [?e :user/id ?id]]
       (d/db conn))


  )
