(ns finance.views.transaction-panel
  "Slide-in panel for adding transactions."
  (:require [re-frame.core :as rf]
            [clojure.string :as str]
            [finance.db :as db]
            [finance.utils.currency :as currency]
            [finance.components.icons :refer [icon]]))

(defn preview-card []
  (let [preview @(rf/subscribe [:tx/form-preview])
        {:keys [amount type category description currency date]} preview
        cat-icon (get db/category-icons category "📦")
        is-income? (= type :income)
        display-amount (if is-income? amount (- amount))]
    [:div.flow-tx-preview
     {:class (if is-income? "flow-tx-preview--income" "flow-tx-preview--expense")}
     [:div.flow-tx-preview__inner
      [:div.flow-tx-preview__header
       [:div
        [:div.flow-tx-preview__label "Preview"]
        [:div.flow-tx-preview__amount
         (currency/format-currency display-amount currency {:show-sign? true})]
        [:div.flow-tx-preview__name
         (if (str/blank? description) "Transaction Name" description)]]
       [:div.flow-tx-preview__icon-box cat-icon]]
      [:div.flow-tx-preview__footer
       [:div.flow-tx-preview__badge
        [icon :calendar {:width 14 :height 14}]
        [:span date]]
       [:div.flow-tx-preview__category
        [:span.flow-tx-preview__category-dot]
        [:span (str/capitalize (name category))]]]]]))

(defn type-toggle []
  (let [current-type @(rf/subscribe [:tx/form-field :type])]
    [:div.flow-panel-segmented
     [:button.flow-panel-segmented__option
      {:class (when (= current-type :expense) "flow-panel-segmented__option--active flow-panel-segmented__option--expense")
       :on-click #(rf/dispatch [:tx/update-form-field :type :expense])}
      "Expense"]
     [:button.flow-panel-segmented__option
      {:class (when (= current-type :income) "flow-panel-segmented__option--active flow-panel-segmented__option--income")
       :on-click #(rf/dispatch [:tx/update-form-field :type :income])}
      "Income"]]))

(defn name-input []
  (let [description @(rf/subscribe [:tx/form-field :description])]
    [:div.flow-panel-field
     [:label.flow-panel-field__label "Transaction Name"]
     [:div.flow-panel-field__input-wrapper.flow-panel-field__input-wrapper--icon
      [icon :file-text {:width 18 :height 18 :class "flow-panel-field__icon"}]
      [:input.flow-panel-field__input
       {:type "text"
        :placeholder "e.g., Grocery shopping"
        :value (or description "")
        :on-change #(rf/dispatch [:tx/update-form-field :description (-> % .-target .-value)])}]]]))

(defn currency-amount-row []
  (let [amount @(rf/subscribe [:tx/form-field :amount])
        currency @(rf/subscribe [:tx/form-currency])
        currencies @(rf/subscribe [:dashboard/available-currencies])]
    [:div.flow-panel-row
     [:div.flow-panel-field.flow-panel-field--currency
      [:label.flow-panel-field__label "Currency"]
      [:div.flow-panel-field__select-wrapper
       [:select.flow-panel-field__select
        {:value (name currency)
         :on-change #(rf/dispatch [:tx/update-form-field :currency (keyword (-> % .-target .-value))])}
        (for [c currencies]
          ^{:key c}
          [:option {:value (name c)} (name c)])]
       [icon :chevron-down {:width 16 :height 16 :class "flow-panel-field__select-icon"}]]]
     [:div.flow-panel-field.flow-panel-field--amount
      [:label.flow-panel-field__label "Amount"]
      [:div.flow-panel-field__input-wrapper
       [:span.flow-panel-field__prefix (currency/currency-symbol currency)]
       [:input.flow-panel-field__input.flow-panel-field__input--amount
        {:type "text"
         :inputMode "decimal"
         :placeholder "0.00"
         :value (or amount "")
         :on-change #(let [val (-> % .-target .-value)
                           cleaned (str/replace val #"[^\d.]" "")]
                       (rf/dispatch [:tx/update-form-field :amount cleaned]))}]]]]))

(defn category-dropdown []
  (let [categories @(rf/subscribe [:tx/categories])
        selected @(rf/subscribe [:tx/form-field :category])
        selected-icon (get db/category-icons selected "📦")]
    [:div.flow-panel-field
     [:label.flow-panel-field__label "Category"]
     [:div.flow-panel-field__select-wrapper.flow-panel-field__select-wrapper--with-icon
      [:span.flow-panel-field__category-icon selected-icon]
      [:select.flow-panel-field__select.flow-panel-field__select--with-icon
       {:value (name selected)
        :on-change #(rf/dispatch [:tx/update-form-field :category (keyword (-> % .-target .-value))])}
       (for [cat categories]
         ^{:key cat}
         [:option {:value (name cat)} (str/capitalize (name cat))])]
      [icon :chevron-down {:width 16 :height 16 :class "flow-panel-field__select-icon"}]]]))

(defn date-picker []
  (let [date @(rf/subscribe [:tx/form-field :date])
        today (.toISOString (js/Date.))]
    [:div.flow-panel-field
     [:label.flow-panel-field__label "Date"]
     [:div.flow-panel-field__input-wrapper.flow-panel-field__input-wrapper--icon
      [icon :calendar {:width 18 :height 18 :class "flow-panel-field__icon"}]
      [:input.flow-panel-field__input
       {:type "date"
        :value (or date (subs today 0 10))
        :on-change #(rf/dispatch [:tx/update-form-field :date (-> % .-target .-value)])}]]]))

(defn wallet-dropdown []
  [:div.flow-panel-field.flow-panel-field--disabled
   [:label.flow-panel-field__label
    "Source Account / Wallet"
    [:span.flow-panel-field__badge "Coming Soon"]]
   [:div.flow-panel-field__input-wrapper.flow-panel-field__input-wrapper--icon
    [icon :wallet {:width 18 :height 18 :class "flow-panel-field__icon"}]
    [:select.flow-panel-field__select.flow-panel-field__select--icon-left
     {:disabled true}
     [:option "Main Checking (**** 4532)"]]
    [icon :chevron-down {:width 16 :height 16 :class "flow-panel-field__select-icon"}]]])

(defn recurring-toggle []
  (let [is-recurring @(rf/subscribe [:tx/form-is-recurring])]
    [:div.flow-panel-toggle-field
     [:div.flow-panel-toggle-field__info
      [:span.flow-panel-toggle-field__label "Make Recurring"]
      [:span.flow-panel-toggle-field__description "Repeat this transaction monthly"]]
     [:button.flow-toggle
      {:class (when is-recurring "flow-toggle--active")
       :on-click #(rf/dispatch [:tx/update-form-field :is-recurring (not is-recurring)])
       :role "switch"
       :aria-checked (str is-recurring)}
      [:span.flow-toggle__track]
      [:span.flow-toggle__thumb]]]))

(defn notes-input []
  (let [notes @(rf/subscribe [:tx/form-notes])]
    [:div.flow-panel-field
     [:label.flow-panel-field__label "Notes " [:span.flow-panel-field__optional "(optional)"]]
     [:textarea.flow-panel-field__textarea
      {:placeholder "Add any additional details..."
       :rows 3
       :value notes
       :on-change #(rf/dispatch [:tx/update-form-field :notes (-> % .-target .-value)])}]]))

(defn transaction-panel []
  (let [panel @(rf/subscribe [:app/panel])
        is-open? (and (:open? panel) (= (:mode panel) :add-transaction))
        form-valid? @(rf/subscribe [:tx/form-valid?])
        loading? @(rf/subscribe [:app/loading?])]
    [:<>
     (when is-open?
       [:div.flow-modal-backdrop.flow-modal-backdrop--visible
        {:on-click #(rf/dispatch [:app/close-panel])}])
     [:div.flow-panel
      {:class (when is-open? "flow-panel--visible")}
      [:div.flow-panel__header
       [:button.flow-panel__back
        {:on-click #(rf/dispatch [:app/close-panel])}
        [icon :arrow-left {:width 20 :height 20}]]
       [:h2.flow-panel__title "New Transaction"]
       [:button.flow-panel__close
        {:on-click #(rf/dispatch [:app/close-panel])}
        [icon :x {:width 20 :height 20}]]]

      [:div.flow-panel__body
       [preview-card]
       [type-toggle]
       [name-input]
       [currency-amount-row]
       [category-dropdown]
       [date-picker]
       [wallet-dropdown]
       [recurring-toggle]
       [notes-input]]

      [:div.flow-panel__footer
       [:button.flow-btn.flow-btn--secondary
        {:on-click #(rf/dispatch [:app/close-panel])}
        "Cancel"]
       [:button.flow-btn.flow-btn--primary
        {:disabled (or (not form-valid?) loading?)
         :on-click #(rf/dispatch [:tx/create-transaction])}
        (if loading?
          [:span.flow-btn__loading "Saving..."]
          "Save Transaction")]]]]))
