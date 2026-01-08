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
                 [::fetch-summary]
                 [::load-theme]]}))

;; =============================================================================
;; Theme Events
;; =============================================================================

(rf/reg-event-fx
 ::load-theme
 (fn [{:keys [db]} _]
   (let [saved-theme (-> js/localStorage (.getItem "theme"))
         theme (if saved-theme (keyword saved-theme) :light)]
     {:db (assoc db :theme theme)
      :dispatch [::apply-theme theme]})))

(rf/reg-event-fx
 ::toggle-theme
 (fn [{:keys [db]} _]
   (let [current-theme (:theme db)
         new-theme (if (= current-theme :light) :dark :light)]
     {:db (assoc db :theme new-theme)
      :dispatch [::apply-theme new-theme]})))

(rf/reg-event-fx
 ::apply-theme
 (fn [_ [_ theme]]
   {:fx [[:set-theme theme]]}))

;; Effect handler for setting theme
(rf/reg-fx
 :set-theme
 (fn [theme]
   (.setItem js/localStorage "theme" (name theme))
   (.setAttribute (.-documentElement js/document) "data-theme" (name theme))))

;; =============================================================================
;; Navigation
;; =============================================================================

(rf/reg-event-db
 ::set-active-view
 (fn [db [_ view]]
   (assoc db :active-view view)))

;; =============================================================================
;; Panel Events (for side panel)
;; =============================================================================

(rf/reg-event-db
 ::open-panel
 (fn [db [_ mode data]]
   (-> db
       (assoc-in [:panel :open?] true)
       (assoc-in [:panel :mode] mode)
       (assoc-in [:panel :data] data))))

(rf/reg-event-db
 ::close-panel
 (fn [db _]
   (-> db
       (assoc-in [:panel :open?] false)
       (assoc-in [:panel :mode] nil)
       (assoc-in [:panel :data] nil))))

;; =============================================================================
;; Filter Events
;; =============================================================================

(rf/reg-event-db
 ::update-filter
 (fn [db [_ field value]]
   (assoc-in db [:filter field] value)))

(rf/reg-event-db
 ::clear-filters
 (fn [db _]
   (assoc db :filter (:filter db/default-db))))

;; =============================================================================
;; Toast Events
;; =============================================================================

(rf/reg-event-fx
 ::show-toast
 (fn [{:keys [db]} [_ toast]]
   (let [id (random-uuid)
         toast-with-id (assoc toast :id id)]
     {:db (update db :toast-queue conj toast-with-id)
      :dispatch-later [{:ms (or (:duration toast) 5000)
                        :dispatch [::dismiss-toast id]}]})))

(rf/reg-event-db
 ::dismiss-toast
 (fn [db [_ id]]
   (update db :toast-queue (fn [q] (filterv #(not= (:id %) id) q)))))

;; =============================================================================
;; Chart Events
;; =============================================================================

(rf/reg-event-db
 ::set-chart-range
 (fn [db [_ range]]
   (assoc db :chart-time-range range)))

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
                   :on-success [::create-transaction-success]
                   :on-failure [::api-error]}})))

(rf/reg-event-fx
 ::create-transaction-success
 (fn [{:keys [db]} [_ _response]]
   {:db (-> db
            (assoc :loading? false)
            (assoc :error nil)
            (assoc :transaction-form (:transaction-form db/default-db))
            (assoc-in [:panel :open?] false)
            (assoc :active-view :transactions))
    :dispatch-n [[::fetch-transactions]
                 [::fetch-summary]
                 [::show-toast {:type :success
                                :title "Transaction Added"
                                :message "Your transaction was saved successfully."}]]}))

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
                 [::fetch-summary]
                 [::show-toast {:type :success
                                :title "Transaction Deleted"
                                :message "The transaction was removed."}]]}))

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

(rf/reg-event-fx
 ::api-error
 (fn [{:keys [db]} [_ error]]
   {:db (-> db
            (assoc :loading? false)
            (assoc :error (or (get-in error [:response :error])
                              "An error occurred")))
    :dispatch [::show-toast {:type :error
                             :title "Error"
                             :message (or (get-in error [:response :error])
                                          "An error occurred")}]}))

(rf/reg-event-db
 ::clear-error
 (fn [db _]
   (assoc db :error nil)))
