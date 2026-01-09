(ns finance.storage.datomic
  "Datomic storage implementation."
  (:require [datomic.api :as d]
            [clojure.string :as str]))

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
    :db/doc "Account creation timestamp"}])

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
    :db/doc "Owner user ID"}])

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
  "Saves a transaction to the database. Returns the saved transaction."
  [conn transaction]
  (let [tx-data (transaction->tx-data transaction)]
    @(d/transact conn [tx-data])
    transaction))

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
  "Updates a transaction. Returns the updated transaction or nil."
  [conn id updates]
  (let [db (d/db conn)
        eid (find-entity-id db id)]
    (when eid
      (let [current (d/pull db '[*] eid)
            merged (merge (entity->transaction current) updates)
            tx-data (transaction->tx-data merged)]
        @(d/transact conn [tx-data])
        merged))))

(defn delete-transaction!
  "Deletes a transaction by ID. Returns true if deleted, false otherwise."
  [conn id]
  (let [db (d/db conn)
        eid (find-entity-id db id)]
    (when eid
      @(d/transact conn [[:db/retractEntity eid]])
      true)))

(defn create-conn
  "Creates a Datomic connection.
   Creates the database if it doesn't exist and transacts the schema."
  [uri]
  (d/create-database uri)
  (let [conn (d/connect uri)]
    @(d/transact conn user-schema)
    @(d/transact conn schema)
    conn))

(defn- entity->user
  "Converts a Datomic entity to a user map."
  [entity]
  (when entity
    {:user/id (:user/id entity)
     :user/email (:user/email entity)
     :user/password-hash (:user/password-hash entity)
     :user/name (:user/name entity)
     :user/created-at (:user/created-at entity)}))

(defn save-user!
  "Saves a user to the database. Returns the saved user."
  [conn user]
  (let [tx-data {:user/id (:user/id user)
                 :user/email (str/lower-case (:user/email user))
                 :user/password-hash (:user/password-hash user)
                 :user/name (:user/name user)
                 :user/created-at (or (:user/created-at user) (java.util.Date.))}]
    @(d/transact conn [tx-data])
    user))

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
