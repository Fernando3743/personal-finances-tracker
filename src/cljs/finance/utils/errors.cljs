(ns finance.utils.errors
  "Error handling utilities for API responses.")

(defn extract-error-message
  "Extracts error message from an API error response.
   Attempts to get error from [:response :error] path,
   falls back to provided default message.

   Parameters:
   - error: The error response object
   - default-msg: Default message if error extraction fails

   Returns: String error message"
  [error default-msg]
  (or (get-in error [:response :error])
      default-msg))

(defn log-error
  "Logs an error to the console for debugging.
   Only logs in development mode to avoid exposing details in production.

   Parameters:
   - context: String describing where the error occurred
   - error: The error object to log"
  [context error]
  (when ^boolean goog.DEBUG
    (.error js/console (str "[" context "]") (clj->js error))))
