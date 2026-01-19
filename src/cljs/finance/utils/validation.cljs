(ns finance.utils.validation
  "Input validation utilities for forms and user data."
  (:require [clojure.string :as str]))

(defn valid-email?
  "Validates email format using a reasonable regex pattern.
   Returns true if email is valid, false otherwise."
  [email]
  (when email
    (let [email-pattern #"^[a-zA-Z0-9._%+-]+@[a-zA-Z0-9.-]+\.[a-zA-Z]{2,}$"]
      (some? (re-matches email-pattern (str/trim email))))))

(defn valid-url?
  "Validates URL format and checks for safe protocols.
   Rejects javascript:, data:, and other potentially dangerous protocols.
   Returns true if URL is safe, false otherwise."
  [url]
  (when url
    (let [url-str (str/lower-case (str/trim url))
          safe-protocols #"^(https?|//).*"]
      (and (not (str/starts-with? url-str "javascript:"))
           (not (str/starts-with? url-str "data:"))
           (not (str/starts-with? url-str "vbscript:"))
           (or (re-matches safe-protocols url-str)
               (str/starts-with? url-str "/"))))))

(defn sanitize-url
  "Sanitizes a URL, returning nil if invalid or unsafe.
   Use this before rendering user-supplied URLs."
  [url]
  (when (valid-url? url)
    url))

(defn valid-amount?
  "Validates that a string represents a valid positive decimal number.
   Returns true if valid, false otherwise."
  [amount-str]
  (when amount-str
    (let [cleaned (str/trim (str amount-str))
          parsed (js/parseFloat cleaned)]
      (and (not (str/blank? cleaned))
           (re-matches #"^\d+(\.\d+)?$" cleaned)
           (not (js/isNaN parsed))
           (pos? parsed)))))

(defn clean-amount
  "Cleans an amount string, removing invalid characters.
   Ensures only one decimal point is allowed.
   Returns cleaned string or empty string if invalid."
  [amount-str]
  (when amount-str
    (let [str-val (str amount-str)
          ;; Remove all non-digit and non-decimal characters
          cleaned (str/replace str-val #"[^\d.]" "")
          ;; Split by decimal point
          parts (str/split cleaned #"\.")
          ;; Keep only first decimal point
          result (if (> (count parts) 1)
                   (str (first parts) "." (str/join (rest parts)))
                   cleaned)]
      (if (str/blank? result) "" result))))

(defn valid-password?
  "Validates password meets minimum security requirements.
   Requirements: at least 8 characters, 1 uppercase, 1 lowercase, 1 number.
   Returns {:valid? boolean :message string :checks map}."
  [password]
  (let [checks {:length (>= (count (or password "")) 8)
                :uppercase (boolean (re-find #"[A-Z]" (or password "")))
                :lowercase (boolean (re-find #"[a-z]" (or password "")))
                :number (boolean (re-find #"[0-9]" (or password "")))}
        all-valid? (every? true? (vals checks))]
    (cond
      (str/blank? password)
      {:valid? false :message "Password is required" :checks checks}

      (not (:length checks))
      {:valid? false :message "Password must be at least 8 characters" :checks checks}

      (not (:uppercase checks))
      {:valid? false :message "Password must contain an uppercase letter" :checks checks}

      (not (:lowercase checks))
      {:valid? false :message "Password must contain a lowercase letter" :checks checks}

      (not (:number checks))
      {:valid? false :message "Password must contain a number" :checks checks}

      :else
      {:valid? true :message nil :checks checks})))

(defn passwords-match?
  "Validates that password and confirmation match.
   Returns true if they match, false otherwise."
  [password confirm-password]
  (and (not (str/blank? password))
       (not (str/blank? confirm-password))
       (= password confirm-password)))

(defn valid-theme?
  "Validates theme keyword is one of the allowed values.
   Returns the theme if valid, :light as fallback."
  [theme]
  (if (contains? #{:light :dark} theme)
    theme
    :light))

(defn sanitize-theme-string
  "Sanitizes a theme string from localStorage.
   Returns a valid theme keyword or :light as fallback."
  [theme-str]
  (if (and theme-str (string? theme-str))
    (let [theme-kw (keyword (str/trim theme-str))]
      (valid-theme? theme-kw))
    :light))
