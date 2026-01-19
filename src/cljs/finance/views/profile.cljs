(ns finance.views.profile
  "Profile page with sections for editing, statistics, preferences, and danger zone."
  (:require [re-frame.core :as rf]
            [reagent.core :as r]
            [finance.components.icons :refer [icon]]
            [finance.utils.currency :as currency]))

(defn avatar-section []
  (let [user @(rf/subscribe [:auth/user])
        user-name (or (:user/name user) "User")
        initial (first user-name)]
    [:div {:class "flex items-center gap-4 mb-8"}
     [:div {:class "w-20 h-20 rounded-full bg-gradient-to-br from-purple-600 to-purple-400 flex items-center justify-center text-white text-3xl font-bold"}
      initial]
     [:div
      [:h2 {:class "text-xl font-semibold text-neutral-900 dark:text-neutral-50"} user-name]
      [:p {:class "text-neutral-600 dark:text-neutral-400"} (:user/email user)]
      [:p {:class "flex items-center gap-1.5 text-xs text-neutral-400 dark:text-neutral-500 mt-2"}
       [icon :upload {:width 14 :height 14}]
       "Custom profile photos coming soon"]]]))

(defn profile-edit-section []
  (let [user @(rf/subscribe [:auth/user])
        saving? @(rf/subscribe [:profile/saving?])
        name-val (r/atom (or (:user/name user) ""))
        email-val (r/atom (or (:user/email user) ""))]
    (fn []
      (let [user @(rf/subscribe [:auth/user])
            changed? (or (not= @name-val (:user/name user))
                         (not= @email-val (:user/email user)))]
        [:div {:class "bg-white dark:bg-neutral-800 rounded-xl border border-neutral-200 dark:border-neutral-700 p-5 mb-6"}
         [:div {:class "flex items-center gap-3 mb-4"}
          [:div {:class "p-2 rounded-lg bg-purple-100 dark:bg-purple-900/20 text-purple-700 dark:text-purple-400"}
           [icon :user {:width 20 :height 20}]]
          [:div
           [:h3 {:class "text-lg font-semibold text-neutral-900 dark:text-neutral-50"} "Profile Information"]
           [:p {:class "text-sm text-neutral-600 dark:text-neutral-400"} "Update your personal details"]]]
         [:div {:class "space-y-4"}
          [:div {:class "grid grid-cols-1 md:grid-cols-2 gap-4"}
           [:div {:class "space-y-1.5"}
            [:label {:class "block text-sm font-medium text-neutral-900 dark:text-neutral-50" :for "profile-name"} "Name"]
            [:input {:class (str "w-full h-11 px-4 rounded-lg border border-neutral-200 dark:border-neutral-700 bg-white dark:bg-neutral-900 text-neutral-900 dark:text-neutral-50 "
                                 "focus:outline-none focus:ring-2 focus:ring-purple-700 dark:focus:ring-purple-400 focus:border-transparent")
                     :id "profile-name"
                     :type "text"
                     :value @name-val
                     :on-change #(reset! name-val (-> % .-target .-value))}]]
           [:div {:class "space-y-1.5"}
            [:label {:class "block text-sm font-medium text-neutral-900 dark:text-neutral-50" :for "profile-email"} "Email"]
            [:input {:class (str "w-full h-11 px-4 rounded-lg border border-neutral-200 dark:border-neutral-700 bg-white dark:bg-neutral-900 text-neutral-900 dark:text-neutral-50 "
                                 "focus:outline-none focus:ring-2 focus:ring-purple-700 dark:focus:ring-purple-400 focus:border-transparent")
                     :id "profile-email"
                     :type "email"
                     :value @email-val
                     :on-change #(reset! email-val (-> % .-target .-value))}]]]
          [:div {:class "flex justify-end"}
           [:button {:class (str "px-4 py-2 rounded-lg font-medium text-white "
                                 "bg-gradient-to-r from-purple-700 to-purple-500 "
                                 "hover:shadow-lg disabled:opacity-50 disabled:cursor-not-allowed transition-all")
                     :disabled (or saving? (not changed?))
                     :on-click #(rf/dispatch [:profile/update {:name @name-val
                                                               :email @email-val}])}
            (if saving? "Saving..." "Save Changes")]]]]))))

(defn password-change-section []
  (let [current-pw (r/atom "")
        new-pw (r/atom "")
        confirm-pw (r/atom "")
        saving? @(rf/subscribe [:profile/saving?])]
    (fn []
      (let [passwords-match? (= @new-pw @confirm-pw)
            password-long-enough? (>= (count @new-pw) 8)
            can-submit? (and (not-empty @current-pw)
                             (not-empty @new-pw)
                             passwords-match?
                             password-long-enough?)]
        [:div {:class "bg-white dark:bg-neutral-800 rounded-xl border border-neutral-200 dark:border-neutral-700 p-5 mb-6"}
         [:div {:class "flex items-center gap-3 mb-4"}
          [:div {:class "p-2 rounded-lg bg-purple-100 dark:bg-purple-900/20 text-purple-700 dark:text-purple-400"}
           [icon :key {:width 20 :height 20}]]
          [:div
           [:h3 {:class "text-lg font-semibold text-neutral-900 dark:text-neutral-50"} "Change Password"]
           [:p {:class "text-sm text-neutral-600 dark:text-neutral-400"} "Update your password for better security"]]]
         [:div {:class "space-y-4"}
          [:div {:class "space-y-1.5"}
           [:label {:class "block text-sm font-medium text-neutral-900 dark:text-neutral-50" :for "current-password"} "Current Password"]
           [:input {:class (str "w-full h-11 px-4 rounded-lg border border-neutral-200 dark:border-neutral-700 bg-white dark:bg-neutral-900 text-neutral-900 dark:text-neutral-50 "
                                "placeholder:text-neutral-400 dark:placeholder:text-neutral-500 focus:outline-none focus:ring-2 focus:ring-purple-700 dark:focus:ring-purple-400 focus:border-transparent")
                    :id "current-password"
                    :type "password"
                    :value @current-pw
                    :placeholder "Enter current password"
                    :on-change #(reset! current-pw (-> % .-target .-value))}]]
          [:div {:class "grid grid-cols-1 md:grid-cols-2 gap-4"}
           [:div {:class "space-y-1.5"}
            [:label {:class "block text-sm font-medium text-neutral-900 dark:text-neutral-50" :for "new-password"} "New Password"]
            [:input {:class (str "w-full h-11 px-4 rounded-lg border bg-white dark:bg-neutral-900 text-neutral-900 dark:text-neutral-50 "
                                 "placeholder:text-neutral-400 dark:placeholder:text-neutral-500 focus:outline-none focus:ring-2 focus:ring-purple-700 dark:focus:ring-purple-400 focus:border-transparent "
                                 (if (and (not-empty @new-pw) (not password-long-enough?))
                                   "border-red-500"
                                   "border-neutral-200 dark:border-neutral-700"))
                     :id "new-password"
                     :type "password"
                     :value @new-pw
                     :placeholder "At least 8 characters"
                     :on-change #(reset! new-pw (-> % .-target .-value))}]
            (when (and (not-empty @new-pw) (not password-long-enough?))
              [:span {:class "text-xs text-red-600 dark:text-red-500"} "Password must be at least 8 characters"])]
           [:div {:class "space-y-1.5"}
            [:label {:class "block text-sm font-medium text-neutral-900 dark:text-neutral-50" :for "confirm-password"} "Confirm Password"]
            [:input {:class (str "w-full h-11 px-4 rounded-lg border bg-white dark:bg-neutral-900 text-neutral-900 dark:text-neutral-50 "
                                 "placeholder:text-neutral-400 dark:placeholder:text-neutral-500 focus:outline-none focus:ring-2 focus:ring-purple-700 dark:focus:ring-purple-400 focus:border-transparent "
                                 (if (and (not-empty @confirm-pw) (not passwords-match?))
                                   "border-red-500"
                                   "border-neutral-200 dark:border-neutral-700"))
                     :id "confirm-password"
                     :type "password"
                     :value @confirm-pw
                     :placeholder "Re-enter new password"
                     :on-change #(reset! confirm-pw (-> % .-target .-value))}]
            (when (and (not-empty @confirm-pw) (not passwords-match?))
              [:span {:class "text-xs text-red-600 dark:text-red-500"} "Passwords do not match"])]]
          [:div {:class "flex justify-end"}
           [:button {:class (str "px-4 py-2 rounded-lg font-medium text-white "
                                 "bg-gradient-to-r from-purple-700 to-purple-500 "
                                 "hover:shadow-lg disabled:opacity-50 disabled:cursor-not-allowed transition-all")
                     :disabled (or (not can-submit?) saving?)
                     :on-click #(do
                                  (rf/dispatch [:profile/change-password
                                                {:current_password @current-pw
                                                 :new_password @new-pw}])
                                  (reset! current-pw "")
                                  (reset! new-pw "")
                                  (reset! confirm-pw ""))}
            (if saving? "Changing..." "Change Password")]]]]))))

(defn statistics-section []
  (let [stats @(rf/subscribe [:profile/statistics])
        user @(rf/subscribe [:auth/user])
        created-at (:user/created-at user)
        member-since (when created-at
                       (.toLocaleDateString (js/Date. created-at)
                                            "en-US"
                                            #js {:year "numeric" :month "long" :day "numeric"}))]
    [:div {:class "bg-white dark:bg-neutral-800 rounded-xl border border-neutral-200 dark:border-neutral-700 p-5 mb-6"}
     [:div {:class "flex items-center gap-3 mb-4"}
      [:div {:class "p-2 rounded-lg bg-purple-100 dark:bg-purple-900/20 text-purple-700 dark:text-purple-400"}
       [icon :bar-chart {:width 20 :height 20}]]
      [:div
       [:h3 {:class "text-lg font-semibold text-neutral-900 dark:text-neutral-50"} "Account Statistics"]
       [:p {:class "text-sm text-neutral-600 dark:text-neutral-400"} "Your activity overview"]]]
     [:div {:class "grid grid-cols-2 md:grid-cols-4 gap-4"}
      [:div {:class "p-4 bg-neutral-100 dark:bg-neutral-800 rounded-lg"}
       [:div {:class "flex items-center gap-2 text-neutral-400 dark:text-neutral-500 mb-2"}
        [icon :calendar {:width 20 :height 20}]]
       [:div {:class "text-xs text-neutral-400 dark:text-neutral-500"} "Member Since"]
       [:div {:class "text-sm font-semibold text-neutral-900 dark:text-neutral-50"} (or member-since "N/A")]]
      [:div {:class "p-4 bg-neutral-100 dark:bg-neutral-800 rounded-lg"}
       [:div {:class "flex items-center gap-2 text-neutral-400 dark:text-neutral-500 mb-2"}
        [icon :list {:width 20 :height 20}]]
       [:div {:class "text-xs text-neutral-400 dark:text-neutral-500"} "Total Transactions"]
       [:div {:class "text-lg font-semibold text-neutral-900 dark:text-neutral-50"} (or (:total-transactions stats) 0)]]
      [:div {:class "p-4 bg-green-100/50 dark:bg-green-900/20 rounded-lg"}
       [:div {:class "flex items-center gap-2 text-green-600 dark:text-green-500 mb-2"}
        [icon :trending-up {:width 20 :height 20}]]
       [:div {:class "text-xs text-neutral-400 dark:text-neutral-500"} "Income Entries"]
       [:div {:class "text-lg font-semibold text-green-600 dark:text-green-500"} (or (:income-count stats) 0)]]
      [:div {:class "p-4 bg-red-100/50 dark:bg-red-900/20 rounded-lg"}
       [:div {:class "flex items-center gap-2 text-red-600 dark:text-red-500 mb-2"}
        [icon :trending-down {:width 20 :height 20}]]
       [:div {:class "text-xs text-neutral-400 dark:text-neutral-500"} "Expense Entries"]
       [:div {:class "text-lg font-semibold text-red-600 dark:text-red-500"} (or (:expense-count stats) 0)]]]]))

(defn preferences-section []
  (let [user @(rf/subscribe [:auth/user])
        theme @(rf/subscribe [:app/theme])
        current-currency (or (some-> (:user/preferred-currency user) keyword) :COP)]
    [:div {:class "bg-white dark:bg-neutral-800 rounded-xl border border-neutral-200 dark:border-neutral-700 p-5 mb-6"}
     [:div {:class "flex items-center gap-3 mb-4"}
      [:div {:class "p-2 rounded-lg bg-purple-100 dark:bg-purple-900/20 text-purple-700 dark:text-purple-400"}
       [icon :dashboard {:width 20 :height 20}]]
      [:div
       [:h3 {:class "text-lg font-semibold text-neutral-900 dark:text-neutral-50"} "Preferences"]
       [:p {:class "text-sm text-neutral-600 dark:text-neutral-400"} "Customize your experience"]]]
     [:div {:class "space-y-4"}
      [:div {:class "flex items-center justify-between p-4 bg-neutral-100 dark:bg-neutral-800 rounded-lg"}
       [:div
        [:div {:class "text-sm font-medium text-neutral-900 dark:text-neutral-50"} "Default Currency"]
        [:div {:class "text-xs text-neutral-400 dark:text-neutral-500"} "Currency used for new transactions"]]
       [:div {:class "flex gap-1 p-1 bg-white dark:bg-neutral-800 rounded-lg"}
        (for [curr [:COP :USD]]
          ^{:key curr}
          [:button {:class (str "px-3 py-1.5 rounded-md text-sm font-medium transition-colors "
                                (if (= curr current-currency)
                                  "bg-purple-700 dark:bg-purple-400 text-white"
                                  "text-neutral-600 dark:text-neutral-400 hover:text-neutral-900 dark:hover:text-neutral-50"))
                    :on-click #(rf/dispatch [:profile/update-preferences {:currency curr}])}
           (name curr)])]]
      [:div {:class "flex items-center justify-between p-4 bg-neutral-100 dark:bg-neutral-800 rounded-lg"}
       [:div
        [:div {:class "text-sm font-medium text-neutral-900 dark:text-neutral-50"} "Theme"]
        [:div {:class "text-xs text-neutral-400 dark:text-neutral-500"} "Choose light or dark mode"]]
       [:div {:class "flex gap-1 p-1 bg-white dark:bg-neutral-800 rounded-lg"}
        (for [[key label] [[:light "Light"] [:dark "Dark"]]]
          ^{:key key}
          [:button {:class (str "px-3 py-1.5 rounded-md text-sm font-medium transition-colors "
                                (if (= key theme)
                                  "bg-purple-700 dark:bg-purple-400 text-white"
                                  "text-neutral-600 dark:text-neutral-400 hover:text-neutral-900 dark:hover:text-neutral-50"))
                    :on-click #(rf/dispatch [:app/toggle-theme])}
           label])]]]]))

(defn danger-zone-section []
  (let [show-delete-modal? (r/atom false)
        delete-password (r/atom "")
        deleting? @(rf/subscribe [:profile/deleting?])]
    (fn []
      [:div {:class "bg-white dark:bg-neutral-800 rounded-xl border border-red-500/30 p-5"}
       [:div {:class "flex items-center gap-3 mb-4"}
        [:div {:class "p-2 rounded-lg bg-red-100 dark:bg-red-900/20 text-red-500"}
         [icon :shield {:width 20 :height 20}]]
        [:div
         [:h3 {:class "text-lg font-semibold text-red-600 dark:text-red-500"} "Danger Zone"]
         [:p {:class "text-sm text-neutral-600 dark:text-neutral-400"} "Irreversible actions"]]]
       [:div {:class "space-y-3"}
        [:div {:class "flex items-center justify-between p-4 bg-neutral-100 dark:bg-neutral-800 rounded-lg"}
         [:div
          [:div {:class "text-sm font-medium text-neutral-900 dark:text-neutral-50"} "Export Data"]
          [:div {:class "text-xs text-neutral-400 dark:text-neutral-500"} "Download all your data as a JSON file"]]
         [:button {:class "inline-flex items-center gap-2 px-3 py-2 rounded-lg border border-neutral-200 dark:border-neutral-700 text-neutral-900 dark:text-neutral-50 hover:bg-white dark:hover:bg-neutral-800 transition-colors"
                   :on-click #(rf/dispatch [:profile/export-data])}
          [icon :download {:width 16 :height 16}]
          [:span "Export"]]]
        [:div {:class "flex items-center justify-between p-4 bg-red-100/30 dark:bg-red-900/20 rounded-lg"}
         [:div
          [:div {:class "text-sm font-medium text-neutral-900 dark:text-neutral-50"} "Delete Account"]
          [:div {:class "text-xs text-neutral-400 dark:text-neutral-500"} "Permanently delete your account and all data"]]
         [:button {:class "inline-flex items-center gap-2 px-3 py-2 rounded-lg bg-red-500 text-white hover:bg-red-600 transition-colors"
                   :on-click #(reset! show-delete-modal? true)}
          [icon :trash {:width 16 :height 16}]
          [:span "Delete Account"]]]]

       (when @show-delete-modal?
         [:div {:class "fixed inset-0 bg-black/50 z-modal flex items-center justify-center p-4"
                :on-click #(reset! show-delete-modal? false)}
          [:div {:class "bg-white dark:bg-neutral-800 rounded-xl border border-neutral-200 dark:border-neutral-700 w-full max-w-md p-6"
                 :on-click #(.stopPropagation %)}
           [:div {:class "flex items-center justify-between mb-4"}
            [:h3 {:class "text-lg font-semibold text-neutral-900 dark:text-neutral-50"} "Delete Account"]
            [:button {:class "p-2 rounded-lg text-neutral-400 dark:text-neutral-500 hover:text-neutral-900 dark:hover:text-neutral-50 hover:bg-neutral-100 dark:hover:bg-neutral-800 transition-colors"
                      :on-click #(reset! show-delete-modal? false)}
             [icon :x {:width 20 :height 20}]]]
           [:p {:class "text-red-600 dark:text-red-500 text-sm mb-4"}
            "This action cannot be undone. All your data will be permanently deleted."]
           [:div {:class "space-y-1.5 mb-4"}
            [:label {:class "block text-sm font-medium text-neutral-900 dark:text-neutral-50"} "Enter your password to confirm"]
            [:input {:class (str "w-full h-11 px-4 rounded-lg border border-neutral-200 dark:border-neutral-700 bg-white dark:bg-neutral-900 text-neutral-900 dark:text-neutral-50 "
                                 "placeholder:text-neutral-400 dark:placeholder:text-neutral-500 focus:outline-none focus:ring-2 focus:ring-red-500 focus:border-transparent")
                     :type "password"
                     :placeholder "Your password"
                     :value @delete-password
                     :on-change #(reset! delete-password (-> % .-target .-value))}]]
           [:div {:class "flex gap-3"}
            [:button {:class "flex-1 h-10 rounded-lg font-medium border border-neutral-200 dark:border-neutral-700 text-neutral-900 dark:text-neutral-50 hover:bg-neutral-100 dark:hover:bg-neutral-800 transition-colors"
                      :on-click #(do (reset! show-delete-modal? false)
                                     (reset! delete-password ""))}
             "Cancel"]
            [:button {:class "flex-1 h-10 rounded-lg font-medium bg-red-500 text-white hover:bg-red-600 disabled:opacity-50 disabled:cursor-not-allowed transition-colors"
                      :disabled (or (empty? @delete-password) deleting?)
                      :on-click #(rf/dispatch [:profile/delete-account @delete-password])}
             (if deleting? "Deleting..." "Delete Account")]]]])])))

(defn profile-skeleton []
  [:div {:class "space-y-6 animate-pulse"}
   [:div {:class "flex items-center gap-4"}
    [:div {:class "w-20 h-20 rounded-full bg-neutral-100 dark:bg-neutral-800"}]
    [:div {:class "space-y-2"}
     [:div {:class "h-5 bg-neutral-100 dark:bg-neutral-800 rounded w-32"}]
     [:div {:class "h-4 bg-neutral-100 dark:bg-neutral-800 rounded w-48"}]]]
   (for [i (range 4)]
     ^{:key i}
     [:div {:class "h-40 bg-neutral-100 dark:bg-neutral-800 rounded-xl"}])])

(defn profile-view []
  (r/create-class
   {:component-did-mount
    (fn [_]
      (rf/dispatch [:profile/fetch]))

    :reagent-render
    (fn []
      (let [loading? @(rf/subscribe [:profile/loading?])
            user @(rf/subscribe [:auth/user])]
        [:div {:class "max-w-3xl mx-auto"}
         [:div {:class "mb-6"}
          [:h1 {:class "text-2xl font-bold text-neutral-900 dark:text-neutral-50"} "Profile"]
          [:p {:class "text-neutral-600 dark:text-neutral-400 text-sm mt-1"} "Manage your account settings"]]

         (if loading?
           [profile-skeleton]

           [:div
            [avatar-section]
            [profile-edit-section]
            [password-change-section]
            [statistics-section]
            [preferences-section]
            [danger-zone-section]])]))}))
