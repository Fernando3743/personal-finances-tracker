(ns finance.views.reports
  "Reports and analytics view."
  (:require [finance.components.icons :refer [icon]]))

(defn reports-view []
  [:div.flow-main
   [:div.flow-page-header
    [:h1.flow-page-header__title "Reports"]]
   [:div.flow-card
    [:div.flow-card__body
     {:style {:text-align "center" :padding "3rem"}}
     [:div {:style {:margin-bottom "1rem"}}
      [icon :bar-chart {:width 48 :height 48 :style {:color "var(--color-text-tertiary)"}}]]
     [:h3 {:style {:color "var(--color-text-secondary)" :margin-bottom "0.5rem"}}
      "Coming Soon"]
     [:p {:style {:color "var(--color-text-tertiary)"}}
      "Detailed financial reports and analytics."]]]])
