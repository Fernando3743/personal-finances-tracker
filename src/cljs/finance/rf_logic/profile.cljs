(ns finance.rf-logic.profile
  "Profile page re-frame events and subscriptions."
  (:require [re-frame.core :as rf]
            [ajax.core :as ajax]
            [finance.utils.errors :as errors]))

(rf/reg-event-fx
 :profile/init
 (fn [_ _]
   {:dispatch [:profile/fetch]}))

(rf/reg-event-fx
 :profile/fetch
 (fn [{:keys [db]} _]
   {:db (assoc-in db [:profile :loading?] true)
    :http-xhrio {:method :get
                 :uri "/api/profile"
                 :with-credentials true
                 :response-format (ajax/json-response-format {:keywords? true})
                 :on-success [:profile/fetch-success]
                 :on-failure [:profile/fetch-failure]}}))

(rf/reg-event-db
 :profile/fetch-success
 (fn [db [_ response]]
   (-> db
       (assoc-in [:profile :loading?] false)
       (assoc-in [:profile :statistics] (:statistics response))
       (assoc-in [:profile :error] nil)
       (update-in [:auth :user] merge (:user response)))))

(rf/reg-event-fx
 :profile/fetch-failure
 (fn [{:keys [db]} [_ error]]
   {:db (-> db
            (assoc-in [:profile :loading?] false)
            (assoc-in [:profile :error] (errors/extract-error-message error "Failed to load profile")))
    :dispatch [:app/show-toast {:type :error :message "Failed to load profile"}]}))

(rf/reg-event-fx
 :profile/update
 (fn [{:keys [db]} [_ updates]]
   {:db (assoc-in db [:profile :saving?] true)
    :http-xhrio {:method :put
                 :uri "/api/profile"
                 :params updates
                 :format (ajax/json-request-format)
                 :with-credentials true
                 :response-format (ajax/json-response-format {:keywords? true})
                 :on-success [:profile/update-success]
                 :on-failure [:profile/update-failure]}}))

(rf/reg-event-fx
 :profile/update-success
 (fn [{:keys [db]} [_ response]]
   {:db (-> db
            (assoc-in [:profile :saving?] false)
            (assoc-in [:profile :error] nil)
            (assoc-in [:auth :user] (:user response)))
    :dispatch [:app/show-toast {:type :success :message "Profile updated"}]}))

(rf/reg-event-fx
 :profile/update-failure
 (fn [{:keys [db]} [_ error]]
   {:db (-> db
            (assoc-in [:profile :saving?] false)
            (assoc-in [:profile :error] (errors/extract-error-message error "Update failed")))
    :dispatch [:app/show-toast {:type :error :message (errors/extract-error-message error "Update failed")}]}))

(rf/reg-event-fx
 :profile/change-password
 (fn [{:keys [db]} [_ passwords]]
   {:db (assoc-in db [:profile :saving?] true)
    :http-xhrio {:method :put
                 :uri "/api/profile/password"
                 :params passwords
                 :format (ajax/json-request-format)
                 :with-credentials true
                 :response-format (ajax/json-response-format {:keywords? true})
                 :on-success [:profile/password-change-success]
                 :on-failure [:profile/password-change-failure]}}))

(rf/reg-event-fx
 :profile/password-change-success
 (fn [{:keys [db]} _]
   {:db (-> db
            (assoc-in [:profile :saving?] false)
            (assoc-in [:profile :error] nil))
    :dispatch [:app/show-toast {:type :success :message "Password changed successfully"}]}))

(rf/reg-event-fx
 :profile/password-change-failure
 (fn [{:keys [db]} [_ error]]
   {:db (-> db
            (assoc-in [:profile :saving?] false)
            (assoc-in [:profile :error] (errors/extract-error-message error "Password change failed")))
    :dispatch [:app/show-toast {:type :error :message (errors/extract-error-message error "Password change failed")}]}))

(rf/reg-event-fx
 :profile/update-preferences
 (fn [{:keys [db]} [_ preferences]]
   {:db (assoc-in db [:profile :saving-preferences?] true)
    :http-xhrio {:method :put
                 :uri "/api/profile/preferences"
                 :params preferences
                 :format (ajax/json-request-format)
                 :with-credentials true
                 :response-format (ajax/json-response-format {:keywords? true})
                 :on-success [:profile/preferences-success]
                 :on-failure [:profile/preferences-failure]}}))

(rf/reg-event-fx
 :profile/preferences-success
 (fn [{:keys [db]} [_ response]]
   {:db (-> db
            (assoc-in [:profile :saving-preferences?] false)
            (assoc-in [:auth :user] (:user response)))
    :dispatch [:app/show-toast {:type :success :message "Preferences saved"}]}))

(rf/reg-event-fx
 :profile/preferences-failure
 (fn [{:keys [db]} [_ error]]
   {:db (assoc-in db [:profile :saving-preferences?] false)
    :dispatch [:app/show-toast {:type :error :message (errors/extract-error-message error "Failed to save preferences")}]}))

(rf/reg-event-fx
 :profile/upload-avatar
 (fn [{:keys [db]} [_ file]]
   (let [form-data (doto (js/FormData.)
                     (.append "avatar" file))]
     {:db (assoc-in db [:profile :uploading-avatar?] true)
      :http-xhrio {:method :post
                   :uri "/api/profile/avatar"
                   :body form-data
                   :with-credentials true
                   :response-format (ajax/json-response-format {:keywords? true})
                   :on-success [:profile/avatar-upload-success]
                   :on-failure [:profile/avatar-upload-failure]}})))

(rf/reg-event-fx
 :profile/avatar-upload-success
 (fn [{:keys [db]} [_ response]]
   {:db (-> db
            (assoc-in [:profile :uploading-avatar?] false)
            (assoc-in [:auth :user] (:user response)))
    :dispatch [:app/show-toast {:type :success :message "Avatar uploaded"}]}))

(rf/reg-event-fx
 :profile/avatar-upload-failure
 (fn [{:keys [db]} [_ error]]
   {:db (assoc-in db [:profile :uploading-avatar?] false)
    :dispatch [:app/show-toast {:type :error :message (errors/extract-error-message error "Failed to upload avatar")}]}))

(rf/reg-event-fx
 :profile/delete-avatar
 (fn [{:keys [db]} _]
   {:db (assoc-in db [:profile :uploading-avatar?] true)
    :http-xhrio {:method :delete
                 :uri "/api/profile/avatar"
                 :with-credentials true
                 :response-format (ajax/json-response-format {:keywords? true})
                 :on-success [:profile/avatar-delete-success]
                 :on-failure [:profile/avatar-delete-failure]}}))

(rf/reg-event-fx
 :profile/avatar-delete-success
 (fn [{:keys [db]} _]
   {:db (-> db
            (assoc-in [:profile :uploading-avatar?] false)
            (update-in [:auth :user] dissoc :user/avatar-url))
    :dispatch [:app/show-toast {:type :success :message "Avatar removed"}]}))

(rf/reg-event-fx
 :profile/avatar-delete-failure
 (fn [{:keys [db]} [_ error]]
   {:db (assoc-in db [:profile :uploading-avatar?] false)
    :dispatch [:app/show-toast {:type :error :message "Failed to remove avatar"}]}))

(rf/reg-fx
 :download-file
 (fn [{:keys [url filename]}]
   (let [link (.createElement js/document "a")]
     (set! (.-href link) url)
     (set! (.-download link) filename)
     (.click link))))

(rf/reg-event-fx
 :profile/export-data
 (fn [_ _]
   {:fx [[:dispatch [:app/show-toast {:type :info :message "Preparing export..."}]]]
    :http-xhrio {:method :get
                 :uri "/api/profile/export"
                 :with-credentials true
                 :response-format (ajax/json-response-format {:keywords? true})
                 :on-success [:profile/export-success]
                 :on-failure [:profile/export-failure]}}))

(rf/reg-event-fx
 :profile/export-success
 (fn [_ [_ data]]
   (let [blob (js/Blob. #js [(js/JSON.stringify (clj->js data) nil 2)]
                        #js {:type "application/json"})
         url (.createObjectURL js/URL blob)
         link (.createElement js/document "a")]
     (set! (.-href link) url)
     (set! (.-download link) "finance-data-export.json")
     (.click link)
     (.revokeObjectURL js/URL url)
     {:dispatch [:app/show-toast {:type :success :message "Data exported successfully"}]})))

(rf/reg-event-fx
 :profile/export-failure
 (fn [_ [_ error]]
   {:dispatch [:app/show-toast {:type :error :message "Failed to export data"}]}))

(rf/reg-event-fx
 :profile/delete-account
 (fn [{:keys [db]} [_ password]]
   {:db (assoc-in db [:profile :deleting?] true)
    :http-xhrio {:method :delete
                 :uri "/api/profile"
                 :params {:password password}
                 :format (ajax/json-request-format)
                 :with-credentials true
                 :response-format (ajax/json-response-format {:keywords? true})
                 :on-success [:profile/delete-account-success]
                 :on-failure [:profile/delete-account-failure]}}))

(rf/reg-event-fx
 :profile/delete-account-success
 (fn [{:keys [db]} _]
   {:db (-> db
            (assoc-in [:profile :deleting?] false)
            (assoc-in [:auth :user] nil))
    :dispatch-n [[:app/navigate :login]
                 [:app/show-toast {:type :info :message "Account deleted. Goodbye!"}]]}))

(rf/reg-event-fx
 :profile/delete-account-failure
 (fn [{:keys [db]} [_ error]]
   {:db (assoc-in db [:profile :deleting?] false)
    :dispatch [:app/show-toast {:type :error :message (errors/extract-error-message error "Failed to delete account")}]}))

(rf/reg-sub
 :profile/loading?
 (fn [db _]
   (get-in db [:profile :loading?])))

(rf/reg-sub
 :profile/saving?
 (fn [db _]
   (get-in db [:profile :saving?])))

(rf/reg-sub
 :profile/uploading-avatar?
 (fn [db _]
   (get-in db [:profile :uploading-avatar?])))

(rf/reg-sub
 :profile/deleting?
 (fn [db _]
   (get-in db [:profile :deleting?])))

(rf/reg-sub
 :profile/statistics
 (fn [db _]
   (get-in db [:profile :statistics])))

(rf/reg-sub
 :profile/error
 (fn [db _]
   (get-in db [:profile :error])))

(rf/reg-sub
 :profile/saving-preferences?
 (fn [db _]
   (get-in db [:profile :saving-preferences?])))

(rf/reg-event-db
 :profile/update-edit-form
 (fn [db [_ field value]]
   (assoc-in db [:profile :edit-form field] value)))

(rf/reg-event-db
 :profile/init-edit-form
 (fn [db _]
   (let [user (get-in db [:auth :user])]
     (assoc-in db [:profile :edit-form]
               {:name (or (:user/name user) "")
                :email (or (:user/email user) "")}))))

(rf/reg-event-db
 :profile/update-password-form
 (fn [db [_ field value]]
   (assoc-in db [:profile :password-form field] value)))

(rf/reg-event-db
 :profile/reset-password-form
 (fn [db _]
   (assoc-in db [:profile :password-form]
             {:current-password "" :new-password "" :confirm-password ""})))

(rf/reg-event-db
 :profile/update-delete-form
 (fn [db [_ field value]]
   (assoc-in db [:profile :delete-form field] value)))

(rf/reg-event-db
 :profile/toggle-delete-modal
 (fn [db [_ show?]]
   (-> db
       (assoc-in [:profile :delete-form :show-modal?] show?)
       (assoc-in [:profile :delete-form :password] ""))))

(rf/reg-sub
 :profile/edit-form
 (fn [db _]
   (get-in db [:profile :edit-form] {:name "" :email ""})))

(rf/reg-sub
 :profile/password-form
 (fn [db _]
   (get-in db [:profile :password-form] {:current-password "" :new-password "" :confirm-password ""})))

(rf/reg-sub
 :profile/delete-form
 (fn [db _]
   (get-in db [:profile :delete-form] {:show-modal? false :password ""})))
