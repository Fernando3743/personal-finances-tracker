(ns finance.core
  "Application entry point.
   Sets up middleware, storage, and starts the server."
  (:require [ring.adapter.jetty :as jetty]
            [ring.middleware.json :refer [wrap-json-body wrap-json-response]]
            [ring.middleware.cors :refer [wrap-cors]]
            [ring.middleware.resource :refer [wrap-resource]]
            [ring.middleware.content-type :refer [wrap-content-type]]
            [finance.api.routes :as routes]
            [finance.storage.datomic :as db]
            [nrepl.server :as nrepl]
            [nrepl.cmdline :as nrepl-cmd]
            [cider.nrepl :refer [cider-nrepl-handler]])
  (:gen-class))

(def ^:private db-uri "datomic:mem://finance")

(defn create-app
  "Creates the Ring application with all middleware."
  [conn]
  (-> (routes/app-routes conn)
      (wrap-json-body {:keywords? true})
      wrap-json-response
      (wrap-cors :access-control-allow-origin [#".*"]
                 :access-control-allow-methods [:get :post :put :delete :options])
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
  )
