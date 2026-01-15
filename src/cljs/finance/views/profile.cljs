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
    [:div.profile-avatar-section
     [:div.profile-avatar
      [:span.profile-avatar__initial initial]]
     [:div.profile-avatar-info
      [:h2.profile-avatar-info__name user-name]
      [:p.profile-avatar-info__email (:user/email user)]
      [:p.profile-avatar-coming-soon
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
        [:div.profile-section
         [:div.profile-section__header
          [:div.profile-section__header-content
           [:span.profile-section__icon [icon :user {:width 20 :height 20}]]
           [:div
            [:h3.profile-section__title "Profile Information"]
            [:p.profile-section__subtitle "Update your personal details"]]]]
         [:div.profile-section__body
          [:div.flow-form
           [:div.flow-form__row
            [:div.flow-form__field
             [:label.flow-label {:for "profile-name"} "Name"]
             [:input.flow-input
              {:id "profile-name"
               :type "text"
               :value @name-val
               :on-change #(reset! name-val (-> % .-target .-value))}]]
            [:div.flow-form__field
             [:label.flow-label {:for "profile-email"} "Email"]
             [:input.flow-input
              {:id "profile-email"
               :type "email"
               :value @email-val
               :on-change #(reset! email-val (-> % .-target .-value))}]]]
           [:div.flow-form__actions
            [:button.flow-btn.flow-btn--primary
             {:disabled (or saving? (not changed?))
              :on-click #(rf/dispatch [:profile/update {:name @name-val
                                                        :email @email-val}])}
             (if saving? "Saving..." "Save Changes")]]]]]))))

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
        [:div.profile-section
         [:div.profile-section__header
          [:div.profile-section__header-content
           [:span.profile-section__icon [icon :key {:width 20 :height 20}]]
           [:div
            [:h3.profile-section__title "Change Password"]
            [:p.profile-section__subtitle "Update your password for better security"]]]]
         [:div.profile-section__body
          [:div.flow-form
           [:div.flow-form__field
            [:label.flow-label {:for "current-password"} "Current Password"]
            [:input.flow-input
             {:id "current-password"
              :type "password"
              :value @current-pw
              :placeholder "Enter current password"
              :on-change #(reset! current-pw (-> % .-target .-value))}]]
           [:div.flow-form__row
            [:div.flow-form__field
             [:label.flow-label {:for "new-password"} "New Password"]
             [:input.flow-input
              {:id "new-password"
               :type "password"
               :value @new-pw
               :placeholder "At least 8 characters"
               :class (when (and (not-empty @new-pw) (not password-long-enough?))
                        "flow-input--error")
               :on-change #(reset! new-pw (-> % .-target .-value))}]
             (when (and (not-empty @new-pw) (not password-long-enough?))
               [:span.flow-input-hint.flow-input-hint--error
                "Password must be at least 8 characters"])]
            [:div.flow-form__field
             [:label.flow-label {:for "confirm-password"} "Confirm Password"]
             [:input.flow-input
              {:id "confirm-password"
               :type "password"
               :value @confirm-pw
               :placeholder "Re-enter new password"
               :class (when (and (not-empty @confirm-pw) (not passwords-match?))
                        "flow-input--error")
               :on-change #(reset! confirm-pw (-> % .-target .-value))}]
             (when (and (not-empty @confirm-pw) (not passwords-match?))
               [:span.flow-input-hint.flow-input-hint--error
                "Passwords do not match"])]]
           [:div.flow-form__actions
            [:button.flow-btn.flow-btn--primary
             {:disabled (or (not can-submit?) saving?)
              :on-click #(do
                           (rf/dispatch [:profile/change-password
                                         {:current_password @current-pw
                                          :new_password @new-pw}])
                           (reset! current-pw "")
                           (reset! new-pw "")
                           (reset! confirm-pw ""))}
             (if saving? "Changing..." "Change Password")]]]]]))))

(defn statistics-section []
  (let [stats @(rf/subscribe [:profile/statistics])
        user @(rf/subscribe [:auth/user])
        created-at (:user/created-at user)
        member-since (when created-at
                       (.toLocaleDateString (js/Date. created-at)
                                            "en-US"
                                            #js {:year "numeric" :month "long" :day "numeric"}))]
    [:div.profile-section.profile-section--stats
     [:div.profile-section__header
      [:div.profile-section__header-content
       [:span.profile-section__icon [icon :bar-chart {:width 20 :height 20}]]
       [:div
        [:h3.profile-section__title "Account Statistics"]
        [:p.profile-section__subtitle "Your activity overview"]]]]
     [:div.profile-section__body
      [:div.profile-stats-grid
       [:div.profile-stat-card
        [:div.profile-stat-card__icon
         [icon :calendar {:width 24 :height 24}]]
        [:div.profile-stat-card__content
         [:span.profile-stat-card__label "Member Since"]
         [:span.profile-stat-card__value (or member-since "N/A")]]]
       [:div.profile-stat-card
        [:div.profile-stat-card__icon
         [icon :list {:width 24 :height 24}]]
        [:div.profile-stat-card__content
         [:span.profile-stat-card__label "Total Transactions"]
         [:span.profile-stat-card__value (or (:total-transactions stats) 0)]]]
       [:div.profile-stat-card.profile-stat-card--income
        [:div.profile-stat-card__icon
         [icon :trending-up {:width 24 :height 24}]]
        [:div.profile-stat-card__content
         [:span.profile-stat-card__label "Income Entries"]
         [:span.profile-stat-card__value (or (:income-count stats) 0)]]]
       [:div.profile-stat-card.profile-stat-card--expense
        [:div.profile-stat-card__icon
         [icon :trending-down {:width 24 :height 24}]]
        [:div.profile-stat-card__content
         [:span.profile-stat-card__label "Expense Entries"]
         [:span.profile-stat-card__value (or (:expense-count stats) 0)]]]]]]))

(defn preferences-section []
  (let [user @(rf/subscribe [:auth/user])
        theme @(rf/subscribe [:app/theme])
        current-currency (or (some-> (:user/preferred-currency user) keyword) :COP)]
    [:div.profile-section
     [:div.profile-section__header
      [:div.profile-section__header-content
       [:span.profile-section__icon [icon :dashboard {:width 20 :height 20}]]
       [:div
        [:h3.profile-section__title "Preferences"]
        [:p.profile-section__subtitle "Customize your experience"]]]]
     [:div.profile-section__body
      [:div.profile-preference
       [:div.profile-preference__info
        [:span.profile-preference__label "Default Currency"]
        [:span.profile-preference__desc "Currency used for new transactions"]]
       [:div.flow-segmented
        (for [curr [:COP :USD]]
          ^{:key curr}
          [:button.flow-segmented__option
           {:class (when (= curr current-currency) "flow-segmented__option--active")
            :on-click #(rf/dispatch [:profile/update-preferences {:currency curr}])}
           (name curr)])]]
      [:div.profile-preference
       [:div.profile-preference__info
        [:span.profile-preference__label "Theme"]
        [:span.profile-preference__desc "Choose light or dark mode"]]
       [:div.flow-segmented
        (for [[key label] [[:light "Light"] [:dark "Dark"]]]
          ^{:key key}
          [:button.flow-segmented__option
           {:class (when (= key theme) "flow-segmented__option--active")
            :on-click #(rf/dispatch [:app/toggle-theme])}
           label])]]]]))

(defn danger-zone-section []
  (let [show-delete-modal? (r/atom false)
        delete-password (r/atom "")
        deleting? @(rf/subscribe [:profile/deleting?])]
    (fn []
      [:div.profile-section.profile-section--danger
       [:div.profile-section__header
        [:div.profile-section__header-content
         [:span.profile-section__icon.profile-section__icon--danger
          [icon :shield {:width 20 :height 20}]]
         [:div
          [:h3.profile-section__title.profile-section__title--danger "Danger Zone"]
          [:p.profile-section__subtitle "Irreversible actions"]]]]
       [:div.profile-section__body
        [:div.profile-danger-action
         [:div.profile-danger-action__info
          [:span.profile-danger-action__label "Export Data"]
          [:span.profile-danger-action__desc
           "Download all your data as a JSON file"]]
         [:button.flow-btn.flow-btn--secondary
          {:on-click #(rf/dispatch [:profile/export-data])}
          [icon :download {:width 16 :height 16}]
          [:span "Export"]]]
        [:div.profile-danger-action
         [:div.profile-danger-action__info
          [:span.profile-danger-action__label "Delete Account"]
          [:span.profile-danger-action__desc
           "Permanently delete your account and all data"]]
         [:button.flow-btn.flow-btn--danger
          {:on-click #(reset! show-delete-modal? true)}
          [icon :trash {:width 16 :height 16}]
          [:span "Delete Account"]]]]

       (when @show-delete-modal?
         [:div.flow-modal-overlay
          {:on-click #(reset! show-delete-modal? false)}
          [:div.flow-modal
           {:on-click #(.stopPropagation %)}
           [:div.flow-modal__header
            [:h3.flow-modal__title "Delete Account"]
            [:button.flow-modal__close
             {:on-click #(reset! show-delete-modal? false)}
             [icon :x {:width 20 :height 20}]]]
           [:div.flow-modal__body
            [:p.flow-modal__warning
             "This action cannot be undone. All your data will be permanently deleted."]
            [:div.flow-form__field
             [:label.flow-label "Enter your password to confirm"]
             [:input.flow-input
              {:type "password"
               :placeholder "Your password"
               :value @delete-password
               :on-change #(reset! delete-password (-> % .-target .-value))}]]]
           [:div.flow-modal__footer
            [:button.flow-btn.flow-btn--secondary
             {:on-click #(do (reset! show-delete-modal? false)
                             (reset! delete-password ""))}
             "Cancel"]
            [:button.flow-btn.flow-btn--danger
             {:disabled (or (empty? @delete-password) deleting?)
              :on-click #(rf/dispatch [:profile/delete-account @delete-password])}
             (if deleting? "Deleting..." "Delete Account")]]]])])))

(defn profile-skeleton []
  [:div.profile-skeleton
   [:div.profile-skeleton__avatar]
   (for [i (range 4)]
     ^{:key i}
     [:div.profile-skeleton__section])])

(defn profile-view []
  (r/create-class
   {:component-did-mount
    (fn [_]
      (rf/dispatch [:profile/fetch]))

    :reagent-render
    (fn []
      (let [loading? @(rf/subscribe [:profile/loading?])
            user @(rf/subscribe [:auth/user])]
        [:div.profile-page
         [:div.profile-header
          [:h1.profile-header__title "Profile"]
          [:p.profile-header__subtitle
           (str "Manage your account settings")]]

         (if loading?
           [profile-skeleton]

           [:div.profile-content
            [avatar-section]
            [profile-edit-section]
            [password-change-section]
            [statistics-section]
            [preferences-section]
            [danger-zone-section]])]))}))
