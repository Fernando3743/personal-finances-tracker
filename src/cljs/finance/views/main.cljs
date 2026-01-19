(ns finance.views.main
  "Main layout, navigation, and UI components."
  (:require [re-frame.core :as rf]
            [reagent.core :as r]
            ["react-dom" :as react-dom]
            [finance.views.dashboard :as dashboard]
            [finance.views.transactions :as transactions]
            [finance.views.incomes :as incomes]
            [finance.views.expenses :as expenses]
            [finance.views.recurring :as recurring]
            [finance.views.budgets :as budgets]
            [finance.views.analytics :as analytics]
            [finance.views.wallets :as wallets]
            [finance.views.profile :as profile]
            [finance.views.auth :as auth]
            [finance.views.transaction-panel :as tx-panel]
            [finance.views.recurring-panel :as recurring-panel]
            [finance.routes :as routes]
            [finance.components.icons :refer [icon]]
            [finance.utils.validation :as validation]))

(defn theme-toggle []
  (let [theme @(rf/subscribe [:app/theme])]
    [:button {:class "p-2 rounded-lg text-gray-600 dark:text-gray-400 hover:text-gray-900 dark:hover:text-gray-50 hover:bg-gray-100 dark:hover:bg-neutral-800 transition-colors"
              :on-click #(rf/dispatch [:app/toggle-theme])
              :aria-label (if (= theme :light) "Switch to dark mode" "Switch to light mode")}
     (if (= theme :light)
       [icon :moon {:width 20 :height 20}]
       [icon :sun {:width 20 :height 20}])]))

(defn breadcrumbs []
  (let [active-view @(rf/subscribe [:app/current-route])
        page-name (case active-view
                    :dashboard "Dashboard"
                    :incomes "Incomes"
                    :expenses "Expenses"
                    :transactions "Transactions"
                    :recurring "Recurring"
                    :budgets "Budgets"
                    :analytics "Analytics"
                    :wallets "Wallets"
                    :add-transaction "Add Transaction"
                    :profile "Profile"
                    "Dashboard")]
    [:div {:class "flex items-center gap-2 text-sm text-gray-500 dark:text-gray-400"}
     [:span "Pages"]
     [icon :chevron-right {:width 16 :height 16}]
     [:span {:class "font-medium text-gray-900 dark:text-gray-50"} page-name]]))

(defn user-profile []
  (let [user @(rf/subscribe [:auth/user])
        user-name (or (:user/name user) "User")
        avatar-url (validation/sanitize-url (:user/avatar-url user))
        initial (first user-name)]
    [:div {:class "flex items-center gap-3 pl-4 border-l border-gray-200 dark:border-neutral-700"}
     [:div {:class "hidden md:flex flex-col items-end cursor-pointer"
            :on-click #(rf/dispatch [:app/navigate :profile])
            :title "Go to Profile"}
      [:span {:class "text-sm font-medium text-gray-900 dark:text-gray-50"} user-name]
      [:span {:class "text-xs text-gray-500 dark:text-gray-400"} "Admin"]]
     [:div {:class "w-9 h-9 rounded-full bg-blue-600 flex items-center justify-center text-white font-medium cursor-pointer"
            :on-click #(rf/dispatch [:app/navigate :profile])
            :title "Go to Profile"}
      (if avatar-url
        [:img {:src avatar-url :alt user-name :class "w-full h-full rounded-full object-cover"}]
        initial)]
     [:button {:class "p-2 rounded-lg text-gray-600 dark:text-gray-400 hover:text-red-500 hover:bg-red-100 dark:hover:bg-red-900/20 transition-colors"
               :on-click #(rf/dispatch [:auth/logout])
               :aria-label "Logout"}
      [icon :logout {:width 20 :height 20}]]]))

(defn header []
  [:header {:class "h-16 bg-white border-b border-gray-200 px-8 flex items-center justify-between sticky top-0 z-20 hidden lg:flex dark:bg-neutral-800 dark:border-neutral-700"}
   [breadcrumbs]
   [:div {:class "flex items-center gap-4"}
    [theme-toggle]
    [:button {:class (str "inline-flex items-center gap-2 px-4 py-2 rounded-lg font-medium text-white "
                          "bg-violet-600 hover:bg-violet-700 "
                          "active:scale-[0.98] transition-all duration-200")
              :on-click #(rf/dispatch [:app/open-panel :add-transaction])}
     [icon :plus {:width 16 :height 16}]
     [:span "Add Transaction"]]
    [user-profile]]])

(defn mobile-header []
  (let [user @(rf/subscribe [:auth/user])
        avatar-url (validation/sanitize-url (:user/avatar-url user))
        initial (first (or (:user/name user) "U"))]
    [:header {:class "flex lg:hidden items-center justify-between h-14 px-4 bg-white dark:bg-neutral-800 border-b border-gray-200 dark:border-neutral-700"}
     [:div {:class "flex items-center gap-3 cursor-pointer"
            :on-click #(rf/dispatch [:app/navigate :dashboard])}
      [:div {:class "w-8 h-8 rounded-lg bg-gradient-to-br from-violet-600 to-violet-400 flex items-center justify-center text-white"}
       [icon :dollar {:width 18 :height 18}]]
      [:span {:class "font-semibold text-gray-900 dark:text-gray-50"} "Finance Tracker"]]
     [:div {:class "flex items-center gap-2"}
      [theme-toggle]
      [:div {:class "w-8 h-8 rounded-full bg-blue-600 flex items-center justify-center text-white text-sm font-medium cursor-pointer"
             :on-click #(rf/dispatch [:app/navigate :profile])}
       (if avatar-url
         [:img {:src avatar-url :alt "Profile" :class "w-full h-full rounded-full object-cover"}]
         initial)]]]))

(defn sidebar []
  (let [active-view @(rf/subscribe [:app/current-route])
        user @(rf/subscribe [:auth/user])
        user-name (or (:user/name user) "User")
        avatar-url (validation/sanitize-url (:user/avatar-url user))
        initial (first user-name)
        nav-item (fn [route icon-name label]
                   [:a {:class (str "flex items-center gap-3 px-4 py-3 rounded-lg text-sm font-medium cursor-pointer transition-colors "
                                    (if (= active-view route)
                                      "bg-violet-50 dark:bg-violet-900/20 text-violet-700 dark:text-violet-400"
                                      "text-gray-600 dark:text-gray-400 hover:text-gray-900 dark:hover:text-gray-50 hover:bg-gray-50 dark:hover:bg-neutral-800"))
                        :on-click #(rf/dispatch [:app/navigate route])}
                    [:span {:class (str "w-5 h-5 " (if (= active-view route) "text-violet-600 dark:text-violet-400" "text-gray-400 dark:text-gray-500"))}
                     [icon icon-name {:width 20 :height 20}]]
                    [:span label]])]
    [:aside {:class "w-[280px] border-r border-gray-200 bg-white h-screen flex flex-col fixed left-0 top-0 z-30 hidden lg:flex dark:bg-neutral-800 dark:border-neutral-700"}
     [:div {:class "flex items-center gap-3 p-6 cursor-pointer"
            :on-click #(rf/dispatch [:app/navigate :dashboard])}
      [:div {:class "h-8 w-8 rounded-lg bg-violet-600 flex items-center justify-center text-white font-bold text-lg"}
       "$"]
      [:span {:class "font-bold text-xl text-gray-900 dark:text-gray-50"} "Finance Tracker"]]

     [:nav {:class "flex-1 overflow-y-auto px-4 py-4 space-y-1"}
      [nav-item :dashboard :dashboard "Dashboard"]
      [nav-item :incomes :trending-up "Incomes"]
      [nav-item :expenses :trending-down "Expenses"]
      [nav-item :transactions :list "Transactions"]
      [nav-item :recurring :refresh-cw "Recurring"]
      [nav-item :budgets :piggy-bank "Budgets"]
      [nav-item :analytics :bar-chart "Analytics"]
      [nav-item :wallets :wallet "Wallets"]]

     [:div {:class "p-4 border-t border-gray-200 dark:border-neutral-700 space-y-4"}
      [:button {:class (str "w-full flex items-center justify-start gap-2 h-10 px-4 rounded-md text-sm font-medium "
                            "text-gray-900 dark:text-gray-50 bg-gray-50 dark:bg-neutral-700 "
                            "border border-gray-200 dark:border-neutral-600 "
                            "hover:bg-gray-100 dark:hover:bg-neutral-600 transition-colors")
                :on-click #(rf/dispatch [:app/open-panel :add-transaction])}
       [icon :plus {:width 16 :height 16}]
       [:span "Add New"]]
      [:div {:class (str "flex items-center gap-3 p-2 rounded-lg cursor-pointer transition-colors "
                         (if (= active-view :profile)
                           "bg-violet-50 dark:bg-violet-900/20"
                           "hover:bg-gray-100 dark:hover:bg-neutral-800"))
             :on-click #(rf/dispatch [:app/navigate :profile])}
       [:div {:class "h-10 w-10 rounded-full bg-violet-600 flex items-center justify-center text-white font-medium"}
        (if avatar-url
          [:img {:src avatar-url :alt user-name :class "w-full h-full rounded-full object-cover"}]
          initial)]
       [:div {:class "flex-1 min-w-0"}
        [:div {:class "text-sm font-medium text-gray-900 dark:text-gray-50 truncate"} user-name]
        [:div {:class "text-xs text-gray-500 dark:text-gray-400 truncate"} "Pro Member"]]
       [icon :chevron-right {:width 16 :height 16 :class "text-gray-400 dark:text-gray-500"}]]]]))

(defn tab-bar []
  (let [active-view @(rf/subscribe [:app/current-route])]
    [:nav {:class "fixed bottom-0 left-0 right-0 lg:hidden flex items-center justify-around h-14 bg-white dark:bg-neutral-800 border-t border-gray-200 dark:border-neutral-700 z-30 safe-area-inset-bottom"}
     [:a {:class (str "flex flex-col items-center gap-1 px-4 py-2 rounded-lg transition-colors "
                      (if (= active-view :dashboard)
                        "text-violet-600 dark:text-violet-400"
                        "text-gray-600 dark:text-gray-400"))
          :on-click #(rf/dispatch [:app/navigate :dashboard])}
      [:span [icon :dashboard {:width 20 :height 20}]]
      [:span {:class "text-xs font-medium"} "Dashboard"]]

     [:button {:class (str "flex items-center justify-center w-12 h-12 -mt-6 rounded-full "
                           "bg-violet-600 hover:bg-violet-700 text-white shadow-lg "
                           "active:scale-95 transition-transform")
               :on-click #(rf/dispatch [:app/open-panel :add-transaction])
               :aria-label "Add Transaction"}
      [icon :plus {:width 20 :height 20}]]

     [:a {:class (str "flex flex-col items-center gap-1 px-4 py-2 rounded-lg transition-colors "
                      (if (= active-view :transactions)
                        "text-violet-600 dark:text-violet-400"
                        "text-gray-600 dark:text-gray-400"))
          :on-click #(rf/dispatch [:app/navigate :transactions])}
      [:span [icon :list {:width 20 :height 20}]]
      [:span {:class "text-xs font-medium"} "Transactions"]]]))

(defn toast-icon [type]
  (case type
    :success [icon :check {:width 20 :height 20}]
    :error [icon :alert-circle {:width 20 :height 20}]
    :warning [icon :alert-circle {:width 20 :height 20}]
    :info [icon :alert-circle {:width 20 :height 20}]
    [icon :alert-circle {:width 20 :height 20}]))

(defn toast [{:keys [id type title message]}]
  (let [type-classes (case type
                       :success "bg-green-100 dark:bg-green-900/20 border-green-500 text-green-600 dark:text-green-500"
                       :error "bg-red-100 dark:bg-red-900/20 border-red-500 text-red-600 dark:text-red-500"
                       :warning "bg-amber-100 dark:bg-amber-900/20 border-amber-500 text-amber-500"
                       :info "bg-blue-100 dark:bg-blue-900/20 border-blue-500 text-blue-500"
                       "bg-white dark:bg-neutral-800 border-neutral-200 dark:border-neutral-700 text-neutral-900 dark:text-neutral-50")]
    [:div {:class (str "flex items-start gap-3 p-4 rounded-lg border shadow-lg animate-slide-in-right " type-classes)}
     [:span {:class "flex-shrink-0 mt-0.5"}
      [toast-icon type]]
     [:div {:class "flex-1 min-w-0"}
      (when title [:div {:class "font-medium"} title])
      (when message [:div {:class "text-sm opacity-90 mt-0.5"} message])]
     [:button {:class "flex-shrink-0 p-1 rounded hover:bg-black/10 transition-colors"
               :on-click #(rf/dispatch [:app/dismiss-toast id])}
      [icon :x {:width 16 :height 16}]]]))

(defn toast-container []
  (let [toasts @(rf/subscribe [:app/toast-queue])]
    (react-dom/createPortal
      (r/as-element
        [:div {:class "fixed top-4 right-4 z-80 flex flex-col gap-2 max-w-sm w-full"}
         (for [t toasts]
           ^{:key (:id t)}
           [toast t])])
      (.-body js/document))))

(defn loading-screen []
  [:div {:class "min-h-screen bg-[#F9FAFB] dark:bg-neutral-950 flex flex-col items-center justify-center gap-4"}
   [:div {:class "w-10 h-10 border-4 border-violet-600 dark:border-violet-400 border-t-transparent rounded-full animate-spin"}]
   [:p {:class "text-gray-600 dark:text-gray-400"}  "Loading..."]])

(defn main-content []
  (let [active-view @(rf/subscribe [:app/current-route])]
    [:main {:class "flex-1 p-8 animate-fade-in overflow-y-auto"}
     (case active-view
       :dashboard [dashboard/dashboard-view]
       :incomes [incomes/incomes-view]
       :expenses [expenses/expenses-view]
       :transactions [transactions/transaction-list]
       :recurring [recurring/recurring-view]
       :budgets [budgets/budgets-view]
       :analytics [analytics/analytics-view]
       :wallets [wallets/wallets-view]
       :add-transaction [dashboard/dashboard-view]
       :profile [profile/profile-view]
       [dashboard/dashboard-view])]))

(defn auth-layout []
  (let [active-view @(rf/subscribe [:app/current-route])
        theme @(rf/subscribe [:app/theme])]
    [:div {:class "min-h-screen flex"}
     [:div {:class "hidden lg:flex flex-1 bg-gradient-to-br from-violet-700 via-violet-600 to-violet-500 items-center justify-center p-12"}
      [:div {:class "max-w-md text-white"}
       [:div {:class "flex items-center gap-3 mb-8"}
        [:div {:class "w-12 h-12 rounded-xl bg-white/20 backdrop-blur flex items-center justify-center"}
         [icon :dollar {:width 32 :height 32}]]
        [:h1 {:class "text-3xl font-bold"} "Finance Tracker"]]
       [:blockquote {:class "text-xl text-white/90 italic leading-relaxed"}
        "\"The best way to predict your future is to create it. Start managing your wealth today.\""]]]

     [:div {:class "flex-1 flex items-center justify-center p-6 bg-[#F9FAFB] dark:bg-neutral-950"}
      (case active-view
        :login [auth/login-form]
        :register [auth/register-form]
        [auth/login-form])]

     [:button {:class "fixed top-4 right-4 p-3 rounded-lg bg-white dark:bg-neutral-800 border border-gray-200 dark:border-neutral-700 text-gray-600 dark:text-gray-400 hover:text-gray-900 dark:hover:text-gray-50 transition-colors shadow-sm"
               :on-click #(rf/dispatch [:app/toggle-theme])
               :aria-label (if (= theme :light) "Switch to dark mode" "Switch to light mode")}
      (if (= theme :light)
        [icon :moon {:width 24 :height 24}]
        [icon :sun {:width 24 :height 24}])]]))

(defn app-layout []
  [:div {:class "min-h-screen bg-[#F9FAFB] dark:bg-neutral-950"}
   [:div {:class "lg:hidden fixed top-0 left-0 right-0 z-20"}
    [mobile-header]]
   [sidebar]
   [:div {:class "flex-1 lg:ml-[280px] flex flex-col min-h-screen pt-14 lg:pt-0 pb-14 lg:pb-0"}
    [header]
    [main-content]]
   [tab-bar]
   [tx-panel/transaction-panel]
   [recurring-panel/recurring-panel]])

(defn main-panel []
  (let [authenticated? @(rf/subscribe [:auth/authenticated?])
        auth-initialized? @(rf/subscribe [:auth/initialized?])
        active-view @(rf/subscribe [:app/current-route])]
    [:<>
     (cond
       (not auth-initialized?)
       [loading-screen]

       (and (contains? routes/public-routes active-view) (not authenticated?))
       [auth-layout]

       (not authenticated?)
       (do (rf/dispatch [:app/navigate :login])
           [loading-screen])

       :else
       [app-layout])
     [toast-container]]))
