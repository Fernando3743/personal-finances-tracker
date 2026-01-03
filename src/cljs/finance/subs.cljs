(ns finance.subs
  "Re-frame subscriptions."
  (:require [re-frame.core :as rf]))

;; =============================================================================
;; Basic Subscriptions
;; =============================================================================

(rf/reg-sub
 ::db
 (fn [db _]
   db))

(rf/reg-sub
 ::loading?
 (fn [db _]
   (:loading? db)))

(rf/reg-sub
 ::error
 (fn [db _]
   (:error db)))

(rf/reg-sub
 ::active-view
 (fn [db _]
   (:active-view db)))

;; =============================================================================
;; Transaction Subscriptions
;; =============================================================================

(rf/reg-sub
 ::transactions
 (fn [db _]
   (:transactions db)))

(rf/reg-sub
 ::transaction-count
 :<- [::transactions]
 (fn [transactions _]
   (count transactions)))

(rf/reg-sub
 ::recent-transactions
 :<- [::transactions]
 (fn [transactions _]
   (take 10 transactions)))

;; =============================================================================
;; Summary Subscriptions
;; =============================================================================

(rf/reg-sub
 ::summary
 (fn [db _]
   (:summary db)))

(rf/reg-sub
 ::total-balance
 :<- [::summary]
 (fn [summary _]
   (or (:total-balance summary) 0)))

(rf/reg-sub
 ::total-income
 :<- [::summary]
 (fn [summary _]
   (or (:total-income summary) 0)))

(rf/reg-sub
 ::total-expenses
 :<- [::summary]
 (fn [summary _]
   (or (:total-expenses summary) 0)))

;; =============================================================================
;; Category Breakdown
;; =============================================================================

(rf/reg-sub
 ::category-breakdown
 (fn [db _]
   (:category-breakdown db)))

(rf/reg-sub
 ::categories-list
 :<- [::category-breakdown]
 (fn [breakdown _]
   (:categories breakdown)))

;; =============================================================================
;; Monthly Report
;; =============================================================================

(rf/reg-sub
 ::monthly-report
 (fn [db _]
   (:monthly-report db)))

;; =============================================================================
;; Form Subscriptions
;; =============================================================================

(rf/reg-sub
 ::transaction-form
 (fn [db _]
   (:transaction-form db)))

(rf/reg-sub
 ::form-field
 :<- [::transaction-form]
 (fn [form [_ field]]
   (get form field)))

(rf/reg-sub
 ::categories
 (fn [db _]
   (:categories db)))

(rf/reg-sub
 ::form-valid?
 :<- [::transaction-form]
 (fn [form _]
   (let [{:keys [amount type category]} form]
     (and (not (empty? amount))
          (some? type)
          (some? category)
          (js/parseFloat amount)))))
