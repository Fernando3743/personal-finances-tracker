(ns finance.storage.datomic
  "Datomic storage implementation."
  (:require [datomic.api :as d]
            [clojure.string :as str]
            [clojure.tools.logging :as log]
            [finance.domain.transaction :as tx-domain]
            [finance.domain.user :as user-domain]))

(defn- safe-transact
  "Safely executes a Datomic transaction with error handling and logging."
  [conn tx-data operation-desc]
  (try
    (let [result @(d/transact conn tx-data)]
      (log/debug (str "Transaction succeeded: " operation-desc))
      result)
    (catch Exception e
      (log/error e (str "Transaction failed: " operation-desc))
      (throw (ex-info (str "Database transaction failed: " operation-desc)
                      {:operation operation-desc
                       :tx-data tx-data}
                      e)))))

(def user-schema
  "Datomic schema for users."
  [{:db/ident :user/id
    :db/valueType :db.type/uuid
    :db/unique :db.unique/identity
    :db/cardinality :db.cardinality/one
    :db/doc "Unique identifier for the user"}

   {:db/ident :user/email
    :db/valueType :db.type/string
    :db/unique :db.unique/value
    :db/cardinality :db.cardinality/one
    :db/doc "User email (unique, used for login)"}

   {:db/ident :user/password-hash
    :db/valueType :db.type/string
    :db/cardinality :db.cardinality/one
    :db/doc "BCrypt hashed password"}

   {:db/ident :user/name
    :db/valueType :db.type/string
    :db/cardinality :db.cardinality/one
    :db/doc "Display name"}

   {:db/ident :user/created-at
    :db/valueType :db.type/instant
    :db/cardinality :db.cardinality/one
    :db/doc "Account creation timestamp"}

   {:db/ident :user/updated-at
    :db/valueType :db.type/instant
    :db/cardinality :db.cardinality/one
    :db/doc "Last profile update timestamp"}

   {:db/ident :user/preferred-currency
    :db/valueType :db.type/keyword
    :db/cardinality :db.cardinality/one
    :db/doc "User's preferred currency (:COP or :USD)"}])

(def recurring-schema
  "Datomic schema for recurring transactions."
  [{:db/ident :recurring/id
    :db/valueType :db.type/uuid
    :db/unique :db.unique/identity
    :db/cardinality :db.cardinality/one
    :db/doc "Unique identifier for the recurring transaction"}

   {:db/ident :recurring/amount
    :db/valueType :db.type/bigdec
    :db/cardinality :db.cardinality/one
    :db/doc "Recurring transaction amount (always positive)"}

   {:db/ident :recurring/type
    :db/valueType :db.type/keyword
    :db/cardinality :db.cardinality/one
    :db/doc "Transaction type: :income or :expense"}

   {:db/ident :recurring/category
    :db/valueType :db.type/keyword
    :db/cardinality :db.cardinality/one
    :db/doc "Transaction category"}

   {:db/ident :recurring/description
    :db/valueType :db.type/string
    :db/cardinality :db.cardinality/one
    :db/doc "Description of the recurring transaction"}

   {:db/ident :recurring/currency
    :db/valueType :db.type/keyword
    :db/cardinality :db.cardinality/one
    :db/doc "Currency: :COP or :USD"}

   {:db/ident :recurring/frequency
    :db/valueType :db.type/keyword
    :db/cardinality :db.cardinality/one
    :db/doc "Frequency: :daily, :weekly, :monthly, :yearly"}

   {:db/ident :recurring/start-date
    :db/valueType :db.type/instant
    :db/cardinality :db.cardinality/one
    :db/doc "When the recurring transaction starts"}

   {:db/ident :recurring/end-date
    :db/valueType :db.type/instant
    :db/cardinality :db.cardinality/one
    :db/doc "Optional end date for the recurring transaction"}

   {:db/ident :recurring/next-occurrence
    :db/valueType :db.type/instant
    :db/cardinality :db.cardinality/one
    :db/doc "Next scheduled occurrence date"}

   {:db/ident :recurring/last-generated
    :db/valueType :db.type/instant
    :db/cardinality :db.cardinality/one
    :db/doc "Last time a transaction was generated from this template"}

   {:db/ident :recurring/active?
    :db/valueType :db.type/boolean
    :db/cardinality :db.cardinality/one
    :db/doc "Whether this recurring transaction is active"}

   {:db/ident :recurring/user-id
    :db/valueType :db.type/uuid
    :db/cardinality :db.cardinality/one
    :db/index true
    :db/doc "Owner user ID"}

   {:db/ident :recurring/created-at
    :db/valueType :db.type/instant
    :db/cardinality :db.cardinality/one
    :db/doc "Creation timestamp"}

   {:db/ident :recurring/payment-method
    :db/valueType :db.type/string
    :db/cardinality :db.cardinality/one
    :db/doc "Payment method (e.g., 'Visa ****4242', 'Wells Fargo')"}])

(def wallet-schema
  "Datomic schema for wallets/accounts."
  [{:db/ident :wallet/id
    :db/valueType :db.type/uuid
    :db/unique :db.unique/identity
    :db/cardinality :db.cardinality/one
    :db/doc "Unique identifier for the wallet"}

   {:db/ident :wallet/name
    :db/valueType :db.type/string
    :db/cardinality :db.cardinality/one
    :db/doc "Display name for the wallet (e.g., 'Chase Sapphire Reserve')"}

   {:db/ident :wallet/type
    :db/valueType :db.type/keyword
    :db/cardinality :db.cardinality/one
    :db/doc "Wallet type: :bank, :card, :crypto, :investment"}

   {:db/ident :wallet/institution
    :db/valueType :db.type/string
    :db/cardinality :db.cardinality/one
    :db/doc "Institution name (e.g., 'Chase', 'Coinbase')"}

   {:db/ident :wallet/account-number
    :db/valueType :db.type/string
    :db/cardinality :db.cardinality/one
    :db/doc "Last 4 digits of account number for display"}

   {:db/ident :wallet/balance
    :db/valueType :db.type/bigdec
    :db/cardinality :db.cardinality/one
    :db/doc "Current balance (can be negative for credit cards)"}

   {:db/ident :wallet/currency
    :db/valueType :db.type/keyword
    :db/cardinality :db.cardinality/one
    :db/doc "Currency: :USD, :COP, :BTC, :ETH, etc."}

   {:db/ident :wallet/status
    :db/valueType :db.type/keyword
    :db/cardinality :db.cardinality/one
    :db/doc "Sync status: :active, :synced, :updating, :error"}

   {:db/ident :wallet/color
    :db/valueType :db.type/string
    :db/cardinality :db.cardinality/one
    :db/doc "Hex color code for UI theming"}

   {:db/ident :wallet/icon-url
    :db/valueType :db.type/string
    :db/cardinality :db.cardinality/one
    :db/doc "Optional URL for institution icon"}

   {:db/ident :wallet/balance-label
    :db/valueType :db.type/string
    :db/cardinality :db.cardinality/one
    :db/doc "Custom label for balance (e.g., 'Current Balance', 'Available')"}

   {:db/ident :wallet/user-id
    :db/valueType :db.type/uuid
    :db/cardinality :db.cardinality/one
    :db/index true
    :db/doc "Owner user ID"}

   {:db/ident :wallet/created-at
    :db/valueType :db.type/instant
    :db/cardinality :db.cardinality/one
    :db/doc "Creation timestamp"}

   {:db/ident :wallet/last-updated
    :db/valueType :db.type/instant
    :db/cardinality :db.cardinality/one
    :db/doc "Last balance update timestamp"}])

(def budget-schema
  "Datomic schema for budgets."
  [{:db/ident :budget/id
    :db/valueType :db.type/uuid
    :db/unique :db.unique/identity
    :db/cardinality :db.cardinality/one
    :db/doc "Unique identifier for the budget"}

   {:db/ident :budget/category
    :db/valueType :db.type/keyword
    :db/cardinality :db.cardinality/one
    :db/doc "Category this budget applies to"}

   {:db/ident :budget/amount
    :db/valueType :db.type/bigdec
    :db/cardinality :db.cardinality/one
    :db/doc "Monthly budget amount limit"}

   {:db/ident :budget/currency
    :db/valueType :db.type/keyword
    :db/cardinality :db.cardinality/one
    :db/doc "Currency: :COP or :USD"}

   {:db/ident :budget/year
    :db/valueType :db.type/long
    :db/cardinality :db.cardinality/one
    :db/doc "Budget year"}

   {:db/ident :budget/month
    :db/valueType :db.type/long
    :db/cardinality :db.cardinality/one
    :db/doc "Budget month (1-12)"}

   {:db/ident :budget/alert-threshold
    :db/valueType :db.type/bigdec
    :db/cardinality :db.cardinality/one
    :db/doc "Percentage threshold for alerts (e.g., 0.8 = 80%)"}

   {:db/ident :budget/user-id
    :db/valueType :db.type/uuid
    :db/cardinality :db.cardinality/one
    :db/index true
    :db/doc "Owner user ID"}

   {:db/ident :budget/created-at
    :db/valueType :db.type/instant
    :db/cardinality :db.cardinality/one
    :db/doc "Creation timestamp"}])

(def schema
  "Datomic schema for transactions."
  [{:db/ident :transaction/id
    :db/valueType :db.type/uuid
    :db/unique :db.unique/identity
    :db/cardinality :db.cardinality/one
    :db/doc "Unique identifier for the transaction"}

   {:db/ident :transaction/amount
    :db/valueType :db.type/bigdec
    :db/cardinality :db.cardinality/one
    :db/doc "Transaction amount (always positive)"}

   {:db/ident :transaction/type
    :db/valueType :db.type/keyword
    :db/cardinality :db.cardinality/one
    :db/doc "Transaction type: :income or :expense"}

   {:db/ident :transaction/category
    :db/valueType :db.type/keyword
    :db/cardinality :db.cardinality/one
    :db/doc "Transaction category"}

   {:db/ident :transaction/date
    :db/valueType :db.type/instant
    :db/cardinality :db.cardinality/one
    :db/doc "Transaction date"}

   {:db/ident :transaction/description
    :db/valueType :db.type/string
    :db/cardinality :db.cardinality/one
    :db/doc "Optional transaction description"}

   {:db/ident :transaction/tags
    :db/valueType :db.type/keyword
    :db/cardinality :db.cardinality/many
    :db/doc "Optional set of tags"}

   {:db/ident :transaction/currency
    :db/valueType :db.type/keyword
    :db/cardinality :db.cardinality/one
    :db/doc "Currency code: :COP or :USD"}

   {:db/ident :transaction/exchange-rate
    :db/valueType :db.type/bigdec
    :db/cardinality :db.cardinality/one
    :db/doc "Optional exchange rate at time of creation"}

   {:db/ident :transaction/user-id
    :db/valueType :db.type/uuid
    :db/cardinality :db.cardinality/one
    :db/index true
    :db/doc "Owner user ID"}

   {:db/ident :transaction/recurring-id
    :db/valueType :db.type/uuid
    :db/cardinality :db.cardinality/one
    :db/doc "Reference to the recurring transaction template that generated this"}])

(defn- entity->transaction
  "Converts a Datomic entity to a transaction map."
  [entity]
  (let [base {:transaction/id (:transaction/id entity)
              :transaction/amount (:transaction/amount entity)
              :transaction/type (:transaction/type entity)
              :transaction/category (:transaction/category entity)
              :transaction/date (:transaction/date entity)
              :transaction/currency (or (:transaction/currency entity) :COP)}]
    (cond-> base
      (:transaction/description entity)
      (assoc :transaction/description (:transaction/description entity))

      (seq (:transaction/tags entity))
      (assoc :transaction/tags (set (:transaction/tags entity)))

      (:transaction/exchange-rate entity)
      (assoc :transaction/exchange-rate (:transaction/exchange-rate entity))

      (:transaction/user-id entity)
      (assoc :transaction/user-id (:transaction/user-id entity)))))

(defn- transaction->tx-data
  "Converts a transaction map to Datomic transaction data."
  [transaction]
  (let [base {:transaction/id (:transaction/id transaction)
              :transaction/amount (bigdec (:transaction/amount transaction))
              :transaction/type (:transaction/type transaction)
              :transaction/category (:transaction/category transaction)
              :transaction/date (:transaction/date transaction)
              :transaction/currency (:transaction/currency transaction)}]
    (cond-> base
      (:transaction/description transaction)
      (assoc :transaction/description (:transaction/description transaction))

      (seq (:transaction/tags transaction))
      (assoc :transaction/tags (:transaction/tags transaction))

      (:transaction/exchange-rate transaction)
      (assoc :transaction/exchange-rate (bigdec (:transaction/exchange-rate transaction)))

      (:transaction/user-id transaction)
      (assoc :transaction/user-id (:transaction/user-id transaction)))))

(defn- find-entity-id
  "Finds the Datomic entity ID for a transaction by its UUID."
  [db uuid]
  (ffirst
   (d/q '[:find ?e
          :in $ ?id
          :where [?e :transaction/id ?id]]
        db uuid)))

(defn save-transaction!
  "Saves a transaction to the database. Returns the saved transaction.
   Validates transaction against spec before saving."
  [conn transaction]
  (if (tx-domain/valid? transaction)
    (let [tx-data (transaction->tx-data transaction)]
      (safe-transact conn [tx-data] "save transaction")
      transaction)
    (do
      (log/error (str "Invalid transaction data: " (tx-domain/explain-invalid transaction)))
      (throw (ex-info "Invalid transaction data"
                      {:validation-errors (tx-domain/explain-invalid transaction)})))))

(defn load-transactions
  "Loads all transactions from the database. Returns a vector."
  [conn]
  (let [db (d/db conn)
        entities (d/q '[:find [(pull ?e [*]) ...]
                        :where [?e :transaction/id]]
                      db)]
    (mapv entity->transaction entities)))

(defn load-transaction
  "Loads a single transaction by ID. Returns nil if not found."
  [conn id]
  (let [db (d/db conn)
        entity (d/q '[:find (pull ?e [*]) .
                      :in $ ?id
                      :where [?e :transaction/id ?id]]
                    db id)]
    (when entity
      (entity->transaction entity))))

(defn update-transaction!
  "Updates a transaction. Returns the updated transaction or nil.
   Validates merged transaction against spec before updating."
  [conn id updates]
  (let [db (d/db conn)
        eid (find-entity-id db id)]
    (when eid
      (let [current (d/pull db '[*] eid)
            merged (merge (entity->transaction current) updates)]
        (if (tx-domain/valid? merged)
          (let [tx-data (transaction->tx-data merged)]
            (safe-transact conn [tx-data] "update transaction")
            merged)
          (do
            (log/error (str "Invalid transaction update: " (tx-domain/explain-invalid merged)))
            (throw (ex-info "Invalid transaction update"
                            {:validation-errors (tx-domain/explain-invalid merged)}))))))))

(defn delete-transaction!
  "Deletes a transaction by ID. Returns true if deleted, false otherwise."
  [conn id]
  (let [db (d/db conn)
        eid (find-entity-id db id)]
    (when eid
      (safe-transact conn [[:db/retractEntity eid]] "delete transaction")
      true)))

(defn create-conn
  "Creates a Datomic connection.
   Creates the database if it doesn't exist and transacts the schema."
  [uri]
  (d/create-database uri)
  (let [conn (d/connect uri)]
    (safe-transact conn user-schema "install user schema")
    (safe-transact conn schema "install transaction schema")
    (safe-transact conn recurring-schema "install recurring schema")
    (safe-transact conn budget-schema "install budget schema")
    (safe-transact conn wallet-schema "install wallet schema")
    conn))

(defn- entity->user
  "Converts a Datomic entity to a user map."
  [entity]
  (when entity
    (cond-> {:user/id (:user/id entity)
             :user/email (:user/email entity)
             :user/password-hash (:user/password-hash entity)
             :user/name (:user/name entity)
             :user/created-at (:user/created-at entity)}
      (:user/updated-at entity)
      (assoc :user/updated-at (:user/updated-at entity))

      (:user/preferred-currency entity)
      (assoc :user/preferred-currency (:user/preferred-currency entity)))))

(defn save-user!
  "Saves a user to the database. Returns the saved user.
   Validates user against spec before saving."
  [conn user]
  (if (user-domain/valid? user)
    (let [tx-data {:user/id (:user/id user)
                   :user/email (str/lower-case (:user/email user))
                   :user/password-hash (:user/password-hash user)
                   :user/name (:user/name user)
                   :user/created-at (or (:user/created-at user) (java.util.Date.))}]
      (safe-transact conn [tx-data] "save user")
      user)
    (do
      (log/error "Invalid user data - validation failed")
      (throw (ex-info "Invalid user data" {:user user})))))

(defn find-user-by-email
  "Finds a user by email. Returns nil if not found."
  [conn email]
  (let [db (d/db conn)
        entity (d/q '[:find (pull ?e [*]) .
                      :in $ ?email
                      :where [?e :user/email ?email]]
                    db (str/lower-case email))]
    (entity->user entity)))

(defn find-user-by-id
  "Finds a user by UUID. Returns nil if not found."
  [conn id]
  (let [db (d/db conn)]
    (entity->user
     (d/q '[:find (pull ?e [*]) .
            :in $ ?id
            :where [?e :user/id ?id]]
          db id))))

(defn email-exists?
  "Checks if an email is already registered."
  [conn email]
  (some? (find-user-by-email conn email)))

(defn load-transactions-for-user
  "Loads all transactions for a specific user."
  [conn user-id]
  (let [db (d/db conn)
        entities (d/q '[:find [(pull ?e [*]) ...]
                        :in $ ?user-id
                        :where [?e :transaction/user-id ?user-id]]
                      db user-id)]
    (mapv entity->transaction entities)))

(defn save-transaction-for-user!
  "Saves a transaction with user ownership."
  [conn user-id transaction]
  (let [tx-with-user (assoc transaction :transaction/user-id user-id)]
    (save-transaction! conn tx-with-user)))

(defn user-owns-transaction?
  "Checks if a transaction belongs to a user."
  [conn user-id transaction-id]
  (let [db (d/db conn)]
    (some?
     (d/q '[:find ?e .
            :in $ ?tx-id ?user-id
            :where
            [?e :transaction/id ?tx-id]
            [?e :transaction/user-id ?user-id]]
          db transaction-id user-id))))

(defn- find-user-entity-id
  "Finds the Datomic entity ID for a user by UUID."
  [db user-id]
  (ffirst
   (d/q '[:find ?e
          :in $ ?id
          :where [?e :user/id ?id]]
        db user-id)))

(defn update-user!
  "Updates user profile fields. Returns updated user or nil if not found."
  [conn user-id updates]
  (let [db (d/db conn)
        eid (find-user-entity-id db user-id)]
    (when eid
      (let [valid-keys #{:user/name :user/email :user/password-hash
                         :user/preferred-currency}
            filtered-updates (select-keys updates valid-keys)
            tx-data (-> filtered-updates
                        (assoc :db/id eid)
                        (assoc :user/updated-at (java.util.Date.)))]
        (when (seq filtered-updates)
          (safe-transact conn [tx-data] "update user")
          (find-user-by-id conn user-id))))))

(defn delete-user!
  "Deletes a user and all their transactions. Returns true if deleted."
  [conn user-id]
  (let [db (d/db conn)
        user-eid (find-user-entity-id db user-id)]
    (when user-eid
      (let [tx-eids (d/q '[:find [?e ...]
                           :in $ ?user-id
                           :where [?e :transaction/user-id ?user-id]]
                         db user-id)
            retractions (mapv (fn [eid] [:db/retractEntity eid]) tx-eids)]
        (when (seq retractions)
          (safe-transact conn retractions "delete user transactions"))
        (safe-transact conn [[:db/retractEntity user-eid]] "delete user")
        true))))

(defn get-user-statistics
  "Returns aggregated statistics for a user."
  [conn user-id]
  (let [transactions (load-transactions-for-user conn user-id)
        total-count (count transactions)
        income-txs (filter #(= :income (:transaction/type %)) transactions)
        expense-txs (filter #(= :expense (:transaction/type %)) transactions)
        sum-by-currency (fn [txs]
                          (reduce (fn [acc tx]
                                    (let [curr (:transaction/currency tx)
                                          amt (:transaction/amount tx)]
                                      (update acc curr (fnil + 0M) amt)))
                                  {}
                                  txs))]
    {:total-transactions total-count
     :income-by-currency (sum-by-currency income-txs)
     :expenses-by-currency (sum-by-currency expense-txs)
     :income-count (count income-txs)
     :expense-count (count expense-txs)}))

(defn- entity->recurring
  "Converts a Datomic entity to a recurring transaction map."
  [entity]
  (when entity
    (let [base {:recurring/id (:recurring/id entity)
                :recurring/amount (:recurring/amount entity)
                :recurring/type (:recurring/type entity)
                :recurring/category (:recurring/category entity)
                :recurring/currency (or (:recurring/currency entity) :COP)
                :recurring/frequency (:recurring/frequency entity)
                :recurring/start-date (:recurring/start-date entity)
                :recurring/active? (if (nil? (:recurring/active? entity)) true (:recurring/active? entity))
                :recurring/user-id (:recurring/user-id entity)}]
      (cond-> base
        (:recurring/description entity)
        (assoc :recurring/description (:recurring/description entity))

        (:recurring/end-date entity)
        (assoc :recurring/end-date (:recurring/end-date entity))

        (:recurring/next-occurrence entity)
        (assoc :recurring/next-occurrence (:recurring/next-occurrence entity))

        (:recurring/last-generated entity)
        (assoc :recurring/last-generated (:recurring/last-generated entity))

        (:recurring/created-at entity)
        (assoc :recurring/created-at (:recurring/created-at entity))

        (:recurring/payment-method entity)
        (assoc :recurring/payment-method (:recurring/payment-method entity))))))

(defn- recurring->tx-data
  "Converts a recurring transaction map to Datomic transaction data."
  [recurring]
  (let [base {:recurring/id (:recurring/id recurring)
              :recurring/amount (bigdec (:recurring/amount recurring))
              :recurring/type (:recurring/type recurring)
              :recurring/category (:recurring/category recurring)
              :recurring/currency (:recurring/currency recurring)
              :recurring/frequency (:recurring/frequency recurring)
              :recurring/start-date (:recurring/start-date recurring)
              :recurring/active? (:recurring/active? recurring)
              :recurring/user-id (:recurring/user-id recurring)}]
    (cond-> base
      (:recurring/description recurring)
      (assoc :recurring/description (:recurring/description recurring))

      (:recurring/end-date recurring)
      (assoc :recurring/end-date (:recurring/end-date recurring))

      (:recurring/next-occurrence recurring)
      (assoc :recurring/next-occurrence (:recurring/next-occurrence recurring))

      (:recurring/last-generated recurring)
      (assoc :recurring/last-generated (:recurring/last-generated recurring))

      (:recurring/created-at recurring)
      (assoc :recurring/created-at (:recurring/created-at recurring))

      (:recurring/payment-method recurring)
      (assoc :recurring/payment-method (:recurring/payment-method recurring)))))

(defn- find-recurring-entity-id
  "Finds the Datomic entity ID for a recurring transaction by its UUID."
  [db uuid]
  (ffirst
   (d/q '[:find ?e
          :in $ ?id
          :where [?e :recurring/id ?id]]
        db uuid)))

(defn save-recurring!
  "Saves a recurring transaction to the database."
  [conn recurring]
  (let [tx-data (recurring->tx-data recurring)]
    (safe-transact conn [tx-data] "save recurring transaction")
    recurring))

(defn load-recurring-for-user
  "Loads all recurring transactions for a specific user."
  [conn user-id]
  (let [db (d/db conn)
        entities (d/q '[:find [(pull ?e [*]) ...]
                        :in $ ?user-id
                        :where [?e :recurring/user-id ?user-id]]
                      db user-id)]
    (mapv entity->recurring entities)))

(defn load-recurring-by-id
  "Loads a single recurring transaction by ID."
  [conn id]
  (let [db (d/db conn)
        entity (d/q '[:find (pull ?e [*]) .
                      :in $ ?id
                      :where [?e :recurring/id ?id]]
                    db id)]
    (entity->recurring entity)))

(defn update-recurring!
  "Updates a recurring transaction. Returns the updated record or nil."
  [conn id updates]
  (let [db (d/db conn)
        eid (find-recurring-entity-id db id)]
    (when eid
      (let [current (d/pull db '[*] eid)
            merged (merge (entity->recurring current) updates)
            tx-data (recurring->tx-data merged)]
        (safe-transact conn [tx-data] "update recurring transaction")
        merged))))

(defn delete-recurring!
  "Deletes a recurring transaction by ID."
  [conn id]
  (let [db (d/db conn)
        eid (find-recurring-entity-id db id)]
    (when eid
      (safe-transact conn [[:db/retractEntity eid]] "delete recurring transaction")
      true)))

(defn user-owns-recurring?
  "Checks if a recurring transaction belongs to a user."
  [conn user-id recurring-id]
  (let [db (d/db conn)]
    (some?
     (d/q '[:find ?e .
            :in $ ?rec-id ?user-id
            :where
            [?e :recurring/id ?rec-id]
            [?e :recurring/user-id ?user-id]]
          db recurring-id user-id))))

(defn load-pending-recurring
  "Loads all active recurring transactions where next-occurrence <= now."
  [conn now]
  (let [db (d/db conn)
        entities (d/q '[:find [(pull ?e [*]) ...]
                        :in $ ?now
                        :where
                        [?e :recurring/active? true]
                        [?e :recurring/next-occurrence ?next]
                        [(<= ?next ?now)]]
                      db now)]
    (mapv entity->recurring entities)))

(defn- entity->budget
  "Converts a Datomic entity to a budget map."
  [entity]
  (when entity
    {:budget/id (:budget/id entity)
     :budget/category (:budget/category entity)
     :budget/amount (:budget/amount entity)
     :budget/currency (or (:budget/currency entity) :COP)
     :budget/year (:budget/year entity)
     :budget/month (:budget/month entity)
     :budget/alert-threshold (or (:budget/alert-threshold entity) 0.8M)
     :budget/user-id (:budget/user-id entity)
     :budget/created-at (:budget/created-at entity)}))

(defn- budget->tx-data
  "Converts a budget map to Datomic transaction data."
  [budget]
  {:budget/id (:budget/id budget)
   :budget/category (:budget/category budget)
   :budget/amount (bigdec (:budget/amount budget))
   :budget/currency (:budget/currency budget)
   :budget/year (:budget/year budget)
   :budget/month (:budget/month budget)
   :budget/alert-threshold (bigdec (or (:budget/alert-threshold budget) 0.8))
   :budget/user-id (:budget/user-id budget)
   :budget/created-at (or (:budget/created-at budget) (java.util.Date.))})

(defn- find-budget-entity-id
  "Finds the Datomic entity ID for a budget by its UUID."
  [db uuid]
  (ffirst
   (d/q '[:find ?e
          :in $ ?id
          :where [?e :budget/id ?id]]
        db uuid)))

(defn save-budget!
  "Saves a budget to the database."
  [conn budget]
  (let [tx-data (budget->tx-data budget)]
    (safe-transact conn [tx-data] "save budget")
    budget))

(defn load-budgets-for-user
  "Loads all budgets for a specific user."
  [conn user-id]
  (let [db (d/db conn)
        entities (d/q '[:find [(pull ?e [*]) ...]
                        :in $ ?user-id
                        :where [?e :budget/user-id ?user-id]]
                      db user-id)]
    (mapv entity->budget entities)))

(defn load-budgets-for-month
  "Loads budgets for a specific user, year, and month."
  [conn user-id year month]
  (let [db (d/db conn)
        entities (d/q '[:find [(pull ?e [*]) ...]
                        :in $ ?user-id ?year ?month
                        :where
                        [?e :budget/user-id ?user-id]
                        [?e :budget/year ?year]
                        [?e :budget/month ?month]]
                      db user-id year month)]
    (mapv entity->budget entities)))

(defn load-budget-by-id
  "Loads a single budget by ID."
  [conn id]
  (let [db (d/db conn)
        entity (d/q '[:find (pull ?e [*]) .
                      :in $ ?id
                      :where [?e :budget/id ?id]]
                    db id)]
    (entity->budget entity)))

(defn update-budget!
  "Updates a budget. Returns the updated record or nil."
  [conn id updates]
  (let [db (d/db conn)
        eid (find-budget-entity-id db id)]
    (when eid
      (let [current (d/pull db '[*] eid)
            merged (merge (entity->budget current) updates)
            tx-data (budget->tx-data merged)]
        (safe-transact conn [tx-data] "update budget")
        merged))))

(defn delete-budget!
  "Deletes a budget by ID."
  [conn id]
  (let [db (d/db conn)
        eid (find-budget-entity-id db id)]
    (when eid
      (safe-transact conn [[:db/retractEntity eid]] "delete budget")
      true)))

(defn user-owns-budget?
  "Checks if a budget belongs to a user."
  [conn user-id budget-id]
  (let [db (d/db conn)]
    (some?
     (d/q '[:find ?e .
            :in $ ?budget-id ?user-id
            :where
            [?e :budget/id ?budget-id]
            [?e :budget/user-id ?user-id]]
          db budget-id user-id))))

(defn load-expenses-for-month
  "Loads expenses for a specific user, year, and month."
  [conn user-id year month]
  (let [all-txs (load-transactions-for-user conn user-id)]
    (filter (fn [tx]
              (let [date (:transaction/date tx)
                    cal (doto (java.util.Calendar/getInstance)
                          (.setTime date))]
                (and (= :expense (:transaction/type tx))
                     (= year (.get cal java.util.Calendar/YEAR))
                     (= (dec month) (.get cal java.util.Calendar/MONTH)))))
            all-txs)))

;; ============================================================================
;; Wallet Functions
;; ============================================================================

(defn- entity->wallet
  "Converts a Datomic entity to a wallet map."
  [entity]
  (when entity
    (let [base {:wallet/id (:wallet/id entity)
                :wallet/name (:wallet/name entity)
                :wallet/type (:wallet/type entity)
                :wallet/institution (:wallet/institution entity)
                :wallet/balance (or (:wallet/balance entity) 0M)
                :wallet/currency (or (:wallet/currency entity) :USD)
                :wallet/status (or (:wallet/status entity) :active)
                :wallet/user-id (:wallet/user-id entity)}]
      (cond-> base
        (:wallet/account-number entity)
        (assoc :wallet/account-number (:wallet/account-number entity))

        (:wallet/color entity)
        (assoc :wallet/color (:wallet/color entity))

        (:wallet/icon-url entity)
        (assoc :wallet/icon-url (:wallet/icon-url entity))

        (:wallet/balance-label entity)
        (assoc :wallet/balance-label (:wallet/balance-label entity))

        (:wallet/created-at entity)
        (assoc :wallet/created-at (:wallet/created-at entity))

        (:wallet/last-updated entity)
        (assoc :wallet/last-updated (:wallet/last-updated entity))))))

(defn- wallet->tx-data
  "Converts a wallet map to Datomic transaction data."
  [wallet]
  (let [base {:wallet/id (:wallet/id wallet)
              :wallet/name (:wallet/name wallet)
              :wallet/type (:wallet/type wallet)
              :wallet/institution (:wallet/institution wallet)
              :wallet/balance (bigdec (or (:wallet/balance wallet) 0))
              :wallet/currency (:wallet/currency wallet)
              :wallet/status (:wallet/status wallet)
              :wallet/user-id (:wallet/user-id wallet)
              :wallet/created-at (or (:wallet/created-at wallet) (java.util.Date.))
              :wallet/last-updated (or (:wallet/last-updated wallet) (java.util.Date.))}]
    (cond-> base
      (:wallet/account-number wallet)
      (assoc :wallet/account-number (:wallet/account-number wallet))

      (:wallet/color wallet)
      (assoc :wallet/color (:wallet/color wallet))

      (:wallet/icon-url wallet)
      (assoc :wallet/icon-url (:wallet/icon-url wallet))

      (:wallet/balance-label wallet)
      (assoc :wallet/balance-label (:wallet/balance-label wallet)))))

(defn- find-wallet-entity-id
  "Finds the Datomic entity ID for a wallet by its UUID."
  [db uuid]
  (ffirst
   (d/q '[:find ?e
          :in $ ?id
          :where [?e :wallet/id ?id]]
        db uuid)))

(defn save-wallet!
  "Saves a wallet to the database."
  [conn wallet]
  (let [tx-data (wallet->tx-data wallet)]
    (safe-transact conn [tx-data] "save wallet")
    wallet))

(defn load-wallets-for-user
  "Loads all wallets for a specific user."
  [conn user-id]
  (let [db (d/db conn)
        entities (d/q '[:find [(pull ?e [*]) ...]
                        :in $ ?user-id
                        :where [?e :wallet/user-id ?user-id]]
                      db user-id)]
    (mapv entity->wallet entities)))

(defn load-wallets-by-type
  "Loads wallets of a specific type for a user."
  [conn user-id wallet-type]
  (let [db (d/db conn)
        entities (d/q '[:find [(pull ?e [*]) ...]
                        :in $ ?user-id ?type
                        :where
                        [?e :wallet/user-id ?user-id]
                        [?e :wallet/type ?type]]
                      db user-id wallet-type)]
    (mapv entity->wallet entities)))

(defn load-wallet-by-id
  "Loads a single wallet by ID."
  [conn id]
  (let [db (d/db conn)
        entity (d/q '[:find (pull ?e [*]) .
                      :in $ ?id
                      :where [?e :wallet/id ?id]]
                    db id)]
    (entity->wallet entity)))

(defn update-wallet!
  "Updates a wallet. Returns the updated record or nil."
  [conn id updates]
  (let [db (d/db conn)
        eid (find-wallet-entity-id db id)]
    (when eid
      (let [current (d/pull db '[*] eid)
            merged (-> (entity->wallet current)
                       (merge updates)
                       (assoc :wallet/last-updated (java.util.Date.)))
            tx-data (wallet->tx-data merged)]
        (safe-transact conn [tx-data] "update wallet")
        merged))))

(defn delete-wallet!
  "Deletes a wallet by ID."
  [conn id]
  (let [db (d/db conn)
        eid (find-wallet-entity-id db id)]
    (when eid
      (safe-transact conn [[:db/retractEntity eid]] "delete wallet")
      true)))

(defn user-owns-wallet?
  "Checks if a wallet belongs to a user."
  [conn user-id wallet-id]
  (let [db (d/db conn)]
    (some?
     (d/q '[:find ?e .
            :in $ ?wallet-id ?user-id
            :where
            [?e :wallet/id ?wallet-id]
            [?e :wallet/user-id ?user-id]]
          db wallet-id user-id))))

(defn get-wallet-summary
  "Returns aggregated wallet summary for a user."
  [conn user-id]
  (let [wallets (load-wallets-for-user conn user-id)
        by-type (group-by :wallet/type wallets)
        total-by-currency (reduce (fn [acc w]
                                    (let [curr (:wallet/currency w)
                                          bal (:wallet/balance w)]
                                      (update acc curr (fnil + 0M) bal)))
                                  {}
                                  wallets)
        fiat-currencies #{:USD :COP :EUR :GBP}
        crypto-currencies #{:BTC :ETH :SOL}]
    {:total-wallets (count wallets)
     :by-type {:bank (count (:bank by-type))
               :card (count (:card by-type))
               :crypto (count (:crypto by-type))
               :investment (count (:investment by-type))}
     :total-by-currency total-by-currency
     :fiat-total (reduce + 0M (vals (select-keys total-by-currency fiat-currencies)))
     :crypto-total (reduce + 0M (vals (select-keys total-by-currency crypto-currencies)))}))
