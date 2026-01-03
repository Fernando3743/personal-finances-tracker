(ns finance.storage.edn
  "EDN file-based storage implementation.
   Demonstrates Open/Closed principle - can add new storage backends
   without changing domain logic."
  (:require [finance.storage.protocol :as proto]
            [clojure.edn :as edn]
            [clojure.java.io :as io]))

(defn- ensure-file-exists!
  "Creates the data file if it doesn't exist."
  [file-path]
  (let [file (io/file file-path)]
    (when-not (.exists file)
      (io/make-parents file)
      (spit file "[]"))
    file-path))

(defn- read-edn-file
  "Reads and parses an EDN file."
  [file-path]
  (try
    (let [content (slurp file-path)]
      (if (empty? content)
        []
        (edn/read-string content)))
    (catch Exception _
      [])))

(defn- write-edn-file
  "Writes data to an EDN file with pretty printing."
  [file-path data]
  (spit file-path (pr-str data)))

(defrecord EDNStore [file-path]
  proto/TransactionStore

  (save-transaction! [_ transaction]
    (ensure-file-exists! file-path)
    (let [transactions (read-edn-file file-path)
          updated (conj transactions transaction)]
      (write-edn-file file-path updated)
      transaction))

  (load-transactions [_]
    (ensure-file-exists! file-path)
    (read-edn-file file-path))

  (load-transaction [_ id]
    (ensure-file-exists! file-path)
    (let [transactions (read-edn-file file-path)]
      (first (filter #(= id (:transaction/id %)) transactions))))

  (update-transaction! [_ id updates]
    (ensure-file-exists! file-path)
    (let [transactions (read-edn-file file-path)
          idx (first (keep-indexed
                      (fn [i tx]
                        (when (= id (:transaction/id tx)) i))
                      transactions))]
      (when idx
        (let [updated-tx (merge (nth transactions idx) updates)
              updated-list (assoc (vec transactions) idx updated-tx)]
          (write-edn-file file-path updated-list)
          updated-tx))))

  (delete-transaction! [_ id]
    (ensure-file-exists! file-path)
    (let [transactions (read-edn-file file-path)
          original-count (count transactions)
          filtered (vec (remove #(= id (:transaction/id %)) transactions))]
      (when (< (count filtered) original-count)
        (write-edn-file file-path filtered)
        true))))

(defn create-store
  "Factory function to create an EDN store."
  [file-path]
  (->EDNStore file-path))
