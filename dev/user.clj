(ns user
  "REPL development utilities.
   Auto-loaded when starting nREPL with :nrepl alias."
  (:require [finance.core :as core]))

(defonce ^:private server (atom nil))

(defn start
  "Start the server on port 3000 (or specified port)."
  ([] (start 3000))
  ([port]
   (if @server
     (println "Server already running. Use (restart) to restart.")
     (do
       (reset! server (core/start-server port))
       (println "Server started.")))))

(defn stop
  "Stop the running server."
  []
  (if-let [s @server]
    (do
      (.stop s)
      (reset! server nil)
      (println "Server stopped."))
    (println "No server running.")))

(defn restart
  "Restart the server."
  ([] (restart 3000))
  ([port]
   (stop)
   (start port)))

(comment
  ;; Usage:
  (start)      ; Start on port 3000
  (start 8080) ; Start on custom port
  (stop)       ; Stop the server
  (restart)    ; Restart on port 3000
  )
