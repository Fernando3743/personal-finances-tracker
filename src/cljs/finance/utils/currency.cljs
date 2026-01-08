(ns finance.utils.currency
  "Currency formatting utilities.")

;; Currency configuration
(def available-currencies [:COP :USD])

(def currency-config
  {:COP {:code "COP"
         :prefix "$"
         :locale "es-CO"
         :decimal-places 0}
   :USD {:code "USD"
         :prefix "US$"
         :locale "en-US"
         :decimal-places 2}})

(defn currency-symbol
  "Returns the symbol/prefix for a currency."
  [currency]
  (get-in currency-config [(or currency :COP) :prefix] "$"))

(defn format-currency
  "Formats a number as currency based on currency code.
   currency: :COP or :USD (defaults to :COP)
   opts: {:show-sign? boolean}"
  ([amount]
   (format-currency amount :COP {}))
  ([amount currency]
   (format-currency amount currency {}))
  ([amount currency {:keys [show-sign?] :or {show-sign? false}}]
   (let [config (get currency-config (or currency :COP) (get currency-config :COP))
         {:keys [prefix locale decimal-places]} config
         abs-val (js/Math.abs (or amount 0))
         formatted (.toLocaleString abs-val locale
                                    #js {:minimumFractionDigits decimal-places
                                         :maximumFractionDigits decimal-places})
         with-prefix (str prefix formatted)]
     (cond
       (and show-sign? (pos? amount)) (str "+" with-prefix)
       (neg? amount) (str "-" with-prefix)
       :else with-prefix))))

(defn currency-name
  "Returns the display name for a currency."
  [currency]
  (case currency
    :COP "Colombian Peso"
    :USD "US Dollar"
    "Unknown"))
