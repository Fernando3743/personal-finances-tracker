(ns finance.migration
  "Data migration utilities for schema evolution."
  (:require [datomic.api :as d]))

(defn migrate-add-currency!
  "Migrates existing transactions to add :COP currency.
   Run this once after schema update.
   Returns the count of migrated transactions."
  [conn]
  (let [db (d/db conn)
        entities-without-currency
        (d/q '[:find ?e
               :where
               [?e :transaction/id]
               (not [?e :transaction/currency])]
             db)]
    (if (seq entities-without-currency)
      (do
        @(d/transact conn
           (mapv (fn [[eid]]
                   {:db/id eid
                    :transaction/currency :COP})
                 entities-without-currency))
        (count entities-without-currency))
      0)))

(defn check-migration-status
  "Checks how many transactions are missing currency field.
   Returns {:total n :missing-currency n :migrated n}"
  [conn]
  (let [db (d/db conn)
        total (ffirst
               (d/q '[:find (count ?e)
                      :where [?e :transaction/id]]
                    db))
        missing (count
                 (d/q '[:find ?e
                        :where
                        [?e :transaction/id]
                        (not [?e :transaction/currency])]
                      db))]
    {:total (or total 0)
     :missing-currency missing
     :migrated (- (or total 0) missing)}))
