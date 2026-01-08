(ns finance.rf-logic.transactions
  "Transactions page logic - CRUD, filtering, form management."
  (:require [re-frame.core :as rf]
            [clojure.string :as str]
            [finance.db :as db]
            [day8.re-frame.http-fx]
            [ajax.core :as ajax]))

(def api-base "http://localhost:3000/api")

(rf/reg-event-fx
 :tx/fetch-transactions
 (fn [{:keys [db]} _]
   {:db (assoc db :loading? true)
    :http-xhrio {:method :get
                 :uri (str api-base "/transactions")
                 :response-format (ajax/json-response-format {:keywords? true})
                 :on-success [:tx/fetch-transactions-success]
                 :on-failure [:app/api-error]}}))

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
         payload (cond-> {:amount (js/parseFloat (:amount form))
                          :type (name (:type form))
                          :category (name (:category form))
                          :description (:description form)
                          :tags (mapv name (:tags form))
                          :currency (name (or (:currency form) :COP))}
                   (:exchange-rate form)
                   (assoc :exchange-rate (:exchange-rate form)))]
     {:db (assoc db :loading? true)
      :http-xhrio {:method :post
                   :uri (str api-base "/transactions")
                   :params payload
                   :format (ajax/json-request-format)
                   :response-format (ajax/json-response-format {:keywords? true})
                   :on-success [:tx/create-transaction-success]
                   :on-failure [:app/api-error]}})))

(rf/reg-event-fx
 :tx/create-transaction-success
 (fn [{:keys [db]} [_ _response]]
   {:db (-> db
            (assoc :loading? false)
            (assoc :error nil)
            (assoc :transaction-form (:transaction-form db/default-db))
            (assoc-in [:panel :open?] false))
    :fx [[:dispatch [:app/navigate :transactions]]]
    :dispatch-n [[:tx/fetch-transactions]
                 [:dashboard/fetch-summary]
                 [:app/show-toast
                  {:type :success
                   :title "Transaction Added"
                   :message "Your transaction was saved successfully."}]]}))

(rf/reg-event-fx
 :tx/delete-transaction
 (fn [{:keys [db]} [_ id]]
   {:db (assoc db :loading? true)
    :http-xhrio {:method :delete
                 :uri (str api-base "/transactions/" id)
                 :format (ajax/json-request-format)
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
 :tx/transaction-count
 :<- [:tx/transactions]
 (fn [transactions _]
   (count transactions)))

(rf/reg-sub
 :tx/recent-transactions
 :<- [:tx/transactions]
 (fn [transactions _]
   (take 5 transactions)))

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
   (or (not (str/blank? (:search fltr)))
       (some? (:type fltr))
       (some? (:category fltr))
       (some? (:currency fltr)))))

(rf/reg-sub
 :tx/filtered-transactions
 :<- [:tx/transactions]
 :<- [:tx/filter]
 (fn [[transactions fltr] _]
   (let [{:keys [search type category currency sort-by sort-dir]} fltr]
     (cond->> transactions
       (some? currency)
       (filter #(= (:transaction/currency %) currency))

       (not (str/blank? search))
       (filter #(or (str/includes? (str/lower-case (or (:transaction/description %) ""))
                                   (str/lower-case search))
                    (str/includes? (str/lower-case (name (or (:transaction/category %) :other)))
                                   (str/lower-case search))))

       (some? type)
       (filter #(= (:transaction/type %) type))

       (some? category)
       (filter #(= (:transaction/category %) category))

       true
       (sort-by (case sort-by
                  :date :transaction/date
                  :amount :transaction/amount
                  :category :transaction/category
                  :transaction/date))

       (= sort-dir :desc)
       reverse))))

(rf/reg-sub
 :tx/transactions-by-date
 :<- [:tx/filtered-transactions]
 (fn [transactions _]
   (group-by (fn [tx]
               (when-let [date (:transaction/date tx)]
                 (let [d (js/Date. date)]
                   (.toLocaleDateString d "en-US" #js {:year "numeric"
                                                       :month "short"
                                                       :day "numeric"}))))
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
