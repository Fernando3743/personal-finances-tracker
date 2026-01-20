(ns finance.rf-logic.wallets
  "Wallets page logic - CRUD for wallets and filtering."
  (:require [re-frame.core :as rf]
            [finance.db :as db]
            [day8.re-frame.http-fx]
            [ajax.core :as ajax]
            [finance.utils.errors :as errors]
            [clojure.string :as str]))

;; =============================================================================
;; Events - Initialization
;; =============================================================================

(rf/reg-event-fx
 :wallets/init
 (fn [_ _]
   {:dispatch-n [[:wallets/fetch]
                 [:wallets/fetch-summary]]}))

;; =============================================================================
;; Events - Fetching Data
;; =============================================================================

(rf/reg-event-fx
 :wallets/fetch
 (fn [{:keys [db]} _]
   {:db (assoc-in db [:wallets :loading?] true)
    :http-xhrio {:method :get
                 :uri "/api/wallets"
                 :with-credentials true
                 :response-format (ajax/json-response-format {:keywords? true})
                 :on-success [:wallets/fetch-success]
                 :on-failure [:wallets/fetch-failure]}}))

(rf/reg-event-db
 :wallets/fetch-success
 (fn [db [_ response]]
   (-> db
       (assoc-in [:wallets :loading?] false)
       (assoc-in [:wallets :error] nil)
       (assoc-in [:wallets :accounts] (:wallets response)))))

(rf/reg-event-fx
 :wallets/fetch-failure
 (fn [{:keys [db]} [_ error]]
   {:db (-> db
            (assoc-in [:wallets :loading?] false)
            (assoc-in [:wallets :error] (errors/extract-error-message error "Failed to load wallets")))
    :dispatch [:app/show-toast
               {:type :error
                :title "Error"
                :message "Failed to load wallets."}]}))

(rf/reg-event-fx
 :wallets/fetch-summary
 (fn [{:keys [db]} _]
   {:http-xhrio {:method :get
                 :uri "/api/wallets/summary"
                 :with-credentials true
                 :response-format (ajax/json-response-format {:keywords? true})
                 :on-success [:wallets/fetch-summary-success]
                 :on-failure [:wallets/fetch-summary-failure]}}))

(rf/reg-event-db
 :wallets/fetch-summary-success
 (fn [db [_ response]]
   (assoc db :wallet-summary response)))

(rf/reg-event-db
 :wallets/fetch-summary-failure
 (fn [db [_ error]]
   (errors/log-error "Failed to load wallet summary" error)
   db))

;; =============================================================================
;; Events - Create Wallet
;; =============================================================================

(rf/reg-event-fx
 :wallets/create
 (fn [{:keys [db]} _]
   (let [form (:wallet-form db)
         payload {:name (:name form)
                  :type (name (:type form))
                  :institution (:institution form)
                  :account-number (:account-number form)
                  :balance (js/parseFloat (or (:balance form) "0"))
                  :currency (name (or (:currency form) :USD))
                  :color (:color form)
                  :balance-label (:balance-label form)}]
     {:db (assoc-in db [:wallets :loading?] true)
      :http-xhrio {:method :post
                   :uri "/api/wallets"
                   :params payload
                   :format (ajax/json-request-format)
                   :with-credentials true
                   :response-format (ajax/json-response-format {:keywords? true})
                   :on-success [:wallets/create-success]
                   :on-failure [:wallets/create-failure]}})))

(rf/reg-event-fx
 :wallets/create-success
 (fn [{:keys [db]} _]
   {:db (-> db
            (assoc-in [:wallets :loading?] false)
            (assoc-in [:wallet-panel :open?] false)
            (assoc-in [:wallet-panel :mode] nil)
            (assoc :wallet-form (:wallet-form db/default-db)))
    :dispatch-n [[:wallets/fetch]
                 [:wallets/fetch-summary]
                 [:app/show-toast
                  {:type :success
                   :title "Wallet Created"
                   :message "Your wallet was added successfully."}]]}))

(rf/reg-event-fx
 :wallets/create-failure
 (fn [{:keys [db]} [_ error]]
   {:db (-> db
            (assoc-in [:wallets :loading?] false)
            (assoc-in [:wallets :error] (errors/extract-error-message error "Failed to create wallet")))
    :dispatch [:app/show-toast
               {:type :error
                :title "Error"
                :message "Failed to create wallet."}]}))

;; =============================================================================
;; Events - Update Wallet
;; =============================================================================

(rf/reg-event-fx
 :wallets/update
 (fn [{:keys [db]} [_ id updates]]
   {:db (assoc-in db [:wallets :loading?] true)
    :http-xhrio {:method :put
                 :uri (str "/api/wallets/" id)
                 :params updates
                 :format (ajax/json-request-format)
                 :with-credentials true
                 :response-format (ajax/json-response-format {:keywords? true})
                 :on-success [:wallets/update-success]
                 :on-failure [:wallets/update-failure]}}))

(rf/reg-event-fx
 :wallets/update-success
 (fn [{:keys [db]} _]
   {:db (-> db
            (assoc-in [:wallets :loading?] false)
            (assoc-in [:wallet-panel :open?] false)
            (assoc-in [:wallet-panel :mode] nil)
            (assoc-in [:wallets :selected-wallet-id] nil)
            (assoc :wallet-form (:wallet-form db/default-db)))
    :dispatch-n [[:wallets/fetch]
                 [:wallets/fetch-summary]
                 [:app/show-toast
                  {:type :success
                   :title "Updated"
                   :message "Wallet updated."}]]}))

(rf/reg-event-fx
 :wallets/update-failure
 (fn [{:keys [db]} [_ error]]
   {:db (-> db
            (assoc-in [:wallets :loading?] false)
            (assoc-in [:wallets :error] (errors/extract-error-message error "Failed to update")))
    :dispatch [:app/show-toast
               {:type :error
                :title "Error"
                :message "Failed to update wallet."}]}))

;; =============================================================================
;; Events - Delete Wallet
;; =============================================================================

(rf/reg-event-fx
 :wallets/delete
 (fn [{:keys [db]} [_ id]]
   {:db (assoc-in db [:wallets :loading?] true)
    :http-xhrio {:method :delete
                 :uri (str "/api/wallets/" id)
                 :format (ajax/json-request-format)
                 :with-credentials true
                 :response-format (ajax/json-response-format {:keywords? true})
                 :on-success [:wallets/delete-success]
                 :on-failure [:wallets/delete-failure]}}))

(rf/reg-event-fx
 :wallets/delete-success
 (fn [{:keys [db]} _]
   {:db (assoc-in db [:wallets :loading?] false)
    :dispatch-n [[:wallets/fetch]
                 [:wallets/fetch-summary]
                 [:app/show-toast
                  {:type :success
                   :title "Deleted"
                   :message "Wallet deleted."}]]}))

(rf/reg-event-fx
 :wallets/delete-failure
 (fn [{:keys [db]} [_ error]]
   {:db (-> db
            (assoc-in [:wallets :loading?] false)
            (assoc-in [:wallets :error] (errors/extract-error-message error "Failed to delete")))
    :dispatch [:app/show-toast
               {:type :error
                :title "Error"
                :message "Failed to delete wallet."}]}))

;; =============================================================================
;; Events - Sync Wallet
;; =============================================================================

(rf/reg-event-fx
 :wallets/sync
 (fn [{:keys [db]} [_ id]]
   {:db (assoc-in db [:wallets :accounts]
                  (mapv (fn [w]
                          (if (= (:wallet/id w) id)
                            (assoc w :wallet/status :updating)
                            w))
                        (get-in db [:wallets :accounts])))
    :http-xhrio {:method :post
                 :uri (str "/api/wallets/" id "/sync")
                 :format (ajax/json-request-format)
                 :with-credentials true
                 :response-format (ajax/json-response-format {:keywords? true})
                 :on-success [:wallets/sync-success]
                 :on-failure [:wallets/sync-failure id]}}))

(rf/reg-event-fx
 :wallets/sync-success
 (fn [{:keys [db]} _]
   {:dispatch-n [[:wallets/fetch]
                 [:app/show-toast
                  {:type :success
                   :title "Synced"
                   :message "Wallet synced successfully."}]]}))

(rf/reg-event-fx
 :wallets/sync-failure
 (fn [{:keys [db]} [_ id error]]
   {:db (assoc-in db [:wallets :accounts]
                  (mapv (fn [w]
                          (if (= (:wallet/id w) id)
                            (assoc w :wallet/status :error)
                            w))
                        (get-in db [:wallets :accounts])))
    :dispatch [:app/show-toast
               {:type :error
                :title "Sync Failed"
                :message "Failed to sync wallet."}]}))

(rf/reg-event-fx
 :wallets/refresh-all
 (fn [{:keys [db]} _]
   {:db (assoc-in db [:wallets :loading?] true)
    :http-xhrio {:method :post
                 :uri "/api/wallets/refresh"
                 :format (ajax/json-request-format)
                 :with-credentials true
                 :response-format (ajax/json-response-format {:keywords? true})
                 :on-success [:wallets/refresh-all-success]
                 :on-failure [:wallets/refresh-all-failure]}}))

(rf/reg-event-fx
 :wallets/refresh-all-success
 (fn [{:keys [db]} _]
   {:db (assoc-in db [:wallets :loading?] false)
    :dispatch-n [[:wallets/fetch]
                 [:wallets/fetch-summary]
                 [:app/show-toast
                  {:type :success
                   :title "Refreshed"
                   :message "All wallets refreshed."}]]}))

(rf/reg-event-fx
 :wallets/refresh-all-failure
 (fn [{:keys [db]} [_ error]]
   {:db (assoc-in db [:wallets :loading?] false)
    :dispatch [:app/show-toast
               {:type :error
                :title "Error"
                :message "Failed to refresh wallets."}]}))

;; =============================================================================
;; Events - UI State
;; =============================================================================

(rf/reg-event-db
 :wallets/set-view-mode
 (fn [db [_ mode]]
   (assoc-in db [:wallets :view-mode] mode)))

(rf/reg-event-db
 :wallets/set-filter
 (fn [db [_ filter-type]]
   (assoc-in db [:wallets :filter] filter-type)))

;; =============================================================================
;; Events - Form Management
;; =============================================================================

(rf/reg-event-db
 :wallets/update-form-field
 (fn [db [_ field value]]
   (assoc-in db [:wallet-form field] value)))

(rf/reg-event-db
 :wallets/reset-form
 (fn [db _]
   (assoc db :wallet-form (:wallet-form db/default-db))))

;; =============================================================================
;; Events - Panel Management
;; =============================================================================

(rf/reg-event-fx
 :wallets/open-panel
 (fn [{:keys [db]} [_ mode wallet-id]]
   (let [fx {:db (-> db
                     (assoc-in [:wallet-panel :open?] true)
                     (assoc-in [:wallet-panel :mode] mode)
                     (assoc-in [:wallets :selected-wallet-id] wallet-id))}]
     (if (and (= mode :edit) wallet-id)
       (assoc fx :dispatch [:wallets/prepare-edit wallet-id])
       fx))))

(rf/reg-event-db
 :wallets/close-panel
 (fn [db _]
   (-> db
       (assoc-in [:wallet-panel :open?] false)
       (assoc-in [:wallet-panel :mode] nil)
       (assoc-in [:wallets :selected-wallet-id] nil)
       (assoc :wallet-form (:wallet-form db/default-db)))))

(rf/reg-event-db
 :wallets/prepare-edit
 (fn [db [_ wallet-id]]
   (let [wallet (some #(when (= (:wallet/id %) wallet-id) %) (get-in db [:wallets :accounts] []))
         form {:name (or (:wallet/name wallet) "")
               :type (or (:wallet/type wallet) :bank)
               :institution (or (:wallet/institution wallet) "")
               :account-number (or (:wallet/account-number wallet) "")
               :balance (str (or (:wallet/balance wallet) 0))
               :currency (or (:wallet/currency wallet) :USD)
               :color (:wallet/color wallet)
               :balance-label (or (:wallet/balance-label wallet) "Current Balance")}]
     (assoc db :wallet-form form))))

;; =============================================================================
;; Subscriptions - Raw Data
;; =============================================================================

(rf/reg-sub
 :wallets/accounts
 (fn [db _]
   (get-in db [:wallets :accounts] [])))

(rf/reg-sub
 :wallets/loading?
 (fn [db _]
   (get-in db [:wallets :loading?] false)))

(rf/reg-sub
 :wallets/error
 (fn [db _]
   (get-in db [:wallets :error])))

(rf/reg-sub
 :wallets/view-mode
 (fn [db _]
   (get-in db [:wallets :view-mode] :grid)))

(rf/reg-sub
 :wallets/filter
 (fn [db _]
   (get-in db [:wallets :filter] :all)))

(rf/reg-sub
 :wallets/summary
 (fn [db _]
   (:wallet-summary db)))

;; =============================================================================
;; Subscriptions - Filtered Data
;; =============================================================================

(rf/reg-sub
 :wallets/filtered-accounts
 :<- [:wallets/accounts]
 :<- [:wallets/filter]
 (fn [[accounts filter-type] _]
   (if (= filter-type :all)
     accounts
     (filter #(= (:wallet/type %) filter-type) accounts))))

(rf/reg-sub
 :wallets/accounts-by-type
 :<- [:wallets/accounts]
 (fn [accounts _]
   (group-by :wallet/type accounts)))

(rf/reg-sub
 :wallets/bank-accounts
 :<- [:wallets/accounts]
 (fn [accounts _]
   (filter #(= :bank (:wallet/type %)) accounts)))

(rf/reg-sub
 :wallets/card-accounts
 :<- [:wallets/accounts]
 (fn [accounts _]
   (filter #(= :card (:wallet/type %)) accounts)))

(rf/reg-sub
 :wallets/crypto-accounts
 :<- [:wallets/accounts]
 (fn [accounts _]
   (filter #(= :crypto (:wallet/type %)) accounts)))

(rf/reg-sub
 :wallets/investment-accounts
 :<- [:wallets/accounts]
 (fn [accounts _]
   (filter #(= :investment (:wallet/type %)) accounts)))

(rf/reg-sub
 :wallets/fiat-accounts
 :<- [:wallets/accounts]
 (fn [accounts _]
   (filter #(#{:USD :COP :EUR :GBP} (:wallet/currency %)) accounts)))

(rf/reg-sub
 :wallets/crypto-wallets
 :<- [:wallets/accounts]
 (fn [accounts _]
   (filter #(#{:BTC :ETH :SOL} (:wallet/currency %)) accounts)))

;; =============================================================================
;; Subscriptions - Aggregations
;; =============================================================================

(rf/reg-sub
 :wallets/total-balance
 :<- [:wallets/accounts]
 (fn [accounts _]
   (reduce + 0 (map #(or (:wallet/balance %) 0) accounts))))

(rf/reg-sub
 :wallets/total-by-currency
 :<- [:wallets/accounts]
 (fn [accounts _]
   (reduce (fn [acc w]
             (let [curr (:wallet/currency w)
                   bal (or (:wallet/balance w) 0)]
               (update acc curr (fnil + 0) bal)))
           {}
           accounts)))

(rf/reg-sub
 :wallets/total-assets
 :<- [:wallets/accounts]
 (fn [accounts _]
   (reduce + 0 (filter pos? (map #(or (:wallet/balance %) 0) accounts)))))

(rf/reg-sub
 :wallets/total-liabilities
 :<- [:wallets/accounts]
 (fn [accounts _]
   (Math/abs (reduce + 0 (filter neg? (map #(or (:wallet/balance %) 0) accounts))))))

(rf/reg-sub
 :wallets/count
 :<- [:wallets/accounts]
 (fn [accounts _]
   (count accounts)))

(rf/reg-sub
 :wallets/active-count
 :<- [:wallets/accounts]
 (fn [accounts _]
   (count (filter #(= :active (:wallet/status %)) accounts))))

;; =============================================================================
;; Subscriptions - Form & Panel
;; =============================================================================

(rf/reg-sub
 :wallets/form
 (fn [db _]
   (:wallet-form db)))

(rf/reg-sub
 :wallets/form-field
 :<- [:wallets/form]
 (fn [form [_ field]]
   (get form field)))

(rf/reg-sub
 :wallets/form-valid?
 :<- [:wallets/form]
 (fn [form _]
   (let [{:keys [name institution type]} form]
     (and (not (str/blank? name))
          (not (str/blank? institution))
          (some? type)))))

(rf/reg-sub
 :wallets/panel-open?
 (fn [db _]
   (get-in db [:wallet-panel :open?] false)))

(rf/reg-sub
 :wallets/panel-mode
 (fn [db _]
   (get-in db [:wallet-panel :mode])))

(rf/reg-sub
 :wallets/selected-wallet-id
 (fn [db _]
   (get-in db [:wallets :selected-wallet-id])))

(rf/reg-sub
 :wallets/selected-wallet
 :<- [:wallets/accounts]
 :<- [:wallets/selected-wallet-id]
 (fn [[accounts id] _]
   (some #(when (= (:wallet/id %) id) %) accounts)))

;; =============================================================================
;; Subscriptions - Currency Converter
;; =============================================================================

(rf/reg-sub
 :wallets/converter
 (fn [db _]
   (:currency-converter db)))

(rf/reg-sub
 :wallets/converter-field
 :<- [:wallets/converter]
 (fn [converter [_ field]]
   (get converter field)))

;; =============================================================================
;; Subscriptions - Market Data
;; =============================================================================

(rf/reg-sub
 :wallets/market-data
 (fn [db _]
   (:market-data db)))

(rf/reg-sub
 :wallets/market-rates
 :<- [:wallets/market-data]
 (fn [data _]
   (:rates data {})))
