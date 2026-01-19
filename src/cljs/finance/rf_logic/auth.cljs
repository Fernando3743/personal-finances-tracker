(ns finance.rf-logic.auth
  "Authentication re-frame events and subscriptions."
  (:require [re-frame.core :as rf]
            [ajax.core :as ajax]
            [finance.routes :as routes]
            [finance.utils.errors :as errors]))

(rf/reg-event-fx
 :auth/check-session
 (fn [{:keys [db]} _]
   {:db (assoc-in db [:auth :loading?] true)
    :http-xhrio {:method :get
                 :uri "/api/auth/me"
                 :with-credentials true
                 :response-format (ajax/json-response-format {:keywords? true})
                 :on-success [:auth/check-session-success]
                 :on-failure [:auth/check-session-failure]}}))

(rf/reg-event-fx
 :auth/check-session-success
 (fn [{:keys [db]} [_ response]]
   (let [current-route (:current-route db)
         on-public-route? (contains? routes/public-routes current-route)]
     (cond-> {:db (-> db
                      (assoc-in [:auth :user] (:user response))
                      (assoc-in [:auth :loading?] false)
                      (assoc-in [:auth :initialized?] true)
                      (assoc-in [:auth :error] nil))}
       on-public-route? (assoc :dispatch [:app/navigate :dashboard])
       (not on-public-route?) (assoc :dispatch [:app/set-route current-route])))))

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
                 :uri "/api/auth/login"
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
                 [:app/show-toast {:type :success :title "Welcome back!" :message "You have successfully signed in."}]]}))

(rf/reg-event-db
 :auth/login-failure
 (fn [db [_ error]]
   (let [error-msg (errors/extract-error-message error "Login failed")]
     (errors/log-error "Login" error)
     (-> db
         (assoc-in [:auth :loading?] false)
         (assoc-in [:auth :error] error-msg)))))

(rf/reg-event-fx
 :auth/register
 (fn [{:keys [db]} [_ email password name]]
   {:db (-> db
            (assoc-in [:auth :loading?] true)
            (assoc-in [:auth :error] nil))
    :http-xhrio {:method :post
                 :uri "/api/auth/register"
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
   (let [error-msg (errors/extract-error-message error "Registration failed")]
     (errors/log-error "Registration" error)
     (-> db
         (assoc-in [:auth :loading?] false)
         (assoc-in [:auth :error] error-msg)))))

(rf/reg-event-fx
 :auth/logout
 (fn [{:keys [db]} _]
   {:db (assoc-in db [:auth :loading?] true)
    :http-xhrio {:method :post
                 :uri "/api/auth/logout"
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

(rf/reg-event-db
 :auth/update-login-form
 (fn [db [_ field value]]
   (assoc-in db [:auth :login-form field] value)))

(rf/reg-event-db
 :auth/reset-login-form
 (fn [db _]
   (assoc-in db [:auth :login-form] {:email "" :password "" :remember-me false})))

(rf/reg-event-db
 :auth/update-register-form
 (fn [db [_ field value]]
   (assoc-in db [:auth :register-form field] value)))

(rf/reg-event-db
 :auth/reset-register-form
 (fn [db _]
   (assoc-in db [:auth :register-form] {:name "" :email "" :password "" :confirm-password ""})))

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

(rf/reg-sub
 :auth/login-form
 (fn [db _]
   (get-in db [:auth :login-form] {:email "" :password "" :remember-me false})))

(rf/reg-sub
 :auth/login-form-field
 :<- [:auth/login-form]
 (fn [form [_ field]]
   (get form field "")))

(rf/reg-sub
 :auth/register-form
 (fn [db _]
   (get-in db [:auth :register-form] {:name "" :email "" :password "" :confirm-password ""})))

(rf/reg-sub
 :auth/register-form-field
 :<- [:auth/register-form]
 (fn [form [_ field]]
   (get form field "")))
