(ns finance.core
  "Application entry point."
  (:require [reagent.dom :as rdom]
            [re-frame.core :as rf]
            [finance.events :as events]
            [finance.subs :as subs]
            [finance.views.main :as main]))

(defn ^:dev/after-load mount-root
  "Mount the root component."
  []
  (rf/clear-subscription-cache!)
  (let [root-el (.getElementById js/document "app")]
    (rdom/unmount-component-at-node root-el)
    (rdom/render [main/main-panel] root-el)))

(defn init
  "Initialize the application."
  []
  (rf/dispatch-sync [::events/initialize-db])
  (rf/dispatch [::events/initialize-app])
  (mount-root))
