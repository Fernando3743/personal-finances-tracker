(ns finance.views.transactions.activity-feed
  "Activity feed view with grouped transactions and sidebar widgets."
  (:require [re-frame.core :as rf]
            [finance.views.transactions.pagination :refer [pagination]]
            [finance.views.components.charts.donut-chart :refer [donut-chart]]
            [finance.db :as db]
            [finance.utils.currency :as currency]
            [finance.components.icons :refer [icon]]))

(defn format-time [date-str]
  (when date-str
    (let [date (js/Date. date-str)]
      (.toLocaleTimeString date "en-US" #js {:hour "numeric"
                                              :minute "2-digit"
                                              :hour12 true}))))

(defn current-month-label []
  (let [now (js/Date.)]
    (.toLocaleDateString now "en-US" #js {:month "long" :year "numeric"})))

(defn filter-chips-row []
  "Filter chips for month, account, and category."
  [:div {:class "flex flex-wrap gap-3 mb-6"}
   ;; Month dropdown
   [:button {:class (str "px-4 py-2 bg-white dark:bg-neutral-800 border border-neutral-200 dark:border-neutral-700 "
                         "rounded-full text-sm font-medium text-neutral-700 dark:text-neutral-300 "
                         "hover:border-violet-600 hover:text-violet-600 transition-colors flex items-center gap-2 shadow-sm")}
    [:span (current-month-label)]
    [icon :chevron-down {:width 18 :height 18}]]

   ;; Account dropdown
   [:button {:class (str "px-4 py-2 bg-white dark:bg-neutral-800 border border-neutral-200 dark:border-neutral-700 "
                         "rounded-full text-sm font-medium text-neutral-700 dark:text-neutral-300 "
                         "hover:border-violet-600 hover:text-violet-600 transition-colors flex items-center gap-2 shadow-sm")}
    [:span "All Accounts"]
    [icon :chevron-down {:width 18 :height 18}]]

   ;; Category dropdown
   [:button {:class (str "px-4 py-2 bg-white dark:bg-neutral-800 border border-neutral-200 dark:border-neutral-700 "
                         "rounded-full text-sm font-medium text-neutral-700 dark:text-neutral-300 "
                         "hover:border-violet-600 hover:text-violet-600 transition-colors flex items-center gap-2 shadow-sm")}
    [:span "Category"]
    [icon :chevron-down {:width 18 :height 18}]]])

(defn transaction-card-enhanced
  [{:keys [transaction/id transaction/amount transaction/type
           transaction/category transaction/description
           transaction/date transaction/currency]}]
  (let [cat-icon (get db/category-icons category "📦")
        is-income? (= type :income)
        curr (or currency :COP)
        display-amount (if is-income? amount (- amount))]
    [:div {:class (str "group relative flex items-center gap-5 p-5 bg-white dark:bg-neutral-800 rounded-2xl "
                       "border border-transparent hover:border-violet-600/20 hover:shadow-lg "
                       "transition-all duration-300 cursor-pointer")}
     ;; Icon
     [:div {:class (str "w-14 h-14 rounded-2xl flex items-center justify-center text-2xl flex-shrink-0 "
                        (if is-income?
                          "bg-green-100 dark:bg-green-900/20 text-green-600 dark:text-green-500"
                          "bg-red-100 dark:bg-red-900/20 text-red-600 dark:text-red-500"))}
      cat-icon]

     ;; Details
     [:div {:class "flex-1 min-w-0 flex flex-col justify-center"}
      [:div {:class "flex justify-between items-start mb-1"}
       [:h4 {:class "font-bold text-neutral-900 dark:text-neutral-100 truncate text-base"}
        (or description "No description")]
       [:p {:class (str "font-bold text-base ml-4 "
                        (if is-income?
                          "text-green-600 dark:text-green-500"
                          "text-neutral-900 dark:text-neutral-50"))}
        (currency/format-currency display-amount curr {:show-sign? is-income?})]]

      [:div {:class "flex justify-between items-center"}
       [:div {:class "flex items-center gap-2 text-sm text-neutral-500 dark:text-neutral-400"}
        [:span (format-time date)]
        [:span {:class "w-1 h-1 rounded-full bg-neutral-300 dark:bg-neutral-600"}]
        [:span {:class "px-2 py-0.5 rounded-md bg-neutral-100 dark:bg-neutral-700 text-xs font-medium text-neutral-600 dark:text-neutral-300"}
         (name (or category :other))]]

       ;; Arrow indicator (shows on hover)
       [:div {:class "opacity-0 group-hover:opacity-100 transition-opacity text-violet-600"}
        [icon :arrow-right {:width 20 :height 20}]]]]]))

(defn date-group-header [label total]
  [:div {:class "flex items-center justify-between mb-4"}
   [:h3 {:class "font-bold text-lg text-neutral-900 dark:text-white flex items-center gap-2"}
    label
    [:span {:class "text-sm font-normal text-neutral-500 dark:text-neutral-400"}
     (when (string? label) (subs label 0 (min (count label) 10)))]]
   [:span {:class (str "text-xs font-bold uppercase tracking-wider "
                       (if (>= total 0)
                         "text-green-600 dark:text-green-500"
                         "text-red-600 dark:text-red-500"))}
    (currency/format-currency total :COP {:show-sign? true})]])

(defn grouped-transaction-list []
  (let [groups @(rf/subscribe [:tx/grouped-by-date-feed])
        daily-totals @(rf/subscribe [:tx/daily-totals])]
    [:div {:class "space-y-8"}
     (for [{:keys [label date transactions]} groups]
       ^{:key (or date label)}
       [:section {:class "flex flex-col gap-4"}
        [date-group-header label (get daily-totals date 0)]
        [:div {:class "space-y-3"}
         (for [tx transactions]
           ^{:key (or (:transaction/id tx) (random-uuid))}
           [transaction-card-enhanced tx])]])]))

;; Sidebar Widgets

(defn add-transaction-button-full []
  [:button {:class (str "w-full py-4 bg-violet-600 hover:bg-violet-700 text-white rounded-2xl "
                        "shadow-lg shadow-violet-600/25 font-bold text-sm tracking-wide "
                        "flex items-center justify-center gap-2 transition-transform active:scale-95")
            :on-click #(rf/dispatch [:app/open-panel :add-transaction])}
   [icon :plus {:width 20 :height 20}]
   [:span "ADD TRANSACTION"]])

(defn total-spent-widget []
  (let [cat-data @(rf/subscribe [:tx/top-categories-data])
        total (:total cat-data)]
    [:div {:class "bg-white dark:bg-neutral-800 rounded-2xl p-6 shadow-sm border border-neutral-100 dark:border-neutral-700"}
     [:div {:class "flex justify-between items-start mb-2"}
      [:h4 {:class "font-bold text-lg text-neutral-900 dark:text-white"} "Total Spent"]
      [:div {:class "bg-neutral-100 dark:bg-neutral-700 p-1 rounded-lg"}
       [icon :calendar {:width 16 :height 16 :class "text-neutral-500"}]]]

     [:p {:class "text-xs text-neutral-500 dark:text-neutral-400 mb-4"}
      (let [now (js/Date.)
            month-start (js/Date. (.getFullYear now) (.getMonth now) 1)]
        (str (.toLocaleDateString month-start "en-US" #js {:month "short" :day "numeric"})
             " - "
             (.toLocaleDateString now "en-US" #js {:month "short" :day "numeric" :year "numeric"})))]

     [:div {:class "flex items-end gap-3 mb-4"}
      [:span {:class "text-4xl font-bold text-neutral-900 dark:text-white tracking-tight"}
       (currency/format-currency total :COP)]]

     [:div {:class "flex items-center gap-2 bg-red-100 dark:bg-red-900/20 text-red-600 dark:text-red-500 px-3 py-2 rounded-lg w-fit"}
      [icon :trending-up {:width 16 :height 16}]
      [:span {:class "text-xs font-bold"} "+12% vs last month"]]]))

(defn top-categories-widget []
  (let [cat-data @(rf/subscribe [:tx/top-categories-data])
        segments (:segments cat-data)
        total (:total cat-data)
        has-data? (and (seq segments) (pos? total))]
    [:div {:class "bg-white dark:bg-neutral-800 rounded-2xl p-6 shadow-sm border border-neutral-100 dark:border-neutral-700 flex flex-col gap-5"}
     [:div {:class "flex justify-between items-center"}
      [:h4 {:class "font-bold text-lg text-neutral-900 dark:text-white"} "Top Categories"]
      (when has-data?
        [:a {:class "text-violet-600 text-xs font-bold hover:underline cursor-pointer"
             :href "#"} "VIEW ALL"])]

     (if has-data?
       [:<>
        ;; Category items with progress bars
        (for [{:keys [category amount percent color]} (take 3 segments)]
          ^{:key category}
          [:div {:class "flex flex-col gap-2"}
           [:div {:class "flex justify-between text-sm font-medium"}
            [:span {:class "flex items-center gap-2 text-neutral-700 dark:text-neutral-300"}
             [:span {:class "w-2 h-2 rounded-full" :style {:background-color color}}]
             (name category)]
            [:span {:class "text-neutral-900 dark:text-white font-bold"}
             (currency/format-currency amount :COP)]]
           [:div {:class "h-2 w-full bg-neutral-100 dark:bg-neutral-700 rounded-full overflow-hidden"}
            [:div {:class "h-full rounded-full transition-all"
                   :style {:width (str percent "%")
                           :background-color color}}]]])

        ;; Donut summary
        [:div {:class "mt-4 pt-6 border-t border-neutral-100 dark:border-neutral-700 flex items-center gap-4"}
         [:div {:class "w-16 h-16"}
          [donut-chart {:segments segments
                        :size 64
                        :stroke-width 8}]]
         [:div
          [:p {:class "text-xs text-neutral-500 dark:text-neutral-400"} "Most spending in"]
          [:p {:class "font-bold text-neutral-900 dark:text-white"}
           (name (:category (first segments)))]]]]

       ;; Empty state
       [:div {:class "flex flex-col items-center justify-center py-6 text-center"}
        [:div {:class "text-3xl mb-2"} "📊"]
        [:p {:class "text-sm text-neutral-500 dark:text-neutral-400"} "No expenses yet"]])]))

(defn insights-promo-card []
  [:div {:class (str "relative overflow-hidden bg-gradient-to-br from-violet-600 to-purple-900 "
                     "rounded-2xl p-6 text-white shadow-lg")}
   [:div {:class "relative z-10 flex flex-col gap-2"}
    [:div {:class "w-8 h-8 bg-white/20 rounded-lg flex items-center justify-center backdrop-blur-sm mb-2"}
     [:span {:class "text-lg"} "💡"]]
    [:h4 {:class "font-bold text-lg leading-tight"} "Save $50 this week"]
    [:p {:class "text-xs text-white/80 leading-relaxed mb-2"}
     "You're spending 15% more on dining out than usual."]
    [:button {:class "text-xs font-bold bg-white text-violet-600 px-3 py-2 rounded-lg w-fit hover:bg-neutral-100 transition-colors"}
     "See Budget"]]

   ;; Decorative circle
   [:div {:class "absolute -bottom-10 -right-10 w-32 h-32 bg-white/10 rounded-full blur-2xl"}]])

(defn right-sidebar []
  [:div {:class "w-full lg:w-[360px] shrink-0 lg:sticky lg:top-6 flex flex-col gap-6 animate-stagger"}
   [add-transaction-button-full]
   [total-spent-widget]
   [top-categories-widget]
   [insights-promo-card]])

(defn activity-feed-view []
  "Complete activity feed view with left column feed and right sidebar."
  [:div {:class "flex flex-col lg:flex-row gap-8 items-start"}
   ;; Left column: Feed
   [:div {:class "flex-1 w-full min-w-0 flex flex-col gap-6 pb-20"}
    [filter-chips-row]
    [grouped-transaction-list]
    [pagination]]

   ;; Right sidebar: Widgets
   [right-sidebar]])
