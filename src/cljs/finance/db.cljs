(ns finance.db
  "Initial app-db state and related constants."
  (:require [finance.utils.currency :as currency]))

;; Re-export currency constants for convenience
(def available-currencies currency/available-currencies)
(def currency-config currency/currency-config)

(def default-categories
  [:groceries :restaurants :transportation :utilities
   :entertainment :healthcare :shopping :salary
   :freelance :investments :gifts :other])

;; Category icons (emoji) mapping
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
  {:transactions []
   :loading? false
   :error nil
   :active-view :dashboard  ; :dashboard, :transactions, :add-transaction
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
   :chart-time-range :month}) ; :week, :month, :year
