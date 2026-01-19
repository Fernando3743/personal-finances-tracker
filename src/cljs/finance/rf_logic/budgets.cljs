(ns finance.rf-logic.budgets
  "Budgets page logic - CRUD for budgets and status tracking."
  (:require [re-frame.core :as rf]
            [finance.db :as db]
            [day8.re-frame.http-fx]
            [ajax.core :as ajax]
            [finance.utils.errors :as errors]
            [finance.constants :as const]))

(rf/reg-event-fx
 :budgets/init
 (fn [_ _]
   {:dispatch-n [[:budgets/fetch]
                 [:budgets/fetch-status]]}))

(rf/reg-event-fx
 :budgets/fetch
 (fn [{:keys [db]} _]
   {:db (assoc-in db [:budgets :loading?] true)
    :http-xhrio {:method :get
                 :uri "/api/budgets"
                 :with-credentials true
                 :response-format (ajax/json-response-format {:keywords? true})
                 :on-success [:budgets/fetch-success]
                 :on-failure [:budgets/fetch-failure]}}))

(rf/reg-event-fx
 :budgets/fetch-success
 (fn [{:keys [db]} [_ response]]
   {:db (-> db
            (assoc-in [:budgets :error] nil)
            (assoc-in [:budgets :items] (:budgets response)))
    :dispatch [:budgets/fetch-status]}))

(rf/reg-event-db
 :budgets/fetch-failure
 (fn [db [_ error]]
   (-> db
       (assoc-in [:budgets :loading?] false)
       (assoc-in [:budgets :error] (errors/extract-error-message error "Failed to load budgets")))))

(rf/reg-event-fx
 :budgets/fetch-status
 (fn [{:keys [db]} [_ year month]]
   (let [params (cond-> {}
                  year (assoc :year year)
                  month (assoc :month month))]
     {:db (assoc-in db [:budgets :loading?] true)
      :http-xhrio {:method :get
                   :uri "/api/budgets/status"
                   :params params
                   :with-credentials true
                   :response-format (ajax/json-response-format {:keywords? true})
                   :on-success [:budgets/fetch-status-success]
                   :on-failure [:budgets/fetch-status-failure]}})))

(rf/reg-event-db
 :budgets/fetch-status-success
 (fn [db [_ response]]
   (-> db
       (assoc-in [:budgets :loading?] false)
       (assoc-in [:budgets :status] response))))

(rf/reg-event-db
 :budgets/fetch-status-failure
 (fn [db [_ error]]
   (-> db
       (assoc-in [:budgets :loading?] false)
       (assoc-in [:budgets :error] (errors/extract-error-message error "Failed to load budget status")))))

(rf/reg-event-fx
 :budgets/create
 (fn [{:keys [db]} _]
   (let [form (:budget-form db)
         payload {:category (name (:category form))
                  :amount (js/parseFloat (:amount form))
                  :currency (name (or (:currency form) :COP))
                  :alert-threshold (/ (:alert-threshold form) 100)}]
     {:db (assoc-in db [:budgets :loading?] true)
      :http-xhrio {:method :post
                   :uri "/api/budgets"
                   :params payload
                   :format (ajax/json-request-format)
                   :with-credentials true
                   :response-format (ajax/json-response-format {:keywords? true})
                   :on-success [:budgets/create-success]
                   :on-failure [:budgets/create-failure]}})))

(rf/reg-event-fx
 :budgets/create-success
 (fn [{:keys [db]} _]
   {:db (-> db
            (assoc-in [:budgets :loading?] false)
            (assoc-in [:budgets-panel :open?] false)
            (assoc-in [:budgets-panel :mode] nil)
            (assoc :budget-form (:budget-form db/default-db)))
    :dispatch-n [[:budgets/fetch]
                 [:app/show-toast
                  {:type :success
                   :title "Budget Created"
                   :message "Your budget was created successfully."}]]}))

(rf/reg-event-fx
 :budgets/create-failure
 (fn [{:keys [db]} [_ error]]
   {:db (-> db
            (assoc-in [:budgets :loading?] false)
            (assoc-in [:budgets :error] (errors/extract-error-message error "Failed to create budget")))
    :dispatch [:app/show-toast
               {:type :error
                :title "Error"
                :message "Failed to create budget."}]}))

(rf/reg-event-fx
 :budgets/update
 (fn [{:keys [db]} [_ id updates]]
   {:db (assoc-in db [:budgets :loading?] true)
    :http-xhrio {:method :put
                 :uri (str "/api/budgets/" id)
                 :params updates
                 :format (ajax/json-request-format)
                 :with-credentials true
                 :response-format (ajax/json-response-format {:keywords? true})
                 :on-success [:budgets/update-success]
                 :on-failure [:budgets/update-failure]}}))

(rf/reg-event-fx
 :budgets/update-success
 (fn [{:keys [db]} _]
   {:db (-> db
            (assoc-in [:budgets :loading?] false)
            (assoc-in [:budgets-panel :open?] false)
            (assoc-in [:budgets-panel :mode] nil)
            (assoc-in [:budgets :selected-budget-id] nil)
            (assoc :budget-form (:budget-form db/default-db)))
    :dispatch-n [[:budgets/fetch]
                 [:app/show-toast
                  {:type :success
                   :title "Updated"
                   :message "Budget updated."}]]}))

(rf/reg-event-fx
 :budgets/update-failure
 (fn [{:keys [db]} [_ error]]
   {:db (-> db
            (assoc-in [:budgets :loading?] false)
            (assoc-in [:budgets :error] (errors/extract-error-message error "Failed to update")))
    :dispatch [:app/show-toast
               {:type :error
                :title "Error"
                :message "Failed to update budget."}]}))

(rf/reg-event-fx
 :budgets/delete
 (fn [{:keys [db]} [_ id]]
   {:db (assoc-in db [:budgets :loading?] true)
    :http-xhrio {:method :delete
                 :uri (str "/api/budgets/" id)
                 :format (ajax/json-request-format)
                 :with-credentials true
                 :response-format (ajax/json-response-format {:keywords? true})
                 :on-success [:budgets/delete-success]
                 :on-failure [:budgets/delete-failure]}}))

(rf/reg-event-fx
 :budgets/delete-success
 (fn [{:keys [db]} _]
   {:db (assoc-in db [:budgets :loading?] false)
    :dispatch-n [[:budgets/fetch]
                 [:app/show-toast
                  {:type :success
                   :title "Deleted"
                   :message "Budget deleted."}]]}))

(rf/reg-event-fx
 :budgets/delete-failure
 (fn [{:keys [db]} [_ error]]
   {:db (-> db
            (assoc-in [:budgets :loading?] false)
            (assoc-in [:budgets :error] (errors/extract-error-message error "Failed to delete")))
    :dispatch [:app/show-toast
               {:type :error
                :title "Error"
                :message "Failed to delete budget."}]}))

(rf/reg-event-db
 :budgets/update-form-field
 (fn [db [_ field value]]
   (assoc-in db [:budget-form field] value)))

(rf/reg-event-db
 :budgets/reset-form
 (fn [db _]
   (assoc db :budget-form (:budget-form db/default-db))))

;; Panel Management Events
(rf/reg-event-fx
 :budgets/open-panel
 (fn [{:keys [db]} [_ mode budget-id]]
   (let [fx {:db (-> db
                     (assoc-in [:budgets-panel :open?] true)
                     (assoc-in [:budgets-panel :mode] mode)
                     (assoc-in [:budgets :selected-budget-id] budget-id))}]
     (if (and (= mode :edit) budget-id)
       (assoc fx :dispatch [:budgets/prepare-edit budget-id])
       fx))))

(rf/reg-event-db
 :budgets/close-panel
 (fn [db _]
   (-> db
       (assoc-in [:budgets-panel :open?] false)
       (assoc-in [:budgets-panel :mode] nil)
       (assoc-in [:budgets :selected-budget-id] nil)
       (assoc :budget-form (:budget-form db/default-db)))))

;; View & Filter Management Events
(rf/reg-event-db
 :budgets/set-view-mode
 (fn [db [_ mode]]
   (assoc-in db [:budgets :view-mode] mode)))

(rf/reg-event-db
 :budgets/set-search-query
 (fn [db [_ query]]
   (assoc-in db [:budgets :search-query] query)))

(rf/reg-event-db
 :budgets/set-filter-status
 (fn [db [_ status]]
   (assoc-in db [:budgets :filter-status] status)))

(rf/reg-event-db
 :budgets/set-time-period
 (fn [db [_ period]]
   (assoc-in db [:budgets :time-period] period)))

(rf/reg-event-db
 :budgets/set-comparison-month
 (fn [db [_ month]]
   (assoc-in db [:budgets :comparison-month] month)))

;; Edit Preparation Event
(rf/reg-event-db
 :budgets/prepare-edit
 (fn [db [_ budget-id]]
   (let [budget (some #(when (= (:budget/id %) budget-id) %) (get-in db [:budgets :items] []))
         form {:category (:budget/category budget)
               :amount (str (:budget/amount budget))
               :currency (:budget/currency budget)
               :alert-threshold (int (* 100 (or (:budget/alert-threshold budget) 0.8)))
               :budget-type (or (:budget/budget-type budget) :fixed)
               :notes (or (:budget/notes budget) "")}]
     (assoc db :budget-form form))))

(rf/reg-sub
 :budgets/items
 (fn [db _]
   (get-in db [:budgets :items] [])))

(rf/reg-sub
 :budgets/loading?
 (fn [db _]
   (get-in db [:budgets :loading?] false)))

(rf/reg-sub
 :budgets/error
 (fn [db _]
   (get-in db [:budgets :error])))

(rf/reg-sub
 :budgets/status
 (fn [db _]
   (get-in db [:budgets :status] {})))

(rf/reg-sub
 :budgets/status-year
 :<- [:budgets/status]
 (fn [status _]
   (:year status)))

(rf/reg-sub
 :budgets/status-month
 :<- [:budgets/status]
 (fn [status _]
   (:month status)))

(rf/reg-sub
 :budgets/status-by-currency
 :<- [:budgets/status]
 (fn [status _]
   (:status status {})))

(rf/reg-sub
 :budgets/all-budgets
 :<- [:budgets/status]
 (fn [status _]
   (:all-budgets status [])))

(rf/reg-sub
 :budgets/by-category
 :<- [:budgets/items]
 (fn [items _]
   (group-by :budget/category items)))

(rf/reg-sub
 :budgets/exceeded
 :<- [:budgets/status-by-currency]
 (fn [by-currency _]
   (reduce-kv (fn [acc _curr data]
                (+ acc (or (:exceeded-count data) 0)))
              0
              by-currency)))

(rf/reg-sub
 :budgets/warning
 :<- [:budgets/status-by-currency]
 (fn [by-currency _]
   (reduce-kv (fn [acc _curr data]
                (+ acc (or (:warning-count data) 0)))
              0
              by-currency)))

(rf/reg-sub
 :budgets/form
 (fn [db _]
   (:budget-form db)))

(rf/reg-sub
 :budgets/form-field
 :<- [:budgets/form]
 (fn [form [_ field]]
   (get form field)))

(rf/reg-sub
 :budgets/form-valid?
 :<- [:budgets/form]
 (fn [form _]
   (let [{:keys [category amount]} form]
     (and (some? category)
          (not (empty? (str amount)))
          (pos? (js/parseFloat amount))))))

(rf/reg-sub
 :budgets/count
 :<- [:budgets/items]
 (fn [items _]
   (count items)))

(rf/reg-sub
 :budgets/total-budgeted
 :<- [:budgets/all-budgets]
 (fn [budgets _]
   (reduce + 0 (map :budget/amount budgets))))

(rf/reg-sub
 :budgets/total-spent
 :<- [:budgets/status-by-currency]
 (fn [by-currency _]
   (reduce-kv (fn [acc _curr data]
                (+ acc (or (:total-spent data) 0)))
              0
              by-currency)))

(rf/reg-sub
 :budgets/budget-summaries
 :<- [:budgets/status-by-currency]
 (fn [by-currency _]
   (->> by-currency
        vals
        (mapcat :budgets)
        (sort-by :percentage >))))

;; View State Subscriptions
(rf/reg-sub
 :budgets/view-mode
 (fn [db _]
   (get-in db [:budgets :view-mode] :grid)))

(rf/reg-sub
 :budgets/search-query
 (fn [db _]
   (get-in db [:budgets :search-query] "")))

(rf/reg-sub
 :budgets/filter-status
 (fn [db _]
   (get-in db [:budgets :filter-status] :all)))

(rf/reg-sub
 :budgets/time-period
 (fn [db _]
   (get-in db [:budgets :time-period] :this-month)))

(rf/reg-sub
 :budgets/panel-open?
 (fn [db _]
   (get-in db [:budgets-panel :open?] false)))

(rf/reg-sub
 :budgets/panel-mode
 (fn [db _]
   (get-in db [:budgets-panel :mode])))

(rf/reg-sub
 :budgets/selected-budget-id
 (fn [db _]
   (get-in db [:budgets :selected-budget-id])))

(rf/reg-sub
 :budgets/selected-budget
 :<- [:budgets/all-budgets]
 :<- [:budgets/selected-budget-id]
 (fn [[budgets id] _]
   (some #(when (= (:budget/id %) id) %) budgets)))

;; Filtered & Sorted Data Subscriptions
(rf/reg-sub
 :budgets/filtered-budgets
 :<- [:budgets/budget-summaries]
 :<- [:budgets/search-query]
 :<- [:budgets/filter-status]
 (fn [[summaries query filter-status] _]
   (let [filtered (if (and query (not (empty? query)))
                    (filter #(clojure.string/includes?
                              (clojure.string/lower-case (name (get-in % [:budget :budget/category])))
                              (clojure.string/lower-case query))
                            summaries)
                    summaries)
         status-filtered (case filter-status
                           :on-track (filter #(= :ok (:status %)) filtered)
                           :near-limit (filter #(= :warning (:status %)) filtered)
                           :over-budget (filter #(= :exceeded (:status %)) filtered)
                           :all filtered)]
     status-filtered)))

(rf/reg-sub
 :budgets/by-status
 :<- [:budgets/budget-summaries]
 (fn [summaries _]
   (group-by :status summaries)))

;; Insights & Analytics Subscriptions
(rf/reg-sub
 :budgets/top-saver
 :<- [:budgets/budget-summaries]
 (fn [summaries _]
   (->> summaries
        (filter #(and (= :ok (:status %))
                      (pos? (:remaining %))))
        (sort-by :remaining >)
        first)))

(rf/reg-sub
 :budgets/watch-out
 :<- [:budgets/budget-summaries]
 (fn [summaries _]
   (->> summaries
        (filter #(or (= :exceeded (:status %))
                     (= :warning (:status %))))
        (sort-by :percentage >)
        first)))

(rf/reg-sub
 :budgets/days-left-in-month
 (fn [_ _]
   (let [now (js/Date.)
         year (.getFullYear now)
         month (.getMonth now)
         last-day (.getDate (js/Date. year (inc month) 0))
         current-day (.getDate now)]
     (- last-day current-day))))

(rf/reg-sub
 :budgets/daily-avg-remaining
 :<- [:budgets/total-budgeted]
 :<- [:budgets/total-spent]
 :<- [:budgets/days-left-in-month]
 (fn [[total-budgeted total-spent days-left] _]
   (let [remaining (- total-budgeted total-spent)]
     (if (pos? days-left)
       (/ remaining days-left)
       0))))
