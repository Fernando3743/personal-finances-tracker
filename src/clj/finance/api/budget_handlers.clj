(ns finance.api.budget-handlers
  "API handlers for budgets."
  (:require [finance.domain.budget :as budget]
            [finance.storage.datomic :as db]
            [finance.api.common :as common]
            [clojure.tools.logging :as log]))

(defn list-budgets
  "GET /api/budgets - Returns current user's budgets."
  [conn request]
  (let [user-id (:user-id request)
        year (common/safe-parse-int (get-in request [:params :year]))
        month (common/safe-parse-int (get-in request [:params :month]))]
    (if (or (nil? year) (nil? month)
            (and (common/validate-year year) (common/validate-month month)))
      (let [all-budgets (db/load-budgets-for-user conn user-id)
            filtered (if (and year month)
                       (budget/for-month all-budgets year month)
                       all-budgets)]
        (log/info "Listed budgets for user" user-id "year" year "month" month "count" (count filtered))
        (common/json-response {:budgets filtered
                               :count (count filtered)}))
      (common/error-response "Invalid year or month parameters" 400))))

(defn get-budget
  "GET /api/budgets/:id - Returns a single budget if owned by user."
  [conn id request]
  (let [user-id (:user-id request)
        budget-id (common/safe-parse-uuid id)]
    (if-not budget-id
      (common/error-response "Invalid budget ID format" 400)
      (if (db/user-owns-budget? conn user-id budget-id)
        (if-let [item (db/load-budget-by-id conn budget-id)]
          (do
            (log/info "Retrieved budget" budget-id "for user" user-id)
            (common/json-response item))
          (common/error-response "Budget not found" 404))
        (do
          (log/warn "Unauthorized budget access attempt: user" user-id "budget" budget-id)
          (common/error-response "Budget not found" 404))))))

(defn create-budget
  "POST /api/budgets - Creates a new budget for current user."
  [conn request]
  (let [user-id (:user-id request)
        {:keys [category amount currency year month alert-threshold]} (:body request)]
    (if (and category amount)
      (let [parsed-year (when year (int year))
            parsed-month (when month (int month))
            current (budget/current-year-month)
            final-year (or parsed-year (:year current))
            final-month (or parsed-month (:month current))]
        (if (and (common/validate-year final-year) (common/validate-month final-month))
          (let [b (budget/create-budget
                   (keyword category)
                   (bigdec amount)
                   {:currency (keyword (or currency "COP"))
                    :year final-year
                    :month final-month
                    :alert-threshold (when alert-threshold (bigdec alert-threshold))})
                b-with-user (assoc b :budget/user-id user-id)]
            (if (budget/valid? b-with-user)
              (do
                (db/save-budget! conn b-with-user)
                (log/info "Created budget" (:budget/id b-with-user) "for user" user-id
                          "category" category "year" final-year "month" final-month)
                (common/json-response b-with-user 201))
              (do
                (log/warn "Invalid budget creation attempt:" (budget/explain-invalid b-with-user))
                (common/error-response (budget/explain-invalid b-with-user) 400))))
          (common/error-response "Invalid year or month values" 400)))
      (common/error-response "Missing required fields: category, amount" 400))))

(defn update-budget
  "PUT /api/budgets/:id - Updates a budget if owned by user."
  [conn id request]
  (let [user-id (:user-id request)
        uuid (common/safe-parse-uuid id)
        updates (:body request)]
    (if-not uuid
      (common/error-response "Invalid budget ID format" 400)
      (if (db/user-owns-budget? conn user-id uuid)
        (if-let [_existing (db/load-budget-by-id conn uuid)]
          (let [converted-updates (common/convert-updates "budget" updates)
                year (:budget/year converted-updates)
                month (:budget/month converted-updates)]
            (if (or (and year (not (common/validate-year year)))
                    (and month (not (common/validate-month month))))
              (common/error-response "Invalid year or month values" 400)
              (if-let [updated (db/update-budget! conn uuid converted-updates)]
                (do
                  (log/info "Updated budget" uuid "for user" user-id)
                  (common/json-response updated))
                (do
                  (log/error "Failed to update budget" uuid)
                  (common/error-response "Update failed" 500)))))
          (common/error-response "Budget not found" 404))
        (do
          (log/warn "Unauthorized budget update attempt: user" user-id "budget" uuid)
          (common/error-response "Budget not found" 404))))))

(defn delete-budget
  "DELETE /api/budgets/:id - Deletes a budget if owned by user."
  [conn id request]
  (let [user-id (:user-id request)
        uuid (common/safe-parse-uuid id)]
    (if-not uuid
      (common/error-response "Invalid budget ID format" 400)
      (if (db/user-owns-budget? conn user-id uuid)
        (if (db/delete-budget! conn uuid)
          (do
            (log/info "Deleted budget" uuid "for user" user-id)
            (common/json-response {:deleted true}))
          (common/error-response "Budget not found" 404))
        (do
          (log/warn "Unauthorized budget deletion attempt: user" user-id "budget" uuid)
          (common/error-response "Budget not found" 404))))))

(defn get-budget-status
  "GET /api/budgets/status - Returns budget status with spending for current month."
  [conn request]
  (let [user-id (:user-id request)
        {:keys [year month]} (budget/current-year-month)
        query-year-str (get-in request [:params :year])
        query-month-str (get-in request [:params :month])
        query-year (or (common/safe-parse-int query-year-str) year)
        query-month (or (common/safe-parse-int query-month-str) month)]
    (if (and (common/validate-year query-year) (common/validate-month query-month))
      (let [budgets (db/load-budgets-for-month conn user-id query-year query-month)
            expenses (db/load-expenses-for-month conn user-id query-year query-month)
            spending-by-category (reduce (fn [acc tx]
                                           (let [cat (:transaction/category tx)
                                                 curr (:transaction/currency tx)
                                                 amt (:transaction/amount tx)]
                                             (update-in acc [curr cat] (fnil + 0M) amt)))
                                         {}
                                         expenses)
            budgets-by-currency (group-by :budget/currency budgets)
            status-by-currency (reduce-kv
                                (fn [acc currency currency-budgets]
                                  (let [cat-spending (get spending-by-category currency {})
                                        summaries (budget/budgets-status currency-budgets cat-spending)]
                                    (assoc acc currency
                                           {:budgets (budget/sort-by-percentage summaries)
                                            :total-budgeted (budget/total-budgeted currency-budgets)
                                            :total-spent (budget/total-spent summaries)
                                            :exceeded-count (count (budget/exceeded-budgets summaries))
                                            :warning-count (count (budget/warning-budgets summaries))})))
                                {}
                                budgets-by-currency)]
        (log/info "Retrieved budget status for user" user-id "year" query-year "month" query-month)
        (common/json-response {:year query-year
                               :month query-month
                               :status status-by-currency
                               :all-budgets budgets}))
      (common/error-response "Invalid year or month parameters" 400))))

(defn copy-budgets-to-month
  "POST /api/budgets/copy - Copies budgets from one month to another."
  [conn request]
  (let [user-id (:user-id request)
        {:keys [from-year from-month to-year to-month]} (:body request)]
    (if (and from-year from-month to-year to-month)
      (let [parsed-from-year (int from-year)
            parsed-from-month (int from-month)
            parsed-to-year (int to-year)
            parsed-to-month (int to-month)]
        (if (and (common/validate-year parsed-from-year)
                 (common/validate-month parsed-from-month)
                 (common/validate-year parsed-to-year)
                 (common/validate-month parsed-to-month))
          (let [source-budgets (db/load-budgets-for-month conn user-id parsed-from-year parsed-from-month)
                new-budgets (doall
                             (for [b source-budgets]
                               (let [new-budget (-> b
                                                    (assoc :budget/id (random-uuid))
                                                    (assoc :budget/year parsed-to-year)
                                                    (assoc :budget/month parsed-to-month)
                                                    (assoc :budget/created-at (java.util.Date.)))]
                                 (db/save-budget! conn new-budget)
                                 new-budget)))]
            (log/info "Copied" (count new-budgets) "budgets for user" user-id
                      "from" parsed-from-year "/" parsed-from-month
                      "to" parsed-to-year "/" parsed-to-month)
            (common/json-response {:copied (vec new-budgets)
                                   :count (count new-budgets)}))
          (common/error-response "Invalid year or month values" 400)))
      (common/error-response "Missing required fields: from-year, from-month, to-year, to-month" 400))))
