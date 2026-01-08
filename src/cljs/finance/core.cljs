(ns finance.core
  "Application entry point."
  (:require [reagent.dom :as rdom]
            [re-frame.core :as rf]
            [finance.views.main :as main]
            [finance.rf-logic.app :as app]
            [finance.rf-logic.dashboard]
            [finance.rf-logic.transactions]))

(defn ^:dev/after-load mount-root
  []
  (rf/clear-subscription-cache!)
  (let [root-el (.getElementById js/document "app")]
    (rdom/unmount-component-at-node root-el)
    (rdom/render [main/main-panel] root-el)))

(defn init
  []
  (rf/dispatch-sync [:app/initialize-db])
  (app/start-router!)
  (rf/dispatch [:app/initialize-app])
  (mount-root))
