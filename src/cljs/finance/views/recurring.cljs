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
    [:div {:class (str "bg-white dark:bg-neutral-800 rounded-xl border border-neutral-200 dark:border-neutral-700 p-4 "
                       (when-not active? "opacity-50"))}
     [:div {:class "flex items-center justify-between mb-3"}
      [:div {:class (str "px-2 py-0.5 rounded-full text-xs font-medium "
                         (if is-income? "bg-green-100 dark:bg-green-900/20 text-green-600 dark:text-green-500" "bg-red-100 dark:bg-red-900/20 text-red-600 dark:text-red-500"))}
       (if is-income? "Income" "Expense")]
      [:div {:class "flex items-center gap-2"}
       [:span {:class "px-2 py-0.5 rounded bg-neutral-100 dark:bg-neutral-800 text-xs font-medium text-neutral-600 dark:text-neutral-400"}
        (frequency-label frequency)]
       [:button {:class "p-1.5 rounded-lg text-neutral-400 dark:text-neutral-500 hover:text-purple-700 dark:hover:text-purple-400 transition-colors"
                 :on-click #(rf/dispatch [:recurring/toggle-active id])
                 :title (if active? "Deactivate" "Activate")}
        [icon (if active? :toggle-right :toggle-left) {:width 20 :height 20}]]]]
     [:div {:class "flex items-center gap-3"}
      [:span {:class "text-2xl"} cat-icon]
      [:div {:class "flex-1 min-w-0"}
       [:div {:class "text-sm font-medium text-neutral-900 dark:text-neutral-50 truncate"} (or description "No description")]
       [:div {:class "flex items-center gap-2 text-xs text-neutral-400 dark:text-neutral-500 mt-0.5"}
        [:span (name (or category :other))]
        [:span "•"]
        [:span (name curr)]]]
      [:div {:class (str "font-mono font-semibold "
                         (if is-income? "text-green-600 dark:text-green-500" "text-red-600 dark:text-red-500"))}
       (currency/format-currency amount curr)]]
     [:div {:class "flex items-center justify-between mt-4 pt-3 border-t border-neutral-200 dark:border-neutral-700"}
      [:div {:class "text-sm"}
       [:span {:class "text-neutral-400 dark:text-neutral-500"} "Next: "]
       [:span {:class "text-neutral-900 dark:text-neutral-50 font-medium"} (format-date next-occurrence)]]
      [:button {:class "p-2 rounded-lg text-neutral-400 dark:text-neutral-500 hover:text-red-500 hover:bg-red-100 dark:hover:bg-red-900/20 transition-colors"
                :on-click #(when (js/confirm "Delete this recurring transaction?")
                             (rf/dispatch [:recurring/delete id]))}
       [icon :trash {:width 16 :height 16}]]]]))

(defn recurring-form []
  (let [form @(rf/subscribe [:recurring/form])
        form-valid? @(rf/subscribe [:recurring/form-valid?])
        loading? @(rf/subscribe [:recurring/loading?])
        categories @(rf/subscribe [:tx/categories])]
    [:div {:class "bg-white dark:bg-neutral-800 rounded-xl border border-neutral-200 dark:border-neutral-700 p-5"}
     [:h3 {:class "text-lg font-semibold text-neutral-900 dark:text-neutral-50 mb-4"} "Add Recurring Transaction"]

     [:div {:class "space-y-4"}
      [:div {:class "grid grid-cols-2 gap-3"}
       [:div {:class "space-y-1.5"}
        [:label {:class "block text-sm font-medium text-neutral-900 dark:text-neutral-50"} "Amount"]
        [:input {:class (str "w-full h-11 px-4 rounded-lg border border-neutral-200 dark:border-neutral-700 bg-white dark:bg-neutral-900 text-neutral-900 dark:text-neutral-50 "
                             "placeholder:text-neutral-400 dark:placeholder:text-neutral-500 focus:outline-none focus:ring-2 focus:ring-purple-700 dark:focus:ring-purple-400 focus:border-transparent")
                 :type "number"
                 :placeholder "0.00"
                 :value (:amount form)
                 :on-change #(rf/dispatch [:recurring/update-form-field :amount (-> % .-target .-value)])}]]
       [:div {:class "space-y-1.5"}
        [:label {:class "block text-sm font-medium text-neutral-900 dark:text-neutral-50"} "Currency"]
        [:div {:class "flex gap-1 p-1 bg-neutral-100 dark:bg-neutral-800 rounded-lg h-11"}
         [:button {:class (str "flex-1 rounded-md text-sm font-medium transition-colors "
                               (if (= :COP (:currency form))
                                 "bg-white dark:bg-neutral-800 text-neutral-900 dark:text-neutral-50 shadow-sm"
                                 "text-neutral-600 dark:text-neutral-400 hover:text-neutral-900 dark:hover:text-neutral-50"))
                   :on-click #(rf/dispatch [:recurring/update-form-field :currency :COP])}
          "COP"]
         [:button {:class (str "flex-1 rounded-md text-sm font-medium transition-colors "
                               (if (= :USD (:currency form))
                                 "bg-white dark:bg-neutral-800 text-neutral-900 dark:text-neutral-50 shadow-sm"
                                 "text-neutral-600 dark:text-neutral-400 hover:text-neutral-900 dark:hover:text-neutral-50"))
                   :on-click #(rf/dispatch [:recurring/update-form-field :currency :USD])}
          "USD"]]]]

      [:div {:class "space-y-1.5"}
       [:label {:class "block text-sm font-medium text-neutral-900 dark:text-neutral-50"} "Type"]
       [:div {:class "flex gap-1 p-1 bg-neutral-100 dark:bg-neutral-800 rounded-lg"}
        [:button {:class (str "flex-1 px-4 py-2 rounded-md text-sm font-medium transition-colors "
                              (if (= :expense (:type form))
                                "bg-red-500 text-white shadow-sm"
                                "text-neutral-600 dark:text-neutral-400 hover:text-neutral-900 dark:hover:text-neutral-50"))
                  :on-click #(rf/dispatch [:recurring/update-form-field :type :expense])}
         "Expense"]
        [:button {:class (str "flex-1 px-4 py-2 rounded-md text-sm font-medium transition-colors "
                              (if (= :income (:type form))
                                "bg-green-500 text-white shadow-sm"
                                "text-neutral-600 dark:text-neutral-400 hover:text-neutral-900 dark:hover:text-neutral-50"))
                  :on-click #(rf/dispatch [:recurring/update-form-field :type :income])}
         "Income"]]]

      [:div {:class "space-y-1.5"}
       [:label {:class "block text-sm font-medium text-neutral-900 dark:text-neutral-50"} "Frequency"]
       [:div {:class "flex gap-1 p-1 bg-neutral-100 dark:bg-neutral-800 rounded-lg"}
        (for [freq [:daily :weekly :monthly :yearly]]
          ^{:key freq}
          [:button {:class (str "flex-1 px-2 py-2 rounded-md text-xs font-medium transition-colors "
                                (if (= freq (:frequency form))
                                  "bg-white dark:bg-neutral-800 text-neutral-900 dark:text-neutral-50 shadow-sm"
                                  "text-neutral-600 dark:text-neutral-400 hover:text-neutral-900 dark:hover:text-neutral-50"))
                    :on-click #(rf/dispatch [:recurring/update-form-field :frequency freq])}
           (frequency-label freq)])]]

      [:div {:class "space-y-1.5"}
       [:label {:class "block text-sm font-medium text-neutral-900 dark:text-neutral-50"} "Category"]
       [:select {:class (str "w-full h-11 px-4 rounded-lg border border-neutral-200 dark:border-neutral-700 bg-white dark:bg-neutral-900 text-neutral-900 dark:text-neutral-50 "
                             "focus:outline-none focus:ring-2 focus:ring-purple-700 dark:focus:ring-purple-400 focus:border-transparent")
                 :value (name (or (:category form) :other))
                 :on-change #(rf/dispatch [:recurring/update-form-field :category (keyword (-> % .-target .-value))])}
        (for [cat categories]
          ^{:key cat}
          [:option {:value (name cat)} (str (get db/category-icons cat "📦") " " (name cat))])]]

      [:div {:class "space-y-1.5"}
       [:label {:class "block text-sm font-medium text-neutral-900 dark:text-neutral-50"} "Description"]
       [:input {:class (str "w-full h-11 px-4 rounded-lg border border-neutral-200 dark:border-neutral-700 bg-white dark:bg-neutral-900 text-neutral-900 dark:text-neutral-50 "
                            "placeholder:text-neutral-400 dark:placeholder:text-neutral-500 focus:outline-none focus:ring-2 focus:ring-purple-700 dark:focus:ring-purple-400 focus:border-transparent")
                :type "text"
                :placeholder "Description (optional)"
                :value (:description form)
                :on-change #(rf/dispatch [:recurring/update-form-field :description (-> % .-target .-value)])}]]

      [:div {:class "grid grid-cols-2 gap-3"}
       [:div {:class "space-y-1.5"}
        [:label {:class "block text-sm font-medium text-neutral-900 dark:text-neutral-50"} "Start Date"]
        [:input {:class (str "w-full h-11 px-4 rounded-lg border border-neutral-200 dark:border-neutral-700 bg-white dark:bg-neutral-900 text-neutral-900 dark:text-neutral-50 "
                             "focus:outline-none focus:ring-2 focus:ring-purple-700 dark:focus:ring-purple-400 focus:border-transparent")
                 :type "date"
                 :value (or (:start-date form) "")
                 :on-change #(rf/dispatch [:recurring/update-form-field :start-date (-> % .-target .-value)])}]]
       [:div {:class "space-y-1.5"}
        [:label {:class "block text-sm font-medium text-neutral-900 dark:text-neutral-50"} "End Date (Optional)"]
        [:input {:class (str "w-full h-11 px-4 rounded-lg border border-neutral-200 dark:border-neutral-700 bg-white dark:bg-neutral-900 text-neutral-900 dark:text-neutral-50 "
                             "focus:outline-none focus:ring-2 focus:ring-purple-700 dark:focus:ring-purple-400 focus:border-transparent")
                 :type "date"
                 :value (or (:end-date form) "")
                 :on-change #(rf/dispatch [:recurring/update-form-field :end-date (-> % .-target .-value)])}]]]

      [:div {:class "flex gap-3 pt-2"}
       [:button {:class "flex-1 h-10 rounded-lg font-medium border border-neutral-200 dark:border-neutral-700 text-neutral-900 dark:text-neutral-50 hover:bg-neutral-100 dark:hover:bg-neutral-800 transition-colors"
                 :on-click #(rf/dispatch [:recurring/reset-form])}
        "Reset"]
       [:button {:class (str "flex-1 h-10 rounded-lg font-medium text-white "
                             "bg-gradient-to-r from-purple-700 to-purple-500 "
                             "hover:shadow-lg disabled:opacity-50 disabled:cursor-not-allowed transition-all")
                 :disabled (or (not form-valid?) loading?)
                 :on-click #(rf/dispatch [:recurring/create])}
        (if loading? "Creating..." "Create Recurring")]]]]))

(defn recurring-view []
  (let [items @(rf/subscribe [:recurring/items])
        active-items @(rf/subscribe [:recurring/active-items])
        inactive-items @(rf/subscribe [:recurring/inactive-items])
        loading? @(rf/subscribe [:recurring/loading?])
        upcoming @(rf/subscribe [:recurring/upcoming])]
    [:div {:class "space-y-6"}
     [:div {:class "flex flex-col md:flex-row md:items-center justify-between gap-4 mb-6"}
      [:div
       [:h1 {:class "text-2xl font-bold text-neutral-900 dark:text-neutral-50"} "Recurring Transactions"]
       [:p {:class "text-neutral-600 dark:text-neutral-400 text-sm mt-1"} "Manage your recurring income and expenses"]]
      [:button {:class (str "inline-flex items-center gap-2 px-4 py-2 rounded-lg font-medium text-white "
                            "bg-gradient-to-r from-purple-700 to-purple-500 "
                            "hover:shadow-lg disabled:opacity-50 transition-all")
                :on-click #(rf/dispatch [:recurring/generate-now])
                :disabled loading?}
       [icon :refresh-cw {:width 16 :height 16}]
       "Generate Pending"]]

     [:div {:class "grid grid-cols-1 lg:grid-cols-3 gap-6"}
      [:div {:class "lg:col-span-2 space-y-6"}
       (when (seq upcoming)
         [:div
          [:h3 {:class "text-sm font-medium text-neutral-600 dark:text-neutral-400 uppercase tracking-wide mb-3"} "Upcoming (Next 7 Days)"]
          [:div {:class "space-y-3"}
           (for [item upcoming]
             ^{:key (:recurring/id item)}
             [recurring-card item])]])

       (when (seq active-items)
         [:div
          [:h3 {:class "text-sm font-medium text-neutral-600 dark:text-neutral-400 uppercase tracking-wide mb-3"}
           (str "Active (" (count active-items) ")")]
          [:div {:class "space-y-3"}
           (for [item active-items]
             ^{:key (:recurring/id item)}
             [recurring-card item])]])

       (when (seq inactive-items)
         [:div
          [:h3 {:class "text-sm font-medium text-neutral-400 dark:text-neutral-500 uppercase tracking-wide mb-3"}
           (str "Inactive (" (count inactive-items) ")")]
          [:div {:class "space-y-3"}
           (for [item inactive-items]
             ^{:key (:recurring/id item)}
             [recurring-card item])]])

       (when (and (empty? items) (not loading?))
         [:div {:class "text-center py-16 bg-white dark:bg-neutral-800 rounded-xl border border-neutral-200 dark:border-neutral-700"}
          [:div {:class "text-5xl mb-4"} "🔄"]
          [:h3 {:class "text-lg font-semibold text-neutral-900 dark:text-neutral-50 mb-2"} "No recurring transactions"]
          [:p {:class "text-neutral-600 dark:text-neutral-400"} "Create a recurring transaction to automate your finances"]])]

      [:div
       [recurring-form]]]]))
