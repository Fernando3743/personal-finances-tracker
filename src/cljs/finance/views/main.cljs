(ns finance.views.main
  "Main layout and navigation."
  (:require [re-frame.core :as rf]
            [finance.events :as events]
            [finance.subs :as subs]
            [finance.views.dashboard :as dashboard]
            [finance.views.transactions :as transactions]))

(defn nav-link
  "Navigation link component."
  [view label]
  (let [active-view @(rf/subscribe [::subs/active-view])]
    [:a.nav-link
     {:class (when (= active-view view) "active")
      :on-click #(rf/dispatch [::events/set-active-view view])}
     label]))

(defn navigation
  "Navigation bar component."
  []
  [:nav.nav
   [nav-link :dashboard "Dashboard"]
   [nav-link :transactions "Transactions"]
   [nav-link :add-transaction "Add New"]])

(defn header
  "Application header."
  []
  [:header
   [:h1 "Personal Finance Tracker"]
   [navigation]])

(defn error-banner
  "Error message display."
  []
  (let [error @(rf/subscribe [::subs/error])]
    (when error
      [:div.error
       error
       [:button.btn
        {:style {:margin-left "10px" :padding "5px 10px"}
         :on-click #(rf/dispatch [::events/clear-error])}
        "Dismiss"]])))

(defn loading-indicator
  "Loading spinner."
  []
  (let [loading? @(rf/subscribe [::subs/loading?])]
    (when loading?
      [:div.loading "Loading..."])))

(defn main-content
  "Main content area - renders based on active view."
  []
  (let [active-view @(rf/subscribe [::subs/active-view])]
    [:main
     (case active-view
       :dashboard [dashboard/dashboard-view]
       :transactions [transactions/transaction-list]
       :add-transaction [transactions/add-transaction-form]
       [dashboard/dashboard-view])]))

(defn main-panel
  "Root component."
  []
  [:div.container
   [header]
   [error-banner]
   [loading-indicator]
   [main-content]])
