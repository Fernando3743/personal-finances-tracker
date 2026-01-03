(ns finance.views.transactions
  "Transaction list and form views."
  (:require [re-frame.core :as rf]
            [finance.subs :as subs]
            [finance.events :as events]))

(defn format-currency
  "Formats a number as currency."
  [amount]
  (let [formatted (.toFixed (js/Math.abs amount) 2)]
    (if (neg? amount)
      (str "-$" formatted)
      (str "$" formatted))))

(defn format-date
  "Formats a date string for display."
  [date-str]
  (if date-str
    (let [date (js/Date. date-str)]
      (.toLocaleDateString date "en-US" #js {:year "numeric"
                                              :month "short"
                                              :day "numeric"}))
    "Unknown"))

(defn transaction-item
  "Single transaction display component."
  [{:keys [transaction/id transaction/amount transaction/type
           transaction/category transaction/description transaction/date]}]
  [:li.transaction-item
   [:div {:style {:flex 1}}
    [:div {:style {:display "flex" :align-items "center" :gap "10px"}}
     [:strong (or description "No description")]
     [:span.category-badge (name (or category :other))]]
    [:div {:style {:font-size "0.85rem" :color "#666" :margin-top "5px"}}
     (format-date date)]]
   [:div {:style {:display "flex" :align-items "center" :gap "15px"}}
    [:span {:class (if (= type :income) "amount-income" "amount-expense")}
     (format-currency (if (= type :income) amount (- amount)))]
    [:button.btn.btn-danger
     {:style {:padding "5px 10px" :font-size "0.85rem"}
      :on-click #(when (js/confirm "Delete this transaction?")
                   (rf/dispatch [::events/delete-transaction (str id)]))}
     "Delete"]]])

(defn transaction-list
  "Full transaction list view."
  []
  (let [transactions @(rf/subscribe [::subs/transactions])
        count @(rf/subscribe [::subs/transaction-count])]
    [:div.card
     [:div {:style {:display "flex"
                    :justify-content "space-between"
                    :align-items "center"
                    :margin-bottom "15px"}}
      [:h2 (str "All Transactions (" count ")")]
      [:button.btn
       {:on-click #(rf/dispatch [::events/set-active-view :add-transaction])}
       "Add Transaction"]]
     (if (seq transactions)
       [:ul.transaction-list
        (for [tx transactions]
          ^{:key (or (:transaction/id tx) (random-uuid))}
          [transaction-item tx])]
       [:p {:style {:text-align "center" :color "#666" :padding "40px"}}
        "No transactions yet. Click 'Add Transaction' to get started!"])]))

;; =============================================================================
;; Add Transaction Form
;; =============================================================================

(defn form-input
  "Reusable form input component."
  [{:keys [label field type options]}]
  (let [value @(rf/subscribe [::subs/form-field field])]
    [:div.form-group
     [:label label]
     (case type
       :select
       [:select
        {:value (if (keyword? value) (name value) value)
         :on-change #(rf/dispatch [::events/update-form-field
                                   field
                                   (keyword (-> % .-target .-value))])}
        (for [opt options]
          ^{:key opt}
          [:option {:value (name opt)} (name opt)])]

       :number
       [:input
        {:type "number"
         :step "0.01"
         :value value
         :placeholder "0.00"
         :on-change #(rf/dispatch [::events/update-form-field
                                   field
                                   (-> % .-target .-value)])}]

       ;; default text
       [:input
        {:type "text"
         :value value
         :on-change #(rf/dispatch [::events/update-form-field
                                   field
                                   (-> % .-target .-value)])}])]))

(defn add-transaction-form
  "Form for adding new transactions."
  []
  (let [categories @(rf/subscribe [::subs/categories])
        form-valid? @(rf/subscribe [::subs/form-valid?])
        loading? @(rf/subscribe [::subs/loading?])]
    [:div.card
     [:h2 "Add New Transaction"]

     [form-input {:label "Amount"
                  :field :amount
                  :type :number}]

     [form-input {:label "Type"
                  :field :type
                  :type :select
                  :options [:expense :income]}]

     [form-input {:label "Category"
                  :field :category
                  :type :select
                  :options categories}]

     [form-input {:label "Description"
                  :field :description
                  :type :text}]

     [:div {:style {:display "flex" :gap "10px" :margin-top "20px"}}
      [:button.btn
       {:disabled (or (not form-valid?) loading?)
        :on-click #(rf/dispatch [::events/create-transaction])}
       (if loading? "Saving..." "Save Transaction")]

      [:button.btn
       {:style {:background "#999"}
        :on-click #(do
                     (rf/dispatch [::events/reset-form])
                     (rf/dispatch [::events/set-active-view :transactions]))}
       "Cancel"]]]))
