(ns finance.domain.recurring
  "Pure domain logic for recurring transactions."
  (:require [clojure.spec.alpha :as s])
  (:import [java.util Calendar Date]))

(s/def :recurring/id uuid?)
(s/def :recurring/amount number?)
(s/def :recurring/type #{:income :expense})
(s/def :recurring/category keyword?)
(s/def :recurring/description (s/and string? #(<= (count %) 500)))
(s/def :recurring/currency #{:COP :USD})
(s/def :recurring/frequency #{:daily :weekly :monthly :yearly})
(s/def :recurring/start-date inst?)
(s/def :recurring/end-date inst?)
(s/def :recurring/next-occurrence inst?)
(s/def :recurring/last-generated inst?)
(s/def :recurring/active? boolean?)
(s/def :recurring/user-id uuid?)
(s/def :recurring/payment-method (s/and string? #(<= (count %) 100)))

(s/def ::recurring
  (s/keys :req [:recurring/id
                :recurring/amount
                :recurring/type
                :recurring/category
                :recurring/currency
                :recurring/frequency
                :recurring/start-date
                :recurring/active?
                :recurring/user-id]
          :opt [:recurring/description
                :recurring/end-date
                :recurring/next-occurrence
                :recurring/last-generated
                :recurring/payment-method]))

(def frequencies
  #{:daily :weekly :monthly :yearly})

(defn calculate-next-occurrence
  "Calculates the next occurrence date from a given date based on frequency."
  [from-date frequency]
  (let [cal (doto (Calendar/getInstance)
              (.setTime from-date))]
    (case frequency
      :daily (.add cal Calendar/DAY_OF_MONTH 1)
      :weekly (.add cal Calendar/WEEK_OF_YEAR 1)
      :monthly (.add cal Calendar/MONTH 1)
      :yearly (.add cal Calendar/YEAR 1))
    (.getTime cal)))

(defn create-recurring
  "Creates a new recurring transaction template."
  ([amount type category frequency]
   (create-recurring amount type category frequency {}))
  ([amount type category frequency {:keys [description currency start-date end-date active? payment-method]}]
   (let [start (or start-date (Date.))
         next-occ (calculate-next-occurrence start frequency)]
     (cond-> {:recurring/id (random-uuid)
              :recurring/amount (abs amount)
              :recurring/type type
              :recurring/category category
              :recurring/currency (or currency :COP)
              :recurring/frequency frequency
              :recurring/start-date start
              :recurring/next-occurrence next-occ
              :recurring/active? (if (nil? active?) true active?)
              :recurring/created-at (Date.)}
       description (assoc :recurring/description description)
       end-date (assoc :recurring/end-date end-date)
       payment-method (assoc :recurring/payment-method payment-method)))))

(defn valid?
  "Validates a recurring transaction against spec."
  [recurring]
  (s/valid? ::recurring recurring))

(defn explain-invalid
  "Returns explanation if recurring transaction is invalid, nil otherwise."
  [recurring]
  (when-not (valid? recurring)
    (s/explain-str ::recurring recurring)))

(defn should-generate?
  "Checks if a transaction should be generated from this recurring template."
  [recurring now]
  (and (:recurring/active? recurring)
       (let [next-occ (:recurring/next-occurrence recurring)]
         (and next-occ
              (not (.after next-occ now))))
       (if-let [end-date (:recurring/end-date recurring)]
         (not (.after now end-date))
         true)))

(defn generate-transaction
  "Creates a transaction from a recurring template."
  [recurring]
  {:transaction/id (random-uuid)
   :transaction/amount (:recurring/amount recurring)
   :transaction/type (:recurring/type recurring)
   :transaction/category (:recurring/category recurring)
   :transaction/date (Date.)
   :transaction/currency (:recurring/currency recurring)
   :transaction/description (or (:recurring/description recurring) "")
   :transaction/tags #{}
   :transaction/recurring-id (:recurring/id recurring)})

(defn advance-next-occurrence
  "Updates the recurring template with the next occurrence after generation."
  [recurring]
  (let [current-next (:recurring/next-occurrence recurring)
        new-next (calculate-next-occurrence current-next (:recurring/frequency recurring))]
    (assoc recurring
           :recurring/next-occurrence new-next
           :recurring/last-generated (Date.))))

(defn by-type
  "Filters recurring transactions by type."
  [recurrings type]
  (filter #(= type (:recurring/type %)) recurrings))

(defn active-only
  "Filters to only active recurring transactions."
  [recurrings]
  (filter :recurring/active? recurrings))

(defn inactive-only
  "Filters to only inactive recurring transactions."
  [recurrings]
  (filter #(not (:recurring/active? %)) recurrings))

(defn by-frequency
  "Filters recurring transactions by frequency."
  [recurrings frequency]
  (filter #(= frequency (:recurring/frequency %)) recurrings))

(defn sort-by-next-occurrence
  "Sorts recurring transactions by next occurrence date."
  ([recurrings]
   (sort-by-next-occurrence recurrings :asc))
  ([recurrings order]
   (let [comparator (if (= order :asc) compare #(compare %2 %1))]
     (sort-by :recurring/next-occurrence comparator recurrings))))

(defn upcoming
  "Returns recurring transactions that will occur within n days."
  [recurrings n]
  (let [cal (doto (Calendar/getInstance)
              (.add Calendar/DAY_OF_MONTH n))
        limit (.getTime cal)]
    (->> recurrings
         active-only
         (filter #(when-let [next-occ (:recurring/next-occurrence %)]
                    (not (.after next-occ limit))))
         (sort-by-next-occurrence :asc))))
