(ns finance.config
  "Application configuration management.
   Loads configuration from .env file in project root."
  (:require [clojure.java.io :as io]
            [clojure.string :as str]))

(defn- parse-env-line
  "Parses a single line from .env file. Returns [key value] or nil.
   Empty values are stored as empty strings."
  [line]
  (when-let [trimmed (some-> line str/trim)]
    (when (and (seq trimmed)
               (not (str/starts-with? trimmed "#")))
      (let [[k v] (str/split trimmed #"=" 2)]
        (when k
          [(str/trim k) (if v (str/trim v) "")])))))

(defn- load-env-file
  "Loads .env file from project root. Returns map of key-value pairs."
  []
  (let [env-file (io/file ".env")]
    (if (.exists env-file)
      (->> (slurp env-file)
           str/split-lines
           (keep parse-env-line)
           (into {}))
      {})))

(def ^:private env-vars
  "Cached environment variables from .env file merged with system env."
  (delay (merge (load-env-file) (System/getenv))))

(defn get-env
  "Gets a required config value from .env file or system environment.
   Throws if key is missing."
  [key]
  (if-let [value (get @env-vars key)]
    value
    (throw (ex-info (str "Missing required environment variable: " key)
                    {:key key}))))

(defn get-env-optional
  "Gets a config value, returning nil if not found."
  [key]
  (get @env-vars key))

(defn- not-blank
  "Returns nil if string is blank, otherwise the string."
  [s]
  (when (and s (seq (str/trim s)))
    s))

(defn redis-config
  "Returns Redis connection configuration from .env file."
  []
  {:pool {}
   :spec {:host (get-env "REDIS_HOST")
          :port (Integer/parseInt (get-env "REDIS_PORT"))
          :password (not-blank (get-env-optional "REDIS_PASSWORD"))
          :timeout-ms (Integer/parseInt (get-env "REDIS_TIMEOUT"))}})

(def session-ttl-seconds
  "Session TTL in seconds (14 days, with sliding expiration)."
  1209600)
