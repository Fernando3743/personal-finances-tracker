(ns finance.db
  "Initial app-db state and related constants.")

(def default-categories
  [:groceries :restaurants :transportation :utilities
   :entertainment :healthcare :shopping :salary
   :freelance :investments :gifts :other])

(def category-icons
  {:groceries "🛒"
   :restaurants "🍽️"
   :transportation "🚗"
   :utilities "💡"
   :entertainment "🎬"
   :healthcare "🏥"
   :shopping "🛍️"
   :salary "💰"
   :freelance "💼"
   :investments "📈"
   :gifts "🎁"
   :other "📦"})

(def default-db
  {;; Authentication state
   :auth {:user nil              ; Current user map or nil
          :loading? false        ; Auth operation in progress
          :initialized? false    ; Has auth been checked on load?
          :error nil}            ; Auth error message

   :transactions []
   :loading? false
   :error nil
   :current-route :dashboard  ; :dashboard, :transactions, :add-transaction, :login, :register
   :categories default-categories

   ;; UI State
   :theme :light            ; :light or :dark
   :panel {:open? false
           :mode nil}       ; :add, :edit
   :toast-queue []

   ;; Filter state
   :filter {:search ""
            :type nil       ; nil, :income, :expense
            :category nil
            :currency nil   ; nil (all), :COP, :USD
            :sort-by :date
            :sort-dir :desc}

   ;; Form state for adding transactions
   :transaction-form {:amount ""
                      :type :expense
                      :category :other
                      :description ""
                      :date nil
                      :tags #{}
                      :currency :COP
                      :exchange-rate nil}

   ;; Summary data (now with per-currency breakdown)
   :summary {:by-currency {:COP {:balance 0 :income 0 :expenses 0 :count 0}
                           :USD {:balance 0 :income 0 :expenses 0 :count 0}}
             :total-balance 0
             :total-income 0
             :total-expenses 0
             :transaction-count 0}

   ;; Category breakdown (now with per-currency breakdown)
   :category-breakdown {:by-currency {}
                        :categories []
                        :totals {:income 0
                                 :expenses 0
                                 :balance 0}}

   ;; Monthly report (now with per-currency breakdown)
   :monthly-report {:by-currency {}
                    :months []
                    :averages nil}

   ;; Chart time range
   :chart-time-range :month ; :week, :month, :year

   ;; Profile page state
   :profile {:loading? false
             :saving? false
             :uploading-avatar? false
             :deleting? false
             :statistics nil
             :error nil}

   ;; Incomes page filter
   :incomes-filter {:search ""
                    :category nil
                    :currency nil
                    :sort-by :date
                    :sort-dir :desc}

   ;; Expenses page filter
   :expenses-filter {:search ""
                     :category nil
                     :currency nil
                     :sort-by :date
                     :sort-dir :desc}

   ;; Recurring transactions state
   :recurring {:items []
               :loading? false
               :error nil}

   ;; Recurring form state
   :recurring-form {:amount ""
                    :type :expense
                    :category :other
                    :description ""
                    :currency :COP
                    :frequency :monthly
                    :start-date nil
                    :end-date nil
                    :active true}

   ;; Budgets state
   :budgets {:items []
             :status {}
             :loading? false
             :error nil}

   ;; Budget form state
   :budget-form {:category nil
                 :amount ""
                 :currency :COP
                 :alert-threshold 80}

   ;; Analytics state
   :analytics {:loading? false
               :income-expense-data []
               :category-trends []
               :budget-comparison []
               :date-range {:start nil :end nil}
               :selected-view :overview}})
