(ns finance.api.common
  "Common utilities for API handlers."
  (:require [ring.util.response :as response]
            [clojure.tools.logging :as log]))

(defn json-response
  "Creates a JSON response with the given body and status."
  ([body] (json-response body 200))
  ([body status]
   (-> (response/response body)
       (response/status status)
       (response/content-type "application/json"))))

(defn error-response
  "Creates an error response with logging."
  ([message status] (error-response message status nil))
  ([message status ex]
   (when ex
     (log/error ex message))
   (json-response {:error message} status)))

(defn safe-parse-uuid
  "Safely parses a UUID string, returning nil on failure."
  [s]
  (when s
    (try
      (parse-uuid s)
      (catch IllegalArgumentException e
        (log/debug e (str "Failed to parse UUID: " s))
        nil))))

(defn safe-parse-int
  "Safely parses an integer string, returning nil on failure."
  [s]
  (when s
    (try
      (Integer/parseInt s)
      (catch NumberFormatException e
        (log/debug e (str "Failed to parse integer: " s))
        nil))))

(defn safe-parse-bigdec
  "Safely parses a bigdecimal string, returning nil on failure."
  [s]
  (when s
    (try
      (bigdec s)
      (catch Exception e
        (log/debug e (str "Failed to parse bigdec: " s))
        nil))))

(defn validate-month
  "Validates that month is between 1 and 12."
  [month]
  (and month (>= month 1) (<= month 12)))

(defn validate-year
  "Validates that year is reasonable (1900-2100)."
  [year]
  (and year (>= year 1900) (<= year 2100)))

(defn with-owned-resource
  "Higher-order function for checking resource ownership.
   Takes ownership check fn, load fn, id, user-id, and success handler.
   Returns appropriate error response if not owned or not found."
  [check-fn load-fn id user-id success-fn]
  (if (check-fn user-id id)
    (if-let [resource (load-fn id)]
      (success-fn resource)
      (error-response "Resource not found" 404))
    (error-response "Resource not found" 404)))

(defn convert-updates
  "Converts request body updates to namespaced keywords with proper type coercion.
   Takes a namespace (e.g., 'transaction') and a map of updates."
  [ns-name updates]
  (reduce-kv
   (fn [m k v]
     (let [key-name (name k)
           new-key (keyword ns-name key-name)]
       (assoc m new-key
              (case key-name
                "type" (keyword v)
                "category" (keyword v)
                "currency" (keyword v)
                "frequency" (keyword v)
                "tags" (when v (set (map keyword v)))
                "amount" (when v (safe-parse-bigdec (str v)))
                "exchange-rate" (when v (safe-parse-bigdec (str v)))
                "alert-threshold" (when v (safe-parse-bigdec (str v)))
                "active" (boolean v)
                v))))
   {}
   updates))
