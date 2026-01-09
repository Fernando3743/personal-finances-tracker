(ns finance.views.auth
  "Login and registration views."
  (:require [re-frame.core :as rf]
            [reagent.core :as r]))

(defn login-form []
  (let [email (r/atom "")
        password (r/atom "")]
    (fn []
      (let [loading? @(rf/subscribe [:auth/loading?])
            error @(rf/subscribe [:auth/error])]
        [:div.flow-auth-container
         [:div.flow-auth-card
          [:h1.flow-auth-title "Sign In"]
          [:p.flow-auth-subtitle "Welcome back to Finance Tracker"]

          (when error
            [:div.flow-auth-error
             [:span error]])

          [:form.flow-auth-form
           {:on-submit (fn [e]
                         (.preventDefault e)
                         (rf/dispatch [:auth/login @email @password]))}

           [:div.flow-input-group
            [:label.flow-label {:for "email"} "Email"]
            [:input.flow-input
             {:id "email"
              :type "email"
              :placeholder "you@example.com"
              :value @email
              :disabled loading?
              :on-change #(reset! email (-> % .-target .-value))}]]

           [:div.flow-input-group
            [:label.flow-label {:for "password"} "Password"]
            [:input.flow-input
             {:id "password"
              :type "password"
              :placeholder "Enter your password"
              :value @password
              :disabled loading?
              :on-change #(reset! password (-> % .-target .-value))}]]

           [:button.flow-btn.flow-btn-primary.flow-btn-full
            {:type "submit"
             :disabled (or loading? (empty? @email) (empty? @password))}
            (if loading? "Signing in..." "Sign In")]]

          [:p.flow-auth-link
           "Don't have an account? "
           [:a {:on-click #(rf/dispatch [:app/navigate :register])}
            "Create one"]]]]))))

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
        [:div.flow-auth-container
         [:div.flow-auth-card
          [:h1.flow-auth-title "Create Account"]
          [:p.flow-auth-subtitle "Start tracking your finances today"]

          (when error
            [:div.flow-auth-error
             [:span error]])

          [:form.flow-auth-form
           {:on-submit (fn [e]
                         (.preventDefault e)
                         (when (and passwords-match? password-long-enough?)
                           (rf/dispatch [:auth/register @email @password @name])))}

           [:div.flow-input-group
            [:label.flow-label {:for "name"} "Name"]
            [:input.flow-input
             {:id "name"
              :type "text"
              :placeholder "Your name"
              :value @name
              :disabled loading?
              :on-change #(reset! name (-> % .-target .-value))}]]

           [:div.flow-input-group
            [:label.flow-label {:for "email"} "Email"]
            [:input.flow-input
             {:id "email"
              :type "email"
              :placeholder "you@example.com"
              :value @email
              :disabled loading?
              :on-change #(reset! email (-> % .-target .-value))}]]

           [:div.flow-input-group
            [:label.flow-label {:for "password"} "Password"]
            [:input.flow-input
             {:id "password"
              :type "password"
              :placeholder "At least 8 characters"
              :value @password
              :disabled loading?
              :class (when (and (not-empty @password) (not password-long-enough?))
                       "flow-input-error")
              :on-change #(reset! password (-> % .-target .-value))}]
            (when (and (not-empty @password) (not password-long-enough?))
              [:span.flow-input-hint.flow-input-hint-error
               "Password must be at least 8 characters"])]

           [:div.flow-input-group
            [:label.flow-label {:for "confirm-password"} "Confirm Password"]
            [:input.flow-input
             {:id "confirm-password"
              :type "password"
              :placeholder "Re-enter your password"
              :value @confirm-password
              :disabled loading?
              :class (when (and (not-empty @confirm-password) (not passwords-match?))
                       "flow-input-error")
              :on-change #(reset! confirm-password (-> % .-target .-value))}]
            (when (and (not-empty @confirm-password) (not passwords-match?))
              [:span.flow-input-hint.flow-input-hint-error
               "Passwords do not match"])]

           [:button.flow-btn.flow-btn-primary.flow-btn-full
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
            "Sign in"]]]]))))
