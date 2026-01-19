(ns finance.views.incomes
  "Incomes page view - display income transactions."
  (:require [re-frame.core :as rf]
            [clojure.string :as str]
            [finance.db :as db]
            [finance.utils.currency :as currency]
            [finance.components.icons :refer [icon]]))

(defn format-date-short [date-str]
  (when date-str
    (let [date (js/Date. date-str)]
      (.toLocaleDateString date "en-US" #js {:month "short" :day "numeric"}))))

(defn summary-card [{:keys [title value subtitle icon-name variant]}]
  (let [variant-classes (case variant
                          :income "border-l-4 border-l-green-500"
                          :expense "border-l-4 border-l-red-500"
                          "")]
    [:div {:class (str "bg-white dark:bg-neutral-800 rounded-xl border border-neutral-200 dark:border-neutral-700 p-4 " variant-classes)}
     [:div {:class "flex items-start gap-3"}
      [:div {:class (str "p-2 rounded-lg "
                         (case variant
                           :income "bg-green-100 dark:bg-green-900/20 text-green-600 dark:text-green-500"
                           :expense "bg-red-100 dark:bg-red-900/20 text-red-600 dark:text-red-500"
                           "bg-purple-100 dark:bg-purple-900/20 text-purple-700 dark:text-purple-400"))}
       [icon icon-name {:width 24 :height 24}]]
      [:div
       [:div {:class "text-xl font-bold text-neutral-900 dark:text-neutral-50"} value]
       [:div {:class "text-sm text-neutral-600 dark:text-neutral-400"} title]
       (when subtitle
         [:div {:class "text-xs text-neutral-400 dark:text-neutral-500 mt-0.5"} subtitle])]]]))

(defn search-input []
  (let [search @(rf/subscribe [:incomes/filter-search])]
    [:div {:class "relative flex items-center"}
     [:span {:class "absolute left-3 text-neutral-400 dark:text-neutral-500"}
      [icon :search {:width 18 :height 18}]]
     [:input {:class (str "w-full h-10 pl-10 pr-9 rounded-lg border border-neutral-200 dark:border-neutral-700 bg-white dark:bg-neutral-900 text-neutral-900 dark:text-neutral-50 "
                          "placeholder:text-neutral-400 dark:placeholder:text-neutral-500 focus:outline-none focus:ring-2 focus:ring-purple-700 dark:focus:ring-purple-400 focus:border-transparent")
              :type "text"
              :placeholder "Search incomes..."
              :value (or search "")
              :on-change #(rf/dispatch [:incomes/update-filter :search (-> % .-target .-value)])}]
     (when (and search (not (str/blank? search)))
       [:button {:class "absolute right-3 p-0.5 rounded text-neutral-400 dark:text-neutral-500 hover:text-neutral-900 dark:hover:text-neutral-50 transition-colors"
                 :on-click #(rf/dispatch [:incomes/update-filter :search ""])}
        [icon :x {:width 14 :height 14}]])]))

(defn filter-chip [{:keys [label active? on-click on-clear]}]
  [:button {:class (str "inline-flex items-center gap-1.5 px-3 py-1.5 rounded-full text-sm font-medium transition-colors "
                        (if active?
                          "bg-green-500 text-white"
                          "bg-neutral-100 dark:bg-neutral-800 text-neutral-600 dark:text-neutral-400 hover:text-neutral-900 dark:hover:text-neutral-50"))
            :on-click on-click}
   [:span label]
   (when (and active? on-clear)
     [:span {:class "ml-1 p-0.5 rounded-full hover:bg-white/20"
             :on-click (fn [e] (.stopPropagation e) (on-clear))}
      [icon :x {:width 14 :height 14}]])])

(defn filter-bar []
  (let [filter-category @(rf/subscribe [:incomes/filter-category])
        filter-currency @(rf/subscribe [:incomes/filter-currency])
        has-filters? @(rf/subscribe [:incomes/has-active-filters?])
        income-count @(rf/subscribe [:incomes/filtered-count])]
    [:div {:class "flex flex-col md:flex-row md:items-center justify-between gap-4 mb-6"}
     [:div {:class "flex flex-col md:flex-row md:items-center gap-3"}
      [search-input]
      [:div {:class "flex flex-wrap gap-2"}
       [filter-chip {:label "All Currencies"
                     :active? (nil? filter-currency)
                     :on-click #(rf/dispatch [:incomes/update-filter :currency nil])}]
       [filter-chip {:label "COP"
                     :active? (= filter-currency :COP)
                     :on-click #(rf/dispatch [:incomes/update-filter :currency :COP])
                     :on-clear #(rf/dispatch [:incomes/update-filter :currency nil])}]
       [filter-chip {:label "USD"
                     :active? (= filter-currency :USD)
                     :on-click #(rf/dispatch [:incomes/update-filter :currency :USD])
                     :on-clear #(rf/dispatch [:incomes/update-filter :currency nil])}]
       (when filter-category
         [filter-chip {:label (name filter-category)
                       :active? true
                       :on-click #(rf/dispatch [:incomes/update-filter :category nil])
                       :on-clear #(rf/dispatch [:incomes/update-filter :category nil])}])]]
     [:div {:class "flex items-center gap-3"}
      [:span {:class "text-sm text-neutral-600 dark:text-neutral-400"} (str income-count " incomes")]
      (when has-filters?
        [:button {:class "px-3 py-1.5 rounded-lg text-sm font-medium text-neutral-600 dark:text-neutral-400 hover:text-neutral-900 dark:hover:text-neutral-50 hover:bg-neutral-100 dark:hover:bg-neutral-800 transition-colors"
                  :on-click #(rf/dispatch [:incomes/clear-filters])}
         "Clear filters"])]]))

(defn income-table-row [{:keys [transaction/id transaction/amount
                                transaction/category transaction/description
                                transaction/date transaction/currency]}]
  (let [cat-icon (get db/category-icons category "💰")
        curr (or currency :COP)]
    [:tr {:class "border-b border-neutral-200 dark:border-neutral-700 hover:bg-neutral-100/50 dark:hover:bg-neutral-800/50 transition-colors"}
     [:td {:class "py-3 px-4 text-sm text-neutral-600 dark:text-neutral-400 whitespace-nowrap"}
      (format-date-short date)]
     [:td {:class "py-3 px-4"}
      [:div {:class "flex items-center gap-2"}
       [:span {:class "text-xl"} cat-icon]
       [:span {:class "text-sm text-neutral-900 dark:text-neutral-50 truncate max-w-[200px]"} (or description "No description")]]]
     [:td {:class "py-3 px-4"}
      [:span {:class "px-2 py-0.5 rounded text-xs font-medium bg-neutral-100 dark:bg-neutral-800 text-neutral-600 dark:text-neutral-400"} (name (or category :other))]]
     [:td {:class "py-3 px-4"}
      [:span {:class "px-2 py-0.5 rounded text-xs font-medium bg-purple-100 dark:bg-purple-900/20 text-purple-700 dark:text-purple-400"} (name curr)]]
     [:td {:class "py-3 px-4 text-right font-mono font-semibold text-green-600 dark:text-green-500"}
      (currency/format-currency amount curr {:show-sign? true})]
     [:td {:class "py-3 px-4 text-right"}
      [:button {:class "p-2 rounded-lg text-neutral-400 dark:text-neutral-500 hover:text-red-500 hover:bg-red-100 dark:hover:bg-red-900/20 transition-colors"
                :on-click #(when (js/confirm "Delete this income?")
                             (rf/dispatch [:tx/delete-transaction (str id)]))}
       [icon :trash {:width 16 :height 16}]]]]))

(defn income-table [incomes]
  [:div {:class "hidden md:block overflow-x-auto bg-white dark:bg-neutral-800 rounded-xl border border-neutral-200 dark:border-neutral-700"}
   [:table {:class "w-full"}
    [:thead {:class "bg-neutral-100 dark:bg-neutral-800"}
     [:tr
      [:th {:class "py-3 px-4 text-left text-xs font-medium text-neutral-600 dark:text-neutral-400 uppercase tracking-wide"} "Date"]
      [:th {:class "py-3 px-4 text-left text-xs font-medium text-neutral-600 dark:text-neutral-400 uppercase tracking-wide"} "Description"]
      [:th {:class "py-3 px-4 text-left text-xs font-medium text-neutral-600 dark:text-neutral-400 uppercase tracking-wide"} "Category"]
      [:th {:class "py-3 px-4 text-left text-xs font-medium text-neutral-600 dark:text-neutral-400 uppercase tracking-wide"} "Currency"]
      [:th {:class "py-3 px-4 text-right text-xs font-medium text-neutral-600 dark:text-neutral-400 uppercase tracking-wide"} "Amount"]
      [:th {:class "py-3 px-4 w-[60px]"}]]]
    [:tbody
     (for [t incomes]
       ^{:key (or (:transaction/id t) (random-uuid))}
       [income-table-row t])]]])

(defn income-card [{:keys [transaction/id transaction/amount
                           transaction/category transaction/description
                           transaction/date transaction/currency]}]
  (let [cat-icon (get db/category-icons category "💰")
        curr (or currency :COP)]
    [:div {:class "bg-white dark:bg-neutral-800 rounded-xl border border-neutral-200 dark:border-neutral-700 p-4"}
     [:div {:class "flex items-center gap-3"}
      [:span {:class "text-2xl"} cat-icon]
      [:div {:class "flex-1 min-w-0"}
       [:div {:class "text-sm font-medium text-neutral-900 dark:text-neutral-50 truncate"} (or description "No description")]
       [:div {:class "flex items-center gap-2 text-xs text-neutral-400 dark:text-neutral-500 mt-0.5"}
        [:span (name (or category :other))]
        [:span "·"]
        [:span (name curr)]
        [:span "·"]
        [:span (format-date-short date)]]]
      [:div {:class "font-mono font-semibold text-green-600 dark:text-green-500"}
       (currency/format-currency amount curr {:show-sign? true})]]
     [:button {:class "absolute top-3 right-3 p-2 rounded-lg text-neutral-400 dark:text-neutral-500 hover:text-red-500 hover:bg-red-100 dark:hover:bg-red-900/20 transition-colors"
               :on-click #(when (js/confirm "Delete this income?")
                            (rf/dispatch [:tx/delete-transaction (str id)]))}
      [icon :trash {:width 16 :height 16}]]]))

(defn income-cards [incomes]
  [:div {:class "space-y-3"}
   (for [t incomes]
     ^{:key (or (:transaction/id t) (random-uuid))}
     [income-card t])])

(defn incomes-view []
  (let [incomes @(rf/subscribe [:incomes/filtered-list])
        grouped @(rf/subscribe [:incomes/grouped-by-date])
        loading? @(rf/subscribe [:app/loading?])
        total-by-currency @(rf/subscribe [:incomes/total-by-currency])
        top-category @(rf/subscribe [:incomes/top-category])]
    [:div {:class "space-y-6"}
     [:div {:class "mb-6"}
      [:h1 {:class "text-2xl font-bold text-neutral-900 dark:text-neutral-50"} "Incomes"]
      [:p {:class "text-neutral-600 dark:text-neutral-400 text-sm mt-1"} "Track all your income sources"]]

     [:div {:class "grid grid-cols-1 sm:grid-cols-2 lg:grid-cols-3 gap-4 mb-6"}
      (for [[curr total] total-by-currency]
        ^{:key curr}
        [summary-card {:title (str "Total " (name curr))
                       :value (currency/format-currency total curr)
                       :icon-name :trending-up
                       :variant :income}])
      (when top-category
        [summary-card {:title "Top Source"
                       :value (name top-category)
                       :subtitle "Most frequent income"
                       :icon-name :star
                       :variant :income}])]

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

       (empty? incomes)
       [:div {:class "text-center py-16 bg-white dark:bg-neutral-800 rounded-xl border border-neutral-200 dark:border-neutral-700"}
        [:div {:class "text-5xl mb-4"} "💰"]
        [:h3 {:class "text-lg font-semibold text-neutral-900 dark:text-neutral-50 mb-2"} "No incomes found"]
        [:p {:class "text-neutral-600 dark:text-neutral-400 mb-6"}
         (if @(rf/subscribe [:incomes/has-active-filters?])
           "Try adjusting your filters to see more results"
           "Start tracking your income by adding your first income transaction")]
        [:button {:class (str "inline-flex items-center gap-2 px-4 py-2 rounded-lg font-medium text-white "
                              "bg-gradient-to-r from-purple-700 to-purple-500 "
                              "hover:shadow-lg transition-all")
                  :on-click #(rf/dispatch [:app/navigate :add-transaction])}
         "Add Income"]]

       :else
       [:<>
        [income-table incomes]
        [:div {:class "md:hidden space-y-6"}
         (for [[date-str txs] (sort-by first > grouped)]
           ^{:key (or date-str "unknown")}
           [:div
            [:div {:class "text-sm font-medium text-neutral-600 dark:text-neutral-400 uppercase tracking-wide mb-3"}
             (or date-str "Unknown date")]
            [income-cards txs]])]])]))
