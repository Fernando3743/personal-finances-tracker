(ns finance.rf-logic.app
  "Shared application logic - init, routing, theme, toast, panel, errors."
  (:require [re-frame.core :as rf]
            [finance.db :as db]
            [finance.routes :as routes]
            [finance.utils.validation :as validation]
            [finance.utils.errors :as errors]
            [finance.constants :as const]))

(rf/reg-event-db
 :app/initialize-db
 (fn [_ _]
   db/default-db))

(rf/reg-event-fx
 :app/initialize-app
 (fn [{:keys [db]} _]
   {:db (assoc db :loading? true)
    :dispatch-n [[:auth/check-session]
                 [:app/load-theme]]}))

(rf/reg-event-fx
 :app/api-error
 (fn [{:keys [db]} [_ error]]
   (let [error-msg (errors/extract-error-message error "An error occurred")]
     (errors/log-error "API Error" error)
     {:db (-> db
              (assoc :loading? false)
              (assoc :error error-msg))
      :dispatch [:app/show-toast
                 {:type :error
                  :title "Error"
                  :message error-msg}]})))

(rf/reg-event-db
 :app/clear-error
 (fn [db _]
   (assoc db :error nil)))

(def route-init-events
  "Maps routes to their initialization events.
   These events are dispatched when navigating to authenticated routes."
  {:dashboard       :dashboard/init
   :transactions    :transactions/init
   :add-transaction :transactions/init
   :profile         :profile/init
   :wallets         :wallets/init})

(rf/reg-event-fx
 :app/set-route
 (fn [{:keys [db]} [_ route]]
   (let [authenticated? (some? (get-in db [:auth :user]))
         is-public-route? (contains? routes/public-routes route)
         init-event (get route-init-events route)]
     (cond
       (and authenticated? is-public-route?)
       {:dispatch [:app/navigate :dashboard]}

       (and authenticated? (= route :add-transaction))
       {:db (assoc db :current-route :add-transaction)
        :dispatch-n [[:dashboard/init]
                     [:app/open-panel :add-transaction]]}

       :else
       (cond-> {:db (assoc db :current-route route)}
         (and authenticated? init-event)
         (assoc :dispatch [init-event]))))))

(rf/reg-event-fx
 :app/navigate
 (fn [_ [_ route]]
   {:fx [[:navigate! route]]}))

(rf/reg-fx
 :navigate!
 (fn [route]
   (let [path (routes/path-for route)]
     (.pushState js/history nil "" path)
     (rf/dispatch [:app/set-route route]))))

(rf/reg-event-fx
 :app/load-theme
 (fn [{:keys [db]} _]
   (let [saved-theme (-> js/localStorage (.getItem "theme"))
         theme (validation/sanitize-theme-string saved-theme)]
     {:db (assoc db :theme theme)
      :dispatch [:app/apply-theme theme]})))

(rf/reg-event-fx
 :app/toggle-theme
 (fn [{:keys [db]} _]
   (let [current-theme (:theme db)
         new-theme (if (= current-theme :light) :dark :light)]
     {:db (assoc db :theme new-theme)
      :dispatch [:app/apply-theme new-theme]})))

(rf/reg-event-fx
 :app/apply-theme
 (fn [_ [_ theme]]
   {:fx [[:set-theme theme]]}))

(rf/reg-fx
 :set-theme
 (fn [theme]
   (.setItem js/localStorage "theme" (name theme))
   (.setAttribute (.-documentElement js/document) "data-theme" (name theme))))

(rf/reg-event-fx
 :app/show-toast
 (fn [{:keys [db]} [_ toast]]
   (let [id (random-uuid)
         toast-with-id (assoc toast :id id)]
     {:db (update db :toast-queue conj toast-with-id)
      :dispatch-later [{:ms (or (:duration toast) const/toast-duration-ms)
                        :dispatch [:app/dismiss-toast id]}]})))

(rf/reg-event-db
 :app/dismiss-toast
 (fn [db [_ id]]
   (update db :toast-queue (fn [q] (filterv #(not= (:id %) id) q)))))

(rf/reg-event-fx
 :app/open-panel
 (fn [{:keys [db]} [_ mode data]]
   (cond-> {:db (-> db
                    (assoc-in [:panel :open?] true)
                    (assoc-in [:panel :mode] mode)
                    (assoc-in [:panel :data] data))}
     (= mode :add-transaction)
     (assoc :dispatch [:tx/reset-form]))))

(rf/reg-event-db
 :app/close-panel
 (fn [db _]
   (-> db
       (assoc-in [:panel :open?] false)
       (assoc-in [:panel :mode] nil)
       (assoc-in [:panel :data] nil))))

(rf/reg-sub
 :app/db
 (fn [db _]
   db))

(rf/reg-sub
 :app/loading?
 (fn [db _]
   (:loading? db)))

(rf/reg-sub
 :app/error
 (fn [db _]
   (:error db)))

(rf/reg-sub
 :app/current-route
 (fn [db _]
   (:current-route db)))

(rf/reg-sub
 :app/theme
 (fn [db _]
   (:theme db)))

(rf/reg-sub
 :app/toast-queue
 (fn [db _]
   (:toast-queue db)))

(rf/reg-sub
 :app/panel
 (fn [db _]
   (:panel db)))

(rf/reg-sub
 :app/panel-open?
 :<- [:app/panel]
 (fn [panel _]
   (:open? panel)))

(rf/reg-sub
 :app/panel-mode
 :<- [:app/panel]
 (fn [panel _]
   (:mode panel)))

(defn- on-navigate [_event]
  (let [path (.-pathname js/location)
        match (routes/match-route path)]
    (when match
      (rf/dispatch [:app/set-route (:handler match)]))))

(defn start-router! []
  (.addEventListener js/window "popstate" on-navigate)
  (on-navigate nil))

(defn stop-router! []
  (.removeEventListener js/window "popstate" on-navigate))
