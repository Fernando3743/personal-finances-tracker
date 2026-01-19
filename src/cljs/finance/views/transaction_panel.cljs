(ns finance.views.transaction-panel
  "Slide-in panel for adding transactions."
  (:require [re-frame.core :as rf]
            [clojure.string :as str]
            [finance.db :as db]
            [finance.utils.currency :as currency]
            [finance.components.icons :refer [icon]]
            [finance.utils.validation :as validation]
            [finance.constants :as const]))

(defn preview-card []
  (let [preview @(rf/subscribe [:tx/form-preview])
        {:keys [amount type category description currency date]} preview
        cat-icon (get db/category-icons category "📦")
        is-income? (= type :income)
        display-amount (if is-income? amount (- amount))]
    [:div {:class "rounded-2xl bg-gradient-to-br from-violet-500 via-purple-600 to-indigo-700 p-6 mb-8 text-white shadow-lg relative overflow-hidden"}
     [:div {:class "absolute -right-10 -top-10 h-32 w-32 rounded-full bg-white/10 blur-2xl"}]
     [:div {:class "absolute -bottom-10 -left-10 h-24 w-24 rounded-full bg-white/10 blur-xl"}]
     [:div {:class "relative z-10"}
      [:p {:class "text-xs font-medium uppercase tracking-wider text-purple-200 mb-1"} "Preview"]
      [:div {:class "flex items-start justify-between"}
       [:div
        [:h3 {:class "text-3xl font-bold tracking-tight mb-0.5"}
         (currency/format-currency display-amount currency {:show-sign? true})]
        [:p {:class "text-purple-100 font-medium opacity-90 text-lg"}
         (if (str/blank? description) "Transaction Name" description)]]
       [:div {:class "h-10 w-10 rounded-xl bg-white/20 backdrop-blur-sm flex items-center justify-center shadow-inner border border-white/10"}
        [:span {:class "text-xl"} cat-icon]]]
      [:div {:class "mt-6 flex items-center justify-between text-sm text-purple-100"}
       [:div {:class "flex items-center bg-black/20 px-3 py-1 rounded-full backdrop-blur-md"}
        [icon :calendar {:width 14 :height 14 :class "mr-1.5"}]
        [:span (or date "Today")]]
       [:div {:class "flex items-center"}
        [:span {:class "h-2 w-2 rounded-full bg-green-400 mr-2"}]
        [:span (str/capitalize (name category))]]]]]))

(defn type-toggle []
  (let [current-type @(rf/subscribe [:tx/form-field :type])]
    [:div {:class "p-1.5 rounded-xl bg-gray-100 dark:bg-gray-800/80 flex border border-gray-200 dark:border-gray-700 mb-6"}
     [:button {:class (str "flex-1 py-2.5 text-sm font-semibold text-center rounded-lg transition-all duration-200 "
                           (if (= current-type :expense)
                             "bg-white dark:bg-gray-700 text-violet-600 shadow-sm"
                             "text-gray-500 dark:text-gray-400"))
               :on-click #(rf/dispatch [:tx/update-form-field :type :expense])}
      "Expense"]
     [:button {:class (str "flex-1 py-2.5 text-sm font-semibold text-center rounded-lg transition-all duration-200 "
                           (if (= current-type :income)
                             "bg-white dark:bg-gray-700 text-violet-600 shadow-sm"
                             "text-gray-500 dark:text-gray-400"))
               :on-click #(rf/dispatch [:tx/update-form-field :type :income])}
      "Income"]]))

(defn name-input []
  (let [description @(rf/subscribe [:tx/form-field :description])]
    [:div {:class "space-y-2"}
     [:label {:class "block text-sm font-medium text-gray-500 dark:text-gray-400"} "Transaction Name"]
     [:div {:class "relative group"}
      [icon :file-text {:width 18 :height 18 :class "absolute left-3 top-1/2 -translate-y-1/2 text-gray-400 group-focus-within:text-violet-500 transition-colors"}]
      [:input {:class (str "w-full pl-10 pr-4 py-3 rounded-xl border border-gray-200 dark:border-gray-700 bg-gray-50 dark:bg-gray-800 text-gray-900 dark:text-gray-50 "
                           "placeholder:text-gray-400 focus:outline-none focus:ring-2 focus:ring-violet-500 focus:border-transparent shadow-sm "
                           "transition-all sm:text-sm")
               :type "text"
               :placeholder "e.g., Starbucks"
               :value (or description "")
               :on-change #(rf/dispatch [:tx/update-form-field :description (-> % .-target .-value)])}]]]))

(defn currency-amount-row []
  (let [amount @(rf/subscribe [:tx/form-field :amount])
        currency @(rf/subscribe [:tx/form-currency])
        currencies @(rf/subscribe [:dashboard/available-currencies])]
    [:div {:class "grid grid-cols-2 gap-4"}
     [:div {:class "space-y-2"}
      [:label {:class "block text-sm font-medium text-gray-500 dark:text-gray-400"} "Currency"]
      [:div {:class "relative"}
       [:select {:class (str "w-full pl-3 pr-8 py-3 rounded-xl border border-gray-200 dark:border-gray-700 bg-gray-50 dark:bg-gray-800 text-gray-900 dark:text-gray-50 "
                             "appearance-none focus:outline-none focus:ring-2 focus:ring-violet-500 focus:border-transparent shadow-sm "
                             "transition-all sm:text-sm")
                 :value (name currency)
                 :on-change #(rf/dispatch [:tx/update-form-field :currency (keyword (-> % .-target .-value))])}
        (for [c currencies]
          ^{:key c}
          [:option {:value (name c)} (name c)])]
       [icon :chevron-down {:width 16 :height 16 :class "absolute right-2 top-1/2 -translate-y-1/2 text-gray-400 pointer-events-none"}]]]
     [:div {:class "space-y-2"}
      [:label {:class "block text-sm font-medium text-gray-500 dark:text-gray-400"} "Amount"]
      [:div {:class "relative group"}
       [:span {:class "absolute left-3 top-1/2 -translate-y-1/2 text-gray-400 font-bold group-focus-within:text-violet-500 transition-colors"}
        (currency/currency-symbol currency)]
       [:input {:class (str "w-full pl-8 pr-4 py-3 rounded-xl border border-gray-200 dark:border-gray-700 bg-gray-50 dark:bg-gray-800 text-gray-900 dark:text-gray-50 "
                            "placeholder:text-gray-400 focus:outline-none focus:ring-2 focus:ring-violet-500 focus:border-transparent shadow-sm "
                            "transition-all sm:text-sm font-bold text-lg")
                :type "number"
                :step "0.01"
                :placeholder "0.00"
                :value (or amount "")
                :on-change #(let [val (-> % .-target .-value)
                                  cleaned (validation/clean-amount val)]
                              (rf/dispatch [:tx/update-form-field :amount cleaned]))}]]]]))


(defn category-dropdown []
  (let [categories @(rf/subscribe [:tx/categories])
        selected @(rf/subscribe [:tx/form-field :category])
        selected-icon (get db/category-icons selected "📦")
        colors (const/get-category-colors selected)]
    [:div {:class "space-y-2 relative"}
     [:label {:class "block text-sm font-medium text-gray-500 dark:text-gray-400"} "Category"]
     [:button {:class (str "w-full flex items-center justify-between p-3 border border-gray-200 dark:border-gray-700 rounded-xl bg-gray-50 dark:bg-gray-800 text-left "
                           "focus:outline-none focus:ring-2 focus:ring-violet-500 focus:border-transparent shadow-sm group transition-all")
               :type "button"}
      [:span {:class "flex items-center"}
       [:span {:class (str "h-8 w-8 rounded-lg flex items-center justify-center mr-3 group-hover:scale-110 transition-transform " (:bg colors) " " (:text colors))}
        [:span {:class "text-sm"} selected-icon]]
       [:span {:class "font-medium text-gray-900 dark:text-gray-50"} (str/capitalize (name selected))]]
      [icon :chevron-down {:width 16 :height 16 :class "text-gray-400"}]]
     [:select {:class "absolute inset-0 w-full h-full opacity-0 cursor-pointer"
               :value (name selected)
               :on-change #(rf/dispatch [:tx/update-form-field :category (keyword (-> % .-target .-value))])}
      (for [cat categories]
        ^{:key cat}
        [:option {:value (name cat)} (str/capitalize (name cat))])]]))

(defn date-picker []
  (let [date @(rf/subscribe [:tx/form-field :date])
        today (.toISOString (js/Date.))]
    [:div {:class "space-y-2"}
     [:label {:class "block text-sm font-medium text-gray-500 dark:text-gray-400"} "Date"]
     [:div {:class "relative group"}
      [icon :calendar {:width 18 :height 18 :class "absolute left-3 top-1/2 -translate-y-1/2 text-gray-400 group-focus-within:text-violet-500 transition-colors"}]
      [:input {:class (str "w-full pl-10 pr-4 py-3 rounded-xl border border-gray-200 dark:border-gray-700 bg-gray-50 dark:bg-gray-800 text-gray-900 dark:text-gray-50 "
                           "focus:outline-none focus:ring-2 focus:ring-violet-500 focus:border-transparent shadow-sm "
                           "transition-all sm:text-sm")
               :type "date"
               :value (or date (subs today 0 10))
               :on-change #(rf/dispatch [:tx/update-form-field :date (-> % .-target .-value)])}]]]))

(defn wallet-dropdown []
  [:div {:class "space-y-2"}
   [:label {:class "block text-sm font-medium text-gray-500 dark:text-gray-400"} "Source Account / Wallet"]
   [:div {:class "relative"}
    [icon :wallet {:width 18 :height 18 :class "absolute left-3 top-1/2 -translate-y-1/2 text-gray-400"}]
    [:select {:class (str "w-full pl-10 pr-10 py-3 rounded-xl border border-gray-200 dark:border-gray-700 bg-gray-50 dark:bg-gray-800 text-gray-900 dark:text-gray-50 "
                          "appearance-none focus:outline-none focus:ring-2 focus:ring-violet-500 focus:border-transparent shadow-sm "
                          "transition-all sm:text-sm")}
     [:option "Main Checking (**** 4532)"]
     [:option "Savings Account (**** 8841)"]
     [:option "Credit Card (**** 9921)"]
     [:option "Cash Wallet"]]
    [icon :chevron-down {:width 16 :height 16 :class "absolute right-3 top-1/2 -translate-y-1/2 text-gray-400 pointer-events-none"}]]])

(defn recurring-toggle []
  (let [is-recurring @(rf/subscribe [:tx/form-is-recurring])]
    [:div {:class "flex items-center justify-between py-2"}
     [:div {:class "flex flex-col"}
      [:span {:class "text-sm font-medium text-gray-900 dark:text-gray-50"} "Recurring Transaction"]
      [:span {:class "text-xs text-gray-500 dark:text-gray-400"} "Repeat this monthly"]]
     [:label {:class "relative inline-flex items-center cursor-pointer"}
      [:input {:type "checkbox"
               :class "sr-only peer"
               :checked is-recurring
               :on-change #(rf/dispatch [:tx/update-form-field :is-recurring (not is-recurring)])}]
      [:div {:class (str "w-11 h-6 bg-gray-200 peer-focus:outline-none "
                         "rounded-full peer dark:bg-gray-700 peer-checked:after:translate-x-full peer-checked:after:border-white "
                         "after:content-[''] after:absolute after:top-[2px] after:left-[2px] after:bg-white after:border-gray-300 "
                         "after:border after:rounded-full after:h-5 after:w-5 after:transition-all dark:border-gray-600 peer-checked:bg-violet-500")}]]]))

(defn notes-input []
  (let [notes @(rf/subscribe [:tx/form-notes])]
    [:div {:class "space-y-2"}
     [:label {:class "block text-sm font-medium text-gray-500 dark:text-gray-400"} "Notes / Description"]
     [:textarea {:class (str "w-full p-3 rounded-xl border border-gray-200 dark:border-gray-700 bg-gray-50 dark:bg-gray-800 text-gray-900 dark:text-gray-50 "
                             "placeholder:text-gray-400 focus:outline-none focus:ring-2 focus:ring-violet-500 focus:border-transparent shadow-sm "
                             "transition-all sm:text-sm resize-none")
                 :placeholder "Add any details here..."
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
       [:div {:class "fixed inset-0 bg-black/50 backdrop-blur-sm z-40 transition-opacity"
              :on-click #(rf/dispatch [:app/close-panel])}])
     [:div {:class (str "fixed top-0 right-0 h-full w-full max-w-md bg-white dark:bg-neutral-800 shadow-xl z-50 "
                        "flex flex-col overflow-hidden transform transition-transform duration-300 ease-out "
                        (if is-open? "translate-x-0" "translate-x-full"))}
      [:div {:class "flex-shrink-0 flex items-center justify-between px-6 py-4 border-b border-gray-200 dark:border-gray-700"}
       [:h2 {:class "text-xl font-bold text-gray-900 dark:text-gray-50"} "New Transaction"]
       [:button {:class "p-2 rounded-lg text-gray-500 dark:text-gray-400 hover:text-gray-900 dark:hover:text-gray-50 hover:bg-gray-100 dark:hover:bg-gray-700 transition-colors"
                 :on-click #(rf/dispatch [:app/close-panel])}
        [icon :x {:width 20 :height 20}]]]

      [:div {:class "flex-1 min-h-0 overflow-y-auto p-6"}
       [preview-card]
       [type-toggle]
       [:div {:class "space-y-6"}
        [name-input]
        [currency-amount-row]
        [category-dropdown]
        [date-picker]
        [wallet-dropdown]
        [recurring-toggle]
        [notes-input]]]

      [:div {:class "flex-shrink-0 flex gap-3 px-6 py-4 border-t border-gray-200 dark:border-gray-700 bg-gray-50 dark:bg-gray-800/50"}
       [:button {:class (str "flex-1 py-3 px-4 rounded-xl font-medium border border-gray-200 dark:border-gray-700 text-gray-500 dark:text-gray-400 "
                             "hover:bg-white dark:hover:bg-gray-700 shadow-sm transition-colors")
                 :on-click #(rf/dispatch [:app/close-panel])}
        "Cancel"]
       [:button {:class (str "flex-1 py-3 px-4 rounded-xl font-medium text-white "
                             "bg-violet-500 hover:bg-violet-600 "
                             "shadow-lg shadow-violet-500/30 disabled:opacity-50 disabled:cursor-not-allowed "
                             "transition-all")
                 :disabled (or (not form-valid?) loading?)
                 :on-click #(rf/dispatch [:tx/create-transaction])}
        (if loading? "Saving..." "Save Transaction")]]]]))
