(ns finance.rf-logic.recurring
  "Recurring transactions page logic - CRUD for recurring templates."
  (:require [re-frame.core :as rf]
            [finance.db :as db]
            [day8.re-frame.http-fx]
            [ajax.core :as ajax]
            [finance.utils.errors :as errors]
            [finance.utils.date :as date-utils]
            [finance.constants :as const]))

(rf/reg-event-fx
 :recurring/init
 (fn [_ _]
   {:dispatch [:recurring/fetch]}))

(rf/reg-event-fx
 :recurring/fetch
 (fn [{:keys [db]} _]
   {:db (assoc-in db [:recurring :loading?] true)
    :http-xhrio {:method :get
                 :uri "/api/recurring"
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
       (assoc-in [:recurring :error] (errors/extract-error-message error "Failed to load recurring transactions")))))

(rf/reg-event-fx
 :recurring/create
 (fn [{:keys [db]} _]
   (let [form (:recurring-form db)
         payload {:amount (js/parseFloat (:amount form))
                  :type (name (:type form))
                  :category (name (:category form))
                  :description (or (:name form) (:description form))
                  :currency (name (or (:currency form) :COP))
                  :frequency (name (:frequency form))
                  :start-date (when (:start-date form) (.getTime (js/Date. (:start-date form))))
                  :end-date (when (:end-date form) (.getTime (js/Date. (:end-date form))))
                  :active (:active form)
                  :payment-method (:payment-method form)}]
     {:db (assoc-in db [:recurring :loading?] true)
      :http-xhrio {:method :post
                   :uri "/api/recurring"
                   :params payload
                   :format (ajax/json-request-format)
                   :with-credentials true
                   :response-format (ajax/json-response-format {:keywords? true})
                   :on-success [:recurring/create-success]
                   :on-failure [:recurring/create-failure]}})))

(rf/reg-event-fx
 :recurring/create-from-transaction
 (fn [{:keys [_db]} [_ tx-form]]
   (let [payload {:amount (js/parseFloat (:amount tx-form))
                  :type (name (:type tx-form))
                  :category (name (:category tx-form))
                  :description (:description tx-form)
                  :currency (name (or (:currency tx-form) :COP))
                  :frequency "monthly"
                  :start-date (.getTime (js/Date.))
                  :active true}]
     {:http-xhrio {:method :post
                   :uri "/api/recurring"
                   :params payload
                   :format (ajax/json-request-format)
                   :with-credentials true
                   :response-format (ajax/json-response-format {:keywords? true})
                   :on-success [:recurring/create-from-tx-success]
                   :on-failure [:recurring/create-failure]}})))

(rf/reg-event-fx
 :recurring/create-from-tx-success
 (fn [_ _]
   {:dispatch-n [[:recurring/fetch]
                 [:app/show-toast
                  {:type :success
                   :title "Recurring Created"
                   :message "This transaction will repeat monthly."}]]}))

(rf/reg-event-fx
 :recurring/create-success
 (fn [{:keys [db]} _]
   {:db (-> db
            (assoc-in [:recurring :loading?] false)
            (assoc-in [:recurring-panel :open?] false)
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
            (assoc-in [:recurring :error] (errors/extract-error-message error "Failed to create")))
    :dispatch [:app/show-toast
               {:type :error
                :title "Error"
                :message "Failed to create recurring transaction."}]}))

(rf/reg-event-fx
 :recurring/update
 (fn [{:keys [db]} [_ id updates]]
   {:db (assoc-in db [:recurring :loading?] true)
    :http-xhrio {:method :put
                 :uri (str "/api/recurring/" id)
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
            (assoc-in [:recurring :error] (errors/extract-error-message error "Failed to update")))
    :dispatch [:app/show-toast
               {:type :error
                :title "Error"
                :message "Failed to update recurring transaction."}]}))

(rf/reg-event-fx
 :recurring/delete
 (fn [{:keys [db]} [_ id]]
   {:db (assoc-in db [:recurring :loading?] true)
    :http-xhrio {:method :delete
                 :uri (str "/api/recurring/" id)
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
            (assoc-in [:recurring :error] (errors/extract-error-message error "Failed to delete")))
    :dispatch [:app/show-toast
               {:type :error
                :title "Error"
                :message "Failed to delete recurring transaction."}]}))

(rf/reg-event-fx
 :recurring/toggle-active
 (fn [{:keys [db]} [_ id]]
   {:db (assoc-in db [:recurring :toggling?] true)
    :http-xhrio {:method :post
                 :uri (str "/api/recurring/" id "/toggle")
                 :format (ajax/json-request-format)
                 :with-credentials true
                 :response-format (ajax/json-response-format {:keywords? true})
                 :on-success [:recurring/toggle-success]
                 :on-failure [:recurring/toggle-failure]}}))

(rf/reg-event-fx
 :recurring/toggle-success
 (fn [{:keys [db]} _]
   {:db (assoc-in db [:recurring :toggling?] false)
    :dispatch-n [[:recurring/fetch]
                 [:app/show-toast
                  {:type :success
                   :title "Updated"
                   :message "Status toggled."}]]}))

(rf/reg-event-fx
 :recurring/toggle-failure
 (fn [{:keys [db]} [_ error]]
   {:db (assoc-in db [:recurring :toggling?] false)
    :dispatch [:app/show-toast
               {:type :error
                :title "Error"
                :message (errors/extract-error-message error "Failed to toggle status.")}]}))

(rf/reg-event-fx
 :recurring/generate-now
 (fn [{:keys [db]} _]
   {:db (assoc-in db [:recurring :loading?] true)
    :http-xhrio {:method :post
                 :uri "/api/recurring/generate"
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
                :message (errors/extract-error-message error "Failed to generate transactions.")}]}))

(rf/reg-event-db
 :recurring/update-form-field
 (fn [db [_ field value]]
   (assoc-in db [:recurring-form field] value)))

(rf/reg-event-db
 :recurring/reset-form
 (fn [db _]
   (assoc db :recurring-form (:recurring-form db/default-db))))

(rf/reg-event-fx
 :recurring/open-panel
 (fn [{:keys [db]} _]
   {:db (assoc-in db [:recurring-panel :open?] true)
    :dispatch [:recurring/reset-form]}))

(rf/reg-event-db
 :recurring/close-panel
 (fn [db _]
   (assoc-in db [:recurring-panel :open?] false)))

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
 :recurring/toggling?
 (fn [db _]
   (get-in db [:recurring :toggling?] false)))

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

(rf/reg-sub
 :recurring/panel-open?
 (fn [db _]
   (get-in db [:recurring-panel :open?] false)))

(rf/reg-event-db
 :recurring/set-view-mode
 (fn [db [_ mode]]
   (assoc-in db [:recurring :view-mode] mode)))

(rf/reg-event-db
 :recurring/set-search-query
 (fn [db [_ query]]
   (assoc-in db [:recurring :search-query] query)))

(rf/reg-event-db
 :recurring/set-calendar-month
 (fn [db [_ date]]
   (assoc-in db [:recurring :calendar-month] date)))

(rf/reg-event-db
 :recurring/prev-month
 (fn [db _]
   (let [current (or (get-in db [:recurring :calendar-month]) (js/Date.))
         prev-month (js/Date. (.getFullYear current) (dec (.getMonth current)) 1)]
     (assoc-in db [:recurring :calendar-month] prev-month))))

(rf/reg-event-db
 :recurring/next-month
 (fn [db _]
   (let [current (or (get-in db [:recurring :calendar-month]) (js/Date.))
         next-month (js/Date. (.getFullYear current) (inc (.getMonth current)) 1)]
     (assoc-in db [:recurring :calendar-month] next-month))))

(rf/reg-event-db
 :recurring/go-to-today
 (fn [db _]
   (assoc-in db [:recurring :calendar-month] (js/Date.))))

(rf/reg-event-fx
 :recurring/pay-now
 (fn [{:keys [db]} [_ id]]
   {:db (assoc-in db [:recurring :loading?] true)
    :http-xhrio {:method :post
                 :uri (str "/api/recurring/" id "/pay")
                 :format (ajax/json-request-format)
                 :with-credentials true
                 :response-format (ajax/json-response-format {:keywords? true})
                 :on-success [:recurring/pay-now-success]
                 :on-failure [:recurring/pay-now-failure]}}))

(rf/reg-event-fx
 :recurring/pay-now-success
 (fn [{:keys [db]} _]
   {:db (assoc-in db [:recurring :loading?] false)
    :dispatch-n [[:recurring/fetch]
                 [:tx/fetch-transactions]
                 [:dashboard/fetch-summary]
                 [:app/show-toast
                  {:type :success
                   :title "Payment Recorded"
                   :message "Transaction created and next date updated."}]]}))

(rf/reg-event-fx
 :recurring/pay-now-failure
 (fn [{:keys [db]} [_ error]]
   {:db (assoc-in db [:recurring :loading?] false)
    :dispatch [:app/show-toast
               {:type :error
                :title "Error"
                :message (errors/extract-error-message error "Failed to record payment.")}]}))

(rf/reg-event-fx
 :recurring/skip-payment
 (fn [{:keys [db]} [_ id]]
   {:db (assoc-in db [:recurring :loading?] true)
    :http-xhrio {:method :post
                 :uri (str "/api/recurring/" id "/skip")
                 :format (ajax/json-request-format)
                 :with-credentials true
                 :response-format (ajax/json-response-format {:keywords? true})
                 :on-success [:recurring/skip-success]
                 :on-failure [:recurring/skip-failure]}}))

(rf/reg-event-fx
 :recurring/skip-success
 (fn [{:keys [db]} _]
   {:db (assoc-in db [:recurring :loading?] false)
    :dispatch-n [[:recurring/fetch]
                 [:app/show-toast
                  {:type :success
                   :title "Payment Skipped"
                   :message "Next occurrence date has been advanced."}]]}))

(rf/reg-event-fx
 :recurring/skip-failure
 (fn [{:keys [db]} [_ error]]
   {:db (assoc-in db [:recurring :loading?] false)
    :dispatch [:app/show-toast
               {:type :error
                :title "Error"
                :message (errors/extract-error-message error "Failed to skip payment.")}]}))

(rf/reg-sub
 :recurring/view-mode
 (fn [db _]
   (get-in db [:recurring :view-mode] :list)))

(rf/reg-sub
 :recurring/search-query
 (fn [db _]
   (get-in db [:recurring :search-query] "")))

(rf/reg-sub
 :recurring/calendar-month
 (fn [db _]
   (or (get-in db [:recurring :calendar-month]) (js/Date.))))

(rf/reg-sub
 :recurring/filtered-items
 :<- [:recurring/items]
 :<- [:recurring/search-query]
 (fn [[items query] _]
   (if (empty? query)
     items
     (let [q (clojure.string/lower-case query)]
       (filter #(or (clojure.string/includes?
                     (clojure.string/lower-case (or (:recurring/description %) "")) q)
                    (clojure.string/includes?
                     (clojure.string/lower-case (name (or (:recurring/category %) :other))) q))
               items)))))

(defn- calculate-monthly-amount [amount freq]
  (case (keyword freq)
    :daily (* amount 30)
    :weekly (* amount 4.33)
    :monthly amount
    :yearly (/ amount 12)
    amount))

(rf/reg-sub
 :recurring/total-monthly-cost
 :<- [:recurring/active-items]
 (fn [items _]
   (reduce (fn [total item]
             (let [amount (:recurring/amount item)
                   freq (:recurring/frequency item)]
               (+ total (calculate-monthly-amount amount freq))))
           0
           (filter #(= :expense (keyword (:recurring/type %))) items))))

(rf/reg-sub
 :recurring/total-monthly-income
 :<- [:recurring/active-items]
 (fn [items _]
   (reduce (fn [total item]
             (let [amount (:recurring/amount item)
                   freq (:recurring/frequency item)]
               (+ total (calculate-monthly-amount amount freq))))
           0
           (filter #(= :income (keyword (:recurring/type %))) items))))

(rf/reg-sub
 :recurring/expense-count
 :<- [:recurring/active-items]
 (fn [items _]
   (count (filter #(= :expense (keyword (:recurring/type %))) items))))

(rf/reg-sub
 :recurring/income-count
 :<- [:recurring/active-items]
 (fn [items _]
   (count (filter #(= :income (keyword (:recurring/type %))) items))))

(rf/reg-sub
 :recurring/next-payment
 :<- [:recurring/sorted-by-next]
 (fn [items _]
   (first (filter #(= :expense (keyword (:recurring/type %))) items))))

(rf/reg-sub
 :recurring/next-income
 :<- [:recurring/sorted-by-next]
 (fn [items _]
   (first (filter #(= :income (keyword (:recurring/type %))) items))))

(rf/reg-sub
 :recurring/active-count
 :<- [:recurring/active-items]
 (fn [items _]
   (count items)))

(defn- same-day? [d1 d2]
  (and (= (.getDate d1) (.getDate d2))
       (= (.getMonth d1) (.getMonth d2))
       (= (.getFullYear d1) (.getFullYear d2))))

(rf/reg-sub
 :recurring/items-for-date
 :<- [:recurring/active-items]
 (fn [items [_ date]]
   (filter #(when-let [next-occ (:recurring/next-occurrence %)]
              (same-day? (js/Date. next-occ) date))
           items)))

(rf/reg-sub
 :recurring/upcoming-this-week
 :<- [:recurring/active-items]
 (fn [items _]
   (let [now (js/Date.)
         week-later (js/Date. (.getFullYear now) (.getMonth now) (+ (.getDate now) 7))]
     (->> items
          (filter #(when-let [next-occ (:recurring/next-occurrence %)]
                     (let [occ-date (js/Date. next-occ)]
                       (and (>= (.getTime occ-date) (.getTime now))
                            (<= (.getTime occ-date) (.getTime week-later))))))
          (sort-by #(.getTime (js/Date. (:recurring/next-occurrence %))))))))

(rf/reg-sub
 :recurring/weekly-total
 :<- [:recurring/upcoming-this-week]
 (fn [items _]
   (reduce (fn [total item]
             (if (= :expense (keyword (:recurring/type item)))
               (+ total (:recurring/amount item))
               total))
           0
           items)))
