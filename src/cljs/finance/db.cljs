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
            :sort-field :date
            :sort-dir :desc}

   ;; Form state for adding transactions
   :transaction-form {:amount ""
                      :type :expense
                      :category :other
                      :description ""
                      :date nil
                      :tags #{}
                      :currency :COP
                      :exchange-rate nil
                      :notes ""
                      :is-recurring false}

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
                    :sort-field :date
                    :sort-dir :desc}

   ;; Expenses page filter
   :expenses-filter {:search ""
                     :category nil
                     :currency nil
                     :sort-field :date
                     :sort-dir :desc}

   ;; Recurring transactions state
   :recurring {:items []
               :loading? false
               :error nil
               :view-mode :list       ; :list, :cards, :calendar
               :search-query ""
               :calendar-month nil}   ; js/Date for currently viewed month

   ;; Recurring panel state
   :recurring-panel {:open? false}

   ;; Recurring form state
   :recurring-form {:name ""
                    :amount ""
                    :type :expense
                    :category :entertainment
                    :description ""
                    :currency :COP
                    :frequency :monthly
                    :start-date nil
                    :end-date nil
                    :active true
                    :payment-method ""}

   ;; Budgets state
   :budgets {:items []
             :status {}
             :loading? false
             :error nil
             :view-mode :grid        ; :table, :grid, :envelope
             :search-query ""
             :filter-status :all     ; :all, :on-track, :near-limit, :over-budget
             :selected-budget-id nil
             :time-period :this-month
             :comparison-month nil}

   ;; Budgets panel state
   :budgets-panel {:open? false
                   :mode nil}  ; :create or :edit

   ;; Budget form state
   :budget-form {:category nil
                 :amount ""
                 :currency :COP
                 :alert-threshold 80
                 :budget-type :fixed  ; :fixed or :variable
                 :notes ""}

   ;; Analytics state
   :analytics {:loading? false
               :income-expense-data []
               :category-trends []
               :budget-comparison []
               :date-range {:start nil :end nil}
               :selected-view :overview}

   ;; Wallets state
   :wallets {:accounts []
             :loading? false
             :error nil
             :view-mode :grid        ; :grid, :list, :portfolio
             :filter :all            ; :all, :bank, :card, :crypto, :investment
             :selected-wallet-id nil}

   ;; Wallet summary
   :wallet-summary {:total-wallets 0
                    :by-type {:bank 0 :card 0 :crypto 0 :investment 0}
                    :total-by-currency {}
                    :fiat-total 0
                    :crypto-total 0}

   ;; Wallet panel state
   :wallet-panel {:open? false
                  :mode nil}  ; :create or :edit

   ;; Wallet form state
   :wallet-form {:name ""
                 :type :bank           ; :bank, :card, :crypto, :investment
                 :institution ""
                 :account-number ""
                 :balance ""
                 :currency :USD
                 :color nil
                 :balance-label "Current Balance"}

   ;; Currency converter (for portfolio view)
   :currency-converter {:from-amount 1000
                        :from-currency :USD
                        :to-currency :BTC
                        :result nil
                        :loading? false}

   ;; Market data (for portfolio view)
   :market-data {:rates {}
                 :loading? false
                 :last-updated nil}})
