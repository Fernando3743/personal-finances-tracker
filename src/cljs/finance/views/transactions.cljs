(ns finance.views.transactions
  "Transaction list, filters, and form views."
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
    [:div {:class "relative flex-1 max-w-xs"}
     [:span {:class "absolute left-3 top-1/2 -translate-y-1/2 text-neutral-400 dark:text-neutral-500"}
      [icon :search {:width 18 :height 18}]]
     [:input {:class (str "w-full h-10 pl-10 pr-10 rounded-lg border border-neutral-200 dark:border-neutral-700 bg-white dark:bg-neutral-900 text-neutral-900 dark:text-neutral-50 "
                          "placeholder:text-neutral-400 dark:placeholder:text-neutral-500 focus:outline-none focus:ring-2 focus:ring-purple-700 dark:focus:ring-purple-400 focus:border-transparent "
                          "transition-all duration-200")
              :type "text"
              :placeholder "Search transactions..."
              :value (or search "")
              :on-change #(rf/dispatch [:tx/update-filter :search (-> % .-target .-value)])}]
     (when (and search (not (str/blank? search)))
       [:button {:class "absolute right-3 top-1/2 -translate-y-1/2 text-neutral-400 dark:text-neutral-500 hover:text-neutral-900 dark:hover:text-neutral-50 transition-colors"
                 :on-click #(rf/dispatch [:tx/update-filter :search ""])}
        [icon :x {:width 14 :height 14}]])]))

(defn filter-chip [{:keys [label active? on-click on-clear variant]}]
  (let [variant-classes (case variant
                          :income (if active? "bg-green-100 dark:bg-green-900/20 text-green-600 dark:text-green-500 border-green-500" "")
                          :expense (if active? "bg-red-100 dark:bg-red-900/20 text-red-600 dark:text-red-500 border-red-500" "")
                          (if active? "bg-purple-100 dark:bg-purple-900/20 text-purple-700 dark:text-purple-400 border-purple-700 dark:border-purple-400" ""))]
    [:button {:class (str "inline-flex items-center gap-1.5 px-3 py-1.5 rounded-full text-sm font-medium transition-colors border "
                          (if active?
                            variant-classes
                            "border-neutral-200 dark:border-neutral-700 text-neutral-600 dark:text-neutral-400 hover:text-neutral-900 dark:hover:text-neutral-50 hover:border-neutral-300 dark:hover:border-neutral-600"))
              :on-click on-click}
     [:span label]
     (when (and active? on-clear)
       [:span {:class "ml-1 hover:bg-black/10 rounded-full p-0.5"
               :on-click (fn [e]
                           (.stopPropagation e)
                           (on-clear))}
        [icon :x {:width 14 :height 14}]])]))

(defn filter-bar []
  (let [filter-type @(rf/subscribe [:tx/filter-type])
        filter-category @(rf/subscribe [:tx/filter-category])
        filter-currency @(rf/subscribe [:tx/filter-currency])
        has-filters? @(rf/subscribe [:tx/has-active-filters?])
        tx-count @(rf/subscribe [:tx/transaction-count])
        currencies @(rf/subscribe [:dashboard/available-currencies])]
    [:div {:class "bg-white dark:bg-neutral-800 rounded-xl border border-neutral-200 dark:border-neutral-700 p-4 mb-6"}
     [:div {:class "flex flex-col lg:flex-row lg:items-center justify-between gap-4"}
      [:div {:class "flex flex-wrap items-center gap-3"}
       [search-input]
       [:div {:class "flex flex-wrap items-center gap-2"}
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

        [:span {:class "text-neutral-200 dark:text-neutral-700 mx-1"} "|"]

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

      [:div {:class "flex items-center gap-3"}
       [:span {:class "text-sm text-neutral-600 dark:text-neutral-400"} (str tx-count " transactions")]
       (when has-filters?
         [:button {:class "text-sm text-purple-700 dark:text-purple-400 hover:text-purple-800 dark:hover:text-purple-300 font-medium transition-colors"
                   :on-click #(rf/dispatch [:tx/clear-filters])}
          "Clear filters"])]]]))

(defn transaction-table-row [{:keys [transaction/id transaction/amount transaction/type
                                     transaction/category transaction/description
                                     transaction/date transaction/currency]}]
  (let [cat-icon (get db/category-icons category "📦")
        is-income? (= type :income)
        curr (or currency :COP)
        display-amount (if is-income? amount (- amount))]
    [:tr {:class "border-b border-neutral-200 dark:border-neutral-700 last:border-0 hover:bg-neutral-100 dark:hover:bg-neutral-800 transition-colors"}
     [:td {:class "py-3 px-4 text-sm text-neutral-600 dark:text-neutral-400 whitespace-nowrap"}
      (format-date-short date)]
     [:td {:class "py-3 px-4"}
      [:div {:class "flex items-center gap-3"}
       [:span {:class "text-lg"} cat-icon]
       [:span {:class "text-sm text-neutral-900 dark:text-neutral-50"} (or description "No description")]]]
     [:td {:class "py-3 px-4"}
      [:span {:class "inline-flex px-2 py-0.5 rounded bg-neutral-100 dark:bg-neutral-800 text-xs font-medium text-neutral-600 dark:text-neutral-400"}
       (name (or category :other))]]
     [:td {:class "py-3 px-4"}
      [:span {:class "inline-flex px-2 py-0.5 rounded bg-neutral-100 dark:bg-neutral-800 text-xs font-medium text-neutral-600 dark:text-neutral-400"}
       (name curr)]]
     [:td {:class (str "py-3 px-4 text-right font-mono font-semibold text-sm "
                       (if is-income? "text-green-600 dark:text-green-500" "text-red-600 dark:text-red-500"))}
      (currency/format-currency display-amount curr {:show-sign? true})]
     [:td {:class "py-3 px-4 text-right"}
      [:button {:class "p-2 rounded-lg text-neutral-400 dark:text-neutral-500 hover:text-red-500 hover:bg-red-100 dark:hover:bg-red-900/20 transition-colors"
                :on-click #(when (js/confirm "Delete this transaction?")
                             (rf/dispatch [:tx/delete-transaction (str id)]))}
       [icon :trash {:width 16 :height 16}]]]]))

(defn transaction-table [transactions]
  [:div {:class "overflow-x-auto"}
   [:table {:class "w-full"}
    [:thead {:class "bg-neutral-100 dark:bg-neutral-800"}
     [:tr
      [:th {:class "py-3 px-4 text-left text-xs font-medium text-neutral-600 dark:text-neutral-400 uppercase tracking-wide"} "Date"]
      [:th {:class "py-3 px-4 text-left text-xs font-medium text-neutral-600 dark:text-neutral-400 uppercase tracking-wide"} "Description"]
      [:th {:class "py-3 px-4 text-left text-xs font-medium text-neutral-600 dark:text-neutral-400 uppercase tracking-wide"} "Category"]
      [:th {:class "py-3 px-4 text-left text-xs font-medium text-neutral-600 dark:text-neutral-400 uppercase tracking-wide"} "Currency"]
      [:th {:class "py-3 px-4 text-right text-xs font-medium text-neutral-600 dark:text-neutral-400 uppercase tracking-wide"} "Amount"]
      [:th {:class "py-3 px-4" :style {:width "60px"}}]]]
    [:tbody
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
    [:div {:class "flex items-center gap-3 p-4 bg-white dark:bg-neutral-800 rounded-lg border border-neutral-200 dark:border-neutral-700"}
     [:div {:class "flex-1 flex items-center gap-3 min-w-0"}
      [:span {:class "text-2xl"} cat-icon]
      [:div {:class "flex-1 min-w-0"}
       [:div {:class "text-sm font-medium text-neutral-900 dark:text-neutral-50 truncate"} (or description "No description")]
       [:div {:class "flex items-center gap-2 text-xs text-neutral-400 dark:text-neutral-500 mt-0.5"}
        [:span (name (or category :other))]
        [:span "•"]
        [:span (name curr)]
        [:span "•"]
        [:span (format-date-short date)]]]
      [:div {:class (str "font-mono font-semibold "
                         (if is-income? "text-green-600 dark:text-green-500" "text-red-600 dark:text-red-500"))}
       (currency/format-currency display-amount curr {:show-sign? true})]]
     [:button {:class "p-2 rounded-lg text-neutral-400 dark:text-neutral-500 hover:text-red-500 hover:bg-red-100 dark:hover:bg-red-900/20 transition-colors"
               :on-click #(when (js/confirm "Delete this transaction?")
                            (rf/dispatch [:tx/delete-transaction (str id)]))}
      [icon :trash {:width 16 :height 16}]]]))

(defn transaction-cards [transactions]
  [:div {:class "space-y-2"}
   (for [t transactions]
     ^{:key (or (:transaction/id t) (random-uuid))}
     [transaction-card t])])

(defn transaction-list []
  (let [transactions @(rf/subscribe [:tx/filtered-transactions])
        grouped @(rf/subscribe [:tx/transactions-by-date])
        loading? @(rf/subscribe [:app/loading?])]
    [:div {:class "space-y-6"}
     [filter-bar]

     (cond
       loading?
       [:div {:class "space-y-4"}
        (for [i (range 5)]
          ^{:key i}
          [:div {:class "flex items-center gap-4 p-4 bg-neutral-100 dark:bg-neutral-800 rounded-lg animate-pulse"}
           [:div {:class "w-10 h-10 rounded-full bg-neutral-200 dark:bg-neutral-700"}]
           [:div {:class "flex-1 space-y-2"}
            [:div {:class "h-4 bg-neutral-200 dark:bg-neutral-700 rounded w-3/5"}]
            [:div {:class "h-3 bg-neutral-200 dark:bg-neutral-700 rounded w-2/5"}]]])]

       (empty? transactions)
       [:div {:class "text-center py-16"}
        [:div {:class "text-5xl mb-4"} "📋"]
        [:h3 {:class "text-lg font-semibold text-neutral-900 dark:text-neutral-50 mb-2"} "No transactions found"]
        [:p {:class "text-neutral-600 dark:text-neutral-400 mb-6"}
         (if @(rf/subscribe [:tx/has-active-filters?])
           "Try adjusting your filters to see more results"
           "Start tracking your finances by adding your first transaction")]
        [:button {:class (str "inline-flex items-center gap-2 px-4 py-2 rounded-lg font-medium text-white "
                              "bg-gradient-to-r from-purple-700 to-purple-500 "
                              "hover:shadow-lg transition-all")
                  :on-click #(rf/dispatch [:app/navigate :add-transaction])}
         "Add Transaction"]]

       :else
       [:<>
        [:div {:class "hidden lg:block bg-white dark:bg-neutral-800 rounded-xl border border-neutral-200 dark:border-neutral-700 overflow-hidden"}
         [transaction-table transactions]]

        [:div {:class "lg:hidden space-y-6"}
         (for [[date-str txs] (sort-by first > grouped)]
           ^{:key (or date-str "unknown")}
           [:div
            [:div {:class "text-xs font-medium text-neutral-400 dark:text-neutral-500 uppercase tracking-wide mb-2 px-1"}
             (or date-str "Unknown date")]
            [transaction-cards txs]])]])]))

(defn amount-input []
  (let [amount @(rf/subscribe [:tx/form-field :amount])
        tx-type @(rf/subscribe [:tx/form-field :type])
        curr @(rf/subscribe [:tx/form-currency])]
    [:div {:class "flex items-center gap-2"}
     [:span {:class (str "text-3xl font-bold "
                         (if (= tx-type :income) "text-green-600 dark:text-green-500" "text-red-600 dark:text-red-500"))}
      (currency/currency-symbol curr)]
     [:input {:class "flex-1 text-3xl font-bold bg-transparent text-neutral-900 dark:text-neutral-50 outline-none placeholder:text-neutral-400 dark:placeholder:text-neutral-500"
              :type "text"
              :inputMode "decimal"
              :placeholder "0.00"
              :value (or amount "")
              :on-change #(let [val (-> % .-target .-value)
                                cleaned (str/replace val #"[^\d.]" "")]
                            (rf/dispatch [:tx/update-form-field :amount cleaned]))}]]))

(defn type-toggle []
  (let [current-type @(rf/subscribe [:tx/form-field :type])]
    [:div {:class "flex gap-1 p-1 bg-neutral-100 dark:bg-neutral-800 rounded-lg"}
     [:button {:class (str "flex-1 px-4 py-2 rounded-md text-sm font-medium transition-colors "
                           (if (= current-type :expense)
                             "bg-red-500 text-white shadow-sm"
                             "text-neutral-600 dark:text-neutral-400 hover:text-neutral-900 dark:hover:text-neutral-50"))
               :on-click #(rf/dispatch [:tx/update-form-field :type :expense])}
      "Expense"]
     [:button {:class (str "flex-1 px-4 py-2 rounded-md text-sm font-medium transition-colors "
                           (if (= current-type :income)
                             "bg-green-500 text-white shadow-sm"
                             "text-neutral-600 dark:text-neutral-400 hover:text-neutral-900 dark:hover:text-neutral-50"))
               :on-click #(rf/dispatch [:tx/update-form-field :type :income])}
      "Income"]]))

(defn category-picker []
  (let [categories @(rf/subscribe [:tx/categories])
        selected @(rf/subscribe [:tx/form-field :category])]
    [:div {:class "space-y-2"}
     [:label {:class "block text-sm font-medium text-neutral-900 dark:text-neutral-50"} "Category"]
     [:div {:class "grid grid-cols-3 sm:grid-cols-4 gap-2"}
      (for [cat categories]
        (let [cat-icon (get db/category-icons cat "📦")
              active? (= cat selected)]
          ^{:key cat}
          [:button {:class (str "flex flex-col items-center gap-1 p-3 rounded-lg border transition-all "
                                (if active?
                                  "border-purple-700 dark:border-purple-400 bg-purple-100 dark:bg-purple-900/20"
                                  "border-neutral-200 dark:border-neutral-700 hover:border-neutral-300 dark:hover:border-neutral-600"))
                    :on-click #(rf/dispatch [:tx/update-form-field :category cat])}
           [:span {:class "text-xl"} cat-icon]
           [:span {:class "text-xs text-neutral-600 dark:text-neutral-400"} (name cat)]
           (when active?
             [:span {:class "absolute top-1 right-1 text-purple-700 dark:text-purple-400"}
              [icon :check {:width 16 :height 16}]])]))]]))

(defn description-input []
  (let [description @(rf/subscribe [:tx/form-field :description])]
    [:div {:class "space-y-1.5"}
     [:label {:class "block text-sm font-medium text-neutral-900 dark:text-neutral-50" :for "description"} "Description"]
     [:input {:class (str "w-full h-11 px-4 rounded-lg border border-neutral-200 dark:border-neutral-700 bg-white dark:bg-neutral-900 text-neutral-900 dark:text-neutral-50 "
                          "placeholder:text-neutral-400 dark:placeholder:text-neutral-500 "
                          "focus:outline-none focus:ring-2 focus:ring-purple-700 dark:focus:ring-purple-400 focus:border-transparent "
                          "transition-all duration-200")
              :type "text"
              :id "description"
              :placeholder "What was this for?"
              :value (or description "")
              :on-change #(rf/dispatch [:tx/update-form-field :description (-> % .-target .-value)])}]]))

(defn date-input []
  (let [date @(rf/subscribe [:tx/form-field :date])
        today (.toISOString (js/Date.))]
    [:div {:class "space-y-1.5"}
     [:label {:class "block text-sm font-medium text-neutral-900 dark:text-neutral-50" :for "date"} "Date"]
     [:div {:class "relative"}
      [:span {:class "absolute left-3 top-1/2 -translate-y-1/2 text-neutral-400 dark:text-neutral-500 pointer-events-none"}
       [icon :calendar {:width 18 :height 18}]]
      [:input {:class (str "w-full h-11 pl-10 pr-4 rounded-lg border border-neutral-200 dark:border-neutral-700 bg-white dark:bg-neutral-900 text-neutral-900 dark:text-neutral-50 "
                           "focus:outline-none focus:ring-2 focus:ring-purple-700 dark:focus:ring-purple-400 focus:border-transparent "
                           "transition-all duration-200")
               :type "date"
               :id "date"
               :value (or date (subs today 0 10))
               :on-change #(rf/dispatch [:tx/update-form-field :date (-> % .-target .-value)])}]]]))

(defn currency-selector []
  (let [currencies @(rf/subscribe [:dashboard/available-currencies])
        selected @(rf/subscribe [:tx/form-currency])]
    [:div {:class "space-y-1.5"}
     [:label {:class "block text-sm font-medium text-neutral-900 dark:text-neutral-50"} "Currency"]
     [:div {:class "flex gap-1 p-1 bg-neutral-100 dark:bg-neutral-800 rounded-lg"}
      (for [curr currencies]
        ^{:key curr}
        [:button {:class (str "flex-1 px-4 py-2 rounded-md text-sm font-medium transition-colors "
                              (if (= curr selected)
                                "bg-white dark:bg-neutral-700 text-neutral-900 dark:text-neutral-50 shadow-sm"
                                "text-neutral-600 dark:text-neutral-400 hover:text-neutral-900 dark:hover:text-neutral-50"))
                  :on-click #(rf/dispatch [:tx/update-form-field :currency curr])}
         (name curr)])]]))

(defn exchange-rate-input []
  (let [rate @(rf/subscribe [:tx/form-field :exchange-rate])
        curr @(rf/subscribe [:tx/form-currency])]
    (when (= curr :USD)
      [:div {:class "space-y-1.5"}
       [:label {:class "block text-sm font-medium text-neutral-900 dark:text-neutral-50" :for "exchange-rate"} "Exchange Rate (COP/USD) - Optional"]
       [:input {:class (str "w-full h-11 px-4 rounded-lg border border-neutral-200 dark:border-neutral-700 bg-white dark:bg-neutral-900 text-neutral-900 dark:text-neutral-50 "
                            "placeholder:text-neutral-400 dark:placeholder:text-neutral-500 "
                            "focus:outline-none focus:ring-2 focus:ring-purple-700 dark:focus:ring-purple-400 focus:border-transparent "
                            "transition-all duration-200")
                :type "number"
                :id "exchange-rate"
                :placeholder "e.g., 4000"
                :step "0.01"
                :value (or rate "")
                :on-change #(let [v (-> % .-target .-value)]
                              (rf/dispatch [:tx/update-form-field
                                            :exchange-rate
                                            (when (not (str/blank? v))
                                              (js/parseFloat v))]))}]])))

(defn add-transaction-form []
  (let [form-valid? @(rf/subscribe [:tx/form-valid?])
        loading? @(rf/subscribe [:app/loading?])]
    [:div {:class "max-w-lg mx-auto space-y-6"}
     [:div {:class "mb-6"}
      [:h2 {:class "text-2xl font-bold text-neutral-900 dark:text-neutral-50"} "Add Transaction"]]

     [:div {:class "bg-white dark:bg-neutral-800 rounded-xl border border-neutral-200 dark:border-neutral-700 p-6 space-y-6"}
      [:div {:class "space-y-4"}
       [amount-input]
       [type-toggle]]

      [currency-selector]
      [exchange-rate-input]
      [category-picker]
      [description-input]
      [date-input]]

     [:div {:class "flex gap-3"}
      [:button {:class (str "flex-1 h-11 rounded-lg font-medium border border-neutral-200 dark:border-neutral-700 text-neutral-900 dark:text-neutral-50 "
                            "hover:bg-neutral-100 dark:hover:bg-neutral-800 transition-colors")
                :on-click #(do
                             (rf/dispatch [:tx/reset-form])
                             (rf/dispatch [:app/navigate :transactions]))}
       "Cancel"]
      [:button {:class (str "flex-1 h-11 rounded-lg font-medium text-white "
                            "bg-gradient-to-r from-purple-700 to-purple-500 "
                            "hover:shadow-lg disabled:opacity-50 disabled:cursor-not-allowed "
                            "transition-all")
                :disabled (or (not form-valid?) loading?)
                :on-click #(rf/dispatch [:tx/create-transaction])}
       (if loading? "Saving..." "Save Transaction")]]]))
