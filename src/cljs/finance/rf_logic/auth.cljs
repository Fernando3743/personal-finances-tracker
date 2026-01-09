(ns finance.rf-logic.auth
  "Authentication re-frame events and subscriptions."
  (:require [re-frame.core :as rf]
            [ajax.core :as ajax]))

(def api-base "http://localhost:3000/api")

(rf/reg-event-fx
 :auth/check-session
 (fn [{:keys [db]} _]
   {:db (assoc-in db [:auth :loading?] true)
    :http-xhrio {:method :get
                 :uri (str api-base "/auth/me")
                 :with-credentials true
                 :response-format (ajax/json-response-format {:keywords? true})
                 :on-success [:auth/check-session-success]
                 :on-failure [:auth/check-session-failure]}}))

(rf/reg-event-fx
 :auth/check-session-success
 (fn [{:keys [db]} [_ response]]
   {:db (-> db
            (assoc-in [:auth :user] (:user response))
            (assoc-in [:auth :loading?] false)
            (assoc-in [:auth :initialized?] true)
            (assoc-in [:auth :error] nil))
    :dispatch-n [[:tx/fetch-transactions]
                 [:dashboard/fetch-summary]]}))

(rf/reg-event-db
 :auth/check-session-failure
 (fn [db _]
   (-> db
       (assoc-in [:auth :user] nil)
       (assoc-in [:auth :loading?] false)
       (assoc-in [:auth :initialized?] true))))

(rf/reg-event-fx
 :auth/login
 (fn [{:keys [db]} [_ email password]]
   {:db (-> db
            (assoc-in [:auth :loading?] true)
            (assoc-in [:auth :error] nil))
    :http-xhrio {:method :post
                 :uri (str api-base "/auth/login")
                 :params {:email email :password password}
                 :format (ajax/json-request-format)
                 :with-credentials true
                 :response-format (ajax/json-response-format {:keywords? true})
                 :on-success [:auth/login-success]
                 :on-failure [:auth/login-failure]}}))

(rf/reg-event-fx
 :auth/login-success
 (fn [{:keys [db]} [_ response]]
   {:db (-> db
            (assoc-in [:auth :user] (:user response))
            (assoc-in [:auth :loading?] false)
            (assoc-in [:auth :error] nil))
    :dispatch-n [[:app/navigate :dashboard]
                 [:tx/fetch-transactions]
                 [:dashboard/fetch-summary]
                 [:app/show-toast {:type :success :message "Welcome back!"}]]}))

(rf/reg-event-db
 :auth/login-failure
 (fn [db [_ error]]
   (-> db
       (assoc-in [:auth :loading?] false)
       (assoc-in [:auth :error] (get-in error [:response :error] "Login failed")))))

(rf/reg-event-fx
 :auth/register
 (fn [{:keys [db]} [_ email password name]]
   {:db (-> db
            (assoc-in [:auth :loading?] true)
            (assoc-in [:auth :error] nil))
    :http-xhrio {:method :post
                 :uri (str api-base "/auth/register")
                 :params {:email email :password password :name name}
                 :format (ajax/json-request-format)
                 :with-credentials true
                 :response-format (ajax/json-response-format {:keywords? true})
                 :on-success [:auth/register-success]
                 :on-failure [:auth/register-failure]}}))

(rf/reg-event-fx
 :auth/register-success
 (fn [{:keys [db]} [_ response]]
   {:db (-> db
            (assoc-in [:auth :user] (:user response))
            (assoc-in [:auth :loading?] false)
            (assoc-in [:auth :error] nil))
    :dispatch-n [[:app/navigate :dashboard]
                 [:app/show-toast {:type :success :message "Account created successfully!"}]]}))

(rf/reg-event-db
 :auth/register-failure
 (fn [db [_ error]]
   (-> db
       (assoc-in [:auth :loading?] false)
       (assoc-in [:auth :error] (get-in error [:response :error] "Registration failed")))))

(rf/reg-event-fx
 :auth/logout
 (fn [{:keys [db]} _]
   {:db (assoc-in db [:auth :loading?] true)
    :http-xhrio {:method :post
                 :uri (str api-base "/auth/logout")
                 :format (ajax/json-request-format)
                 :with-credentials true
                 :response-format (ajax/json-response-format {:keywords? true})
                 :on-success [:auth/logout-success]
                 :on-failure [:auth/logout-success]}})) ; Logout locally even if server fails

(rf/reg-event-fx
 :auth/logout-success
 (fn [{:keys [db]} _]
   {:db (-> db
            (assoc-in [:auth :user] nil)
            (assoc-in [:auth :loading?] false)
            (assoc :transactions []))
    :dispatch-n [[:app/navigate :login]
                 [:app/show-toast {:type :info :message "Logged out"}]]}))

(rf/reg-event-db
 :auth/clear-error
 (fn [db _]
   (assoc-in db [:auth :error] nil)))

(rf/reg-sub
 :auth/user
 (fn [db _]
   (get-in db [:auth :user])))

(rf/reg-sub
 :auth/loading?
 (fn [db _]
   (get-in db [:auth :loading?])))

(rf/reg-sub
 :auth/initialized?
 (fn [db _]
   (get-in db [:auth :initialized?])))

(rf/reg-sub
 :auth/error
 (fn [db _]
   (get-in db [:auth :error])))

(rf/reg-sub
 :auth/authenticated?
 :<- [:auth/user]
 (fn [user _]
   (some? user)))
