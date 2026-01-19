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
    [:div {:class (str "rounded-xl p-4 mb-6 "
                       (if is-income?
                         "bg-gradient-to-r from-emerald-500 to-emerald-600"
                         "bg-gradient-to-r from-red-500 to-red-600")
                       " text-white")}
     [:div {:class "flex justify-between items-start"}
      [:div
       [:div {:class "text-xs uppercase tracking-wide text-white/70 mb-1"} "Preview"]
       [:div {:class "text-2xl font-bold"}
        (currency/format-currency display-amount currency {:show-sign? true})]
       [:div {:class "text-sm text-white/90 mt-1"}
        (if (str/blank? description) "Transaction Name" description)]]
      [:div {:class "text-3xl"} cat-icon]]
     [:div {:class "flex items-center gap-4 mt-4 pt-3 border-t border-white/20 text-sm"}
      [:div {:class "flex items-center gap-1.5 text-white/80"}
       [icon :calendar {:width 14 :height 14}]
       [:span date]]
      [:div {:class "flex items-center gap-1.5"}
       [:span {:class "w-2 h-2 rounded-full bg-white/50"}]
       [:span {:class "text-white/90"} (str/capitalize (name category))]]]]))

(defn type-toggle []
  (let [current-type @(rf/subscribe [:tx/form-field :type])]
    [:div {:class "flex gap-1 p-1 bg-neutral-100 dark:bg-neutral-800 rounded-lg mb-6"}
     [:button {:class (str "flex-1 px-4 py-2.5 rounded-md text-sm font-medium transition-colors "
                           (if (= current-type :expense)
                             "bg-red-500 text-white shadow-sm"
                             "text-neutral-600 dark:text-neutral-400 hover:text-neutral-900 dark:hover:text-neutral-50"))
               :on-click #(rf/dispatch [:tx/update-form-field :type :expense])}
      "Expense"]
     [:button {:class (str "flex-1 px-4 py-2.5 rounded-md text-sm font-medium transition-colors "
                           (if (= current-type :income)
                             "bg-green-500 text-white shadow-sm"
                             "text-neutral-600 dark:text-neutral-400 hover:text-neutral-900 dark:hover:text-neutral-50"))
               :on-click #(rf/dispatch [:tx/update-form-field :type :income])}
      "Income"]]))

(defn name-input []
  (let [description @(rf/subscribe [:tx/form-field :description])]
    [:div {:class "mb-4"}
     [:label {:class "block text-sm font-medium text-neutral-900 dark:text-neutral-50 mb-1.5"} "Transaction Name"]
     [:div {:class "relative"}
      [icon :file-text {:width 18 :height 18 :class "absolute left-3 top-1/2 -translate-y-1/2 text-neutral-400 dark:text-neutral-500"}]
      [:input {:class (str "w-full h-11 pl-10 pr-4 rounded-lg border border-neutral-200 dark:border-neutral-700 bg-white dark:bg-neutral-900 text-neutral-900 dark:text-neutral-50 "
                           "placeholder:text-neutral-400 dark:placeholder:text-neutral-500 focus:outline-none focus:ring-2 focus:ring-purple-700 dark:focus:ring-purple-400 focus:border-transparent "
                           "transition-all duration-200")
               :type "text"
               :placeholder "e.g., Grocery shopping"
               :value (or description "")
               :on-change #(rf/dispatch [:tx/update-form-field :description (-> % .-target .-value)])}]]]))

(defn currency-amount-row []
  (let [amount @(rf/subscribe [:tx/form-field :amount])
        currency @(rf/subscribe [:tx/form-currency])
        currencies @(rf/subscribe [:dashboard/available-currencies])]
    [:div {:class "grid grid-cols-3 gap-3 mb-4"}
     [:div
      [:label {:class "block text-sm font-medium text-neutral-900 dark:text-neutral-50 mb-1.5"} "Currency"]
      [:div {:class "relative"}
       [:select {:class (str "w-full h-11 px-3 pr-8 rounded-lg border border-neutral-200 dark:border-neutral-700 bg-white dark:bg-neutral-900 text-neutral-900 dark:text-neutral-50 "
                             "appearance-none focus:outline-none focus:ring-2 focus:ring-purple-700 dark:focus:ring-purple-400 focus:border-transparent "
                             "transition-all duration-200")
                 :value (name currency)
                 :on-change #(rf/dispatch [:tx/update-form-field :currency (keyword (-> % .-target .-value))])}
        (for [c currencies]
          ^{:key c}
          [:option {:value (name c)} (name c)])]
       [icon :chevron-down {:width 16 :height 16 :class "absolute right-3 top-1/2 -translate-y-1/2 text-neutral-400 dark:text-neutral-500 pointer-events-none"}]]]
     [:div {:class "col-span-2"}
      [:label {:class "block text-sm font-medium text-neutral-900 dark:text-neutral-50 mb-1.5"} "Amount"]
      [:div {:class "relative"}
       [:span {:class "absolute left-3 top-1/2 -translate-y-1/2 text-neutral-600 dark:text-neutral-400 font-medium"}
        (currency/currency-symbol currency)]
       [:input {:class (str "w-full h-11 pl-10 pr-4 rounded-lg border border-neutral-200 dark:border-neutral-700 bg-white dark:bg-neutral-900 text-neutral-900 dark:text-neutral-50 text-right font-mono "
                            "placeholder:text-neutral-400 dark:placeholder:text-neutral-500 focus:outline-none focus:ring-2 focus:ring-purple-700 dark:focus:ring-purple-400 focus:border-transparent "
                            "transition-all duration-200")
                :type "text"
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
    [:div {:class "mb-4"}
     [:label {:class "block text-sm font-medium text-neutral-900 dark:text-neutral-50 mb-1.5"} "Category"]
     [:div {:class "relative"}
      [:span {:class "absolute left-3 top-1/2 -translate-y-1/2 text-lg pointer-events-none"} selected-icon]
      [:select {:class (str "w-full h-11 pl-10 pr-8 rounded-lg border border-neutral-200 dark:border-neutral-700 bg-white dark:bg-neutral-900 text-neutral-900 dark:text-neutral-50 "
                            "appearance-none focus:outline-none focus:ring-2 focus:ring-purple-700 dark:focus:ring-purple-400 focus:border-transparent "
                            "transition-all duration-200")
                :value (name selected)
                :on-change #(rf/dispatch [:tx/update-form-field :category (keyword (-> % .-target .-value))])}
       (for [cat categories]
         ^{:key cat}
         [:option {:value (name cat)} (str/capitalize (name cat))])]
      [icon :chevron-down {:width 16 :height 16 :class "absolute right-3 top-1/2 -translate-y-1/2 text-neutral-400 dark:text-neutral-500 pointer-events-none"}]]]))

(defn date-picker []
  (let [date @(rf/subscribe [:tx/form-field :date])
        today (.toISOString (js/Date.))]
    [:div {:class "mb-4"}
     [:label {:class "block text-sm font-medium text-neutral-900 dark:text-neutral-50 mb-1.5"} "Date"]
     [:div {:class "relative"}
      [icon :calendar {:width 18 :height 18 :class "absolute left-3 top-1/2 -translate-y-1/2 text-neutral-400 dark:text-neutral-500"}]
      [:input {:class (str "w-full h-11 pl-10 pr-4 rounded-lg border border-neutral-200 dark:border-neutral-700 bg-white dark:bg-neutral-900 text-neutral-900 dark:text-neutral-50 "
                           "focus:outline-none focus:ring-2 focus:ring-purple-700 dark:focus:ring-purple-400 focus:border-transparent "
                           "transition-all duration-200")
               :type "date"
               :value (or date (subs today 0 10))
               :on-change #(rf/dispatch [:tx/update-form-field :date (-> % .-target .-value)])}]]]))

(defn wallet-dropdown []
  [:div {:class "mb-4 opacity-50"}
   [:label {:class "flex items-center gap-2 text-sm font-medium text-neutral-900 dark:text-neutral-50 mb-1.5"}
    "Source Account / Wallet"
    [:span {:class "px-1.5 py-0.5 rounded bg-purple-100 dark:bg-purple-900/20 text-purple-700 dark:text-purple-400 text-xs font-medium"} "Coming Soon"]]
   [:div {:class "relative"}
    [icon :wallet {:width 18 :height 18 :class "absolute left-3 top-1/2 -translate-y-1/2 text-neutral-400 dark:text-neutral-500"}]
    [:select {:class (str "w-full h-11 pl-10 pr-8 rounded-lg border border-neutral-200 dark:border-neutral-700 bg-white dark:bg-neutral-900 text-neutral-900 dark:text-neutral-50 "
                          "appearance-none cursor-not-allowed")
              :disabled true}
     [:option "Main Checking (**** 4532)"]]
    [icon :chevron-down {:width 16 :height 16 :class "absolute right-3 top-1/2 -translate-y-1/2 text-neutral-400 dark:text-neutral-500 pointer-events-none"}]]])

(defn recurring-toggle []
  (let [is-recurring @(rf/subscribe [:tx/form-is-recurring])]
    [:div {:class "flex items-center justify-between p-4 bg-neutral-100 dark:bg-neutral-800 rounded-lg mb-4"}
     [:div
      [:span {:class "block text-sm font-medium text-neutral-900 dark:text-neutral-50"} "Make Recurring"]
      [:span {:class "block text-xs text-neutral-400 dark:text-neutral-500 mt-0.5"} "Repeat this transaction monthly"]]
     [:button {:class (str "relative w-11 h-6 rounded-full transition-colors "
                           (if is-recurring "bg-purple-700 dark:bg-purple-400" "bg-neutral-200 dark:bg-neutral-700"))
               :on-click #(rf/dispatch [:tx/update-form-field :is-recurring (not is-recurring)])
               :role "switch"
               :aria-checked (str is-recurring)}
      [:span {:class (str "absolute top-1 w-4 h-4 rounded-full bg-white shadow transition-transform "
                          (if is-recurring "left-6" "left-1"))}]]]))

(defn notes-input []
  (let [notes @(rf/subscribe [:tx/form-notes])]
    [:div {:class "mb-4"}
     [:label {:class "block text-sm font-medium text-neutral-900 dark:text-neutral-50 mb-1.5"}
      "Notes "
      [:span {:class "text-neutral-400 dark:text-neutral-500 font-normal"} "(optional)"]]
     [:textarea {:class (str "w-full px-4 py-3 rounded-lg border border-neutral-200 dark:border-neutral-700 bg-white dark:bg-neutral-900 text-neutral-900 dark:text-neutral-50 "
                             "placeholder:text-neutral-400 dark:placeholder:text-neutral-500 focus:outline-none focus:ring-2 focus:ring-purple-700 dark:focus:ring-purple-400 focus:border-transparent "
                             "transition-all duration-200 resize-none")
                 :placeholder "Add any additional details..."
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
       [:div {:class "fixed inset-0 bg-black/50 z-40 transition-opacity"
              :on-click #(rf/dispatch [:app/close-panel])}])
     [:div {:class (str "fixed top-0 right-0 h-full w-full max-w-md bg-white dark:bg-neutral-800 shadow-xl z-50 "
                        "transform transition-transform duration-300 ease-out "
                        (if is-open? "translate-x-0" "translate-x-full"))}
      [:div {:class "flex items-center justify-between h-14 px-4 border-b border-neutral-200 dark:border-neutral-700"}
       [:button {:class "p-2 rounded-lg text-neutral-600 dark:text-neutral-400 hover:text-neutral-900 dark:hover:text-neutral-50 hover:bg-neutral-100 dark:hover:bg-neutral-800 transition-colors lg:hidden"
                 :on-click #(rf/dispatch [:app/close-panel])}
        [icon :arrow-left {:width 20 :height 20}]]
       [:h2 {:class "text-lg font-semibold text-neutral-900 dark:text-neutral-50"} "New Transaction"]
       [:button {:class "p-2 rounded-lg text-neutral-600 dark:text-neutral-400 hover:text-neutral-900 dark:hover:text-neutral-50 hover:bg-neutral-100 dark:hover:bg-neutral-800 transition-colors"
                 :on-click #(rf/dispatch [:app/close-panel])}
        [icon :x {:width 20 :height 20}]]]

      [:div {:class "flex-1 overflow-y-auto p-4"}
       [preview-card]
       [type-toggle]
       [name-input]
       [currency-amount-row]
       [category-dropdown]
       [date-picker]
       [wallet-dropdown]
       [recurring-toggle]
       [notes-input]]

      [:div {:class "flex gap-3 p-4 border-t border-neutral-200 dark:border-neutral-700"}
       [:button {:class (str "flex-1 h-11 rounded-lg font-medium border border-neutral-200 dark:border-neutral-700 text-neutral-900 dark:text-neutral-50 "
                             "hover:bg-neutral-100 dark:hover:bg-neutral-800 transition-colors")
                 :on-click #(rf/dispatch [:app/close-panel])}
        "Cancel"]
       [:button {:class (str "flex-1 h-11 rounded-lg font-medium text-white "
                             "bg-gradient-to-r from-purple-700 to-purple-500 "
                             "hover:shadow-lg disabled:opacity-50 disabled:cursor-not-allowed "
                             "transition-all")
                 :disabled (or (not form-valid?) loading?)
                 :on-click #(rf/dispatch [:tx/create-transaction])}
        (if loading? "Saving..." "Save Transaction")]]]]))
