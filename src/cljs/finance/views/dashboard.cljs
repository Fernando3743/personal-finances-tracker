(ns finance.views.dashboard
  "Dashboard view with summary and charts."
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

(defn balance-card
  "Displays the current balance."
  []
  (let [balance @(rf/subscribe [::subs/total-balance])
        income @(rf/subscribe [::subs/total-income])
        expenses @(rf/subscribe [::subs/total-expenses])]
    [:div.card
     [:h2 "Balance Overview"]
     [:div.balance-display
      {:class (if (neg? balance) "amount-expense" "amount-income")}
      (format-currency balance)]
     [:div {:style {:display "flex" :justify-content "space-around" :margin-top "20px"}}
      [:div {:style {:text-align "center"}}
       [:div {:style {:font-size "0.9rem" :color "#666"}} "Income"]
       [:div.amount-income (format-currency income)]]
      [:div {:style {:text-align "center"}}
       [:div {:style {:font-size "0.9rem" :color "#666"}} "Expenses"]
       [:div.amount-expense (format-currency expenses)]]]]))

(defn category-item
  "Single category breakdown item."
  [{:keys [category total expenses income expense-percentage]}]
  [:div.transaction-item
   [:div
    [:strong (name category)]
    [:span.category-badge (str (count []) " transactions")]]
   [:div {:style {:text-align "right"}}
    [:div {:class (if (neg? total) "amount-expense" "amount-income")}
     (format-currency total)]
    (when (pos? expenses)
      [:div {:style {:font-size "0.8rem" :color "#666"}}
       (str (.toFixed expense-percentage 1) "% of expenses")])]])

(defn category-breakdown-card
  "Displays spending by category."
  []
  (let [breakdown @(rf/subscribe [::subs/category-breakdown])
        categories (:categories breakdown)]
    [:div.card
     [:h2 "Spending by Category"]
     (if (seq categories)
       [:ul.transaction-list
        (for [cat categories]
          ^{:key (:category cat)}
          [category-item cat])]
       [:p {:style {:text-align "center" :color "#666" :padding "20px"}}
        "No transactions yet. Add some to see your breakdown!"])]))

(defn recent-transactions-card
  "Shows recent transactions."
  []
  (let [transactions @(rf/subscribe [::subs/recent-transactions])]
    [:div.card
     [:h2 "Recent Transactions"]
     (if (seq transactions)
       [:ul.transaction-list
        (for [tx transactions]
          ^{:key (or (:transaction/id tx) (random-uuid))}
          [:li.transaction-item
           [:div
            [:strong (or (:transaction/description tx) "No description")]
            [:span.category-badge (name (or (:transaction/category tx) :other))]]
           [:div {:class (if (= :income (:transaction/type tx))
                           "amount-income"
                           "amount-expense")}
            (format-currency
             (if (= :income (:transaction/type tx))
               (:transaction/amount tx)
               (- (:transaction/amount tx))))]])]
       [:p {:style {:text-align "center" :color "#666" :padding "20px"}}
        "No transactions yet."])
     [:div {:style {:text-align "center" :margin-top "15px"}}
      [:button.btn
       {:on-click #(rf/dispatch [::events/set-active-view :transactions])}
       "View All Transactions"]]]))

(defn dashboard-view
  "Main dashboard component."
  []
  [:div
   [balance-card]
   [:div {:style {:display "grid"
                  :grid-template-columns "1fr 1fr"
                  :gap "20px"}}
    [category-breakdown-card]
    [recent-transactions-card]]])
