(ns finance.views.auth
  "Login and registration views."
  (:require [re-frame.core :as rf]
            [finance.components.icons :refer [icon]]
            [finance.utils.validation :as validation]
            [finance.constants :as const]))

(defn login-form []
  (let [loading? @(rf/subscribe [:auth/loading?])
        error @(rf/subscribe [:auth/error])
        form @(rf/subscribe [:auth/login-form])
        email (:email form)
        password (:password form)
        remember-me (:remember-me form)
        email-valid? (validation/valid-email? email)]
        [:div {:class "w-full max-w-md mx-auto p-8 bg-white dark:bg-neutral-800 rounded-2xl shadow-lg border border-neutral-200 dark:border-neutral-700"}
         [:div {:class "flex justify-center mb-6 lg:hidden"}
          [:div {:class "w-12 h-12 rounded-xl bg-gradient-to-br from-purple-600 to-purple-400 flex items-center justify-center text-white"}
           [icon :dollar {:width 32 :height 32}]]]

         [:div {:class "text-center mb-8"}
          [:h2 {:class "text-2xl font-semibold text-neutral-900 dark:text-neutral-50 mb-2"} "Welcome back"]
          [:p {:class "text-neutral-600 dark:text-neutral-400 text-sm"} "Enter your credentials to access your dashboard."]]

         (when error
           [:div {:class "mb-6 p-4 rounded-lg bg-red-100 dark:bg-red-900/20 border border-red-500 text-red-600 dark:text-red-500 text-sm"}
            [:span error]])

         [:form {:class "space-y-5"
                 :on-submit (fn [e]
                              (.preventDefault e)
                              (rf/dispatch [:auth/login email password]))}
          [:div {:class "space-y-1.5"}
           [:label {:class "block text-sm font-medium text-neutral-900 dark:text-neutral-50" :for "email"} "Email address"]
           [:div {:class "relative"}
            (when (empty? email)
              [:div {:class "absolute left-3 top-1/2 -translate-y-1/2 text-neutral-400 dark:text-neutral-500 pointer-events-none"}
               [icon :mail {:width 20 :height 20}]])
            [:input {:class (str "w-full h-11 px-4 rounded-lg border border-neutral-200 dark:border-neutral-700 bg-white dark:bg-neutral-900 text-neutral-900 dark:text-neutral-50 "
                                 "placeholder:text-neutral-400 dark:placeholder:text-neutral-500 "
                                 "focus:outline-none focus:ring-2 focus:ring-purple-700 dark:focus:ring-purple-400 focus:border-transparent "
                                 "disabled:opacity-50 disabled:cursor-not-allowed "
                                 "transition-all duration-200 "
                                 (when (empty? email) "pl-10"))
                     :id "email"
                     :type "email"
                     :placeholder "you@example.com"
                     :value email
                     :disabled loading?
                     :on-change #(rf/dispatch [:auth/update-login-form :email (-> % .-target .-value)])}]]]

          [:div {:class "space-y-1.5"}
           [:label {:class "block text-sm font-medium text-neutral-900 dark:text-neutral-50" :for "password"} "Password"]
           [:div {:class "relative"}
            (when (empty? password)
              [:div {:class "absolute left-3 top-1/2 -translate-y-1/2 text-neutral-400 dark:text-neutral-500 pointer-events-none"}
               [icon :lock {:width 20 :height 20}]])
            [:input {:class (str "w-full h-11 px-4 rounded-lg border border-neutral-200 dark:border-neutral-700 bg-white dark:bg-neutral-900 text-neutral-900 dark:text-neutral-50 "
                                 "placeholder:text-neutral-400 dark:placeholder:text-neutral-500 "
                                 "focus:outline-none focus:ring-2 focus:ring-purple-700 dark:focus:ring-purple-400 focus:border-transparent "
                                 "disabled:opacity-50 disabled:cursor-not-allowed "
                                 "transition-all duration-200 "
                                 (when (empty? password) "pl-10"))
                     :id "password"
                     :type "password"
                     :placeholder "••••••••"
                     :value password
                     :disabled loading?
                     :on-change #(rf/dispatch [:auth/update-login-form :password (-> % .-target .-value)])}]]]

          [:div {:class "flex items-center justify-between text-sm"}
           [:label {:class "flex items-center gap-2 cursor-pointer"}
            [:input {:type "checkbox"
                     :class "w-4 h-4 rounded border-neutral-200 dark:border-neutral-700 text-purple-700 dark:text-purple-400 focus:ring-purple-700 dark:focus:ring-purple-400 focus:ring-offset-0"
                     :id "remember-me"
                     :checked remember-me
                     :on-change #(rf/dispatch [:auth/update-login-form :remember-me (not remember-me)])}]
            [:span {:class "text-neutral-600 dark:text-neutral-400"} "Remember me"]]
           [:button {:type "button"
                     :class "text-purple-700 dark:text-purple-400 hover:text-purple-800 dark:hover:text-purple-300 font-medium transition-colors"
                     :on-click #(rf/dispatch [:app/show-toast
                                              {:type :info
                                               :title "Coming Soon"
                                               :message "Password reset will be available soon."}])}
            "Forgot password?"]]

          [:button {:class (str "w-full h-11 rounded-lg font-medium text-white "
                                "bg-gradient-to-r from-purple-700 to-purple-500 "
                                "hover:shadow-lg hover:-translate-y-0.5 "
                                "active:scale-[0.98] "
                                "disabled:opacity-50 disabled:cursor-not-allowed disabled:hover:translate-y-0 disabled:hover:shadow-none "
                                "transition-all duration-200")
                    :type "submit"
                    :disabled (or loading? (empty? email) (empty? password) (not email-valid?))}
           (if loading? "Signing in..." "Sign In")]]

         [:div {:class "relative my-8"}
          [:div {:class "absolute inset-0 flex items-center"}
           [:div {:class "w-full border-t border-neutral-200 dark:border-neutral-700"}]]
          [:div {:class "relative flex justify-center text-sm"}
           [:span {:class "px-4 bg-white dark:bg-neutral-800 text-neutral-400 dark:text-neutral-500"} "Or continue with"]]]

         [:div {:class "grid grid-cols-2 gap-3"}
          [:button {:type "button"
                    :class (str "flex items-center justify-center gap-2 h-11 px-4 rounded-lg "
                                "border border-neutral-200 dark:border-neutral-700 bg-white dark:bg-neutral-900 text-neutral-900 dark:text-neutral-50 "
                                "hover:bg-neutral-100 dark:hover:bg-neutral-800 hover:border-neutral-300 dark:hover:border-neutral-600 "
                                "transition-all duration-200")
                    :on-click #(rf/dispatch [:app/show-toast
                                             {:type :info
                                              :title "Coming Soon"
                                              :message "Google login will be available soon."}])}
           [icon :google {:width 20 :height 20}]
           [:span "Google"]]
          [:button {:type "button"
                    :class (str "flex items-center justify-center gap-2 h-11 px-4 rounded-lg "
                                "border border-neutral-200 dark:border-neutral-700 bg-white dark:bg-neutral-900 text-neutral-900 dark:text-neutral-50 "
                                "hover:bg-neutral-100 dark:hover:bg-neutral-800 hover:border-neutral-300 dark:hover:border-neutral-600 "
                                "transition-all duration-200")
                    :on-click #(rf/dispatch [:app/show-toast
                                             {:type :info
                                              :title "Coming Soon"
                                              :message "GitHub login will be available soon."}])}
           [icon :github {:width 20 :height 20}]
           [:span "GitHub"]]]

         [:p {:class "mt-8 text-center text-sm text-neutral-600 dark:text-neutral-400"}
          "Don't have an account? "
          [:button {:type "button"
                    :class "text-purple-700 dark:text-purple-400 hover:text-purple-800 dark:hover:text-purple-300 font-medium transition-colors"
                    :on-click #(rf/dispatch [:app/navigate :register])}
           "Sign up for free"]]]))

(defn register-form []
  (let [loading? @(rf/subscribe [:auth/loading?])
        error @(rf/subscribe [:auth/error])
        form @(rf/subscribe [:auth/register-form])
        name-val (:name form)
        email (:email form)
        password (:password form)
        confirm-password (:confirm-password form)
        email-valid? (validation/valid-email? email)
        password-validation (validation/valid-password? password)
        passwords-match? (validation/passwords-match? password confirm-password)
        password-long-enough? (>= (count password) const/password-min-length)]
        [:div {:class "w-full max-w-md mx-auto p-8 bg-white dark:bg-neutral-800 rounded-2xl shadow-lg border border-neutral-200 dark:border-neutral-700"}
         [:div {:class "flex justify-center mb-6 lg:hidden"}
          [:div {:class "w-12 h-12 rounded-xl bg-gradient-to-br from-purple-600 to-purple-400 flex items-center justify-center text-white"}
           [icon :dollar {:width 32 :height 32}]]]

         [:div {:class "text-center mb-8"}
          [:h2 {:class "text-2xl font-semibold text-neutral-900 dark:text-neutral-50 mb-2"} "Create Account"]
          [:p {:class "text-neutral-600 dark:text-neutral-400 text-sm"} "Start tracking your finances today."]]

         (when error
           [:div {:class "mb-6 p-4 rounded-lg bg-red-100 dark:bg-red-900/20 border border-red-500 text-red-600 dark:text-red-500 text-sm"}
            [:span error]])

         [:form {:class "space-y-5"
                 :on-submit (fn [e]
                              (.preventDefault e)
                              (when (and passwords-match? password-long-enough?)
                                (rf/dispatch [:auth/register email password name-val])))}
          [:div {:class "space-y-1.5"}
           [:label {:class "block text-sm font-medium text-neutral-900 dark:text-neutral-50" :for "name"} "Full name"]
           [:div {:class "relative"}
            [:div {:class "absolute left-3 top-1/2 -translate-y-1/2 text-neutral-400 dark:text-neutral-500 pointer-events-none"}
             [icon :user {:width 20 :height 20}]]
            [:input {:class (str "w-full h-11 pl-10 pr-4 rounded-lg border border-neutral-200 dark:border-neutral-700 bg-white dark:bg-neutral-900 text-neutral-900 dark:text-neutral-50 "
                                 "placeholder:text-neutral-400 dark:placeholder:text-neutral-500 "
                                 "focus:outline-none focus:ring-2 focus:ring-purple-700 dark:focus:ring-purple-400 focus:border-transparent "
                                 "disabled:opacity-50 disabled:cursor-not-allowed "
                                 "transition-all duration-200")
                     :id "name"
                     :type "text"
                     :placeholder "John Doe"
                     :value name-val
                     :disabled loading?
                     :on-change #(rf/dispatch [:auth/update-register-form :name (-> % .-target .-value)])}]]]

          [:div {:class "space-y-1.5"}
           [:label {:class "block text-sm font-medium text-neutral-900 dark:text-neutral-50" :for "reg-email"} "Email address"]
           [:div {:class "relative"}
            [:div {:class "absolute left-3 top-1/2 -translate-y-1/2 text-neutral-400 dark:text-neutral-500 pointer-events-none"}
             [icon :mail {:width 20 :height 20}]]
            [:input {:class (str "w-full h-11 pl-10 pr-4 rounded-lg border border-neutral-200 dark:border-neutral-700 bg-white dark:bg-neutral-900 text-neutral-900 dark:text-neutral-50 "
                                 "placeholder:text-neutral-400 dark:placeholder:text-neutral-500 "
                                 "focus:outline-none focus:ring-2 focus:ring-purple-700 dark:focus:ring-purple-400 focus:border-transparent "
                                 "disabled:opacity-50 disabled:cursor-not-allowed "
                                 "transition-all duration-200")
                     :id "reg-email"
                     :type "email"
                     :placeholder "you@example.com"
                     :value email
                     :disabled loading?
                     :on-change #(rf/dispatch [:auth/update-register-form :email (-> % .-target .-value)])}]]]

          [:div {:class "space-y-1.5"}
           [:label {:class "block text-sm font-medium text-neutral-900 dark:text-neutral-50" :for "reg-password"} "Password"]
           [:div {:class "relative"}
            [:div {:class "absolute left-3 top-1/2 -translate-y-1/2 text-neutral-400 dark:text-neutral-500 pointer-events-none"}
             [icon :lock {:width 20 :height 20}]]
            [:input {:class (str "w-full h-11 pl-10 pr-4 rounded-lg border bg-white dark:bg-neutral-900 text-neutral-900 dark:text-neutral-50 "
                                 "placeholder:text-neutral-400 dark:placeholder:text-neutral-500 "
                                 "focus:outline-none focus:ring-2 focus:ring-purple-700 dark:focus:ring-purple-400 focus:border-transparent "
                                 "disabled:opacity-50 disabled:cursor-not-allowed "
                                 "transition-all duration-200 "
                                 (if (and (not-empty password) (not password-long-enough?))
                                   "border-red-500 focus:ring-red-500"
                                   "border-neutral-200 dark:border-neutral-700"))
                     :id "reg-password"
                     :type "password"
                     :placeholder "At least 8 characters"
                     :value password
                     :disabled loading?
                     :on-change #(rf/dispatch [:auth/update-register-form :password (-> % .-target .-value)])}]]
           (when (and (not-empty password) (not password-long-enough?))
             [:span {:class "text-xs text-red-600 dark:text-red-500"} "Password must be at least 8 characters"])]

          [:div {:class "space-y-1.5"}
           [:label {:class "block text-sm font-medium text-neutral-900 dark:text-neutral-50" :for "confirm-password"} "Confirm password"]
           [:div {:class "relative"}
            [:div {:class "absolute left-3 top-1/2 -translate-y-1/2 text-neutral-400 dark:text-neutral-500 pointer-events-none"}
             [icon :lock {:width 20 :height 20}]]
            [:input {:class (str "w-full h-11 pl-10 pr-4 rounded-lg border bg-white dark:bg-neutral-900 text-neutral-900 dark:text-neutral-50 "
                                 "placeholder:text-neutral-400 dark:placeholder:text-neutral-500 "
                                 "focus:outline-none focus:ring-2 focus:ring-purple-700 dark:focus:ring-purple-400 focus:border-transparent "
                                 "disabled:opacity-50 disabled:cursor-not-allowed "
                                 "transition-all duration-200 "
                                 (if (and (not-empty confirm-password) (not passwords-match?))
                                   "border-red-500 focus:ring-red-500"
                                   "border-neutral-200 dark:border-neutral-700"))
                     :id "confirm-password"
                     :type "password"
                     :placeholder "Re-enter your password"
                     :value confirm-password
                     :disabled loading?
                     :on-change #(rf/dispatch [:auth/update-register-form :confirm-password (-> % .-target .-value)])}]]
           (when (and (not-empty confirm-password) (not passwords-match?))
             [:span {:class "text-xs text-red-600 dark:text-red-500"} "Passwords do not match"])]

          [:button {:class (str "w-full h-11 rounded-lg font-medium text-white "
                                "bg-gradient-to-r from-purple-700 to-purple-500 "
                                "hover:shadow-lg hover:-translate-y-0.5 "
                                "active:scale-[0.98] "
                                "disabled:opacity-50 disabled:cursor-not-allowed disabled:hover:translate-y-0 disabled:hover:shadow-none "
                                "transition-all duration-200")
                    :type "submit"
                    :disabled (or loading?
                                  (empty? name-val)
                                  (empty? email)
                                  (not email-valid?)
                                  (empty? password)
                                  (not passwords-match?)
                                  (not password-long-enough?))}
           (if loading? "Creating account..." "Create Account")]]

         [:p {:class "mt-8 text-center text-sm text-neutral-600 dark:text-neutral-400"}
          "Already have an account? "
          [:button {:type "button"
                    :class "text-purple-700 dark:text-purple-400 hover:text-purple-800 dark:hover:text-purple-300 font-medium transition-colors"
                    :on-click #(rf/dispatch [:app/navigate :login])}
           "Sign in"]]]))
