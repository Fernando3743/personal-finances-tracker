(ns finance.views.wallets
  "Wallets management view."
  (:require [finance.components.icons :refer [icon]]))

(defn wallets-view []
  [:div.flow-main
   [:div.flow-page-header
    [:h1.flow-page-header__title "Wallets"]]
   [:div.flow-card
    [:div.flow-card__body
     {:style {:text-align "center" :padding "3rem"}}
     [:div {:style {:margin-bottom "1rem"}}
      [icon :wallet {:width 48 :height 48 :style {:color "var(--color-text-tertiary)"}}]]
     [:h3 {:style {:color "var(--color-text-secondary)" :margin-bottom "0.5rem"}}
      "Coming Soon"]
     [:p {:style {:color "var(--color-text-tertiary)"}}
      "Manage multiple wallets and accounts in one place."]]]])
