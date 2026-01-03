(ns finance.db
  "Initial app-db state and related constants.")

(def default-categories
  [:groceries :restaurants :transportation :utilities
   :entertainment :healthcare :shopping :salary
   :freelance :investments :gifts :other])

(def default-db
  {:transactions []
   :loading? false
   :error nil
   :active-view :dashboard  ; :dashboard, :transactions, :add-transaction
   :categories default-categories

   ;; Form state for adding transactions
   :transaction-form {:amount ""
                      :type :expense
                      :category :other
                      :description ""
                      :tags #{}}

   ;; Summary data
   :summary {:total-balance 0
             :total-income 0
             :total-expenses 0
             :transaction-count 0}

   ;; Category breakdown
   :category-breakdown {:categories []
                        :totals {:income 0
                                 :expenses 0
                                 :balance 0}}

   ;; Monthly report
   :monthly-report {:months []
                    :averages nil}})
