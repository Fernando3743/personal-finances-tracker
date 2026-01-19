(ns finance.utils.date
  "Date formatting utilities.")

(defn format-date
  "Formats a date string or Date object to a readable format.
   Returns format like 'Dec 25, 2023'.

   Parameters:
   - date: String (ISO date) or js/Date object

   Returns: Formatted date string or nil if invalid"
  [date]
  (when date
    (try
      (let [d (if (string? date) (js/Date. date) date)]
        (.toLocaleDateString d "en-US"
                            #js {:year "numeric"
                                 :month "short"
                                 :day "numeric"}))
      (catch js/Error _e nil))))

(defn format-date-short
  "Formats a date string to a short format.
   Returns format like 'Dec 25'.

   Parameters:
   - date: String (ISO date) or js/Date object

   Returns: Short formatted date string or nil if invalid"
  [date]
  (when date
    (try
      (let [d (if (string? date) (js/Date. date) date)]
        (.toLocaleDateString d "en-US"
                            #js {:month "short"
                                 :day "numeric"}))
      (catch js/Error _e nil))))

(defn format-month-year
  "Formats a date to month and year only.
   Returns format like 'December 2023'.

   Parameters:
   - date: String (ISO date) or js/Date object

   Returns: Month-year formatted string or nil if invalid"
  [date]
  (when date
    (try
      (let [d (if (string? date) (js/Date. date) date)]
        (.toLocaleDateString d "en-US"
                            #js {:year "numeric"
                                 :month "long"}))
      (catch js/Error _e nil))))

(defn format-day
  "Formats a date to day only.
   Returns format like '25'.

   Parameters:
   - date: String (ISO date) or js/Date object

   Returns: Day as string or nil if invalid"
  [date]
  (when date
    (try
      (let [d (if (string? date) (js/Date. date) date)]
        (.toLocaleDateString d "en-US"
                            #js {:day "numeric"}))
      (catch js/Error _e nil))))

(defn today-iso
  "Returns today's date in ISO format (YYYY-MM-DD).

   Returns: String in ISO date format"
  []
  (let [today (js/Date.)
        iso-string (.toISOString today)]
    (subs iso-string 0 10)))
