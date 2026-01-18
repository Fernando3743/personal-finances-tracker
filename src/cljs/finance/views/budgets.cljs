(ns finance.views.budgets
  "Budgets page view."
  (:require [re-frame.core :as rf]
            [finance.db :as db]
            [finance.utils.currency :as currency]
            [finance.components.icons :refer [icon]]))

(defn progress-bar [{:keys [percentage status]}]
  (let [clamped (min 100 (max 0 percentage))
        color-class (case status
                      :exceeded "flow-progress--exceeded"
                      :warning "flow-progress--warning"
                      "flow-progress--ok")]
    [:div.flow-progress
     {:class color-class}
     [:div.flow-progress__bar
      {:style {:width (str clamped "%")}}]]))

(defn budget-card [{:keys [budget spent remaining percentage status]}]
  (let [cat (:budget/category budget)
        amount (:budget/amount budget)
        curr (:budget/currency budget)
        cat-icon (get db/category-icons cat "📦")]
    [:div.flow-budget-card
     {:class (case status
               :exceeded "flow-budget-card--exceeded"
               :warning "flow-budget-card--warning"
               "")}
     [:div.flow-budget-card__header
      [:div.flow-budget-card__category
       [:span.flow-budget-card__icon cat-icon]
       [:span.flow-budget-card__name (name cat)]]
      [:div.flow-budget-card__status
       (case status
         :exceeded [:span.flow-chip.flow-chip--sm.flow-chip--expense "Exceeded"]
         :warning [:span.flow-chip.flow-chip--sm.flow-chip--warning "Warning"]
         [:span.flow-chip.flow-chip--sm.flow-chip--income "On Track"])]]
     [:div.flow-budget-card__amounts
      [:div.flow-budget-card__spent
       [:span.flow-budget-card__label "Spent"]
       [:span.flow-budget-card__value (currency/format-currency (or spent 0) curr)]]
      [:div.flow-budget-card__of "of"]
      [:div.flow-budget-card__total
       [:span.flow-budget-card__label "Budget"]
       [:span.flow-budget-card__value (currency/format-currency amount curr)]]]
     [progress-bar {:percentage (* 100 (or percentage 0)) :status status}]
     [:div.flow-budget-card__footer
      [:span.flow-budget-card__remaining
       (if (neg? remaining)
         (str "Over by " (currency/format-currency (abs remaining) curr))
         (str (currency/format-currency remaining curr) " remaining"))]
      [:span.flow-budget-card__percent
       (str (.toFixed (* 100 (or percentage 0)) 0) "%")]]
     [:button.flow-budget-card__delete
      {:on-click #(when (js/confirm "Delete this budget?")
                    (rf/dispatch [:budgets/delete (:budget/id budget)]))}
      [icon :trash {:width 16 :height 16}]]]))

(defn budget-form []
  (let [form @(rf/subscribe [:budgets/form])
        form-valid? @(rf/subscribe [:budgets/form-valid?])
        loading? @(rf/subscribe [:budgets/loading?])
        categories @(rf/subscribe [:tx/categories])]
    [:div.flow-budget-form
     [:h3.flow-budget-form__title "Add Budget"]

     [:div.flow-form-field
      [:label.flow-label "Category"]
      [:select.flow-select
       {:value (if (:category form) (name (:category form)) "")
        :on-change #(rf/dispatch [:budgets/update-form-field :category (keyword (-> % .-target .-value))])}
       [:option {:value ""} "Select a category..."]
       (for [cat categories]
         ^{:key cat}
         [:option {:value (name cat)} (str (get db/category-icons cat "📦") " " (name cat))])]]

     [:div.flow-form-row
      [:div.flow-form-field
       [:label.flow-label "Budget Amount"]
       [:input.flow-input
        {:type "number"
         :placeholder "0.00"
         :value (:amount form)
         :on-change #(rf/dispatch [:budgets/update-form-field :amount (-> % .-target .-value)])}]]
      [:div.flow-form-field
       [:label.flow-label "Currency"]
       [:div.flow-segmented
        [:button.flow-segmented__option
         {:class (when (= :COP (:currency form)) "flow-segmented__option--active")
          :on-click #(rf/dispatch [:budgets/update-form-field :currency :COP])}
         "COP"]
        [:button.flow-segmented__option
         {:class (when (= :USD (:currency form)) "flow-segmented__option--active")
          :on-click #(rf/dispatch [:budgets/update-form-field :currency :USD])}
         "USD"]]]]

     [:div.flow-form-field
      [:label.flow-label "Alert Threshold (%)"]
      [:input.flow-input
       {:type "number"
        :min "0"
        :max "100"
        :placeholder "80"
        :value (:alert-threshold form)
        :on-change #(rf/dispatch [:budgets/update-form-field :alert-threshold (js/parseInt (-> % .-target .-value))])}]
      [:span.flow-form-hint "Alert when spending reaches this percentage"]]

     [:div.flow-form-actions
      [:button.flow-btn.flow-btn--secondary
       {:on-click #(rf/dispatch [:budgets/reset-form])}
       "Reset"]
      [:button.flow-btn.flow-btn--primary
       {:disabled (or (not form-valid?) loading?)
        :on-click #(rf/dispatch [:budgets/create])}
       (if loading? "Creating..." "Create Budget")]]]))

(defn budget-overview []
  (let [status @(rf/subscribe [:budgets/status])
        total-budgeted @(rf/subscribe [:budgets/total-budgeted])
        total-spent @(rf/subscribe [:budgets/total-spent])
        exceeded @(rf/subscribe [:budgets/exceeded])
        warning @(rf/subscribe [:budgets/warning])
        now (js/Date.)
        month (or (:month status) (inc (.getMonth now)))
        year (or (:year status) (.getFullYear now))]
    [:div.flow-budget-overview
     [:div.flow-budget-overview__period
      [:span.flow-budget-overview__month
       (str month "/" year)]]
     [:div.flow-budget-overview__stats
      [:div.flow-budget-stat
       [:span.flow-budget-stat__label "Total Budgeted"]
       [:span.flow-budget-stat__value (currency/format-currency total-budgeted :COP)]]
      [:div.flow-budget-stat
       [:span.flow-budget-stat__label "Total Spent"]
       [:span.flow-budget-stat__value (currency/format-currency total-spent :COP)]]
      (when (pos? exceeded)
        [:div.flow-budget-stat.flow-budget-stat--alert
         [:span.flow-budget-stat__label "Exceeded"]
         [:span.flow-budget-stat__value (str exceeded " budget(s)")]])
      (when (pos? warning)
        [:div.flow-budget-stat.flow-budget-stat--warning
         [:span.flow-budget-stat__label "Warning"]
         [:span.flow-budget-stat__value (str warning " budget(s)")]])]]))

(defn budgets-view []
  (let [summaries @(rf/subscribe [:budgets/budget-summaries])
        all-budgets @(rf/subscribe [:budgets/all-budgets])
        loading? @(rf/subscribe [:budgets/loading?])]
    [:div.flow-budgets-page
     [:div.flow-page-header
      [:h1.flow-page-title "Budgets"]
      [:p.flow-page-subtitle "Set and track your spending limits"]]

     [budget-overview]

     [:div.flow-budgets-layout
      [:div.flow-budgets-main
       (cond
         loading?
         [:div.flow-tx-loading
          [:div.flow-skeleton.flow-skeleton--animated
           (for [i (range 4)]
             ^{:key i}
             [:div.flow-skeleton__row
              [:div.flow-skeleton__circle]
              [:div.flow-skeleton__lines
               [:div.flow-skeleton__line {:style {:width "70%"}}]
               [:div.flow-skeleton__line.flow-skeleton__line--sm {:style {:width "50%"}}]]])]]

         (empty? all-budgets)
         [:div.flow-empty
          [:div.flow-empty__icon "💵"]
          [:h3.flow-empty__title "No budgets set"]
          [:p.flow-empty__text "Create budgets to track your spending by category"]]

         :else
         [:div.flow-budgets-grid
          (for [summary summaries]
            ^{:key (get-in summary [:budget :budget/id])}
            [budget-card summary])
          (for [budget all-budgets]
            (when-not (some #(= (get-in % [:budget :budget/id]) (:budget/id budget)) summaries)
              ^{:key (:budget/id budget)}
              [budget-card {:budget budget :spent 0 :remaining (:budget/amount budget) :percentage 0 :status :ok}]))])]

      [:div.flow-budgets-sidebar
       [budget-form]]]]))
