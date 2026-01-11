(ns finance.views.main
  "Main layout, navigation, and UI components."
  (:require [re-frame.core :as rf]
            [reagent.core :as r]
            ["react-dom" :as react-dom]
            [finance.views.dashboard :as dashboard]
            [finance.views.transactions :as transactions]
            [finance.views.wallets :as wallets]
            [finance.views.reports :as reports]
            [finance.views.profile :as profile]
            [finance.views.auth :as auth]
            [finance.routes :as routes]
            [finance.components.icons :refer [icon]]))

(defn theme-toggle []
  (let [theme @(rf/subscribe [:app/theme])]
    [:button.flow-theme-toggle
     {:on-click #(rf/dispatch [:app/toggle-theme])
      :aria-label (if (= theme :light) "Switch to dark mode" "Switch to light mode")}
     (if (= theme :light)
       [icon :moon {:width 20 :height 20 :class "flow-icon-moon"}]
       [icon :sun {:width 20 :height 20 :class "flow-icon-sun"}])]))

(defn breadcrumbs []
  (let [active-view @(rf/subscribe [:app/current-route])
        page-name (case active-view
                    :dashboard "Dashboard"
                    :transactions "Transactions"
                    :wallets "Wallets"
                    :reports "Reports"
                    :add-transaction "Add Transaction"
                    :profile "Profile"
                    "Dashboard")]
    [:div.flow-header__breadcrumbs
     [:span.flow-header__breadcrumb "Pages"]
     [icon :chevron-right {:width 16 :height 16}]
     [:span.flow-header__breadcrumb.flow-header__breadcrumb--current page-name]]))

(defn user-profile []
  (let [user @(rf/subscribe [:auth/user])
        user-name (or (:user/name user) "User")
        avatar-url (:user/avatar-url user)
        initial (first user-name)]
    [:div.flow-header__user-section
     [:div.flow-header__user-info
      {:on-click #(rf/dispatch [:app/navigate :profile])
       :style {:cursor "pointer"}
       :title "Go to Profile"}
      [:span.flow-header__user-name user-name]
      [:span.flow-header__user-role "Admin"]]
     [:div.flow-header__avatar
      {:on-click #(rf/dispatch [:app/navigate :profile])
       :style {:cursor "pointer"}
       :title "Go to Profile"}
      (if avatar-url
        [:img {:src avatar-url :alt user-name}]
        initial)]
     [:button.flow-header__logout
      {:on-click #(rf/dispatch [:auth/logout])
       :aria-label "Logout"}
      [icon :logout {:width 20 :height 20}]]]))

(defn header []
  [:header.flow-header
   [breadcrumbs]
   [:div.flow-header__actions
    [theme-toggle]
    [:button.flow-header__add-btn
     {:on-click #(rf/dispatch [:app/navigate :add-transaction])}
     [icon :plus {:width 16 :height 16}]
     [:span "Add Transaction"]]
    [user-profile]]])

(defn mobile-header []
  (let [user @(rf/subscribe [:auth/user])
        avatar-url (:user/avatar-url user)
        initial (first (or (:user/name user) "U"))]
    [:header.flow-header.flow-header--mobile
     [:div.flow-header__left
      [:div.flow-header__logo [icon :dollar {:width 18 :height 18}]]
      [:span.flow-header__title "Finance Tracker"]]
     [:div.flow-header__actions
      [theme-toggle]
      [:div.flow-header__avatar
       {:on-click #(rf/dispatch [:app/navigate :profile])
        :style {:cursor "pointer"}}
       (if avatar-url
         [:img {:src avatar-url :alt "Profile"}]
         initial)]]]))

(defn sidebar []
  (let [active-view @(rf/subscribe [:app/current-route])
        user @(rf/subscribe [:auth/user])
        user-name (or (:user/name user) "User")
        avatar-url (:user/avatar-url user)
        initial (first user-name)]
    [:aside.flow-sidebar
     [:div.flow-sidebar__header
      [:div.flow-sidebar__logo [icon :dollar {:width 20 :height 20}]]
      [:span.flow-sidebar__title "Finance Tracker"]]
     [:nav.flow-sidebar__nav
      [:a.flow-sidebar__item
       {:class (when (= active-view :dashboard) "flow-sidebar__item--active")
        :on-click #(rf/dispatch [:app/navigate :dashboard])}
       [:span.flow-sidebar__icon [icon :dashboard {:width 20 :height 20}]]
       [:span.flow-sidebar__label "Dashboard"]]
      [:a.flow-sidebar__item
       {:class (when (= active-view :transactions) "flow-sidebar__item--active")
        :on-click #(rf/dispatch [:app/navigate :transactions])}
       [:span.flow-sidebar__icon [icon :list {:width 20 :height 20}]]
       [:span.flow-sidebar__label "Transactions"]]
      [:a.flow-sidebar__item
       {:class (when (= active-view :wallets) "flow-sidebar__item--active")
        :on-click #(rf/dispatch [:app/navigate :wallets])}
       [:span.flow-sidebar__icon [icon :wallet {:width 20 :height 20}]]
       [:span.flow-sidebar__label "Wallets"]]
      [:a.flow-sidebar__item
       {:class (when (= active-view :reports) "flow-sidebar__item--active")
        :on-click #(rf/dispatch [:app/navigate :reports])}
       [:span.flow-sidebar__icon [icon :bar-chart {:width 20 :height 20}]]
       [:span.flow-sidebar__label "Reports"]]]
     [:div.flow-sidebar__footer
      [:button.flow-sidebar__add-btn
       {:on-click #(rf/dispatch [:app/navigate :add-transaction])}
       [icon :plus {:width 16 :height 16}]
       [:span "Add New"]]
      [:div.flow-sidebar__profile
       {:on-click #(rf/dispatch [:app/navigate :profile])
        :class (when (= active-view :profile) "flow-sidebar__profile--active")}
       [:div.flow-sidebar__profile-avatar
        (if avatar-url
          [:img {:src avatar-url :alt user-name}]
          initial)]
       [:div.flow-sidebar__profile-info
        [:span.flow-sidebar__profile-name user-name]
        [:span.flow-sidebar__profile-role "Pro Member"]]
       [icon :chevron-right {:width 16 :height 16 :class "flow-sidebar__profile-arrow"}]]]]))

(defn tab-bar []
  (let [active-view @(rf/subscribe [:app/current-route])]
    [:nav.flow-tab-bar
     [:a.flow-tab-bar__item
      {:class (when (= active-view :dashboard) "flow-tab-bar__item--active")
       :on-click #(rf/dispatch [:app/navigate :dashboard])}
      [:span.flow-tab-bar__icon [icon :dashboard {:width 20 :height 20}]]
      [:span.flow-tab-bar__label "Dashboard"]]

     [:button.flow-tab-bar__fab
      {:on-click #(rf/dispatch [:app/navigate :add-transaction])
       :aria-label "Add Transaction"}
      [icon :plus {:width 20 :height 20}]]

     [:a.flow-tab-bar__item
      {:class (when (= active-view :transactions) "flow-tab-bar__item--active")
       :on-click #(rf/dispatch [:app/navigate :transactions])}
      [:span.flow-tab-bar__icon [icon :list {:width 20 :height 20}]]
      [:span.flow-tab-bar__label "Transactions"]]]))

(defn toast-icon [type]
  (case type
    :success [icon :check {:width 20 :height 20}]
    :error [icon :alert-circle {:width 20 :height 20}]
    :warning [icon :alert-circle {:width 20 :height 20}]
    :info [icon :alert-circle {:width 20 :height 20}]
    [icon :alert-circle {:width 20 :height 20}]))

(defn toast [{:keys [id type title message]}]
  [:div.flow-toast
   {:class [(str "flow-toast--" (name type))
            "flow-toast--visible"]}
   [:span.flow-toast__icon
    [toast-icon type]]
   [:div.flow-toast__content
    (when title [:div.flow-toast__title title])
    (when message [:div.flow-toast__message message])]
   [:button.flow-toast__dismiss
    {:on-click #(rf/dispatch [:app/dismiss-toast id])}
    [icon :x {:width 20 :height 20}]]
   [:div.flow-toast__progress
    [:div.flow-toast__progress-bar]]])

(defn toast-container []
  (let [toasts @(rf/subscribe [:app/toast-queue])]
    (react-dom/createPortal
      (r/as-element
        [:div.flow-toast-container
         (for [t toasts]
           ^{:key (:id t)}
           [toast t])])
      (.-body js/document))))

(defn loading-screen []
  [:div.flow-loading-screen
   [:div.flow-loading-spinner]
   [:p "Loading..."]])

(defn main-content []
  (let [active-view @(rf/subscribe [:app/current-route])]
    [:main.flow-main
     {:class "flow-animate flow-animate-fade-in"}
     (case active-view
       :dashboard [dashboard/dashboard-view]
       :transactions [transactions/transaction-list]
       :wallets [wallets/wallets-view]
       :reports [reports/reports-view]
       :add-transaction [transactions/add-transaction-form]
       :profile [profile/profile-view]
       [dashboard/dashboard-view])]))

(defn auth-layout []
  (let [active-view @(rf/subscribe [:app/current-route])
        theme @(rf/subscribe [:app/theme])]
    [:div.flow-auth-split
     {:class (str "flow-theme-" (name theme))}

     [:div.flow-auth-hero
      [:div.flow-auth-hero__content
       [:div.flow-auth-hero__brand
        [:div.flow-auth-hero__logo
         [icon :dollar {:width 32 :height 32}]]
        [:h1.flow-auth-hero__title "Finance Tracker"]]
       [:blockquote.flow-auth-hero__quote
        "\"The best way to predict your future is to create it. Start managing your wealth today.\""]]]

     [:div.flow-auth-panel
      (case active-view
        :login [auth/login-form]
        :register [auth/register-form]
        [auth/login-form])]

     [:button.flow-auth-theme-toggle
      {:on-click #(rf/dispatch [:app/toggle-theme])
       :aria-label (if (= theme :light) "Switch to dark mode" "Switch to light mode")}
      (if (= theme :light)
        [icon :moon {:width 24 :height 24}]
        [icon :sun {:width 24 :height 24}])]]))

(defn app-layout []
  [:div.flow-shell
   [:div.flow-shell__mobile-header
    [mobile-header]]
   [:div.flow-shell__sidebar
    [sidebar]]
   [:div.flow-shell__content
    [:div.flow-shell__header
     [header]]
    [:div.flow-shell__scrollable
     [main-content]]]
   [:div.flow-shell__tab-bar
    [tab-bar]]])

(defn main-panel []
  (let [authenticated? @(rf/subscribe [:auth/authenticated?])
        auth-initialized? @(rf/subscribe [:auth/initialized?])
        active-view @(rf/subscribe [:app/current-route])
        theme @(rf/subscribe [:app/theme])]
    [:<>
     [:div {:class (str "flow-theme-" (name theme))}
      (cond
        ;; Still checking auth status
        (not auth-initialized?)
        [loading-screen]

        (and (contains? routes/public-routes active-view) (not authenticated?))
        [auth-layout]

        ;; Protected routes - check if authenticated
        (not authenticated?)
        (do (rf/dispatch [:app/navigate :login])
            [loading-screen])

        ;; Authenticated - show app
        :else
        [app-layout])]
     [toast-container]]))
