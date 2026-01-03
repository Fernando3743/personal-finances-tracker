(ns finance.storage.protocol
  "Storage protocol definition.
   Demonstrates Dependency Inversion - domain code depends on this abstraction,
   not on concrete implementations like EDN or Datomic.")

(defprotocol TransactionStore
  "Protocol for transaction persistence.
   Implementations must handle CRUD operations for transactions."

  (save-transaction! [store transaction]
    "Saves a transaction to the store. Returns the saved transaction.")

  (load-transactions [store]
    "Loads all transactions from the store. Returns a vector.")

  (load-transaction [store id]
    "Loads a single transaction by ID. Returns nil if not found.")

  (update-transaction! [store id updates]
    "Updates a transaction. Returns the updated transaction or nil.")

  (delete-transaction! [store id]
    "Deletes a transaction by ID. Returns true if deleted, false otherwise."))
