(ns finance.views.main
  "Main layout, navigation, and UI components."
  (:require [re-frame.core :as rf]
            [finance.views.dashboard :as dashboard]
            [finance.views.transactions :as transactions]
            [finance.components.icons :refer [icon]]))

(defn theme-toggle []
  (let [theme @(rf/subscribe [:app/theme])]
    [:button.flow-theme-toggle
     {:on-click #(rf/dispatch [:app/toggle-theme])
      :aria-label (if (= theme :light) "Switch to dark mode" "Switch to light mode")}
     (if (= theme :light)
       [icon :moon {:width 20 :height 20 :class "flow-icon-moon"}]
       [icon :sun {:width 20 :height 20 :class "flow-icon-sun"}])]))

(defn header []
  (let [active-view @(rf/subscribe [:app/current-route])]
    [:header.flow-header
     [:div.flow-header__left
      [:div.flow-header__logo
       [icon :dollar {:width 20 :height 20}]]
      [:span.flow-header__title "Finance Tracker"]]

     [:nav.flow-header__nav
      [:a.flow-header__nav-item
       {:class (when (= active-view :dashboard) "flow-header__nav-item--active")
        :on-click #(rf/dispatch [:app/navigate :dashboard])}
       [:span.flow-header__nav-icon [icon :dashboard {:width 20 :height 20}]]
       "Dashboard"]
      [:a.flow-header__nav-item
       {:class (when (= active-view :transactions) "flow-header__nav-item--active")
        :on-click #(rf/dispatch [:app/navigate :transactions])}
       [:span.flow-header__nav-icon [icon :list {:width 20 :height 20}]]
       "Transactions"]]

     [:div.flow-header__actions
      [theme-toggle]
      [:button.flow-header__add-btn
       {:on-click #(rf/dispatch [:app/navigate :add-transaction])}
       [icon :plus {:width 20 :height 20}]
       [:span "Add Transaction"]]
      [:button.flow-header__add-btn-mobile
       {:on-click #(rf/dispatch [:app/navigate :add-transaction])}
       [icon :plus {:width 20 :height 20}]]]]))

(defn sidebar []
  (let [active-view @(rf/subscribe [:app/current-route])]
    [:aside.flow-sidebar
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
       {:class (when (= active-view :add-transaction) "flow-sidebar__item--active")
        :on-click #(rf/dispatch [:app/navigate :add-transaction])}
       [:span.flow-sidebar__icon [icon :plus {:width 20 :height 20}]]
       [:span.flow-sidebar__label "Add New"]]]]))

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
    (when (seq toasts)
      [:div.flow-toast-container
       (for [t toasts]
         ^{:key (:id t)}
         [toast t])])))

(defn main-content []
  (let [active-view @(rf/subscribe [:app/current-route])]
    [:main.flow-main
     {:class "flow-animate flow-animate-fade-in"}
     (case active-view
       :dashboard [dashboard/dashboard-view]
       :transactions [transactions/transaction-list]
       :add-transaction [transactions/add-transaction-form]
       [dashboard/dashboard-view])]))

(defn main-panel []
  [:div.flow-shell
   [:div.flow-shell__header
    [header]]
   [:div.flow-shell__sidebar
    [sidebar]]
   [:div.flow-shell__content
    [main-content]]
   [:div.flow-shell__tab-bar
    [tab-bar]]
   [toast-container]])
