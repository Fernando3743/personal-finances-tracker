(ns finance.rf-logic.transactions
  "Transactions page logic - CRUD, filtering, form management."
  (:require [re-frame.core :as rf]
            [clojure.string :as str]
            [finance.db :as db]
            [day8.re-frame.http-fx]
            [ajax.core :as ajax]
            [finance.utils.errors :as errors]
            [finance.utils.filters :as filters]
            [finance.utils.date :as date-utils]
            [finance.constants :as const]))

(rf/reg-event-fx
 :transactions/init
 (fn [_ _]
   {:dispatch [:tx/fetch-transactions]}))

(rf/reg-event-fx
 :tx/fetch-transactions
 (fn [{:keys [db]} _]
   {:db (-> db
            (assoc :loading? true)
            (assoc-in [:transactions-state :error] nil))
    :http-xhrio {:method :get
                 :uri "/api/transactions"
                 :with-credentials true
                 :response-format (ajax/json-response-format {:keywords? true})
                 :on-success [:tx/fetch-transactions-success]
                 :on-failure [:tx/fetch-transactions-failure]}}))

(rf/reg-event-fx
 :tx/fetch-transactions-failure
 (fn [{:keys [db]} [_ error]]
   (errors/log-error "Transactions fetch" error)
   {:db (-> db
            (assoc :loading? false)
            (assoc-in [:transactions-state :error]
                      (errors/extract-error-message error "Failed to load transactions")))
    :dispatch [:app/show-toast
               {:type :error
                :title "Error"
                :message "Failed to load transactions. Please try again."}]}))

(rf/reg-event-db
 :tx/fetch-transactions-success
 (fn [db [_ response]]
   (-> db
       (assoc :loading? false)
       (assoc :error nil)
       (assoc :transactions (:transactions response)))))

(rf/reg-event-fx
 :tx/create-transaction
 (fn [{:keys [db]} _]
   (let [form (:transaction-form db)
         is-recurring? (:is-recurring form)
         payload (cond-> {:amount (js/parseFloat (:amount form))
                          :type (name (:type form))
                          :category (name (:category form))
                          :description (:description form)
                          :tags (mapv name (:tags form))
                          :currency (name (or (:currency form) :COP))}
                   (:exchange-rate form)
                   (assoc :exchange-rate (:exchange-rate form))
                   (not (str/blank? (:notes form)))
                   (assoc :notes (:notes form)))]
     {:db (-> db
              (assoc :loading? true)
              (assoc-in [:transaction-form :pending-recurring?] is-recurring?))
      :http-xhrio {:method :post
                   :uri "/api/transactions"
                   :params payload
                   :format (ajax/json-request-format)
                   :with-credentials true
                   :response-format (ajax/json-response-format {:keywords? true})
                   :on-success [:tx/create-transaction-success]
                   :on-failure [:app/api-error]}})))

(rf/reg-event-fx
 :tx/create-transaction-success
 (fn [{:keys [db]} [_ _response]]
   (let [was-recurring? (get-in db [:transaction-form :pending-recurring?])
         form (:transaction-form db)]
     {:db (-> db
              (assoc :loading? false)
              (assoc :error nil)
              (assoc :transaction-form (:transaction-form db/default-db))
              (assoc-in [:panel :open?] false))
      :dispatch-n (cond-> [[:tx/fetch-transactions]
                           [:dashboard/fetch-summary]
                           [:app/show-toast
                            {:type :success
                             :title "Transaction Added"
                             :message "Your transaction was saved successfully."}]]
                    was-recurring?
                    (conj [:recurring/create-from-transaction form]))})))

(rf/reg-event-fx
 :tx/delete-transaction
 (fn [{:keys [db]} [_ id]]
   {:db (assoc db :loading? true)
    :http-xhrio {:method :delete
                 :uri (str "/api/transactions/" id)
                 :format (ajax/json-request-format)
                 :with-credentials true
                 :response-format (ajax/json-response-format {:keywords? true})
                 :on-success [:tx/delete-transaction-success]
                 :on-failure [:app/api-error]}}))

(rf/reg-event-fx
 :tx/delete-transaction-success
 (fn [{:keys [db]} _]
   {:db (assoc db :loading? false)
    :dispatch-n [[:tx/fetch-transactions]
                 [:dashboard/fetch-summary]
                 [:app/show-toast
                  {:type :success
                   :title "Transaction Deleted"
                   :message "The transaction was removed."}]]}))

(rf/reg-event-db
 :tx/update-filter
 (fn [db [_ field value]]
   (assoc-in db [:filter field] value)))

(rf/reg-event-db
 :tx/clear-filters
 (fn [db _]
   (assoc db :filter (:filter db/default-db))))

(rf/reg-event-db
 :tx/update-form-field
 (fn [db [_ field value]]
   (assoc-in db [:transaction-form field] value)))

(rf/reg-event-db
 :tx/reset-form
 (fn [db _]
   (assoc db :transaction-form (:transaction-form db/default-db))))

(rf/reg-sub
 :tx/transactions
 (fn [db _]
   (:transactions db)))

(rf/reg-sub
 :tx/error
 (fn [db _]
   (get-in db [:transactions-state :error])))

(rf/reg-sub
 :tx/transaction-count
 :<- [:tx/transactions]
 (fn [transactions _]
   (count transactions)))

(rf/reg-sub
 :tx/recent-transactions
 :<- [:tx/transactions]
 (fn [transactions _]
   (take const/recent-transactions-limit transactions)))

(rf/reg-sub
 :tx/filter
 (fn [db _]
   (:filter db)))

(rf/reg-sub
 :tx/filter-search
 :<- [:tx/filter]
 (fn [fltr _]
   (:search fltr)))

(rf/reg-sub
 :tx/filter-type
 :<- [:tx/filter]
 (fn [fltr _]
   (:type fltr)))

(rf/reg-sub
 :tx/filter-category
 :<- [:tx/filter]
 (fn [fltr _]
   (:category fltr)))

(rf/reg-sub
 :tx/filter-currency
 :<- [:tx/filter]
 (fn [fltr _]
   (:currency fltr)))

(rf/reg-sub
 :tx/has-active-filters?
 :<- [:tx/filter]
 (fn [fltr _]
   (filters/has-active-filters? fltr)))

(rf/reg-sub
 :tx/filtered-transactions
 :<- [:tx/transactions]
 :<- [:tx/filter]
 (fn [[transactions fltr] _]
   (filters/apply-filters transactions fltr)))

(rf/reg-sub
 :tx/transactions-by-date
 :<- [:tx/filtered-transactions]
 (fn [transactions _]
   (group-by (fn [tx]
               (date-utils/format-date (:transaction/date tx)))
             transactions)))

(rf/reg-sub
 :tx/transaction-form
 (fn [db _]
   (:transaction-form db)))

(rf/reg-sub
 :tx/form-field
 :<- [:tx/transaction-form]
 (fn [form [_ field]]
   (get form field)))

(rf/reg-sub
 :tx/categories
 (fn [db _]
   (:categories db)))

(rf/reg-sub
 :tx/form-valid?
 :<- [:tx/transaction-form]
 (fn [form _]
   (let [{:keys [amount type category]} form]
     (and (not (empty? (str amount)))
          (some? type)
          (some? category)
          (pos? (js/parseFloat amount))))))

(rf/reg-sub
 :tx/form-amount-display
 :<- [:tx/transaction-form]
 (fn [form _]
   (let [amount (:amount form)]
     (if (str/blank? (str amount))
       "0.00"
       (let [parsed (js/parseFloat amount)]
         (if (js/isNaN parsed)
           "0.00"
           (.toFixed parsed 2)))))))

(rf/reg-sub
 :tx/form-currency
 :<- [:tx/transaction-form]
 (fn [form _]
   (or (:currency form) :COP)))

(rf/reg-sub
 :tx/form-preview
 :<- [:tx/transaction-form]
 (fn [form _]
   (let [amount-str (:amount form)
         amount (when (and amount-str (not (str/blank? (str amount-str))))
                  (let [parsed (js/parseFloat amount-str)]
                    (when-not (js/isNaN parsed) parsed)))
         today (js/Date.)]
     {:amount (or amount 0)
      :type (or (:type form) :expense)
      :category (or (:category form) :other)
      :description (or (:description form) "")
      :currency (or (:currency form) :COP)
      :date (or (:date form)
                (.toLocaleDateString today "en-US" #js {:month "short" :day "numeric"}))})))

(rf/reg-sub
 :tx/form-notes
 :<- [:tx/transaction-form]
 (fn [form _]
   (or (:notes form) "")))

(rf/reg-sub
 :tx/form-is-recurring
 :<- [:tx/transaction-form]
 (fn [form _]
   (boolean (:is-recurring form))))
