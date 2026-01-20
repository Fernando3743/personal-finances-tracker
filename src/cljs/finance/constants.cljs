(ns finance.constants
  "Application-wide constants and configuration values.")

(def toast-duration-ms
  "Default duration for toast notifications in milliseconds."
  5000)

(def recent-transactions-limit
  "Number of recent transactions to display."
  5)

(def recent-recurring-limit
  "Number of recent recurring transactions to display."
  5)

(def default-alert-threshold
  "Default budget alert threshold percentage."
  80)

(def frequency-multipliers
  "Multipliers to calculate monthly amounts from different frequencies.
   Note: Assumes 30 days per month and 4.33 weeks per month on average."
  {:daily 30
   :weekly 4.33
   :biweekly 2.17
   :monthly 1
   :quarterly 0.33
   :yearly 0.083})

(def password-min-length
  "Minimum password length requirement."
  8)

(def category-colors
  "Tailwind color classes for transaction categories.
   Maps category keywords to {:bg :text} color classes for light and dark modes."
  {:food {:bg "bg-orange-100 dark:bg-orange-900/30"
          :text "text-orange-600 dark:text-orange-400"}
   :groceries {:bg "bg-green-100 dark:bg-green-900/30"
               :text "text-green-600 dark:text-green-400"}
   :restaurants {:bg "bg-orange-100 dark:bg-orange-900/30"
                 :text "text-orange-600 dark:text-orange-400"}
   :transportation {:bg "bg-blue-100 dark:bg-blue-900/30"
                    :text "text-blue-600 dark:text-blue-400"}
   :entertainment {:bg "bg-pink-100 dark:bg-pink-900/30"
                   :text "text-pink-600 dark:text-pink-400"}
   :utilities {:bg "bg-yellow-100 dark:bg-yellow-900/30"
               :text "text-yellow-600 dark:text-yellow-400"}
   :healthcare {:bg "bg-red-100 dark:bg-red-900/30"
                :text "text-red-600 dark:text-red-400"}
   :shopping {:bg "bg-purple-100 dark:bg-purple-900/30"
              :text "text-purple-600 dark:text-purple-400"}
   :housing {:bg "bg-violet-100 dark:bg-violet-900/30"
             :text "text-violet-600 dark:text-violet-400"}
   :salary {:bg "bg-emerald-100 dark:bg-emerald-900/30"
            :text "text-emerald-600 dark:text-emerald-400"}
   :freelance {:bg "bg-emerald-100 dark:bg-emerald-900/30"
               :text "text-emerald-600 dark:text-emerald-400"}
   :investments {:bg "bg-emerald-100 dark:bg-emerald-900/30"
                 :text "text-emerald-600 dark:text-emerald-400"}
   :gifts {:bg "bg-pink-100 dark:bg-pink-900/30"
           :text "text-pink-600 dark:text-pink-400"}
   :other {:bg "bg-gray-100 dark:bg-gray-800"
           :text "text-gray-600 dark:text-gray-400"}})

(defn get-category-colors
  "Gets the color classes for a category, with fallback to 'other'.

   Parameters:
   - category: Keyword representing the category

   Returns: Map with :bg and :text keys containing Tailwind classes"
  [category]
  (get category-colors category (:other category-colors)))

(def category-icons
  "Maps category keywords to their SVG icon keywords."
  {:groceries      :shopping-cart
   :restaurants    :utensils
   :transportation :car
   :utilities      :zap
   :entertainment  :film
   :healthcare     :activity
   :shopping       :shopping-bag
   :salary         :dollar-sign
   :freelance      :briefcase
   :investments    :trending-up
   :gifts          :gift
   :other          :package
   :food           :utensils
   :housing        :home})

(defn get-category-icon
  "Gets the icon keyword for a category, with fallback to :package."
  [category]
  (get category-icons category :package))

(def budget-status-colors
  "Colors for budget status indicators."
  {:on-track {:bg "bg-green-100 dark:bg-green-900/30"
              :text "text-green-600 dark:text-green-400"
              :ring "ring-green-500"}
   :near-limit {:bg "bg-amber-100 dark:bg-amber-900/30"
                :text "text-amber-600 dark:text-amber-400"
                :ring "ring-amber-500"}
   :over-budget {:bg "bg-red-100 dark:bg-red-900/30"
                 :text "text-red-600 dark:text-red-400"
                 :ring "ring-red-500"}})

(defn get-status-colors
  "Gets the color classes for a budget status.

   Parameters:
   - status: Keyword representing the status (:on-track, :near-limit, :over-budget)

   Returns: Map with :bg, :text, and :ring keys containing Tailwind classes"
  [status]
  (get budget-status-colors status (:on-track budget-status-colors)))
