(ns finance.views.recurring
  "Recurring transactions page view."
  (:require [re-frame.core :as rf]
            [clojure.string :as str]
            [finance.db :as db]
            [finance.utils.currency :as currency]
            [finance.components.icons :refer [icon]]))

(defn format-date [date-str]
  (when date-str
    (let [date (js/Date. date-str)]
      (.toLocaleDateString date "en-US" #js {:month "short" :day "numeric" :year "numeric"}))))

(defn frequency-label [freq]
  (case freq
    :daily "Daily"
    :weekly "Weekly"
    :monthly "Monthly"
    :yearly "Yearly"
    "Unknown"))

(defn recurring-card [{:keys [recurring/id recurring/amount recurring/type
                              recurring/category recurring/description
                              recurring/currency recurring/frequency
                              recurring/next-occurrence recurring/active?]}]
  (let [cat-icon (get db/category-icons category "📦")
        curr (or currency :COP)
        is-income? (= type :income)]
    [:div.flow-recurring-card
     {:class (when-not active? "flow-recurring-card--inactive")}
     [:div.flow-recurring-card__header
      [:div.flow-recurring-card__type
       {:class (if is-income? "flow-recurring-card__type--income" "flow-recurring-card__type--expense")}
       (if is-income? "Income" "Expense")]
      [:div.flow-recurring-card__frequency
       [:span.flow-chip.flow-chip--sm (frequency-label frequency)]]
      [:button.flow-recurring-card__toggle
       {:on-click #(rf/dispatch [:recurring/toggle-active id])
        :title (if active? "Deactivate" "Activate")}
       [icon (if active? :toggle-right :toggle-left) {:width 24 :height 24}]]]
     [:div.flow-recurring-card__main
      [:div.flow-recurring-card__icon cat-icon]
      [:div.flow-recurring-card__content
       [:div.flow-recurring-card__description (or description "No description")]
       [:div.flow-recurring-card__meta
        [:span.flow-recurring-card__category (name (or category :other))]
        [:span.flow-recurring-card__separator "."]
        [:span.flow-recurring-card__currency (name curr)]]]
      [:div.flow-recurring-card__amount
       {:class (if is-income?
                 "flow-recurring-card__amount--income"
                 "flow-recurring-card__amount--expense")}
       (currency/format-currency amount curr)]]
     [:div.flow-recurring-card__footer
      [:div.flow-recurring-card__next
       [:span.flow-recurring-card__next-label "Next:"]
       [:span.flow-recurring-card__next-date (format-date next-occurrence)]]
      [:button.flow-btn.flow-btn--icon.flow-btn--ghost
       {:on-click #(when (js/confirm "Delete this recurring transaction?")
                     (rf/dispatch [:recurring/delete id]))}
       [icon :trash {:width 16 :height 16}]]]]))

(defn recurring-form []
  (let [form @(rf/subscribe [:recurring/form])
        form-valid? @(rf/subscribe [:recurring/form-valid?])
        loading? @(rf/subscribe [:recurring/loading?])
        categories @(rf/subscribe [:tx/categories])]
    [:div.flow-recurring-form
     [:h3.flow-recurring-form__title "Add Recurring Transaction"]

     [:div.flow-form-row
      [:div.flow-form-field
       [:label.flow-label "Amount"]
       [:input.flow-input
        {:type "number"
         :placeholder "0.00"
         :value (:amount form)
         :on-change #(rf/dispatch [:recurring/update-form-field :amount (-> % .-target .-value)])}]]
      [:div.flow-form-field
       [:label.flow-label "Currency"]
       [:div.flow-segmented
        [:button.flow-segmented__option
         {:class (when (= :COP (:currency form)) "flow-segmented__option--active")
          :on-click #(rf/dispatch [:recurring/update-form-field :currency :COP])}
         "COP"]
        [:button.flow-segmented__option
         {:class (when (= :USD (:currency form)) "flow-segmented__option--active")
          :on-click #(rf/dispatch [:recurring/update-form-field :currency :USD])}
         "USD"]]]]

     [:div.flow-form-field
      [:label.flow-label "Type"]
      [:div.flow-segmented
       [:button.flow-segmented__option
        {:class (when (= :expense (:type form)) "flow-segmented__option--active flow-segmented__option--expense")
         :on-click #(rf/dispatch [:recurring/update-form-field :type :expense])}
        "Expense"]
       [:button.flow-segmented__option
        {:class (when (= :income (:type form)) "flow-segmented__option--active flow-segmented__option--income")
         :on-click #(rf/dispatch [:recurring/update-form-field :type :income])}
        "Income"]]]

     [:div.flow-form-field
      [:label.flow-label "Frequency"]
      [:div.flow-segmented
       (for [freq [:daily :weekly :monthly :yearly]]
         ^{:key freq}
         [:button.flow-segmented__option
          {:class (when (= freq (:frequency form)) "flow-segmented__option--active")
           :on-click #(rf/dispatch [:recurring/update-form-field :frequency freq])}
          (frequency-label freq)])]]

     [:div.flow-form-field
      [:label.flow-label "Category"]
      [:select.flow-select
       {:value (name (or (:category form) :other))
        :on-change #(rf/dispatch [:recurring/update-form-field :category (keyword (-> % .-target .-value))])}
       (for [cat categories]
         ^{:key cat}
         [:option {:value (name cat)} (str (get db/category-icons cat "📦") " " (name cat))])]]

     [:div.flow-form-field
      [:label.flow-label "Description"]
      [:input.flow-input
       {:type "text"
        :placeholder "Description (optional)"
        :value (:description form)
        :on-change #(rf/dispatch [:recurring/update-form-field :description (-> % .-target .-value)])}]]

     [:div.flow-form-row
      [:div.flow-form-field
       [:label.flow-label "Start Date"]
       [:input.flow-input
        {:type "date"
         :value (or (:start-date form) "")
         :on-change #(rf/dispatch [:recurring/update-form-field :start-date (-> % .-target .-value)])}]]
      [:div.flow-form-field
       [:label.flow-label "End Date (Optional)"]
       [:input.flow-input
        {:type "date"
         :value (or (:end-date form) "")
         :on-change #(rf/dispatch [:recurring/update-form-field :end-date (-> % .-target .-value)])}]]]

     [:div.flow-form-actions
      [:button.flow-btn.flow-btn--secondary
       {:on-click #(rf/dispatch [:recurring/reset-form])}
       "Reset"]
      [:button.flow-btn.flow-btn--primary
       {:disabled (or (not form-valid?) loading?)
        :on-click #(rf/dispatch [:recurring/create])}
       (if loading? "Creating..." "Create Recurring")]]]))

(defn recurring-view []
  (let [items @(rf/subscribe [:recurring/items])
        active-items @(rf/subscribe [:recurring/active-items])
        inactive-items @(rf/subscribe [:recurring/inactive-items])
        loading? @(rf/subscribe [:recurring/loading?])
        upcoming @(rf/subscribe [:recurring/upcoming])]
    [:div.flow-recurring-page
     [:div.flow-page-header
      [:h1.flow-page-title "Recurring Transactions"]
      [:p.flow-page-subtitle "Manage your recurring income and expenses"]
      [:button.flow-btn.flow-btn--primary
       {:on-click #(rf/dispatch [:recurring/generate-now])
        :disabled loading?}
       [icon :refresh-cw {:width 16 :height 16}]
       "Generate Pending"]]

     [:div.flow-recurring-layout
      [:div.flow-recurring-main
       (when (seq upcoming)
         [:div.flow-recurring-section
          [:h3.flow-recurring-section__title "Upcoming (Next 7 Days)"]
          [:div.flow-recurring-list
           (for [item upcoming]
             ^{:key (:recurring/id item)}
             [recurring-card item])]])

       (when (seq active-items)
         [:div.flow-recurring-section
          [:h3.flow-recurring-section__title
           (str "Active (" (count active-items) ")")]
          [:div.flow-recurring-list
           (for [item active-items]
             ^{:key (:recurring/id item)}
             [recurring-card item])]])

       (when (seq inactive-items)
         [:div.flow-recurring-section
          [:h3.flow-recurring-section__title
           {:class "flow-recurring-section__title--muted"}
           (str "Inactive (" (count inactive-items) ")")]
          [:div.flow-recurring-list
           (for [item inactive-items]
             ^{:key (:recurring/id item)}
             [recurring-card item])]])

       (when (and (empty? items) (not loading?))
         [:div.flow-empty
          [:div.flow-empty__icon "🔄"]
          [:h3.flow-empty__title "No recurring transactions"]
          [:p.flow-empty__text "Create a recurring transaction to automate your finances"]])]

      [:div.flow-recurring-sidebar
       [recurring-form]]]]))
