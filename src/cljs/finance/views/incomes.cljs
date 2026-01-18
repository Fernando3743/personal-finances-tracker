(ns finance.views.incomes
  "Incomes page view - display income transactions."
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
  (let [search @(rf/subscribe [:incomes/filter-search])]
    [:div.flow-search
     [:span.flow-search__icon [icon :search {:width 18 :height 18}]]
     [:input.flow-search__input
      {:type "text"
       :placeholder "Search incomes..."
       :value (or search "")
       :on-change #(rf/dispatch [:incomes/update-filter :search (-> % .-target .-value)])}]
     (when (and search (not (str/blank? search)))
       [:button.flow-search__clear
        {:on-click #(rf/dispatch [:incomes/update-filter :search ""])}
        [icon :x {:width 14 :height 14}]])]))

(defn filter-chip [{:keys [label active? on-click on-clear]}]
  [:button.flow-chip
   {:class (when active? "flow-chip--active flow-chip--income")
    :on-click on-click}
   [:span label]
   (when (and active? on-clear)
     [:span.flow-chip__clear
      {:on-click (fn [e] (.stopPropagation e) (on-clear))}
      [icon :x {:width 14 :height 14}]])])

(defn filter-bar []
  (let [filter-category @(rf/subscribe [:incomes/filter-category])
        filter-currency @(rf/subscribe [:incomes/filter-currency])
        has-filters? @(rf/subscribe [:incomes/has-active-filters?])
        income-count @(rf/subscribe [:incomes/filtered-count])]
    [:div.flow-filter-bar
     [:div.flow-filter-bar__left
      [search-input]
      [:div.flow-filter-bar__chips
       [filter-chip {:label "All Currencies"
                     :active? (nil? filter-currency)
                     :on-click #(rf/dispatch [:incomes/update-filter :currency nil])}]
       [filter-chip {:label "COP"
                     :active? (= filter-currency :COP)
                     :on-click #(rf/dispatch [:incomes/update-filter :currency :COP])
                     :on-clear #(rf/dispatch [:incomes/update-filter :currency nil])}]
       [filter-chip {:label "USD"
                     :active? (= filter-currency :USD)
                     :on-click #(rf/dispatch [:incomes/update-filter :currency :USD])
                     :on-clear #(rf/dispatch [:incomes/update-filter :currency nil])}]
       (when filter-category
         [filter-chip {:label (name filter-category)
                       :active? true
                       :on-click #(rf/dispatch [:incomes/update-filter :category nil])
                       :on-clear #(rf/dispatch [:incomes/update-filter :category nil])}])]]
     [:div.flow-filter-bar__right
      [:span.flow-filter-bar__count (str income-count " incomes")]
      (when has-filters?
        [:button.flow-btn.flow-btn--ghost.flow-btn--sm
         {:on-click #(rf/dispatch [:incomes/clear-filters])}
         "Clear filters"])]]))

(defn income-table-row [{:keys [transaction/id transaction/amount
                                transaction/category transaction/description
                                transaction/date transaction/currency]}]
  (let [cat-icon (get db/category-icons category "💰")
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
     [:td.flow-tx-table__cell.flow-tx-table__cell--amount.flow-tx-table__cell--income
      (currency/format-currency amount curr {:show-sign? true})]
     [:td.flow-tx-table__cell.flow-tx-table__cell--actions
      [:button.flow-btn.flow-btn--icon.flow-btn--ghost
       {:on-click #(when (js/confirm "Delete this income?")
                     (rf/dispatch [:tx/delete-transaction (str id)]))}
       [icon :trash {:width 16 :height 16}]]]]))

(defn income-table [incomes]
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
     (for [t incomes]
       ^{:key (or (:transaction/id t) (random-uuid))}
       [income-table-row t])]]])

(defn income-card [{:keys [transaction/id transaction/amount
                           transaction/category transaction/description
                           transaction/date transaction/currency]}]
  (let [cat-icon (get db/category-icons category "💰")
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
      [:div.flow-tx-card__amount.flow-tx-card__amount--income
       (currency/format-currency amount curr {:show-sign? true})]]
     [:button.flow-tx-card__delete
      {:on-click #(when (js/confirm "Delete this income?")
                    (rf/dispatch [:tx/delete-transaction (str id)]))}
      [icon :trash {:width 16 :height 16}]]]))

(defn income-cards [incomes]
  [:div.flow-tx-cards
   (for [t incomes]
     ^{:key (or (:transaction/id t) (random-uuid))}
     [income-card t])])

(defn incomes-view []
  (let [incomes @(rf/subscribe [:incomes/filtered-list])
        grouped @(rf/subscribe [:incomes/grouped-by-date])
        loading? @(rf/subscribe [:app/loading?])
        total-by-currency @(rf/subscribe [:incomes/total-by-currency])
        top-category @(rf/subscribe [:incomes/top-category])]
    [:div.flow-transactions-page
     [:div.flow-page-header
      [:h1.flow-page-title "Incomes"]
      [:p.flow-page-subtitle "Track all your income sources"]]

     [:div.flow-summary-cards
      (for [[curr total] total-by-currency]
        ^{:key curr}
        [summary-card {:title (str "Total " (name curr))
                       :value (currency/format-currency total curr)
                       :icon-name :trending-up
                       :variant :income}])
      (when top-category
        [summary-card {:title "Top Source"
                       :value (name top-category)
                       :subtitle "Most frequent income"
                       :icon-name :star
                       :variant :income}])]

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

       (empty? incomes)
       [:div.flow-empty
        [:div.flow-empty__icon "💰"]
        [:h3.flow-empty__title "No incomes found"]
        [:p.flow-empty__text
         (if @(rf/subscribe [:incomes/has-active-filters?])
           "Try adjusting your filters to see more results"
           "Start tracking your income by adding your first income transaction")]
        [:button.flow-btn.flow-btn--primary
         {:on-click #(rf/dispatch [:app/navigate :add-transaction])}
         "Add Income"]]

       :else
       [:<>
        [:div.flow-tx-table-container
         [income-table incomes]]
        [:div.flow-tx-cards-container
         (for [[date-str txs] (sort-by first > grouped)]
           ^{:key (or date-str "unknown")}
           [:div.flow-tx-group
            [:div.flow-tx-group__header (or date-str "Unknown date")]
            [income-cards txs]])]])]))
