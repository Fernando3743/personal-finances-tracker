(ns seeds
  "Development seed data for testing.
   Usage: (seed!) or (seed! {:clear? true :months 6})"
  (:require [finance.storage.datomic :as db]
            [finance.domain.user :as user]
            [finance.domain.transaction :as tx-domain]
            [finance.domain.wallet :as wallet-domain]
            [finance.domain.budget :as budget-domain]
            [finance.domain.recurring :as recurring-domain])
  (:import [java.util Calendar Date]))

(def demo-user
  {:email "demo@example.com"
   :password "demo1234"
   :name "Demo User"})

(def wallet-definitions
  [{:name "Main Checking"
    :type :bank
    :institution "Bancolombia"
    :account-number "4521"
    :balance 8500000M
    :currency :COP
    :status :active}
   {:name "Savings Account"
    :type :bank
    :institution "Davivienda"
    :account-number "7823"
    :balance 15000000M
    :currency :COP
    :status :synced}
   {:name "Visa Signature"
    :type :card
    :institution "Bancolombia"
    :account-number "4422"
    :balance 2500000M
    :currency :COP
    :status :active}
   {:name "USD Account"
    :type :bank
    :institution "Bancolombia"
    :account-number "8891"
    :balance 3500.00M
    :currency :USD
    :status :active}
   {:name "Bitcoin Wallet"
    :type :crypto
    :institution "Binance"
    :account-number ""
    :balance 0.15M
    :currency :BTC
    :status :active}
   {:name "Ethereum Wallet"
    :type :crypto
    :institution "Coinbase"
    :account-number ""
    :balance 2.5M
    :currency :ETH
    :status :synced}
   {:name "Investment Portfolio"
    :type :investment
    :institution "Tyba"
    :account-number "3456"
    :balance 25000000M
    :currency :COP
    :status :active}])

(def budget-definitions
  [{:category :groceries :amount 800000M :alert-threshold 0.8M}
   {:category :restaurants :amount 400000M :alert-threshold 0.8M}
   {:category :transportation :amount 300000M :alert-threshold 0.8M}
   {:category :utilities :amount 350000M :alert-threshold 0.8M}
   {:category :entertainment :amount 250000M :alert-threshold 0.8M}
   {:category :healthcare :amount 200000M :alert-threshold 0.9M}
   {:category :shopping :amount 500000M :alert-threshold 0.8M}])

(def recurring-definitions
  [{:amount 5000000M
    :type :income
    :category :salary
    :description "Monthly Salary"
    :currency :COP
    :frequency :monthly
    :payment-method "Direct Deposit"}
   {:amount 80000M
    :type :expense
    :category :utilities
    :description "Internet Service"
    :currency :COP
    :frequency :monthly
    :payment-method "Auto-debit"}
   {:amount 150000M
    :type :expense
    :category :utilities
    :description "Electricity Bill"
    :currency :COP
    :frequency :monthly}
   {:amount 50000M
    :type :expense
    :category :entertainment
    :description "Streaming Services"
    :currency :COP
    :frequency :monthly}
   {:amount 15.99M
    :type :expense
    :category :entertainment
    :description "Spotify Premium"
    :currency :USD
    :frequency :monthly}
   {:amount 1500000M
    :type :expense
    :category :healthcare
    :description "Health Insurance"
    :currency :COP
    :frequency :monthly}])

(def transaction-templates
  {:income
   [{:category :salary :amount-range [4500000 5500000] :description "Monthly Salary" :frequency 1}
    {:category :freelance :amount-range [500000 2000000] :description "Freelance Work" :frequency 0.3}
    {:category :investments :amount-range [100000 500000] :description "Investment Returns" :frequency 0.15}]
   :expense
   [{:category :groceries :amount-range [50000 200000] :description "Grocery Shopping" :frequency 6}
    {:category :restaurants :amount-range [30000 150000] :description "Dining Out" :frequency 4}
    {:category :transportation :amount-range [10000 100000] :description "Transport" :frequency 15}
    {:category :utilities :amount-range [80000 250000] :description "Utility Bill" :frequency 3}
    {:category :entertainment :amount-range [20000 200000] :description "Entertainment" :frequency 3}
    {:category :healthcare :amount-range [50000 500000] :description "Medical" :frequency 0.5}
    {:category :shopping :amount-range [50000 500000] :description "Shopping" :frequency 2}]})

(defn random-day-in-month
  "Returns a random Date within the given year/month."
  [year month]
  (let [cal (doto (Calendar/getInstance)
              (.set Calendar/YEAR year)
              (.set Calendar/MONTH (dec month))
              (.set Calendar/DAY_OF_MONTH (inc (rand-int 28)))
              (.set Calendar/HOUR_OF_DAY (rand-int 24))
              (.set Calendar/MINUTE (rand-int 60)))]
    (.getTime cal)))

(defn first-week-of-month
  "Returns a random Date in the first week of the given year/month."
  [year month]
  (let [cal (doto (Calendar/getInstance)
              (.set Calendar/YEAR year)
              (.set Calendar/MONTH (dec month))
              (.set Calendar/DAY_OF_MONTH (inc (rand-int 5)))
              (.set Calendar/HOUR_OF_DAY 9)
              (.set Calendar/MINUTE 0))]
    (.getTime cal)))

(defn dates-for-past-months
  "Returns a sequence of [year month] pairs for the past n months."
  [n]
  (let [cal (Calendar/getInstance)]
    (for [i (range n)]
      (let [_ (when (pos? i) (.add cal Calendar/MONTH -1))]
        [(.get cal Calendar/YEAR)
         (inc (.get cal Calendar/MONTH))]))))

(defn random-amount
  "Returns a random amount within the given range."
  [[min-amt max-amt]]
  (bigdec (+ min-amt (rand-int (- max-amt min-amt)))))

(defn generate-transactions-for-month
  "Generates transactions for a specific month based on templates."
  [user-id year month]
  (let [transactions (atom [])]
    (doseq [[tx-type template-list] transaction-templates
            {:keys [category amount-range description frequency]} template-list]
      (let [variation (+ 0.7 (rand 0.6))
            count-for-month (int (Math/ceil (* frequency variation)))]
        (dotimes [_ count-for-month]
          (let [amount (random-amount amount-range)
                currency (if (< (rand) 0.8) :COP :USD)
                date (if (= category :salary)
                       (first-week-of-month year month)
                       (random-day-in-month year month))
                tx (-> (tx-domain/create-transaction
                        amount
                        tx-type
                        category
                        {:description description
                         :date date
                         :currency currency})
                       (assoc :transaction/user-id user-id))]
            (swap! transactions conj tx)))))
    @transactions))

(defn create-wallets
  "Creates wallet entities for a user."
  [user-id]
  (mapv (fn [{:keys [name type institution account-number balance currency status]}]
          (-> (wallet-domain/create-wallet name type institution
                                           {:account-number account-number
                                            :balance balance
                                            :currency currency
                                            :status status})
              (assoc :wallet/user-id user-id)))
        wallet-definitions))

(defn create-budgets-for-month
  "Creates budget entities for a specific month."
  [user-id year month]
  (mapv (fn [{:keys [category amount alert-threshold]}]
          (-> (budget-domain/create-budget category amount
                                           {:currency :COP
                                            :year year
                                            :month month
                                            :alert-threshold alert-threshold})
              (assoc :budget/user-id user-id)))
        budget-definitions))

(defn create-recurring
  "Creates recurring transaction entities for a user."
  [user-id]
  (mapv (fn [{:keys [amount type category description currency frequency payment-method]}]
          (-> (recurring-domain/create-recurring amount type category frequency
                                                 {:description description
                                                  :currency currency
                                                  :payment-method payment-method})
              (assoc :recurring/user-id user-id)))
        recurring-definitions))

(defn seed!
  "Seeds the database with demo data.
   Options:
   - :clear?  - If true, clears existing demo user data first (default: false)
   - :months  - Number of months of transaction history (default: 6)"
  ([] (seed! {}))
  ([{:keys [clear? months] :or {clear? false months 6}}]
   (let [db-uri (or (System/getenv "DATOMIC_URI")
                    (throw (ex-info "DATOMIC_URI environment variable not set" {})))
         conn (db/create-conn db-uri)
         {:keys [email password name]} demo-user]

     (when clear?
       (when-let [existing (db/find-user-by-email conn email)]
         (db/delete-user! conn (:user/id existing))
         (println "Cleared existing demo user")))

     (if (db/email-exists? conn email)
       (println "Demo user already exists. Use {:clear? true} to recreate.")

       (let [new-user (user/create-user email password name)
             _ (db/save-user! conn new-user)
             user-id (:user/id new-user)]

         (println "Created demo user:" email)

         (let [wallets (create-wallets user-id)]
           (doseq [wallet wallets]
             (db/save-wallet! conn wallet))
           (println "Created" (count wallets) "wallets"))

         (let [month-data (dates-for-past-months months)
               all-txs (mapcat (fn [[year month]]
                                 (generate-transactions-for-month user-id year month))
                               month-data)]
           (doseq [tx all-txs]
             (db/save-transaction! conn tx))
           (println "Created" (count all-txs) "transactions"))

         (let [budget-months (take 3 (dates-for-past-months 3))
               all-budgets (mapcat (fn [[year month]]
                                     (create-budgets-for-month user-id year month))
                                   budget-months)]
           (doseq [budget all-budgets]
             (db/save-budget! conn budget))
           (println "Created" (count all-budgets) "budgets"))

         (let [recurrings (create-recurring user-id)]
           (doseq [recurring recurrings]
             (db/save-recurring! conn recurring))
           (println "Created" (count recurrings) "recurring transactions"))

         (println)
         (println "=== Seed Complete ===")
         (println "Login with:" email "/" password))))))

(defn clear!
  "Clears the demo user and all associated data."
  []
  (let [db-uri (or (System/getenv "DATOMIC_URI")
                   (throw (ex-info "DATOMIC_URI environment variable not set" {})))
        conn (db/create-conn db-uri)
        email (:email demo-user)]
    (if-let [existing (db/find-user-by-email conn email)]
      (do
        (db/delete-user! conn (:user/id existing))
        (println "Cleared demo user:" email))
      (println "Demo user not found"))))

(comment
  ;; Usage examples:sts
  (seed! {:clear? true})            ;; Clear and recreate
  (seed! {:clear? true :months 12}) ;; 12 months of history
  (clear!)                          ;; Just clear demo user
  )
