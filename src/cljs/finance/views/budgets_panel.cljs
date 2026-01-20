(ns finance.views.budgets-panel
  "Slide-in panel for adding and editing budgets."
  (:require [re-frame.core :as rf]
            [clojure.string :as str]
            [finance.db :as db]
            [finance.utils.currency :as currency]
            [finance.components.icons :refer [icon]]))

(def category-colors
  {:groceries {:bg "bg-blue-100 dark:bg-blue-900/30"
               :text "text-blue-600 dark:text-blue-400"
               :icon :shopping-cart}
   :restaurants {:bg "bg-orange-100 dark:bg-orange-900/30"
                 :text "text-orange-600 dark:text-orange-400"
                 :icon :utensils}
   :transportation {:bg "bg-indigo-100 dark:bg-indigo-900/30"
                    :text "text-indigo-600 dark:text-indigo-400"
                    :icon :car}
   :utilities {:bg "bg-yellow-100 dark:bg-yellow-900/30"
               :text "text-yellow-600 dark:text-yellow-400"
               :icon :zap}
   :entertainment {:bg "bg-red-100 dark:bg-red-900/30"
                   :text "text-red-600 dark:text-red-400"
                   :icon :film}
   :healthcare {:bg "bg-teal-100 dark:bg-teal-900/30"
                :text "text-teal-600 dark:text-teal-400"
                :icon :activity}
   :shopping {:bg "bg-pink-100 dark:bg-pink-900/30"
              :text "text-pink-600 dark:text-pink-400"
              :icon :shopping-bag}
   :salary {:bg "bg-green-100 dark:bg-green-900/30"
            :text "text-green-600 dark:text-green-400"
            :icon :dollar-sign}
   :freelance {:bg "bg-purple-100 dark:bg-purple-900/30"
               :text "text-purple-600 dark:text-purple-400"
               :icon :briefcase}
   :investments {:bg "bg-emerald-100 dark:bg-emerald-900/30"
                 :text "text-emerald-600 dark:text-emerald-400"
                 :icon :trending-up}
   :gifts {:bg "bg-rose-100 dark:bg-rose-900/30"
           :text "text-rose-600 dark:text-rose-400"
           :icon :gift}
   :other {:bg "bg-gray-100 dark:bg-gray-900/30"
           :text "text-gray-600 dark:text-gray-400"
           :icon :package}})

(defn category-card [{:keys [cat-key label]} selected? on-click]
  (let [colors (get category-colors cat-key
                    {:bg "bg-gray-100 dark:bg-gray-900/30"
                     :text "text-gray-600 dark:text-gray-400"
                     :icon :package})
        {:keys [bg text icon-name]} colors
        category-icon (:icon colors)
        icon-key (or icon-name category-icon)]
    [:label {:class "cursor-pointer group"}
     [:input {:type "radio"
              :name "category"
              :class "peer sr-only"
              :checked selected?
              :on-change on-click}]
     [:div {:class (str "flex flex-col items-center justify-center p-4 rounded-xl border-2 transition-all duration-200 "
                        "group-hover:shadow-md group-hover:-translate-y-0.5 "
                        (if selected?
                          "border-violet-500 bg-violet-50 dark:bg-violet-900/20 shadow-md"
                          "border-gray-200 dark:border-gray-700 bg-white dark:bg-gray-800 hover:border-gray-300 dark:hover:border-gray-600"))}
      [:div {:class (str "h-10 w-10 rounded-full flex items-center justify-center mb-2 transition-transform group-hover:scale-110 " bg " " text)}
       [icon icon-key {:width 20 :height 20}]]
      [:span {:class (str "text-xs font-medium text-center transition-colors "
                          (if selected?
                            "text-violet-600 dark:text-violet-400"
                            "text-gray-600 dark:text-gray-400 group-hover:text-gray-900 dark:group-hover:text-gray-50"))}
       label]]]))

(defn category-grid []
  (let [selected @(rf/subscribe [:budgets/form-field :category])
        categories (map (fn [cat]
                          {:cat-key cat
                           :label (name cat)})
                        db/default-categories)]
    [:div {:class "space-y-2"}
     [:label {:class "block text-sm font-medium text-gray-700 dark:text-gray-300"} "Category"]
     [:div {:class "grid grid-cols-3 gap-3"}
      (for [{:keys [cat-key] :as cat} categories]
        ^{:key cat-key}
        [category-card cat (= cat-key selected)
         #(rf/dispatch [:budgets/update-form-field :category cat-key])])]]))

(defn budget-type-toggle []
  (let [selected @(rf/subscribe [:budgets/form-field :budget-type])
        types [{:key :fixed :label "Fixed" :icon :lock}
               {:key :variable :label "Variable" :icon :trending-up}]]
    [:div {:class "space-y-2"}
     [:label {:class "block text-sm font-medium text-gray-700 dark:text-gray-300"} "Budget Type"]
     [:div {:class "grid grid-cols-2 gap-3"}
      (for [{:keys [key label icon-name]} types]
        ^{:key key}
        [:label {:class "cursor-pointer group"}
         [:input {:type "radio"
                  :name "budget-type"
                  :class "peer sr-only"
                  :checked (= key selected)
                  :on-change #(rf/dispatch [:budgets/update-form-field :budget-type key])}]
         [:div {:class (str "flex items-center justify-center gap-2 rounded-xl border-2 py-3 text-sm font-medium "
                            "transition-all duration-200 group-hover:shadow-sm "
                            (if (= key selected)
                              "border-violet-500 bg-violet-500 text-white shadow-md"
                              "border-gray-200 dark:border-gray-700 bg-white dark:bg-gray-800 text-gray-600 dark:text-gray-400 hover:border-violet-300 dark:hover:border-violet-700 hover:bg-violet-50 dark:hover:bg-violet-900/10"))}
          [icon icon-name {:width 14 :height 14}]
          [:span label]]])]]))

(defn currency-amount-row []
  (let [amount @(rf/subscribe [:budgets/form-field :amount])
        curr @(rf/subscribe [:budgets/form-field :currency])
        currencies [:COP :USD]]
    [:div {:class "grid grid-cols-2 gap-4"}
     [:div {:class "space-y-2"}
      [:label {:class "block text-sm font-medium text-gray-700 dark:text-gray-300"} "Currency"]
      [:div {:class "relative"}
       [:select {:class (str "block w-full pl-3 pr-8 py-3 border border-gray-200 dark:border-gray-700 rounded-xl "
                             "bg-gray-50 dark:bg-gray-800 text-gray-900 dark:text-gray-50 "
                             "appearance-none focus:outline-none focus:ring-2 focus:ring-violet-500 focus:border-transparent "
                             "transition-all sm:text-sm shadow-sm")
                 :value (name (or curr :COP))
                 :on-change #(rf/dispatch [:budgets/update-form-field :currency (keyword (-> % .-target .-value))])}
        (for [c currencies]
          ^{:key c}
          [:option {:value (name c)} (name c)])]
       [:div {:class "absolute inset-y-0 right-0 pr-2 flex items-center pointer-events-none"}
        [icon :chevron-down {:width 16 :height 16 :class "text-gray-400"}]]]]
     [:div {:class "space-y-2"}
      [:label {:class "block text-sm font-medium text-gray-700 dark:text-gray-300"} "Budget Amount"]
      [:div {:class "relative"}
       [:div {:class "absolute inset-y-0 left-0 pl-4 flex items-center pointer-events-none"}
        [:span {:class "text-gray-400 font-semibold"} "$"]]
       [:input {:class (str "block w-full pl-10 pr-3 py-3 border border-gray-200 dark:border-gray-700 rounded-xl "
                            "bg-gray-50 dark:bg-gray-800 text-gray-900 dark:text-gray-50 placeholder-gray-400 "
                            "focus:outline-none focus:ring-2 focus:ring-violet-500 focus:border-transparent "
                            "transition-all sm:text-sm shadow-sm font-semibold")
                :type "number"
                :step "0.01"
                :placeholder "0.00"
                :value (or amount "")
                :on-change #(rf/dispatch [:budgets/update-form-field :amount (-> % .-target .-value)])}]]]]))

(defn alert-threshold-slider []
  (let [threshold @(rf/subscribe [:budgets/form-field :alert-threshold])]
    [:div {:class "space-y-2"}
     [:div {:class "flex items-center justify-between"}
      [:label {:class "block text-sm font-medium text-gray-700 dark:text-gray-300"} "Alert Threshold"]
      [:span {:class "text-sm font-semibold text-violet-600 dark:text-violet-400 px-2.5 py-0.5 bg-violet-50 dark:bg-violet-900/20 rounded-lg"}
       (str threshold "%")]]
     [:input {:class (str "w-full h-2.5 bg-gray-200 dark:bg-gray-700 rounded-lg appearance-none cursor-pointer "
                          "accent-violet-500 transition-all")
              :type "range"
              :min "0"
              :max "100"
              :step "5"
              :value (or threshold 80)
              :on-change #(rf/dispatch [:budgets/update-form-field :alert-threshold (js/parseInt (-> % .-target .-value))])}]
     [:p {:class "text-xs text-gray-500 dark:text-gray-400"}
      "You'll be alerted when spending reaches this percentage"]]))

(defn notes-textarea []
  (let [notes @(rf/subscribe [:budgets/form-field :notes])]
    [:div {:class "space-y-2"}
     [:label {:class "block text-sm font-medium text-gray-700 dark:text-gray-300"} "Notes (Optional)"]
     [:textarea {:class (str "block w-full px-3 py-3 border border-gray-200 dark:border-gray-700 rounded-xl "
                             "bg-gray-50 dark:bg-gray-800 text-gray-900 dark:text-gray-50 placeholder-gray-400 "
                             "focus:outline-none focus:ring-2 focus:ring-violet-500 focus:border-transparent "
                             "transition-all sm:text-sm shadow-sm resize-none")
                 :rows 3
                 :placeholder "Add any notes about this budget..."
                 :value (or notes "")
                 :on-change #(rf/dispatch [:budgets/update-form-field :notes (-> % .-target .-value)])}]]))

(defn budget-preview []
  (let [amount @(rf/subscribe [:budgets/form-field :amount])
        currency @(rf/subscribe [:budgets/form-field :currency])
        category @(rf/subscribe [:budgets/form-field :category])
        total-budgeted @(rf/subscribe [:budgets/total-budgeted])
        parsed-amount (if (and amount (not (str/blank? amount)))
                        (js/parseFloat amount)
                        0)
        monthly-impact (* parsed-amount 12)
        percentage (if (pos? total-budgeted)
                     (* 100 (/ parsed-amount total-budgeted))
                     0)
        cat-label (if category (name category) "selected category")]
    [:div {:class "mt-6 pt-5 border-t border-gray-200 dark:border-gray-700"}
     [:h3 {:class "text-sm font-semibold text-gray-900 dark:text-gray-50 mb-3 flex items-center"}
      [icon :bar-chart {:width 16 :height 16 :class "text-violet-500 mr-2"}]
      "Budget Preview"]
     [:div {:class "bg-gradient-to-br from-violet-50 to-purple-50 dark:from-violet-900/20 dark:to-purple-900/20 rounded-xl p-4 border border-violet-200 dark:border-violet-800 space-y-4"}
      [:div {:class "flex justify-between items-end"}
       [:div
        [:p {:class "text-xs text-gray-500 dark:text-gray-400 mb-1 uppercase tracking-wide font-semibold"} "Annual Budget"]
        [:p {:class "text-2xl font-bold text-violet-600 dark:text-violet-400"}
         (currency/format-currency monthly-impact (or currency :COP))]]
       [:div {:class "text-right"}
        [:span {:class "inline-flex items-center px-2 py-1 rounded-lg bg-white/80 dark:bg-gray-800/80 backdrop-blur-sm text-xs font-medium text-gray-600 dark:text-gray-400 shadow-sm border border-gray-200 dark:border-gray-700"}
         "12 months"]]]
      [:div
       [:div {:class "flex justify-between text-xs mb-2"}
        [:span {:class "text-gray-600 dark:text-gray-400"} "% of Total Budget"]
        [:span {:class "font-medium text-gray-900 dark:text-gray-50"} (str (.toFixed percentage 1) "%")]]
       [:div {:class "h-2 w-full bg-gray-200 dark:bg-gray-700 rounded-full overflow-hidden"}
        [:div {:class "h-full bg-gradient-to-r from-violet-500 to-purple-500 transition-all duration-500"
               :style {:width (str (min 100 percentage) "%")}}]]
       [:div {:class "mt-3 flex items-start gap-2 text-xs text-gray-600 dark:text-gray-400 bg-white/50 dark:bg-gray-800/50 backdrop-blur-sm p-2.5 rounded-lg border border-gray-200 dark:border-gray-700"}
        [icon :info {:width 14 :height 14 :class "text-violet-500 mt-0.5 flex-shrink-0"}]
        [:span (str "This budget will track spending in the ")
         [:strong cat-label]
         " category."]]]]]))

(defn budgets-panel []
  (let [is-open? @(rf/subscribe [:budgets/panel-open?])
        mode @(rf/subscribe [:budgets/panel-mode])
        form-valid? @(rf/subscribe [:budgets/form-valid?])
        loading? @(rf/subscribe [:budgets/loading?])
        selected-budget-id @(rf/subscribe [:budgets/selected-budget-id])]
    [:<>
     (when is-open?
       [:div {:class "fixed inset-0 bg-gray-900/40 backdrop-blur-sm z-40 transition-opacity"
              :on-click #(rf/dispatch [:budgets/close-panel])}])
     [:div {:class (str "fixed top-0 right-0 h-full w-full max-w-md bg-white dark:bg-gray-800 shadow-2xl z-50 "
                        "flex flex-col overflow-hidden transform transition-transform duration-300 ease-out "
                        (if is-open? "translate-x-0" "translate-x-full"))}
      [:div {:class "flex-shrink-0 px-6 py-4 border-b border-gray-200 dark:border-gray-700 flex items-center justify-between"}
       [:h2 {:class "text-xl font-bold text-gray-900 dark:text-gray-50"}
        (if (= mode :edit) "Edit Budget" "New Budget")]
       [:button {:class "text-gray-500 dark:text-gray-400 hover:text-gray-900 dark:hover:text-gray-50 transition-colors focus:outline-none"
                 :on-click #(rf/dispatch [:budgets/close-panel])}
        [icon :x {:width 24 :height 24}]]]

      [:div {:class "flex-1 overflow-y-auto px-6 pt-6 pb-6"}
       [:form {:class "space-y-4"}
        [category-grid]
        [budget-type-toggle]
        [currency-amount-row]
        [alert-threshold-slider]
        [notes-textarea]
        [budget-preview]]]

      [:div {:class "flex-shrink-0 px-6 py-4 border-t border-gray-200 dark:border-gray-700 bg-gray-50 dark:bg-gray-800/50"}
       [:div {:class "flex gap-3"}
        [:button {:class (str "flex-1 py-3 px-4 rounded-xl border border-gray-200 dark:border-gray-700 "
                              "text-gray-500 dark:text-gray-400 font-medium "
                              "hover:bg-white dark:hover:bg-gray-700 transition-colors shadow-sm")
                  :type "button"
                  :on-click #(rf/dispatch [:budgets/close-panel])}
         "Cancel"]
        [:button {:class (str "flex-1 py-3 px-4 rounded-xl bg-violet-500 hover:bg-violet-600 text-white font-medium "
                              "shadow-lg shadow-violet-500/30 transition-all "
                              "disabled:opacity-50 disabled:cursor-not-allowed")
                  :type "button"
                  :disabled (or (not form-valid?) loading?)
                  :on-click #(if (= mode :edit)
                               (rf/dispatch [:budgets/update selected-budget-id
                                             (let [form @(rf/subscribe [:budgets/form])]
                                               {:category (name (:category form))
                                                :amount (js/parseFloat (:amount form))
                                                :currency (name (:currency form))
                                                :alert-threshold (/ (:alert-threshold form) 100)
                                                :budget-type (name (:budget-type form))
                                                :notes (:notes form)})])
                               (rf/dispatch [:budgets/create]))}
         (cond
           loading? (if (= mode :edit) "Updating..." "Creating...")
           (= mode :edit) "Update Budget"
           :else "Create Budget")]]]]]))
