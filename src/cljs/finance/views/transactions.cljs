(ns finance.views.transactions
  "Transaction list, filters, and form views with Flow design system."
  (:require [re-frame.core :as rf]
            [clojure.string :as str]
            [finance.db :as db]
            [finance.utils.currency :as currency]
            [finance.components.icons :refer [icon]]))

(defn format-date [date-str]
  (when date-str
    (let [date (js/Date. date-str)]
      (.toLocaleDateString date "en-US" #js {:year "numeric"
                                              :month "short"
                                              :day "numeric"}))))

(defn format-date-short [date-str]
  (when date-str
    (let [date (js/Date. date-str)]
      (.toLocaleDateString date "en-US" #js {:month "short"
                                              :day "numeric"}))))

(defn search-input []
  (let [search @(rf/subscribe [:tx/filter-search])]
    [:div.flow-search
     [:span.flow-search__icon [icon :search {:width 18 :height 18}]]
     [:input.flow-search__input
      {:type "text"
       :placeholder "Search transactions..."
       :value (or search "")
       :on-change #(rf/dispatch [:tx/update-filter :search (-> % .-target .-value)])}]
     (when (and search (not (str/blank? search)))
       [:button.flow-search__clear
        {:on-click #(rf/dispatch [:tx/update-filter :search ""])}
        [icon :x {:width 14 :height 14}]])]))

(defn filter-chip [{:keys [label active? on-click on-clear variant]}]
  [:button.flow-chip
   {:class [(when active? "flow-chip--active")
            (when variant (str "flow-chip--" (name variant)))]
    :on-click on-click}
   [:span label]
   (when (and active? on-clear)
     [:span.flow-chip__clear
      {:on-click (fn [e]
                   (.stopPropagation e)
                   (on-clear))}
      [icon :x {:width 14 :height 14}]])])

(defn filter-bar []
  (let [filter-type @(rf/subscribe [:tx/filter-type])
        filter-category @(rf/subscribe [:tx/filter-category])
        filter-currency @(rf/subscribe [:tx/filter-currency])
        has-filters? @(rf/subscribe [:tx/has-active-filters?])
        tx-count @(rf/subscribe [:tx/transaction-count])
        currencies @(rf/subscribe [:dashboard/available-currencies])]
    [:div.flow-filter-bar
     [:div.flow-filter-bar__left
      [search-input]
      [:div.flow-filter-bar__chips
       [filter-chip {:label "All"
                     :active? (nil? filter-type)
                     :on-click #(rf/dispatch [:tx/update-filter :type nil])}]
       [filter-chip {:label "Income"
                     :active? (= filter-type :income)
                     :variant :income
                     :on-click #(rf/dispatch [:tx/update-filter :type :income])
                     :on-clear #(rf/dispatch [:tx/update-filter :type nil])}]
       [filter-chip {:label "Expenses"
                     :active? (= filter-type :expense)
                     :variant :expense
                     :on-click #(rf/dispatch [:tx/update-filter :type :expense])
                     :on-clear #(rf/dispatch [:tx/update-filter :type nil])}]

       [:span.flow-filter-bar__separator "|"]

       [filter-chip {:label "All Currencies"
                     :active? (nil? filter-currency)
                     :on-click #(rf/dispatch [:tx/update-filter :currency nil])}]
       (for [curr currencies]
         ^{:key curr}
         [filter-chip {:label (name curr)
                       :active? (= filter-currency curr)
                       :on-click #(rf/dispatch [:tx/update-filter :currency curr])
                       :on-clear #(rf/dispatch [:tx/update-filter :currency nil])}])

       (when filter-category
         [filter-chip {:label (name filter-category)
                       :active? true
                       :on-click #(rf/dispatch [:tx/update-filter :category nil])
                       :on-clear #(rf/dispatch [:tx/update-filter :category nil])}])]]

     [:div.flow-filter-bar__right
      [:span.flow-filter-bar__count (str tx-count " transactions")]
      (when has-filters?
        [:button.flow-btn.flow-btn--ghost.flow-btn--sm
         {:on-click #(rf/dispatch [:tx/clear-filters])}
         "Clear filters"])]]))

(defn transaction-table-row [{:keys [transaction/id transaction/amount transaction/type
                                     transaction/category transaction/description
                                     transaction/date transaction/currency]}]
  (let [cat-icon (get db/category-icons category "📦")
        is-income? (= type :income)
        curr (or currency :COP)
        display-amount (if is-income? amount (- amount))]
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
     [:td.flow-tx-table__cell.flow-tx-table__cell--amount
      {:class (if is-income?
                "flow-tx-table__cell--income"
                "flow-tx-table__cell--expense")}
      (currency/format-currency display-amount curr {:show-sign? true})]
     [:td.flow-tx-table__cell.flow-tx-table__cell--actions
      [:button.flow-btn.flow-btn--icon.flow-btn--ghost
       {:on-click #(when (js/confirm "Delete this transaction?")
                     (rf/dispatch [:tx/delete-transaction (str id)]))}
       [icon :trash {:width 16 :height 16}]]]]))

(defn transaction-table [transactions]
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
     (for [t transactions]
       ^{:key (or (:transaction/id t) (random-uuid))}
       [transaction-table-row t])]]])

(defn transaction-card [{:keys [transaction/id transaction/amount transaction/type
                                transaction/category transaction/description
                                transaction/date transaction/currency]}]
  (let [cat-icon (get db/category-icons category "📦")
        is-income? (= type :income)
        curr (or currency :COP)
        display-amount (if is-income? amount (- amount))]
    [:div.flow-tx-card
     [:div.flow-tx-card__main
      [:div.flow-tx-card__icon cat-icon]
      [:div.flow-tx-card__content
       [:div.flow-tx-card__description (or description "No description")]
       [:div.flow-tx-card__meta
        [:span.flow-tx-card__category (name (or category :other))]
        [:span.flow-tx-card__separator "•"]
        [:span.flow-tx-card__currency (name curr)]
        [:span.flow-tx-card__separator "•"]
        [:span.flow-tx-card__date (format-date-short date)]]]
      [:div.flow-tx-card__amount
       {:class (if is-income?
                 "flow-tx-card__amount--income"
                 "flow-tx-card__amount--expense")}
       (currency/format-currency display-amount curr {:show-sign? true})]]
     [:button.flow-tx-card__delete
      {:on-click #(when (js/confirm "Delete this transaction?")
                    (rf/dispatch [:tx/delete-transaction (str id)]))}
      [icon :trash {:width 16 :height 16}]]]))

(defn transaction-cards [transactions]
  [:div.flow-tx-cards
   (for [t transactions]
     ^{:key (or (:transaction/id t) (random-uuid))}
     [transaction-card t])])

(defn transaction-list []
  (let [transactions @(rf/subscribe [:tx/filtered-transactions])
        grouped @(rf/subscribe [:tx/transactions-by-date])
        loading? @(rf/subscribe [:app/loading?])]
    [:div.flow-transactions-page
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

       (empty? transactions)
       [:div.flow-empty
        [:div.flow-empty__icon "📋"]
        [:h3.flow-empty__title "No transactions found"]
        [:p.flow-empty__text
         (if @(rf/subscribe [:tx/has-active-filters?])
           "Try adjusting your filters to see more results"
           "Start tracking your finances by adding your first transaction")]
        [:button.flow-btn.flow-btn--primary
         {:on-click #(rf/dispatch [:app/navigate :add-transaction])}
         "Add Transaction"]]

       :else
       [:<>
        [:div.flow-tx-table-container
         [transaction-table transactions]]

        [:div.flow-tx-cards-container
         (for [[date-str txs] (sort-by first > grouped)]
           ^{:key (or date-str "unknown")}
           [:div.flow-tx-group
            [:div.flow-tx-group__header (or date-str "Unknown date")]
            [transaction-cards txs]])]])]))

(defn amount-input []
  (let [amount @(rf/subscribe [:tx/form-field :amount])
        display @(rf/subscribe [:tx/form-amount-display])
        tx-type @(rf/subscribe [:tx/form-field :type])
        curr @(rf/subscribe [:tx/form-currency])]
    [:div.flow-amount-input
     [:span.flow-amount-input__currency
      {:class (if (= tx-type :income)
                "flow-amount-input__currency--income"
                "flow-amount-input__currency--expense")}
      (currency/currency-symbol curr)]
     [:input.flow-amount-input__field
      {:type "text"
       :inputMode "decimal"
       :placeholder "0.00"
       :value (or amount "")
       :on-change #(let [val (-> % .-target .-value)
                         cleaned (str/replace val #"[^\d.]" "")]
                     (rf/dispatch [:tx/update-form-field :amount cleaned]))}]]))

(defn type-toggle []
  (let [current-type @(rf/subscribe [:tx/form-field :type])]
    [:div.flow-segmented
     [:button.flow-segmented__option
      {:class (when (= current-type :expense) "flow-segmented__option--active flow-segmented__option--expense")
       :on-click #(rf/dispatch [:tx/update-form-field :type :expense])}
      "Expense"]
     [:button.flow-segmented__option
      {:class (when (= current-type :income) "flow-segmented__option--active flow-segmented__option--income")
       :on-click #(rf/dispatch [:tx/update-form-field :type :income])}
      "Income"]]))

(defn category-picker []
  (let [categories @(rf/subscribe [:tx/categories])
        selected @(rf/subscribe [:tx/form-field :category])]
    [:div.flow-category-picker
     [:label.flow-label "Category"]
     [:div.flow-category-picker__grid
      (for [cat categories]
        (let [cat-icon (get db/category-icons cat "📦")
              active? (= cat selected)]
          ^{:key cat}
          [:button.flow-category-picker__item
           {:class (when active? "flow-category-picker__item--active")
            :on-click #(rf/dispatch [:tx/update-form-field :category cat])}
           [:span.flow-category-picker__icon cat-icon]
           [:span.flow-category-picker__label (name cat)]
           (when active?
             [:span.flow-category-picker__check [icon :check {:width 16 :height 16}]])]))]]))

(defn description-input []
  (let [description @(rf/subscribe [:tx/form-field :description])]
    [:div.flow-form-field
     [:label.flow-label {:for "description"} "Description"]
     [:div.flow-input-wrapper
      [:input.flow-input
       {:type "text"
        :id "description"
        :placeholder "What was this for?"
        :value (or description "")
        :on-change #(rf/dispatch [:tx/update-form-field :description (-> % .-target .-value)])}]]]))

(defn date-input []
  (let [date @(rf/subscribe [:tx/form-field :date])
        today (.toISOString (js/Date.))]
    [:div.flow-form-field
     [:label.flow-label {:for "date"} "Date"]
     [:div.flow-input-wrapper.flow-input-wrapper--icon
      [:span.flow-input__icon [icon :calendar {:width 18 :height 18}]]
      [:input.flow-input
       {:type "date"
        :id "date"
        :value (or date (subs today 0 10))
        :on-change #(rf/dispatch [:tx/update-form-field :date (-> % .-target .-value)])}]]]))

(defn currency-selector []
  (let [currencies @(rf/subscribe [:dashboard/available-currencies])
        selected @(rf/subscribe [:tx/form-currency])]
    [:div.flow-form-field
     [:label.flow-label "Currency"]
     [:div.flow-segmented
      (for [curr currencies]
        ^{:key curr}
        [:button.flow-segmented__option
         {:class (when (= curr selected) "flow-segmented__option--active")
          :on-click #(rf/dispatch [:tx/update-form-field :currency curr])}
         (name curr)])]]))

(defn exchange-rate-input []
  (let [rate @(rf/subscribe [:tx/form-field :exchange-rate])
        curr @(rf/subscribe [:tx/form-currency])]
    (when (= curr :USD)
      [:div.flow-form-field
       [:label.flow-label {:for "exchange-rate"} "Exchange Rate (COP/USD) - Optional"]
       [:div.flow-input-wrapper
        [:input.flow-input
         {:type "number"
          :id "exchange-rate"
          :placeholder "e.g., 4000"
          :step "0.01"
          :value (or rate "")
          :on-change #(let [v (-> % .-target .-value)]
                        (rf/dispatch [:tx/update-form-field
                                      :exchange-rate
                                      (when (not (str/blank? v))
                                        (js/parseFloat v))]))}]]])))

(defn add-transaction-form []
  (let [form-valid? @(rf/subscribe [:tx/form-valid?])
        loading? @(rf/subscribe [:app/loading?])]
    [:div.flow-add-form
     [:div.flow-add-form__header
      [:h2.flow-add-form__title "Add Transaction"]]

     [:div.flow-add-form__body
      [:div.flow-add-form__amount-section
       [amount-input]
       [type-toggle]]

      [currency-selector]
      [exchange-rate-input]
      [category-picker]
      [description-input]
      [date-input]]

     [:div.flow-add-form__footer
      [:button.flow-btn.flow-btn--secondary.flow-btn--lg
       {:on-click #(do
                     (rf/dispatch [:tx/reset-form])
                     (rf/dispatch [:app/navigate :transactions]))}
       "Cancel"]
      [:button.flow-btn.flow-btn--primary.flow-btn--lg
       {:disabled (or (not form-valid?) loading?)
        :on-click #(rf/dispatch [:tx/create-transaction])}
       (if loading?
         [:span.flow-btn__loading "Saving..."]
         "Save Transaction")]]]))
