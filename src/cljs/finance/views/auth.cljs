(ns finance.views.auth
  "Login and registration views."
  (:require [re-frame.core :as rf]
            [reagent.core :as r]
            [finance.components.icons :refer [icon]]))

(defn login-form []
  (let [email (r/atom "")
        password (r/atom "")
        remember-me (r/atom false)]
    (fn []
      (let [loading? @(rf/subscribe [:auth/loading?])
            error @(rf/subscribe [:auth/error])]
        [:div.flow-auth-card.flow-auth-card--enhanced

         [:div.flow-auth-mobile-logo
          [:div.flow-auth-mobile-logo__icon
           [icon :dollar {:width 32 :height 32}]]]

         [:div.flow-auth-header-text
          [:h2.flow-auth-title "Welcome back"]
          [:p.flow-auth-subtitle "Enter your credentials to access your dashboard."]]

         (when error
           [:div.flow-auth-error
            [:span error]])

         [:form.flow-auth-form
          {:on-submit (fn [e]
                        (.preventDefault e)
                        (rf/dispatch [:auth/login @email @password]))}

          [:div.flow-input-group
           [:label.flow-label {:for "email"} "Email address"]
           [:div.flow-input-with-icon
            (when (empty? @email)
              [:div.flow-input-with-icon__icon
               [icon :mail {:width 20 :height 20}]])
            [:input.flow-input
             {:id "email"
              :type "email"
              :placeholder "       you@example.com"
              :value @email
              :disabled loading?
              :on-change #(reset! email (-> % .-target .-value))}]]]
          [:div.flow-input-group
           [:label.flow-label {:for "password"} "Password"]
           [:div.flow-input-with-icon
            (when (empty? @password)
              [:div.flow-input-with-icon__icon
               [icon :lock {:width 20 :height 20}]])
            [:input.flow-input
             {:id "password"
              :type "password"
              :placeholder "       ••••••••"
              :value @password
              :disabled loading?
              :on-change #(reset! password (-> % .-target .-value))}]]]

          [:div.flow-auth-options
           [:div.flow-auth-checkbox
            [:input {:type "checkbox"
                     :id "remember-me"
                     :checked @remember-me
                     :on-change #(swap! remember-me not)}]
            [:label {:for "remember-me"} "Remember me"]]
           [:a.flow-auth-forgot
            {:on-click #(rf/dispatch [:app/show-toast
                                      {:type :info
                                       :title "Coming Soon"
                                       :message "Password reset will be available soon."}])}
            "Forgot password?"]]

          [:button.flow-btn.flow-btn-primary.flow-btn-full.flow-btn--lift
           {:type "submit"
            :disabled (or loading? (empty? @email) (empty? @password))}
           (if loading? "Signing in..." "Sign In")]]

         [:div.flow-auth-divider
          [:div.flow-auth-divider__line]
          [:div.flow-auth-divider__text
           [:span "Or continue with"]]]

         [:div.flow-auth-social
          [:button.flow-auth-social__btn
           {:type "button"
            :on-click #(rf/dispatch [:app/show-toast
                                     {:type :info
                                      :title "Coming Soon"
                                      :message "Google login will be available soon."}])}
           [icon :google {:width 20 :height 20}]
           [:span "Google"]]
          [:button.flow-auth-social__btn
           {:type "button"
            :on-click #(rf/dispatch [:app/show-toast
                                     {:type :info
                                      :title "Coming Soon"
                                      :message "GitHub login will be available soon."}])}
           [icon :github {:width 20 :height 20}]
           [:span "GitHub"]]]

         [:p.flow-auth-link
          "Don't have an account? "
          [:a {:on-click #(rf/dispatch [:app/navigate :register])}
           "Sign up for free"]]]))))

(defn register-form []
  (let [name (r/atom "")
        email (r/atom "")
        password (r/atom "")
        confirm-password (r/atom "")]
    (fn []
      (let [loading? @(rf/subscribe [:auth/loading?])
            error @(rf/subscribe [:auth/error])
            passwords-match? (= @password @confirm-password)
            password-long-enough? (>= (count @password) 8)]
        [:div.flow-auth-card.flow-auth-card--enhanced

         [:div.flow-auth-mobile-logo
          [:div.flow-auth-mobile-logo__icon
           [icon :dollar {:width 32 :height 32}]]]

         [:div.flow-auth-header-text
          [:h2.flow-auth-title "Create Account"]
          [:p.flow-auth-subtitle "Start tracking your finances today."]]

         (when error
           [:div.flow-auth-error
            [:span error]])

         [:form.flow-auth-form
          {:on-submit (fn [e]
                        (.preventDefault e)
                        (when (and passwords-match? password-long-enough?)
                          (rf/dispatch [:auth/register @email @password @name])))}

          [:div.flow-input-group
           [:label.flow-label {:for "name"} "Full name"]
           [:div.flow-input-with-icon
            [:div.flow-input-with-icon__icon
             [icon :user {:width 20 :height 20}]]
            [:input.flow-input
             {:id "name"
              :type "text"
              :placeholder "John Doe"
              :value @name
              :disabled loading?
              :on-change #(reset! name (-> % .-target .-value))}]]]

          [:div.flow-input-group
           [:label.flow-label {:for "reg-email"} "Email address"]
           [:div.flow-input-with-icon
            [:div.flow-input-with-icon__icon
             [icon :mail {:width 20 :height 20}]]
            [:input.flow-input
             {:id "reg-email"
              :type "email"
              :placeholder "you@example.com"
              :value @email
              :disabled loading?
              :on-change #(reset! email (-> % .-target .-value))}]]]

          [:div.flow-input-group
           [:label.flow-label {:for "reg-password"} "Password"]
           [:div.flow-input-with-icon
            [:div.flow-input-with-icon__icon
             [icon :lock {:width 20 :height 20}]]
            [:input.flow-input
             {:id "reg-password"
              :type "password"
              :placeholder "At least 8 characters"
              :value @password
              :disabled loading?
              :class (when (and (not-empty @password) (not password-long-enough?))
                       "flow-input-error")
              :on-change #(reset! password (-> % .-target .-value))}]]
           (when (and (not-empty @password) (not password-long-enough?))
             [:span.flow-input-hint.flow-input-hint-error
              "Password must be at least 8 characters"])]

          [:div.flow-input-group
           [:label.flow-label {:for "confirm-password"} "Confirm password"]
           [:div.flow-input-with-icon
            [:div.flow-input-with-icon__icon
             [icon :lock {:width 20 :height 20}]]
            [:input.flow-input
             {:id "confirm-password"
              :type "password"
              :placeholder "Re-enter your password"
              :value @confirm-password
              :disabled loading?
              :class (when (and (not-empty @confirm-password) (not passwords-match?))
                       "flow-input-error")
              :on-change #(reset! confirm-password (-> % .-target .-value))}]]
           (when (and (not-empty @confirm-password) (not passwords-match?))
             [:span.flow-input-hint.flow-input-hint-error
              "Passwords do not match"])]

          [:button.flow-btn.flow-btn-primary.flow-btn-full.flow-btn--lift
           {:type "submit"
            :disabled (or loading?
                          (empty? @name)
                          (empty? @email)
                          (empty? @password)
                          (not passwords-match?)
                          (not password-long-enough?))}
           (if loading? "Creating account..." "Create Account")]]

         [:p.flow-auth-link
          "Already have an account? "
          [:a {:on-click #(rf/dispatch [:app/navigate :login])}
           "Sign in"]]]))))
