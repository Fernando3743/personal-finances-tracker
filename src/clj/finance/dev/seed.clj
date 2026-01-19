(ns finance.dev.seed
  "Development seed data for testing."
  (:require [finance.storage.datomic :as db]
            [finance.domain.recurring :as recurring]
            [datomic.api :as d])
  (:import [java.util Date Calendar]))

(defn- days-from-now [n]
  (let [cal (doto (Calendar/getInstance)
              (.add Calendar/DAY_OF_MONTH n))]
    (.getTime cal)))

(def sample-recurring
  [{:description "Netflix"
    :amount 15.99
    :type :expense
    :category :entertainment
    :frequency :monthly
    :payment-method "Visa ****4242"
    :days-until-next 3}
   {:description "Spotify"
    :amount 9.99
    :type :expense
    :category :entertainment
    :frequency :monthly
    :payment-method "Visa ****4242"
    :days-until-next 7}
   {:description "AWS Hosting"
    :amount 45.00
    :type :expense
    :category :utilities
    :frequency :monthly
    :payment-method "Mastercard ****8821"
    :days-until-next 12}
   {:description "Gym Membership"
    :amount 49.99
    :type :expense
    :category :healthcare
    :frequency :monthly
    :payment-method "Bank Transfer"
    :days-until-next 5}
   {:description "Phone Bill"
    :amount 65.00
    :type :expense
    :category :utilities
    :frequency :monthly
    :payment-method "Auto-debit"
    :days-until-next 10}
   {:description "Rent"
    :amount 1500.00
    :type :expense
    :category :housing
    :frequency :monthly
    :payment-method "Wells Fargo Checking"
    :days-until-next 1}
   {:description "Car Insurance"
    :amount 180.00
    :type :expense
    :category :transportation
    :frequency :monthly
    :payment-method "Visa ****4242"
    :days-until-next 15}
   {:description "Disney+"
    :amount 7.99
    :type :expense
    :category :entertainment
    :frequency :monthly
    :payment-method "PayPal"
    :days-until-next 20}
   {:description "Internet"
    :amount 79.99
    :type :expense
    :category :utilities
    :frequency :monthly
    :payment-method "Auto-debit"
    :days-until-next 8}
   {:description "Salary"
    :amount 5000.00
    :type :income
    :category :salary
    :frequency :monthly
    :payment-method "Direct Deposit"
    :days-until-next 14}
   {:description "Freelance Retainer"
    :amount 1500.00
    :type :income
    :category :salary
    :frequency :monthly
    :payment-method "Bank Transfer"
    :days-until-next 0}
   {:description "Domain Renewal"
    :amount 14.99
    :type :expense
    :category :utilities
    :frequency :yearly
    :payment-method "Visa ****4242"
    :days-until-next 45}])

(defn seed-recurring!
  "Seeds recurring transactions for a user.
   Usage: (seed-recurring! conn user-id)"
  [conn user-id]
  (println (str "Seeding " (count sample-recurring) " recurring transactions for user " user-id))
  (doseq [{:keys [description amount type category frequency payment-method days-until-next]} sample-recurring]
    (let [start-date (Date.)
          next-occurrence (days-from-now days-until-next)
          rec (-> (recurring/create-recurring
                   (bigdec amount)
                   type
                   category
                   frequency
                   {:description description
                    :currency :USD
                    :start-date start-date
                    :active? true
                    :payment-method payment-method})
                  (assoc :recurring/user-id user-id
                         :recurring/next-occurrence next-occurrence))]
      (db/save-recurring! conn rec)
      (println (str "  Created: " description " (" (name frequency) ", next in " days-until-next " days)"))))
  (println "Done seeding recurring transactions!"))

(defn clear-recurring!
  "Clears all recurring transactions for a user.
   Usage: (clear-recurring! conn user-id)"
  [conn user-id]
  (let [items (db/load-recurring-for-user conn user-id)]
    (println (str "Deleting " (count items) " recurring transactions for user " user-id))
    (doseq [item items]
      (db/delete-recurring! conn (:recurring/id item)))
    (println "Done clearing recurring transactions!")))

(defn get-first-user-id
  "Gets the first user ID in the database for testing."
  [conn]
  (ffirst
   (d/q '[:find ?id
          :where [?e :user/id ?id]]
        (d/db conn))))

(comment
  ;; To seed data, run these in the REPL:

  ;; 1. Connect to database
  (def conn (db/create-conn "datomic:dev://localhost:4334/finance?password=admin"))

  ;; 2. Get a user ID (or use your own)
  (def user-id (get-first-user-id conn))

  ;; 3. Clear existing and seed new data
  (clear-recurring! conn user-id)
  (seed-recurring! conn user-id)

  ;; 4. Verify
  (db/load-recurring-for-user conn user-id)
  )
