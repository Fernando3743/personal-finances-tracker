(ns finance.views.budgets
  "Budgets page view."
  (:require [re-frame.core :as rf]
            [finance.db :as db]
            [finance.utils.currency :as currency]
            [finance.components.icons :refer [icon]]))

(defn progress-bar [{:keys [percentage status]}]
  (let [clamped (min 100 (max 0 percentage))
        bar-color (case status
                    :exceeded "bg-red-500"
                    :warning "bg-amber-500"
                    "bg-green-500")]
    [:div {:class "h-2 bg-neutral-100 dark:bg-neutral-800 rounded-full overflow-hidden"}
     [:div {:class (str "h-full rounded-full transition-all duration-300 " bar-color)
            :style {:width (str clamped "%")}}]]))

(defn budget-card [{:keys [budget spent remaining percentage status]}]
  (let [cat (:budget/category budget)
        amount (:budget/amount budget)
        curr (:budget/currency budget)
        cat-icon (get db/category-icons cat "📦")
        border-class (case status
                       :exceeded "border-red-500"
                       :warning "border-amber-500"
                       "border-neutral-200 dark:border-neutral-700")]
    [:div {:class (str "relative bg-white dark:bg-neutral-800 rounded-xl border p-4 " border-class)}
     [:div {:class "flex items-center justify-between mb-3"}
      [:div {:class "flex items-center gap-2"}
       [:span {:class "text-xl"} cat-icon]
       [:span {:class "font-medium text-neutral-900 dark:text-neutral-50"} (name cat)]]
      [:div
       (case status
         :exceeded [:span {:class "px-2 py-0.5 rounded-full text-xs font-medium bg-red-100 dark:bg-red-900/20 text-red-600 dark:text-red-500"} "Exceeded"]
         :warning [:span {:class "px-2 py-0.5 rounded-full text-xs font-medium bg-amber-100 dark:bg-amber-900/20 text-amber-500"} "Warning"]
         [:span {:class "px-2 py-0.5 rounded-full text-xs font-medium bg-green-100 dark:bg-green-900/20 text-green-600 dark:text-green-500"} "On Track"])]]
     [:div {:class "flex items-baseline gap-2 mb-3"}
      [:div {:class "flex-1"}
       [:span {:class "text-xs text-neutral-400 dark:text-neutral-500"} "Spent"]
       [:div {:class "text-lg font-semibold text-neutral-900 dark:text-neutral-50"} (currency/format-currency (or spent 0) curr)]]
      [:span {:class "text-neutral-400 dark:text-neutral-500"} "of"]
      [:div {:class "flex-1 text-right"}
       [:span {:class "text-xs text-neutral-400 dark:text-neutral-500"} "Budget"]
       [:div {:class "text-lg font-semibold text-neutral-900 dark:text-neutral-50"} (currency/format-currency amount curr)]]]
     [progress-bar {:percentage (* 100 (or percentage 0)) :status status}]
     [:div {:class "flex items-center justify-between mt-3 text-sm"}
      [:span {:class (str "font-medium " (if (neg? remaining) "text-red-600 dark:text-red-500" "text-neutral-600 dark:text-neutral-400"))}
       (if (neg? remaining)
         (str "Over by " (currency/format-currency (abs remaining) curr))
         (str (currency/format-currency remaining curr) " remaining"))]
      [:span {:class "text-neutral-400 dark:text-neutral-500"}
       (str (.toFixed (* 100 (or percentage 0)) 0) "%")]]
     [:button {:class "absolute top-4 right-4 p-1.5 rounded-lg text-neutral-400 dark:text-neutral-500 hover:text-red-500 hover:bg-red-100 dark:hover:bg-red-900/20 transition-colors"
               :on-click #(when (js/confirm "Delete this budget?")
                            (rf/dispatch [:budgets/delete (:budget/id budget)]))}
      [icon :trash {:width 16 :height 16}]]]))

(defn budget-form []
  (let [form @(rf/subscribe [:budgets/form])
        form-valid? @(rf/subscribe [:budgets/form-valid?])
        loading? @(rf/subscribe [:budgets/loading?])
        categories @(rf/subscribe [:tx/categories])]
    [:div {:class "bg-white dark:bg-neutral-800 rounded-xl border border-neutral-200 dark:border-neutral-700 p-5"}
     [:h3 {:class "text-lg font-semibold text-neutral-900 dark:text-neutral-50 mb-4"} "Add Budget"]

     [:div {:class "space-y-4"}
      [:div {:class "space-y-1.5"}
       [:label {:class "block text-sm font-medium text-neutral-900 dark:text-neutral-50"} "Category"]
       [:select {:class (str "w-full h-11 px-4 rounded-lg border border-neutral-200 dark:border-neutral-700 bg-white dark:bg-neutral-900 text-neutral-900 dark:text-neutral-50 "
                             "focus:outline-none focus:ring-2 focus:ring-purple-700 dark:focus:ring-purple-400 focus:border-transparent")
                 :value (if (:category form) (name (:category form)) "")
                 :on-change #(rf/dispatch [:budgets/update-form-field :category (keyword (-> % .-target .-value))])}
        [:option {:value ""} "Select a category..."]
        (for [cat categories]
          ^{:key cat}
          [:option {:value (name cat)} (str (get db/category-icons cat "📦") " " (name cat))])]]

      [:div {:class "grid grid-cols-2 gap-3"}
       [:div {:class "space-y-1.5"}
        [:label {:class "block text-sm font-medium text-neutral-900 dark:text-neutral-50"} "Budget Amount"]
        [:input {:class (str "w-full h-11 px-4 rounded-lg border border-neutral-200 dark:border-neutral-700 bg-white dark:bg-neutral-900 text-neutral-900 dark:text-neutral-50 "
                             "placeholder:text-neutral-400 dark:placeholder:text-neutral-500 focus:outline-none focus:ring-2 focus:ring-purple-700 dark:focus:ring-purple-400 focus:border-transparent")
                 :type "number"
                 :placeholder "0.00"
                 :value (:amount form)
                 :on-change #(rf/dispatch [:budgets/update-form-field :amount (-> % .-target .-value)])}]]
       [:div {:class "space-y-1.5"}
        [:label {:class "block text-sm font-medium text-neutral-900 dark:text-neutral-50"} "Currency"]
        [:div {:class "flex gap-1 p-1 bg-neutral-100 dark:bg-neutral-800 rounded-lg h-11"}
         [:button {:class (str "flex-1 rounded-md text-sm font-medium transition-colors "
                               (if (= :COP (:currency form))
                                 "bg-white dark:bg-neutral-800 text-neutral-900 dark:text-neutral-50 shadow-sm"
                                 "text-neutral-600 dark:text-neutral-400 hover:text-neutral-900 dark:hover:text-neutral-50"))
                   :on-click #(rf/dispatch [:budgets/update-form-field :currency :COP])}
          "COP"]
         [:button {:class (str "flex-1 rounded-md text-sm font-medium transition-colors "
                               (if (= :USD (:currency form))
                                 "bg-white dark:bg-neutral-800 text-neutral-900 dark:text-neutral-50 shadow-sm"
                                 "text-neutral-600 dark:text-neutral-400 hover:text-neutral-900 dark:hover:text-neutral-50"))
                   :on-click #(rf/dispatch [:budgets/update-form-field :currency :USD])}
          "USD"]]]]

      [:div {:class "space-y-1.5"}
       [:label {:class "block text-sm font-medium text-neutral-900 dark:text-neutral-50"} "Alert Threshold (%)"]
       [:input {:class (str "w-full h-11 px-4 rounded-lg border border-neutral-200 dark:border-neutral-700 bg-white dark:bg-neutral-900 text-neutral-900 dark:text-neutral-50 "
                            "placeholder:text-neutral-400 dark:placeholder:text-neutral-500 focus:outline-none focus:ring-2 focus:ring-purple-700 dark:focus:ring-purple-400 focus:border-transparent")
                :type "number"
                :min "0"
                :max "100"
                :placeholder "80"
                :value (:alert-threshold form)
                :on-change #(rf/dispatch [:budgets/update-form-field :alert-threshold (js/parseInt (-> % .-target .-value))])}]
       [:span {:class "text-xs text-neutral-400 dark:text-neutral-500"} "Alert when spending reaches this percentage"]]

      [:div {:class "flex gap-3 pt-2"}
       [:button {:class "flex-1 h-10 rounded-lg font-medium border border-neutral-200 dark:border-neutral-700 text-neutral-900 dark:text-neutral-50 hover:bg-neutral-100 dark:hover:bg-neutral-800 transition-colors"
                 :on-click #(rf/dispatch [:budgets/reset-form])}
        "Reset"]
       [:button {:class (str "flex-1 h-10 rounded-lg font-medium text-white "
                             "bg-gradient-to-r from-purple-700 to-purple-500 "
                             "hover:shadow-lg disabled:opacity-50 disabled:cursor-not-allowed transition-all")
                 :disabled (or (not form-valid?) loading?)
                 :on-click #(rf/dispatch [:budgets/create])}
        (if loading? "Creating..." "Create Budget")]]]]))

(defn budget-overview []
  (let [status @(rf/subscribe [:budgets/status])
        total-budgeted @(rf/subscribe [:budgets/total-budgeted])
        total-spent @(rf/subscribe [:budgets/total-spent])
        exceeded @(rf/subscribe [:budgets/exceeded])
        warning @(rf/subscribe [:budgets/warning])
        now (js/Date.)
        month (or (:month status) (inc (.getMonth now)))
        year (or (:year status) (.getFullYear now))]
    [:div {:class "bg-white dark:bg-neutral-800 rounded-xl border border-neutral-200 dark:border-neutral-700 p-5 mb-6"}
     [:div {:class "flex items-center gap-2 mb-4"}
      [:span {:class "text-lg font-semibold text-neutral-900 dark:text-neutral-50"} (str month "/" year)]]
     [:div {:class "grid grid-cols-2 md:grid-cols-4 gap-4"}
      [:div
       [:span {:class "text-xs text-neutral-400 dark:text-neutral-500"} "Total Budgeted"]
       [:div {:class "text-lg font-semibold text-neutral-900 dark:text-neutral-50"} (currency/format-currency total-budgeted :COP)]]
      [:div
       [:span {:class "text-xs text-neutral-400 dark:text-neutral-500"} "Total Spent"]
       [:div {:class "text-lg font-semibold text-neutral-900 dark:text-neutral-50"} (currency/format-currency total-spent :COP)]]
      (when (pos? exceeded)
        [:div
         [:span {:class "text-xs text-red-600 dark:text-red-500"} "Exceeded"]
         [:div {:class "text-lg font-semibold text-red-600 dark:text-red-500"} (str exceeded " budget(s)")]])
      (when (pos? warning)
        [:div
         [:span {:class "text-xs text-amber-500"} "Warning"]
         [:div {:class "text-lg font-semibold text-amber-500"} (str warning " budget(s)")]])]]))

(defn budgets-view []
  (let [summaries @(rf/subscribe [:budgets/budget-summaries])
        all-budgets @(rf/subscribe [:budgets/all-budgets])
        loading? @(rf/subscribe [:budgets/loading?])]
    [:div {:class "space-y-6"}
     [:div {:class "mb-6"}
      [:h1 {:class "text-2xl font-bold text-neutral-900 dark:text-neutral-50"} "Budgets"]
      [:p {:class "text-neutral-600 dark:text-neutral-400 text-sm mt-1"} "Set and track your spending limits"]]

     [budget-overview]

     [:div {:class "grid grid-cols-1 lg:grid-cols-3 gap-6"}
      [:div {:class "lg:col-span-2"}
       (cond
         loading?
         [:div {:class "space-y-4"}
          (for [i (range 4)]
            ^{:key i}
            [:div {:class "flex items-center gap-4 p-4 bg-neutral-100 dark:bg-neutral-800 rounded-lg animate-pulse"}
             [:div {:class "w-10 h-10 rounded-full bg-neutral-200 dark:bg-neutral-700"}]
             [:div {:class "flex-1 space-y-2"}
              [:div {:class "h-4 bg-neutral-200 dark:bg-neutral-700 rounded w-3/5"}]
              [:div {:class "h-3 bg-neutral-200 dark:bg-neutral-700 rounded w-2/5"}]]])]

         (empty? all-budgets)
         [:div {:class "text-center py-16 bg-white dark:bg-neutral-800 rounded-xl border border-neutral-200 dark:border-neutral-700"}
          [:div {:class "text-5xl mb-4"} "💵"]
          [:h3 {:class "text-lg font-semibold text-neutral-900 dark:text-neutral-50 mb-2"} "No budgets set"]
          [:p {:class "text-neutral-600 dark:text-neutral-400"} "Create budgets to track your spending by category"]]

         :else
         [:div {:class "grid grid-cols-1 md:grid-cols-2 gap-4"}
          (for [summary summaries]
            ^{:key (get-in summary [:budget :budget/id])}
            [budget-card summary])
          (for [budget all-budgets]
            (when-not (some #(= (get-in % [:budget :budget/id]) (:budget/id budget)) summaries)
              ^{:key (:budget/id budget)}
              [budget-card {:budget budget :spent 0 :remaining (:budget/amount budget) :percentage 0 :status :ok}]))])]

      [:div
       [budget-form]]]]))
