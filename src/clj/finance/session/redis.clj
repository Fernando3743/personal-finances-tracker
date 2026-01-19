(ns finance.session.redis
  "Redis-backed session store using Carmine."
  (:require [taoensso.carmine :as car]
            [taoensso.carmine.ring :refer [carmine-store]]
            [finance.config :as config]
            [clojure.tools.logging :as log]))

(defn- redis-available?
  "Checks if Redis is available by attempting a PING command."
  []
  (try
    (let [conn (config/redis-config)]
      (= "PONG" (car/wcar conn (car/ping))))
    (catch Exception e
      (log/error e "Redis connection failed")
      false)))

(defn create-session-store
  "Creates Redis-backed session store. Throws if Redis unavailable."
  []
  (if (redis-available?)
    (do
      (log/info "Session store initialized: Redis")
      (carmine-store (config/redis-config)
                     {:key-prefix "finance:session:"
                      :expiration-secs config/session-ttl-seconds}))
    (throw (ex-info "Redis unavailable - cannot start server" {}))))
