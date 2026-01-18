(ns finance.rf-logic.recurring
  "Recurring transactions page logic - CRUD for recurring templates."
  (:require [re-frame.core :as rf]
            [finance.db :as db]
            [day8.re-frame.http-fx]
            [ajax.core :as ajax]))

(def api-base "http://localhost:3000/api")

(rf/reg-event-fx
 :recurring/init
 (fn [_ _]
   {:dispatch [:recurring/fetch]}))

(rf/reg-event-fx
 :recurring/fetch
 (fn [{:keys [db]} _]
   {:db (assoc-in db [:recurring :loading?] true)
    :http-xhrio {:method :get
                 :uri (str api-base "/recurring")
                 :with-credentials true
                 :response-format (ajax/json-response-format {:keywords? true})
                 :on-success [:recurring/fetch-success]
                 :on-failure [:recurring/fetch-failure]}}))

(rf/reg-event-db
 :recurring/fetch-success
 (fn [db [_ response]]
   (-> db
       (assoc-in [:recurring :loading?] false)
       (assoc-in [:recurring :error] nil)
       (assoc-in [:recurring :items] (:recurring response)))))

(rf/reg-event-db
 :recurring/fetch-failure
 (fn [db [_ error]]
   (-> db
       (assoc-in [:recurring :loading?] false)
       (assoc-in [:recurring :error] (get-in error [:response :error] "Failed to load recurring transactions")))))

(rf/reg-event-fx
 :recurring/create
 (fn [{:keys [db]} _]
   (let [form (:recurring-form db)
         payload {:amount (js/parseFloat (:amount form))
                  :type (name (:type form))
                  :category (name (:category form))
                  :description (:description form)
                  :currency (name (or (:currency form) :COP))
                  :frequency (name (:frequency form))
                  :start-date (when (:start-date form) (.getTime (js/Date. (:start-date form))))
                  :end-date (when (:end-date form) (.getTime (js/Date. (:end-date form))))
                  :active (:active form)}]
     {:db (assoc-in db [:recurring :loading?] true)
      :http-xhrio {:method :post
                   :uri (str api-base "/recurring")
                   :params payload
                   :format (ajax/json-request-format)
                   :with-credentials true
                   :response-format (ajax/json-response-format {:keywords? true})
                   :on-success [:recurring/create-success]
                   :on-failure [:recurring/create-failure]}})))

(rf/reg-event-fx
 :recurring/create-success
 (fn [{:keys [db]} _]
   {:db (-> db
            (assoc-in [:recurring :loading?] false)
            (assoc :recurring-form (:recurring-form db/default-db)))
    :dispatch-n [[:recurring/fetch]
                 [:app/show-toast
                  {:type :success
                   :title "Recurring Created"
                   :message "Your recurring transaction was created successfully."}]]}))

(rf/reg-event-fx
 :recurring/create-failure
 (fn [{:keys [db]} [_ error]]
   {:db (-> db
            (assoc-in [:recurring :loading?] false)
            (assoc-in [:recurring :error] (get-in error [:response :error] "Failed to create")))
    :dispatch [:app/show-toast
               {:type :error
                :title "Error"
                :message "Failed to create recurring transaction."}]}))

(rf/reg-event-fx
 :recurring/update
 (fn [{:keys [db]} [_ id updates]]
   {:db (assoc-in db [:recurring :loading?] true)
    :http-xhrio {:method :put
                 :uri (str api-base "/recurring/" id)
                 :params updates
                 :format (ajax/json-request-format)
                 :with-credentials true
                 :response-format (ajax/json-response-format {:keywords? true})
                 :on-success [:recurring/update-success]
                 :on-failure [:recurring/update-failure]}}))

(rf/reg-event-fx
 :recurring/update-success
 (fn [{:keys [db]} _]
   {:db (assoc-in db [:recurring :loading?] false)
    :dispatch-n [[:recurring/fetch]
                 [:app/show-toast
                  {:type :success
                   :title "Updated"
                   :message "Recurring transaction updated."}]]}))

(rf/reg-event-fx
 :recurring/update-failure
 (fn [{:keys [db]} [_ error]]
   {:db (-> db
            (assoc-in [:recurring :loading?] false)
            (assoc-in [:recurring :error] (get-in error [:response :error] "Failed to update")))
    :dispatch [:app/show-toast
               {:type :error
                :title "Error"
                :message "Failed to update recurring transaction."}]}))

(rf/reg-event-fx
 :recurring/delete
 (fn [{:keys [db]} [_ id]]
   {:db (assoc-in db [:recurring :loading?] true)
    :http-xhrio {:method :delete
                 :uri (str api-base "/recurring/" id)
                 :format (ajax/json-request-format)
                 :with-credentials true
                 :response-format (ajax/json-response-format {:keywords? true})
                 :on-success [:recurring/delete-success]
                 :on-failure [:recurring/delete-failure]}}))

(rf/reg-event-fx
 :recurring/delete-success
 (fn [{:keys [db]} _]
   {:db (assoc-in db [:recurring :loading?] false)
    :dispatch-n [[:recurring/fetch]
                 [:app/show-toast
                  {:type :success
                   :title "Deleted"
                   :message "Recurring transaction deleted."}]]}))

(rf/reg-event-fx
 :recurring/delete-failure
 (fn [{:keys [db]} [_ error]]
   {:db (-> db
            (assoc-in [:recurring :loading?] false)
            (assoc-in [:recurring :error] (get-in error [:response :error] "Failed to delete")))
    :dispatch [:app/show-toast
               {:type :error
                :title "Error"
                :message "Failed to delete recurring transaction."}]}))

(rf/reg-event-fx
 :recurring/toggle-active
 (fn [_ [_ id]]
   {:http-xhrio {:method :post
                 :uri (str api-base "/recurring/" id "/toggle")
                 :format (ajax/json-request-format)
                 :with-credentials true
                 :response-format (ajax/json-response-format {:keywords? true})
                 :on-success [:recurring/toggle-success]
                 :on-failure [:recurring/toggle-failure]}}))

(rf/reg-event-fx
 :recurring/toggle-success
 (fn [_ _]
   {:dispatch-n [[:recurring/fetch]
                 [:app/show-toast
                  {:type :success
                   :title "Updated"
                   :message "Status toggled."}]]}))

(rf/reg-event-fx
 :recurring/toggle-failure
 (fn [_ [_ error]]
   {:dispatch [:app/show-toast
               {:type :error
                :title "Error"
                :message (get-in error [:response :error] "Failed to toggle status.")}]}))

(rf/reg-event-fx
 :recurring/generate-now
 (fn [{:keys [db]} _]
   {:db (assoc-in db [:recurring :loading?] true)
    :http-xhrio {:method :post
                 :uri (str api-base "/recurring/generate")
                 :format (ajax/json-request-format)
                 :with-credentials true
                 :response-format (ajax/json-response-format {:keywords? true})
                 :on-success [:recurring/generate-success]
                 :on-failure [:recurring/generate-failure]}}))

(rf/reg-event-fx
 :recurring/generate-success
 (fn [{:keys [db]} [_ response]]
   (let [count (:count response)]
     {:db (assoc-in db [:recurring :loading?] false)
      :dispatch-n [[:recurring/fetch]
                   [:tx/fetch-transactions]
                   [:dashboard/fetch-summary]
                   [:app/show-toast
                    {:type :success
                     :title "Generated"
                     :message (str count " transaction(s) generated.")}]]})))

(rf/reg-event-fx
 :recurring/generate-failure
 (fn [{:keys [db]} [_ error]]
   {:db (assoc-in db [:recurring :loading?] false)
    :dispatch [:app/show-toast
               {:type :error
                :title "Error"
                :message (get-in error [:response :error] "Failed to generate transactions.")}]}))

(rf/reg-event-db
 :recurring/update-form-field
 (fn [db [_ field value]]
   (assoc-in db [:recurring-form field] value)))

(rf/reg-event-db
 :recurring/reset-form
 (fn [db _]
   (assoc db :recurring-form (:recurring-form db/default-db))))

(rf/reg-sub
 :recurring/items
 (fn [db _]
   (get-in db [:recurring :items] [])))

(rf/reg-sub
 :recurring/loading?
 (fn [db _]
   (get-in db [:recurring :loading?] false)))

(rf/reg-sub
 :recurring/error
 (fn [db _]
   (get-in db [:recurring :error])))

(rf/reg-sub
 :recurring/active-items
 :<- [:recurring/items]
 (fn [items _]
   (filter #(:recurring/active? %) items)))

(rf/reg-sub
 :recurring/inactive-items
 :<- [:recurring/items]
 (fn [items _]
   (filter #(not (:recurring/active? %)) items)))

(rf/reg-sub
 :recurring/by-type
 :<- [:recurring/items]
 (fn [items _]
   (group-by :recurring/type items)))

(rf/reg-sub
 :recurring/income-items
 :<- [:recurring/by-type]
 (fn [by-type _]
   (get by-type :income [])))

(rf/reg-sub
 :recurring/expense-items
 :<- [:recurring/by-type]
 (fn [by-type _]
   (get by-type :expense [])))

(rf/reg-sub
 :recurring/form
 (fn [db _]
   (:recurring-form db)))

(rf/reg-sub
 :recurring/form-field
 :<- [:recurring/form]
 (fn [form [_ field]]
   (get form field)))

(rf/reg-sub
 :recurring/form-valid?
 :<- [:recurring/form]
 (fn [form _]
   (let [{:keys [amount type category frequency]} form]
     (and (not (empty? (str amount)))
          (some? type)
          (some? category)
          (some? frequency)
          (pos? (js/parseFloat amount))))))

(rf/reg-sub
 :recurring/count
 :<- [:recurring/items]
 (fn [items _]
   (count items)))

(rf/reg-sub
 :recurring/sorted-by-next
 :<- [:recurring/active-items]
 (fn [items _]
   (sort-by :recurring/next-occurrence items)))

(rf/reg-sub
 :recurring/upcoming
 :<- [:recurring/sorted-by-next]
 (fn [items _]
   (take 5 items)))
