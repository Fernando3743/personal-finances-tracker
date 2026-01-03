(ns finance.core
  "Application entry point.
   Sets up middleware, storage, and starts the server."
  (:require [ring.adapter.jetty :as jetty]
            [ring.middleware.json :refer [wrap-json-body wrap-json-response]]
            [ring.middleware.cors :refer [wrap-cors]]
            [ring.middleware.resource :refer [wrap-resource]]
            [ring.middleware.content-type :refer [wrap-content-type]]
            [finance.api.routes :as routes]
            [finance.storage.edn :as edn-storage])
  (:gen-class))

(def ^:private data-file "data/transactions.edn")

(defn create-app
  "Creates the Ring application with all middleware."
  [store]
  (-> (routes/app-routes store)
      (wrap-json-body {:keywords? true})
      wrap-json-response
      (wrap-cors :access-control-allow-origin [#".*"]
                 :access-control-allow-methods [:get :post :put :delete :options])
      (wrap-resource "public")
      wrap-content-type))

(defn start-server
  "Starts the Jetty server."
  [port]
  (let [store (edn-storage/create-store data-file)
        app (create-app store)]
    (println (str "Starting server on http://localhost:" port))
    (println (str "Data file: " data-file))
    (jetty/run-jetty app {:port port :join? false})))

(defn -main
  "Main entry point."
  [& args]
  (let [port (Integer/parseInt (or (first args) "3000"))]
    (start-server port)
    (println "Server running. Press Ctrl+C to stop.")))

(comment
  ;; Development helpers
  (def server (start-server 3000))
  (.stop server)
  )
