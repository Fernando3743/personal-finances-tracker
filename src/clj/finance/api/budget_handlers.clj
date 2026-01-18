(ns finance.api.budget-handlers
  "API handlers for budgets."
  (:require [finance.domain.budget :as budget]
            [finance.storage.datomic :as db]
            [ring.util.response :as response])
  (:import [java.util Calendar]))

(defn- json-response
  ([body] (json-response body 200))
  ([body status]
   (-> (response/response body)
       (response/status status)
       (response/content-type "application/json"))))

(defn- error-response
  [message status]
  (json-response {:error message} status))

(defn- current-year-month
  []
  (let [cal (Calendar/getInstance)]
    {:year (.get cal Calendar/YEAR)
     :month (inc (.get cal Calendar/MONTH))}))

(defn list-budgets
  "GET /api/budgets - Returns current user's budgets."
  [conn request]
  (let [user-id (:user-id request)
        year (some-> (get-in request [:params :year]) Integer/parseInt)
        month (some-> (get-in request [:params :month]) Integer/parseInt)
        all-budgets (db/load-budgets-for-user conn user-id)
        filtered (if (and year month)
                   (budget/for-month all-budgets year month)
                   all-budgets)]
    (json-response {:budgets filtered
                    :count (count filtered)})))

(defn get-budget
  "GET /api/budgets/:id - Returns a single budget if owned by user."
  [conn id request]
  (let [user-id (:user-id request)
        budget-id (parse-uuid id)]
    (if (db/user-owns-budget? conn user-id budget-id)
      (if-let [item (db/load-budget-by-id conn budget-id)]
        (json-response item)
        (error-response "Budget not found" 404))
      (error-response "Budget not found" 404))))

(defn create-budget
  "POST /api/budgets - Creates a new budget for current user."
  [conn request]
  (let [user-id (:user-id request)
        {:keys [category amount currency year month alert-threshold]} (:body request)]
    (if (and category amount)
      (let [current (current-year-month)
            b (budget/create-budget
               (keyword category)
               (bigdec amount)
               {:currency (keyword (or currency "COP"))
                :year (or year (:year current))
                :month (or month (:month current))
                :alert-threshold (when alert-threshold (bigdec alert-threshold))})
            b-with-user (assoc b :budget/user-id user-id)]
        (if (budget/valid? b-with-user)
          (do
            (db/save-budget! conn b-with-user)
            (json-response b-with-user 201))
          (error-response (budget/explain-invalid b-with-user) 400)))
      (error-response "Missing required fields: category, amount" 400))))

(defn update-budget
  "PUT /api/budgets/:id - Updates a budget if owned by user."
  [conn id request]
  (let [user-id (:user-id request)
        uuid (parse-uuid id)
        updates (:body request)]
    (if (db/user-owns-budget? conn user-id uuid)
      (if-let [_existing (db/load-budget-by-id conn uuid)]
        (let [converted-updates
              (reduce-kv
               (fn [m k v]
                 (let [key-name (name k)
                       new-key (keyword "budget" key-name)]
                   (assoc m new-key
                          (case key-name
                            "category" (keyword v)
                            "currency" (keyword v)
                            "amount" (bigdec v)
                            "alert-threshold" (bigdec v)
                            "year" (int v)
                            "month" (int v)
                            v))))
               {}
               updates)]
          (if-let [updated (db/update-budget! conn uuid converted-updates)]
            (json-response updated)
            (error-response "Update failed" 500)))
        (error-response "Budget not found" 404))
      (error-response "Budget not found" 404))))

(defn delete-budget
  "DELETE /api/budgets/:id - Deletes a budget if owned by user."
  [conn id request]
  (let [user-id (:user-id request)
        uuid (parse-uuid id)]
    (if (db/user-owns-budget? conn user-id uuid)
      (if (db/delete-budget! conn uuid)
        (json-response {:deleted true})
        (error-response "Budget not found" 404))
      (error-response "Budget not found" 404))))

(defn get-budget-status
  "GET /api/budgets/status - Returns budget status with spending for current month."
  [conn request]
  (let [user-id (:user-id request)
        {:keys [year month]} (current-year-month)
        query-year (or (some-> (get-in request [:params :year]) Integer/parseInt) year)
        query-month (or (some-> (get-in request [:params :month]) Integer/parseInt) month)
        budgets (db/load-budgets-for-month conn user-id query-year query-month)
        expenses (db/load-expenses-for-month conn user-id query-year query-month)
        spending-by-category (reduce (fn [acc tx]
                                       (let [cat (:transaction/category tx)
                                             curr (:transaction/currency tx)
                                             amt (:transaction/amount tx)]
                                         (update-in acc [curr cat] (fnil + 0M) amt)))
                                     {}
                                     expenses)
        status-by-currency (reduce-kv
                            (fn [acc currency cat-spending]
                              (let [currency-budgets (budget/by-currency budgets currency)
                                    summaries (budget/budgets-status currency-budgets cat-spending)]
                                (assoc acc currency
                                       {:budgets (budget/sort-by-percentage summaries)
                                        :total-budgeted (budget/total-budgeted currency-budgets)
                                        :total-spent (budget/total-spent summaries)
                                        :exceeded-count (count (budget/exceeded-budgets summaries))
                                        :warning-count (count (budget/warning-budgets summaries))})))
                            {}
                            spending-by-category)
        budgets-without-spending (filter (fn [b]
                                           (not (get-in spending-by-category
                                                        [(:budget/currency b) (:budget/category b)])))
                                         budgets)
        zero-summaries (map #(budget/budget-summary % 0M) budgets-without-spending)]
    (json-response {:year query-year
                    :month query-month
                    :status status-by-currency
                    :budgets-with-no-spending zero-summaries
                    :all-budgets budgets})))

(defn copy-budgets-to-month
  "POST /api/budgets/copy - Copies budgets from one month to another."
  [conn request]
  (let [user-id (:user-id request)
        {:keys [from-year from-month to-year to-month]} (:body request)]
    (if (and from-year from-month to-year to-month)
      (let [source-budgets (db/load-budgets-for-month conn user-id from-year from-month)
            new-budgets (doall
                         (for [b source-budgets]
                           (let [new-budget (-> b
                                                (assoc :budget/id (random-uuid))
                                                (assoc :budget/year to-year)
                                                (assoc :budget/month to-month)
                                                (assoc :budget/created-at (java.util.Date.)))]
                             (db/save-budget! conn new-budget)
                             new-budget)))]
        (json-response {:copied (vec new-budgets)
                        :count (count new-budgets)}))
      (error-response "Missing required fields: from-year, from-month, to-year, to-month" 400))))
