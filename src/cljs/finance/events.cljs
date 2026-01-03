(ns finance.events
  "Re-frame event handlers."
  (:require [re-frame.core :as rf]
            [finance.db :as db]
            [day8.re-frame.http-fx]
            [ajax.core :as ajax]))

(def api-base "http://localhost:3000/api")

;; =============================================================================
;; Initialization
;; =============================================================================

(rf/reg-event-db
 ::initialize-db
 (fn [_ _]
   db/default-db))

(rf/reg-event-fx
 ::initialize-app
 (fn [{:keys [db]} _]
   {:db (assoc db :loading? true)
    :dispatch-n [[::fetch-transactions]
                 [::fetch-summary]]}))

;; =============================================================================
;; Navigation
;; =============================================================================

(rf/reg-event-db
 ::set-active-view
 (fn [db [_ view]]
   (assoc db :active-view view)))

;; =============================================================================
;; Transaction CRUD Events
;; =============================================================================

(rf/reg-event-fx
 ::fetch-transactions
 (fn [{:keys [db]} _]
   {:db (assoc db :loading? true)
    :http-xhrio {:method :get
                 :uri (str api-base "/transactions")
                 :response-format (ajax/json-response-format {:keywords? true})
                 :on-success [::fetch-transactions-success]
                 :on-failure [::api-error]}}))

(rf/reg-event-db
 ::fetch-transactions-success
 (fn [db [_ response]]
   (-> db
       (assoc :loading? false)
       (assoc :error nil)
       (assoc :transactions (:transactions response)))))

(rf/reg-event-fx
 ::create-transaction
 (fn [{:keys [db]} _]
   (let [form (:transaction-form db)
         payload {:amount (js/parseFloat (:amount form))
                  :type (name (:type form))
                  :category (name (:category form))
                  :description (:description form)
                  :tags (mapv name (:tags form))}]
     {:db (assoc db :loading? true)
      :http-xhrio {:method :post
                   :uri (str api-base "/transactions")
                   :params payload
                   :format (ajax/json-request-format)
                   :response-format (ajax/json-response-format {:keywords? true})
                   :on-success [::create-transaction-success]
                   :on-failure [::api-error]}})))

(rf/reg-event-fx
 ::create-transaction-success
 (fn [{:keys [db]} [_ _response]]
   {:db (-> db
            (assoc :loading? false)
            (assoc :error nil)
            (assoc :transaction-form (:transaction-form db/default-db))
            (assoc :active-view :transactions))
    :dispatch-n [[::fetch-transactions]
                 [::fetch-summary]]}))

(rf/reg-event-fx
 ::delete-transaction
 (fn [{:keys [db]} [_ id]]
   {:db (assoc db :loading? true)
    :http-xhrio {:method :delete
                 :uri (str api-base "/transactions/" id)
                 :format (ajax/json-request-format)
                 :response-format (ajax/json-response-format {:keywords? true})
                 :on-success [::delete-transaction-success]
                 :on-failure [::api-error]}}))

(rf/reg-event-fx
 ::delete-transaction-success
 (fn [{:keys [db]} _]
   {:db (assoc db :loading? false)
    :dispatch-n [[::fetch-transactions]
                 [::fetch-summary]]}))

;; =============================================================================
;; Summary Events
;; =============================================================================

(rf/reg-event-fx
 ::fetch-summary
 (fn [{:keys [_db]} _]
   {:http-xhrio {:method :get
                 :uri (str api-base "/summary")
                 :response-format (ajax/json-response-format {:keywords? true})
                 :on-success [::fetch-summary-success]
                 :on-failure [::api-error]}}))

(rf/reg-event-db
 ::fetch-summary-success
 (fn [db [_ response]]
   (assoc db :summary response)))

(rf/reg-event-fx
 ::fetch-dashboard
 (fn [{:keys [db]} _]
   {:db (assoc db :loading? true)
    :http-xhrio {:method :get
                 :uri (str api-base "/dashboard")
                 :response-format (ajax/json-response-format {:keywords? true})
                 :on-success [::fetch-dashboard-success]
                 :on-failure [::api-error]}}))

(rf/reg-event-db
 ::fetch-dashboard-success
 (fn [db [_ response]]
   (-> db
       (assoc :loading? false)
       (assoc :summary (:balance response))
       (assoc :transactions (:recent-transactions response))
       (assoc :category-breakdown (:category-breakdown response))
       (assoc :monthly-report (:monthly-trend response)))))

;; =============================================================================
;; Form Events
;; =============================================================================

(rf/reg-event-db
 ::update-form-field
 (fn [db [_ field value]]
   (assoc-in db [:transaction-form field] value)))

(rf/reg-event-db
 ::reset-form
 (fn [db _]
   (assoc db :transaction-form (:transaction-form db/default-db))))

;; =============================================================================
;; Error Handling
;; =============================================================================

(rf/reg-event-db
 ::api-error
 (fn [db [_ error]]
   (-> db
       (assoc :loading? false)
       (assoc :error (or (get-in error [:response :error])
                         "An error occurred")))))

(rf/reg-event-db
 ::clear-error
 (fn [db _]
   (assoc db :error nil)))
