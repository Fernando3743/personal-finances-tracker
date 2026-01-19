(ns finance.views.wallets
  "Wallets management view."
  (:require [finance.components.icons :refer [icon]]))

(defn wallets-view []
  [:div {:class "space-y-6"}
   [:div {:class "mb-6"}
    [:h1 {:class "text-2xl font-bold text-neutral-900 dark:text-neutral-50"} "Wallets"]]
   [:div {:class "bg-white dark:bg-neutral-800 rounded-xl border border-neutral-200 dark:border-neutral-700"}
    [:div {:class "flex flex-col items-center justify-center text-center py-16 px-6"}
     [:div {:class "mb-4 text-neutral-400 dark:text-neutral-500"}
      [icon :wallet {:width 48 :height 48}]]
     [:h3 {:class "text-lg font-semibold text-neutral-600 dark:text-neutral-400 mb-2"}
      "Coming Soon"]
     [:p {:class "text-neutral-400 dark:text-neutral-500 max-w-md"}
      "Manage multiple wallets and accounts in one place."]]]])
