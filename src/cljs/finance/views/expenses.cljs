(ns finance.views.expenses
  "Expenses page view - display expense transactions."
  (:require [re-frame.core :as rf]
            [clojure.string :as str]
            [finance.db :as db]
            [finance.utils.currency :as currency]
            [finance.components.icons :refer [icon]]))

(defn format-date-short [date-str]
  (when date-str
    (let [date (js/Date. date-str)]
      (.toLocaleDateString date "en-US" #js {:month "short" :day "numeric"}))))

(defn summary-card [{:keys [title value subtitle icon-name variant]}]
  [:div.flow-summary-card
   {:class (when variant (str "flow-summary-card--" (name variant)))}
   [:div.flow-summary-card__icon
    [icon icon-name {:width 24 :height 24}]]
   [:div.flow-summary-card__content
    [:span.flow-summary-card__title title]
    [:span.flow-summary-card__value value]
    (when subtitle
      [:span.flow-summary-card__subtitle subtitle])]])

(defn search-input []
  (let [search @(rf/subscribe [:expenses/filter-search])]
    [:div.flow-search
     [:span.flow-search__icon [icon :search {:width 18 :height 18}]]
     [:input.flow-search__input
      {:type "text"
       :placeholder "Search expenses..."
       :value (or search "")
       :on-change #(rf/dispatch [:expenses/update-filter :search (-> % .-target .-value)])}]
     (when (and search (not (str/blank? search)))
       [:button.flow-search__clear
        {:on-click #(rf/dispatch [:expenses/update-filter :search ""])}
        [icon :x {:width 14 :height 14}]])]))

(defn filter-chip [{:keys [label active? on-click on-clear]}]
  [:button.flow-chip
   {:class (when active? "flow-chip--active flow-chip--expense")
    :on-click on-click}
   [:span label]
   (when (and active? on-clear)
     [:span.flow-chip__clear
      {:on-click (fn [e] (.stopPropagation e) (on-clear))}
      [icon :x {:width 14 :height 14}]])])

(defn filter-bar []
  (let [filter-category @(rf/subscribe [:expenses/filter-category])
        filter-currency @(rf/subscribe [:expenses/filter-currency])
        has-filters? @(rf/subscribe [:expenses/has-active-filters?])
        expense-count @(rf/subscribe [:expenses/filtered-count])]
    [:div.flow-filter-bar
     [:div.flow-filter-bar__left
      [search-input]
      [:div.flow-filter-bar__chips
       [filter-chip {:label "All Currencies"
                     :active? (nil? filter-currency)
                     :on-click #(rf/dispatch [:expenses/update-filter :currency nil])}]
       [filter-chip {:label "COP"
                     :active? (= filter-currency :COP)
                     :on-click #(rf/dispatch [:expenses/update-filter :currency :COP])
                     :on-clear #(rf/dispatch [:expenses/update-filter :currency nil])}]
       [filter-chip {:label "USD"
                     :active? (= filter-currency :USD)
                     :on-click #(rf/dispatch [:expenses/update-filter :currency :USD])
                     :on-clear #(rf/dispatch [:expenses/update-filter :currency nil])}]
       (when filter-category
         [filter-chip {:label (name filter-category)
                       :active? true
                       :on-click #(rf/dispatch [:expenses/update-filter :category nil])
                       :on-clear #(rf/dispatch [:expenses/update-filter :category nil])}])]]
     [:div.flow-filter-bar__right
      [:span.flow-filter-bar__count (str expense-count " expenses")]
      (when has-filters?
        [:button.flow-btn.flow-btn--ghost.flow-btn--sm
         {:on-click #(rf/dispatch [:expenses/clear-filters])}
         "Clear filters"])]]))

(defn expense-table-row [{:keys [transaction/id transaction/amount
                                 transaction/category transaction/description
                                 transaction/date transaction/currency]}]
  (let [cat-icon (get db/category-icons category "📦")
        curr (or currency :COP)]
    [:tr.flow-tx-table__row
     [:td.flow-tx-table__cell.flow-tx-table__cell--date
      (format-date-short date)]
     [:td.flow-tx-table__cell.flow-tx-table__cell--desc
      [:div.flow-tx-table__desc-content
       [:span.flow-tx-table__icon cat-icon]
       [:span.flow-tx-table__description (or description "No description")]]]
     [:td.flow-tx-table__cell.flow-tx-table__cell--category
      [:span.flow-chip.flow-chip--sm (name (or category :other))]]
     [:td.flow-tx-table__cell.flow-tx-table__cell--currency
      [:span.flow-chip.flow-chip--sm.flow-chip--currency (name curr)]]
     [:td.flow-tx-table__cell.flow-tx-table__cell--amount.flow-tx-table__cell--expense
      (currency/format-currency (- amount) curr {:show-sign? true})]
     [:td.flow-tx-table__cell.flow-tx-table__cell--actions
      [:button.flow-btn.flow-btn--icon.flow-btn--ghost
       {:on-click #(when (js/confirm "Delete this expense?")
                     (rf/dispatch [:tx/delete-transaction (str id)]))}
       [icon :trash {:width 16 :height 16}]]]]))

(defn expense-table [expenses]
  [:div.flow-tx-table__wrapper
   [:table.flow-tx-table
    [:thead.flow-tx-table__head
     [:tr
      [:th.flow-tx-table__header "Date"]
      [:th.flow-tx-table__header "Description"]
      [:th.flow-tx-table__header "Category"]
      [:th.flow-tx-table__header "Currency"]
      [:th.flow-tx-table__header.flow-tx-table__header--right "Amount"]
      [:th.flow-tx-table__header {:style {:width "60px"}}]]]
    [:tbody.flow-tx-table__body
     (for [t expenses]
       ^{:key (or (:transaction/id t) (random-uuid))}
       [expense-table-row t])]]])

(defn expense-card [{:keys [transaction/id transaction/amount
                            transaction/category transaction/description
                            transaction/date transaction/currency]}]
  (let [cat-icon (get db/category-icons category "📦")
        curr (or currency :COP)]
    [:div.flow-tx-card
     [:div.flow-tx-card__main
      [:div.flow-tx-card__icon cat-icon]
      [:div.flow-tx-card__content
       [:div.flow-tx-card__description (or description "No description")]
       [:div.flow-tx-card__meta
        [:span.flow-tx-card__category (name (or category :other))]
        [:span.flow-tx-card__separator "."]
        [:span.flow-tx-card__currency (name curr)]
        [:span.flow-tx-card__separator "."]
        [:span.flow-tx-card__date (format-date-short date)]]]
      [:div.flow-tx-card__amount.flow-tx-card__amount--expense
       (currency/format-currency (- amount) curr {:show-sign? true})]]
     [:button.flow-tx-card__delete
      {:on-click #(when (js/confirm "Delete this expense?")
                    (rf/dispatch [:tx/delete-transaction (str id)]))}
      [icon :trash {:width 16 :height 16}]]]))

(defn expense-cards [expenses]
  [:div.flow-tx-cards
   (for [t expenses]
     ^{:key (or (:transaction/id t) (random-uuid))}
     [expense-card t])])

(defn category-mini-breakdown []
  (let [breakdown @(rf/subscribe [:expenses/category-breakdown])]
    (when (seq breakdown)
      [:div.flow-category-mini
       [:h4.flow-category-mini__title "Spending by Category"]
       [:div.flow-category-mini__list
        (for [{:keys [category amount percentage]} (take 5 breakdown)]
          ^{:key category}
          [:div.flow-category-mini__item
           [:span.flow-category-mini__icon (get db/category-icons category "📦")]
           [:span.flow-category-mini__name (name category)]
           [:span.flow-category-mini__bar
            [:span.flow-category-mini__fill {:style {:width (str percentage "%")}}]]
           [:span.flow-category-mini__percent (str (.toFixed percentage 0) "%")]])]])))

(defn expenses-view []
  (let [expenses @(rf/subscribe [:expenses/filtered-list])
        grouped @(rf/subscribe [:expenses/grouped-by-date])
        loading? @(rf/subscribe [:app/loading?])
        total-by-currency @(rf/subscribe [:expenses/total-by-currency])
        top-category @(rf/subscribe [:expenses/top-category])]
    [:div.flow-transactions-page
     [:div.flow-page-header
      [:h1.flow-page-title "Expenses"]
      [:p.flow-page-subtitle "Track all your spending"]]

     [:div.flow-summary-cards
      (for [[curr total] total-by-currency]
        ^{:key curr}
        [summary-card {:title (str "Total " (name curr))
                       :value (currency/format-currency total curr)
                       :icon-name :trending-down
                       :variant :expense}])
      (when top-category
        [summary-card {:title "Top Category"
                       :value (name top-category)
                       :subtitle "Highest spending"
                       :icon-name :alert-circle
                       :variant :expense}])]

     [category-mini-breakdown]
     [filter-bar]

     (cond
       loading?
       [:div.flow-tx-loading
        [:div.flow-skeleton.flow-skeleton--animated
         (for [i (range 5)]
           ^{:key i}
           [:div.flow-skeleton__row
            [:div.flow-skeleton__circle]
            [:div.flow-skeleton__lines
             [:div.flow-skeleton__line {:style {:width "60%"}}]
             [:div.flow-skeleton__line.flow-skeleton__line--sm {:style {:width "40%"}}]]])]]

       (empty? expenses)
       [:div.flow-empty
        [:div.flow-empty__icon "💸"]
        [:h3.flow-empty__title "No expenses found"]
        [:p.flow-empty__text
         (if @(rf/subscribe [:expenses/has-active-filters?])
           "Try adjusting your filters to see more results"
           "Start tracking your spending by adding your first expense")]
        [:button.flow-btn.flow-btn--primary
         {:on-click #(rf/dispatch [:app/navigate :add-transaction])}
         "Add Expense"]]

       :else
       [:<>
        [:div.flow-tx-table-container
         [expense-table expenses]]
        [:div.flow-tx-cards-container
         (for [[date-str txs] (sort-by first > grouped)]
           ^{:key (or date-str "unknown")}
           [:div.flow-tx-group
            [:div.flow-tx-group__header (or date-str "Unknown date")]
            [expense-cards txs]])]])]))
