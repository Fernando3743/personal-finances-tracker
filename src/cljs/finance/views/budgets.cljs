(ns finance.views.budgets
  "Budgets page view with three view modes: table, grid, and envelope."
  (:require [re-frame.core :as rf]
            [finance.utils.currency :as currency]
            [finance.components.icons :refer [icon]]
            [finance.views.budgets-panel :refer [budgets-panel]]
            [finance.components.category-icon :refer [category-icon]]))

;; Utility Functions
(defn status-color [status]
  (case status
    :exceeded "text-red-600 dark:text-red-500"
    :warning "text-amber-500"
    :ok "text-green-600 dark:text-green-500"
    "text-gray-600 dark:text-gray-400"))

(defn status-bg [status]
  (case status
    :exceeded "bg-red-100 dark:bg-red-900/20"
    :warning "bg-amber-100 dark:bg-amber-900/20"
    :ok "bg-green-100 dark:bg-green-900/20"
    "bg-gray-100 dark:bg-gray-800"))

(defn status-border [status]
  (case status
    :exceeded "border-red-500"
    :warning "border-amber-500"
    :ok "border-gray-200 dark:border-gray-700"
    "border-gray-200 dark:border-gray-700"))

(defn progress-bar-color [status]
  (case status
    :exceeded "bg-red-500"
    :warning "bg-amber-500"
    "bg-green-500"))

;; Reusable Components
(defn progress-bar [{:keys [percentage status]}]
  (let [clamped (min 100 (max 0 percentage))
        bar-color (progress-bar-color status)]
    [:div {:class "h-2 bg-gray-100 dark:bg-gray-800 rounded-full overflow-hidden"}
     [:div {:class (str "h-full rounded-full transition-all duration-300 " bar-color)
            :style {:width (str clamped "%")}}]]))

(defn summary-cards-row []
  (let [total-budgeted @(rf/subscribe [:budgets/total-budgeted])
        total-spent @(rf/subscribe [:budgets/total-spent])
        days-left @(rf/subscribe [:budgets/days-left-in-month])
        daily-avg @(rf/subscribe [:budgets/daily-avg-remaining])
        remaining (- total-budgeted total-spent)
        percentage (if (pos? total-budgeted) (* 100 (/ total-spent total-budgeted)) 0)
        now (js/Date.)
        month (inc (.getMonth now))
        year (.getFullYear now)]
    [:div {:class "grid grid-cols-1 md:grid-cols-2 lg:grid-cols-4 gap-4"}
     ;; Total Monthly Budget
     [:div {:class "bg-white dark:bg-gray-800 rounded-xl border border-gray-200 dark:border-gray-700 p-5"}
      [:div {:class "flex items-center justify-between mb-2"}
       [:span {:class "text-xs text-gray-500 dark:text-gray-400 uppercase tracking-wide font-semibold"} "Total Budget"]
       [icon :calendar {:width 16 :height 16 :class "text-gray-400"}]]
      [:div {:class "text-2xl font-bold text-gray-900 dark:text-gray-50"}
       (currency/format-currency total-budgeted :COP)]
      [:div {:class "text-xs text-gray-500 dark:text-gray-400 mt-1"}
       (str month "/" year)]]

     ;; Left to Spend
     [:div {:class "bg-white dark:bg-gray-800 rounded-xl border border-gray-200 dark:border-gray-700 p-5"}
      [:div {:class "flex items-center justify-between mb-2"}
       [:span {:class "text-xs text-gray-500 dark:text-gray-400 uppercase tracking-wide font-semibold"} "Left to Spend"]
       [:span {:class (str "text-xs font-semibold " (status-color (cond (> percentage 100) :exceeded
                                                                        (> percentage 80) :warning
                                                                        :else :ok)))}
        (str (.toFixed percentage 0) "% Used")]]
      [:div {:class "text-2xl font-bold text-gray-900 dark:text-gray-50"}
       (currency/format-currency remaining :COP)]
      [:div {:class "mt-2"}
       [progress-bar {:percentage percentage
                      :status (cond (> percentage 100) :exceeded
                                    (> percentage 80) :warning
                                    :else :ok)}]]]

     ;; Days Left
     [:div {:class "bg-white dark:bg-gray-800 rounded-xl border border-gray-200 dark:border-gray-700 p-5"}
      [:div {:class "flex items-center justify-between mb-2"}
       [:span {:class "text-xs text-gray-500 dark:text-gray-400 uppercase tracking-wide font-semibold"} "Days Left"]
       [icon :clock {:width 16 :height 16 :class "text-gray-400"}]]
      [:div {:class "text-2xl font-bold text-gray-900 dark:text-gray-50"}
       (str days-left " days")]
      [:div {:class "text-xs text-gray-500 dark:text-gray-400 mt-1"}
       "This month"]]

     ;; Daily Average
     [:div {:class "bg-white dark:bg-gray-800 rounded-xl border border-gray-200 dark:border-gray-700 p-5"}
      [:div {:class "flex items-center justify-between mb-2"}
       [:span {:class "text-xs text-gray-500 dark:text-gray-400 uppercase tracking-wide font-semibold"} "Daily Budget"]
       [icon :trending-up {:width 16 :height 16 :class "text-gray-400"}]]
      [:div {:class "text-2xl font-bold text-gray-900 dark:text-gray-50"}
       (currency/format-currency daily-avg :COP)]
      [:div {:class "text-xs text-gray-500 dark:text-gray-400 mt-1"}
       "Per day remaining"]]]))

(defn search-bar []
  (let [query @(rf/subscribe [:budgets/search-query])]
    [:div {:class "relative flex-1 max-w-md"}
     [:div {:class "absolute inset-y-0 left-0 pl-3 flex items-center pointer-events-none"}
      [icon :search {:width 18 :height 18 :class "text-gray-400"}]]
     [:input {:class (str "block w-full pl-10 pr-3 py-2.5 border border-gray-200 dark:border-gray-700 rounded-xl "
                          "bg-white dark:bg-gray-800 text-gray-900 dark:text-gray-50 placeholder-gray-400 "
                          "focus:outline-none focus:ring-2 focus:ring-violet-500 focus:border-transparent "
                          "transition-all text-sm")
              :type "text"
              :placeholder "Search categories..."
              :value query
              :on-change #(rf/dispatch [:budgets/set-search-query (-> % .-target .-value)])}]]))

(defn filter-tabs []
  (let [filter-status @(rf/subscribe [:budgets/filter-status])
        by-status @(rf/subscribe [:budgets/by-status])
        all-count (count @(rf/subscribe [:budgets/budget-summaries]))
        on-track-count (count (get by-status :ok []))
        near-limit-count (count (get by-status :warning []))
        over-budget-count (count (get by-status :exceeded []))]
    [:div {:class "flex items-center gap-2 overflow-x-auto"}
     [:button {:class (str "px-3 py-1.5 rounded-lg text-xs font-medium transition-colors whitespace-nowrap "
                           (if (= filter-status :all)
                             "bg-gray-900 dark:bg-gray-50 text-white dark:text-gray-900"
                             "bg-gray-100 dark:bg-gray-800 text-gray-600 dark:text-gray-400 hover:bg-gray-200 dark:hover:bg-gray-700"))
               :on-click #(rf/dispatch [:budgets/set-filter-status :all])}
      (str "All (" all-count ")")]
     [:button {:class (str "px-3 py-1.5 rounded-lg text-xs font-medium transition-colors whitespace-nowrap "
                           (if (= filter-status :on-track)
                             "bg-green-500 text-white"
                             "bg-green-100 dark:bg-green-900/20 text-green-600 dark:text-green-500 hover:bg-green-200 dark:hover:bg-green-900/30"))
               :on-click #(rf/dispatch [:budgets/set-filter-status :on-track])}
      (str "On Track (" on-track-count ")")]
     [:button {:class (str "px-3 py-1.5 rounded-lg text-xs font-medium transition-colors whitespace-nowrap "
                           (if (= filter-status :near-limit)
                             "bg-amber-500 text-white"
                             "bg-amber-100 dark:bg-amber-900/20 text-amber-600 dark:text-amber-500 hover:bg-amber-200 dark:hover:bg-amber-900/30"))
               :on-click #(rf/dispatch [:budgets/set-filter-status :near-limit])}
      (str "Near Limit (" near-limit-count ")")]
     [:button {:class (str "px-3 py-1.5 rounded-lg text-xs font-medium transition-colors whitespace-nowrap "
                           (if (= filter-status :over-budget)
                             "bg-red-500 text-white"
                             "bg-red-100 dark:bg-red-900/20 text-red-600 dark:text-red-500 hover:bg-red-200 dark:hover:bg-red-900/30"))
               :on-click #(rf/dispatch [:budgets/set-filter-status :over-budget])}
      (str "Over Budget (" over-budget-count ")")]]))

(defn view-mode-toggle []
  (let [view-mode @(rf/subscribe [:budgets/view-mode])]
    [:div {:class "flex items-center gap-1 p-1 bg-gray-100 dark:bg-gray-800 rounded-xl"}
     [:button {:class (str "px-3 py-1.5 rounded-lg text-xs font-medium transition-all flex items-center gap-1.5 "
                           (if (= view-mode :table)
                             "bg-white dark:bg-gray-700 text-gray-900 dark:text-gray-50 shadow-sm"
                             "text-gray-600 dark:text-gray-400 hover:text-gray-900 dark:hover:text-gray-50"))
               :on-click #(rf/dispatch [:budgets/set-view-mode :table])}
      [icon :table {:width 14 :height 14}]
      [:span "Table"]]
     [:button {:class (str "px-3 py-1.5 rounded-lg text-xs font-medium transition-all flex items-center gap-1.5 "
                           (if (= view-mode :grid)
                             "bg-white dark:bg-gray-700 text-gray-900 dark:text-gray-50 shadow-sm"
                             "text-gray-600 dark:text-gray-400 hover:text-gray-900 dark:hover:text-gray-50"))
               :on-click #(rf/dispatch [:budgets/set-view-mode :grid])}
      [icon :grid {:width 14 :height 14}]
      [:span "Grid"]]
     [:button {:class (str "px-3 py-1.5 rounded-lg text-xs font-medium transition-all flex items-center gap-1.5 "
                           (if (= view-mode :envelope)
                             "bg-white dark:bg-gray-700 text-gray-900 dark:text-gray-50 shadow-sm"
                             "text-gray-600 dark:text-gray-400 hover:text-gray-900 dark:hover:text-gray-50"))
               :on-click #(rf/dispatch [:budgets/set-view-mode :envelope])}
      [icon :layout {:width 14 :height 14}]
      [:span "Envelope"]]]))

(defn empty-state []
  [:div {:class "text-center py-16 bg-white dark:bg-gray-800 rounded-xl border border-gray-200 dark:border-gray-700"}
   [:div {:class "text-5xl mb-4"} "💵"]
   [:h3 {:class "text-lg font-semibold text-gray-900 dark:text-gray-50 mb-2"} "No budgets set"]
   [:p {:class "text-gray-600 dark:text-gray-400 mb-6"} "Create budgets to track your spending by category"]
   [:button {:class "px-4 py-2 bg-violet-500 hover:bg-violet-600 text-white rounded-xl font-medium shadow-lg transition-all inline-flex items-center gap-2"
             :on-click #(rf/dispatch [:budgets/open-panel :create])}
    [icon :plus {:width 18 :height 18}]
    [:span "Create Your First Budget"]]])

;; Table View Components
(defn table-row [{:keys [budget spent remaining percentage status]}]
  (let [cat (:budget/category budget)
        amount (:budget/amount budget)
        curr (:budget/currency budget)
        id (:budget/id budget)]
    [:div {:class (str "grid grid-cols-6 gap-4 px-6 py-4 border-b border-gray-200 dark:border-gray-700 "
                       "hover:bg-gray-50 dark:hover:bg-gray-700/50 transition-colors")}
     ;; Category
     [:div {:class "flex items-center gap-3"}
      [category-icon cat {:size :sm}]
      [:div
       [:div {:class "font-medium text-gray-900 dark:text-gray-50"} (name cat)]
       [:div {:class "text-xs text-gray-500 dark:text-gray-400"} "Fixed"]]]

     ;; Budgeted
     [:div {:class "flex items-center"}
      [:div {:class "text-sm font-semibold text-gray-900 dark:text-gray-50"}
       (currency/format-currency amount curr)]]

     ;; Actual Spent
     [:div {:class "flex items-center"}
      [:div {:class (str "text-sm font-semibold " (status-color status))}
       (currency/format-currency (or spent 0) curr)]]

     ;; Difference
     [:div {:class "flex items-center"}
      [:div {:class (str "text-sm font-semibold " (if (neg? remaining) "text-red-600 dark:text-red-500" "text-green-600 dark:text-green-500"))}
       (if (neg? remaining)
         (str "-" (currency/format-currency (abs remaining) curr))
         (currency/format-currency remaining curr))]]

     ;; Trend (placeholder)
     [:div {:class "flex items-center"}
      [:div {:class "flex items-center gap-1 text-sm"}
       [icon :arrow-up {:width 14 :height 14 :class "text-red-500"}]
       [:span {:class "text-gray-600 dark:text-gray-400"} "—"]]]

     ;; Actions
     [:div {:class "flex items-center justify-end gap-2"}
      [:button {:class "p-1.5 rounded-lg text-gray-400 hover:text-violet-600 hover:bg-violet-100 dark:hover:bg-violet-900/20 transition-colors"
                :on-click #(rf/dispatch [:budgets/open-panel :edit id])}
       [icon :edit {:width 16 :height 16}]]
      [:button {:class "p-1.5 rounded-lg text-gray-400 hover:text-red-600 hover:bg-red-100 dark:hover:bg-red-900/20 transition-colors"
                :on-click #(when (js/confirm "Delete this budget?")
                             (rf/dispatch [:budgets/delete id]))}
       [icon :trash {:width 16 :height 16}]]]]))

(defn table-view []
  (let [budgets @(rf/subscribe [:budgets/filtered-budgets])]
    [:div {:class "bg-white dark:bg-gray-800 rounded-2xl border border-gray-200 dark:border-gray-700 overflow-hidden"}
     ;; Table header
     [:div {:class "grid grid-cols-6 gap-4 px-6 py-3 bg-gray-50 dark:bg-gray-900/50 border-b border-gray-200 dark:border-gray-700"}
      [:div {:class "text-xs font-semibold text-gray-500 dark:text-gray-400 uppercase tracking-wide"} "Category"]
      [:div {:class "text-xs font-semibold text-gray-500 dark:text-gray-400 uppercase tracking-wide"} "Budgeted"]
      [:div {:class "text-xs font-semibold text-gray-500 dark:text-gray-400 uppercase tracking-wide"} "Actual Spent"]
      [:div {:class "text-xs font-semibold text-gray-500 dark:text-gray-400 uppercase tracking-wide"} "Difference"]
      [:div {:class "text-xs font-semibold text-gray-500 dark:text-gray-400 uppercase tracking-wide"} "Trend"]
      [:div {:class "text-xs font-semibold text-gray-500 dark:text-gray-400 uppercase tracking-wide text-right"} "Actions"]]
     ;; Table rows
     (if (empty? budgets)
       [:div {:class "text-center py-12 text-gray-500 dark:text-gray-400"}
        "No budgets match your filters"]
       (for [summary budgets]
         ^{:key (get-in summary [:budget :budget/id])}
         [table-row summary]))]))

;; Grid View Components
(defn budget-card [{:keys [budget spent remaining percentage status]}]
  (let [cat (:budget/category budget)
        amount (:budget/amount budget)
        curr (:budget/currency budget)
        id (:budget/id budget)
        border-class (status-border status)]
    [:div {:class (str "relative bg-white dark:bg-gray-800 rounded-xl border p-5 transition-all hover:shadow-lg " border-class)}
     [:div {:class "flex items-center justify-between mb-3"}
      [:div {:class "flex items-center gap-2"}
       [category-icon cat {:size :sm}]
       [:span {:class "font-medium text-gray-900 dark:text-gray-50"} (name cat)]]
      [:div
       (case status
         :exceeded [:span {:class (str "px-2 py-0.5 rounded-full text-xs font-medium " (status-bg status) " " (status-color status))} "Exceeded"]
         :warning [:span {:class (str "px-2 py-0.5 rounded-full text-xs font-medium " (status-bg status) " " (status-color status))} "Warning"]
         [:span {:class (str "px-2 py-0.5 rounded-full text-xs font-medium " (status-bg status) " " (status-color status))} "On Track"])]]
     [:div {:class "flex items-baseline gap-2 mb-3"}
      [:div {:class "flex-1"}
       [:span {:class "text-xs text-gray-400 dark:text-gray-500"} "Spent"]
       [:div {:class "text-lg font-semibold text-gray-900 dark:text-gray-50"} (currency/format-currency (or spent 0) curr)]]
      [:span {:class "text-gray-400 dark:text-gray-500"} "of"]
      [:div {:class "flex-1 text-right"}
       [:span {:class "text-xs text-gray-400 dark:text-gray-500"} "Budget"]
       [:div {:class "text-lg font-semibold text-gray-900 dark:text-gray-50"} (currency/format-currency amount curr)]]]
     [progress-bar {:percentage (* 100 (or percentage 0)) :status status}]
     [:div {:class "flex items-center justify-between mt-3 text-sm"}
      [:span {:class (str "font-medium " (if (neg? remaining) "text-red-600 dark:text-red-500" "text-gray-600 dark:text-gray-400"))}
       (if (neg? remaining)
         (str "Over by " (currency/format-currency (abs remaining) curr))
         (str (currency/format-currency remaining curr) " remaining"))]
      [:span {:class "text-gray-400 dark:text-gray-500"}
       (str (.toFixed (* 100 (or percentage 0)) 0) "%")]]
     [:div {:class "absolute top-4 right-4 flex gap-1"}
      [:button {:class "p-1.5 rounded-lg text-gray-400 dark:text-gray-500 hover:text-violet-600 hover:bg-violet-100 dark:hover:bg-violet-900/20 transition-colors"
                :on-click #(rf/dispatch [:budgets/open-panel :edit id])}
       [icon :edit {:width 16 :height 16}]]
      [:button {:class "p-1.5 rounded-lg text-gray-400 dark:text-gray-500 hover:text-red-500 hover:bg-red-100 dark:hover:bg-red-900/20 transition-colors"
                :on-click #(when (js/confirm "Delete this budget?")
                             (rf/dispatch [:budgets/delete id]))}
       [icon :trash {:width 16 :height 16}]]]]))

(defn add-budget-card []
  [:button {:class (str "relative bg-white dark:bg-gray-800 rounded-xl border-2 border-dashed border-gray-300 dark:border-gray-600 "
                        "p-5 transition-all hover:border-violet-500 hover:bg-violet-50 dark:hover:bg-violet-900/10 "
                        "flex flex-col items-center justify-center h-full min-h-[200px] group")
            :on-click #(rf/dispatch [:budgets/open-panel :create])}
   [:div {:class "text-4xl mb-3 group-hover:scale-110 transition-transform"} "➕"]
   [:span {:class "text-sm font-medium text-gray-600 dark:text-gray-400 group-hover:text-violet-600 dark:group-hover:text-violet-400"}
    "Add New Budget"]])

(defn grid-view []
  (let [budgets @(rf/subscribe [:budgets/filtered-budgets])]
    (if (empty? budgets)
      [:div {:class "text-center py-12 bg-white dark:bg-gray-800 rounded-xl border border-gray-200 dark:border-gray-700"}
       [:p {:class "text-gray-500 dark:text-gray-400"} "No budgets match your filters"]]
      [:div {:class "grid grid-cols-1 md:grid-cols-2 lg:grid-cols-3 gap-4"}
       (for [summary budgets]
         ^{:key (get-in summary [:budget :budget/id])}
         [budget-card summary])
       [add-budget-card]])))

;; Envelope View Components
(defn circular-progress-section []
  (let [total-budgeted @(rf/subscribe [:budgets/total-budgeted])
        total-spent @(rf/subscribe [:budgets/total-spent])
        percentage (if (pos? total-budgeted) (* 100 (/ total-spent total-budgeted)) 0)
        remaining (- total-budgeted total-spent)
        days-left @(rf/subscribe [:budgets/days-left-in-month])]
    [:div {:class "bg-white dark:bg-gray-800 rounded-2xl border border-gray-200 dark:border-gray-700 p-6 mb-6"}
     [:div {:class "flex items-center justify-between mb-6"}
      [:div
       [:h2 {:class "text-xl font-bold text-gray-900 dark:text-gray-50"} "October Budget"]
       [:p {:class "text-sm text-gray-600 dark:text-gray-400"}
        (str "You have " (currency/format-currency remaining :COP) " left to spend this month.")]]
      [:div {:class "text-right"}
       [:div {:class "text-xs text-gray-500 dark:text-gray-400 mb-1 uppercase tracking-wide font-semibold"} "Total Limit"]
       [:div {:class "text-lg font-bold text-gray-900 dark:text-gray-50"}
        (currency/format-currency total-budgeted :COP)]]]
     [:div {:class "flex items-center gap-8"}
      ;; Circular progress
      [:div {:class "relative w-32 h-32 flex-shrink-0"}
       [:svg {:viewBox "0 0 36 36" :class "w-full h-full transform -rotate-90"}
        [:path {:d "M18 2.0845 a 15.9155 15.9155 0 0 1 0 31.831 a 15.9155 15.9155 0 0 1 0 -31.831"
                :fill "none"
                :stroke "currentColor"
                :stroke-width "3"
                :class "text-gray-200 dark:text-gray-700"}]
        [:path {:d "M18 2.0845 a 15.9155 15.9155 0 0 1 0 31.831 a 15.9155 15.9155 0 0 1 0 -31.831"
                :fill "none"
                :stroke "currentColor"
                :stroke-width "3"
                :stroke-dasharray (str percentage ", 100")
                :class (cond
                         (> percentage 100) "text-red-500"
                         (> percentage 80) "text-amber-500"
                         :else "text-violet-500")}]]
       [:div {:class "absolute inset-0 flex items-center justify-center"}
        [:div {:class "text-center"}
         [:div {:class "text-2xl font-bold text-gray-900 dark:text-gray-50"}
          (str (.toFixed percentage 0) "%")]
         [:div {:class "text-xs text-gray-500 dark:text-gray-400"} "Utilized"]]]]
      ;; Stats
      [:div {:class "flex-1"}
       [:div {:class "grid grid-cols-2 gap-4"}
        [:div
         [:div {:class "text-xs text-gray-500 dark:text-gray-400 mb-1"} "65% Used"]
         [:div {:class "flex items-center gap-2"}
          [:div {:class "flex-1 h-2 bg-gray-200 dark:bg-gray-700 rounded-full overflow-hidden"}
           [:div {:class "h-full bg-violet-500" :style {:width "65%"}}]]
          [:span {:class "text-xs font-medium text-violet-600 dark:text-violet-400"} "65%"]]]
        [:div
         [:div {:class "text-xs text-gray-500 dark:text-gray-400 mb-1"}
          (str days-left " days left")]
         [:div {:class "text-sm font-semibold text-gray-900 dark:text-gray-50"}
          (str days-left " days")]]]]]]))

(defn envelope-card [{:keys [budget spent remaining percentage status]}]
  (let [cat (:budget/category budget)
        amount (:budget/amount budget)
        curr (:budget/currency budget)
        fill-percentage (min 100 (* 100 (or percentage 0)))]
    [:div {:class "bg-white dark:bg-gray-800 rounded-2xl p-6 border-2 border-gray-200 dark:border-gray-700 hover:shadow-lg transition-all"}
     [:div {:class "flex items-center justify-between mb-4"}
      [category-icon cat {:size :lg}]
      [:div {:class "text-right"}
       [:p {:class "text-xs text-gray-500 dark:text-gray-400"} "LIMIT"]
       [:p {:class "text-sm font-semibold text-gray-900 dark:text-gray-50"}
        (currency/format-currency amount curr)]]]
     ;; Visual envelope with fill
     [:div {:class "relative h-32 mb-4 bg-gray-100 dark:bg-gray-900 rounded-xl overflow-hidden"}
      [:div {:class (str "absolute bottom-0 left-0 right-0 transition-all duration-500 "
                         (progress-bar-color status))
             :style {:height (str fill-percentage "%")}}]
      [:div {:class "absolute inset-0 flex items-center justify-center"}
       [:div {:class "text-center"}
        [:p {:class "text-3xl font-bold text-gray-900 dark:text-gray-50"}
         (currency/format-currency (or spent 0) curr)]
        [:p {:class "text-xs text-gray-500 dark:text-gray-400 mt-1"} "Spent"]]]]
     ;; Category name
     [:p {:class "text-center text-sm font-medium text-gray-900 dark:text-gray-50"} (name cat)]]))

(defn quick-insights-panel []
  (let [top-saver @(rf/subscribe [:budgets/top-saver])
        watch-out @(rf/subscribe [:budgets/watch-out])]
    [:div {:class "space-y-4"}
     [:div {:class "bg-white dark:bg-gray-800 rounded-2xl border border-gray-200 dark:border-gray-700 p-5"}
      [:div {:class "flex items-center gap-2 mb-4"}
       [icon :info {:width 18 :height 18 :class "text-violet-500"}]
       [:h3 {:class "text-lg font-semibold text-gray-900 dark:text-gray-50"} "Quick Insights"]]
      [:div {:class "space-y-3"}
       (when top-saver
         [:div {:class "bg-green-50 dark:bg-green-900/20 rounded-xl p-4 border border-green-200 dark:border-green-800"}
          [:div {:class "flex items-start gap-3"}
           [:div {:class "flex-shrink-0"}
            [icon :thumbs-up {:width 20 :height 20 :class "text-green-600 dark:text-green-500"}]]
           [:div {:class "flex-1 min-w-0"}
            [:p {:class "text-xs font-semibold text-green-600 dark:text-green-500 uppercase tracking-wide mb-1"} "Top Saver"]
            [:p {:class "text-sm text-gray-900 dark:text-gray-50"}
             (str "On Track! You've saved "
                  (currency/format-currency (:remaining top-saver) (:budget/currency (:budget top-saver)))
                  " in ")
             [:strong (name (get-in top-saver [:budget :budget/category]))]
             " this month."]]]])
       (when watch-out
         [:div {:class "bg-amber-50 dark:bg-amber-900/20 rounded-xl p-4 border border-amber-200 dark:border-amber-800"}
          [:div {:class "flex items-start gap-3"}
           [:div {:class "flex-shrink-0"}
            [icon :alert-triangle {:width 20 :height 20 :class "text-amber-600 dark:text-amber-500"}]]
           [:div {:class "flex-1 min-w-0"}
            [:p {:class "text-xs font-semibold text-amber-600 dark:text-amber-500 uppercase tracking-wide mb-1"} "Watch Out"]
            [:p {:class "text-sm text-gray-900 dark:text-gray-50"}
             (str "Careful! You've used " (.toFixed (* 100 (:percentage watch-out)) 0) "% of your ")
             [:strong (name (get-in watch-out [:budget :budget/category]))]
             " budget."]]]])]]
     [:button {:class "w-full px-4 py-3 bg-violet-500 hover:bg-violet-600 text-white rounded-xl font-medium shadow-lg transition-all flex items-center justify-center gap-2"
               :on-click #(rf/dispatch [:budgets/open-panel :create])}
      [icon :plus {:width 18 :height 18}]
      [:span "Add Transaction"]]]))

(defn envelope-view []
  (let [budgets @(rf/subscribe [:budgets/filtered-budgets])]
    (if (empty? budgets)
      [:div {:class "text-center py-12 bg-white dark:bg-gray-800 rounded-xl border border-gray-200 dark:border-gray-700"}
       [:p {:class "text-gray-500 dark:text-gray-400"} "No budgets match your filters"]]
      [:div {:class "grid grid-cols-1 lg:grid-cols-3 gap-6"}
       ;; Main envelopes section (2 cols)
       [:div {:class "lg:col-span-2 space-y-4"}
        [circular-progress-section]
        [:div {:class "bg-white dark:bg-gray-800 rounded-2xl border border-gray-200 dark:border-gray-700 p-6"}
         [:div {:class "flex items-center justify-between mb-6"}
          [:h3 {:class "text-lg font-semibold text-gray-900 dark:text-gray-50"} "Envelope Categories"]
          [:button {:class "text-sm text-violet-600 dark:text-violet-400 hover:underline font-medium"
                    :on-click #(rf/dispatch [:budgets/open-panel :create])}
           "Manage Categories"]]
         [:div {:class "grid grid-cols-1 md:grid-cols-2 gap-4"}
          (for [summary budgets]
            ^{:key (get-in summary [:budget :budget/id])}
            [envelope-card summary])
          [add-budget-card]]]]
       ;; Quick Insights sidebar (1 col)
       [:div
        [quick-insights-panel]]])))

;; Main View
(defn budgets-view []
  (let [view-mode @(rf/subscribe [:budgets/view-mode])
        loading? @(rf/subscribe [:budgets/loading?])
        all-budgets @(rf/subscribe [:budgets/all-budgets])]
    [:<>
     [:div {:class "space-y-6"}
      ;; Page header
      [:div {:class "flex flex-col md:flex-row md:items-center justify-between gap-4"}
       [:div
        [:h1 {:class "text-2xl font-bold text-gray-900 dark:text-gray-50"} "Budgets"]
        [:p {:class "text-gray-600 dark:text-gray-400 text-sm mt-1"}
         "Manage your monthly spending limits and track your financial health"]]
       [:div {:class "flex items-center gap-3"}
        [:button {:class "px-4 py-2 bg-violet-500 hover:bg-violet-600 text-white rounded-xl font-medium shadow-lg transition-all flex items-center gap-2"
                  :on-click #(rf/dispatch [:budgets/open-panel :create])}
         [icon :plus {:width 18 :height 18}]
         [:span "Create New Budget"]]]]

      ;; Summary cards
      [summary-cards-row]

      ;; Toolbar with search, filters, and view toggle
      [:div {:class "flex flex-col md:flex-row items-stretch md:items-center justify-between gap-4"}
       [search-bar]
       [:div {:class "flex items-center gap-3"}
        (when (#{:grid :envelope} view-mode)
          [filter-tabs])
        [view-mode-toggle]]]

      ;; Main content area based on view mode
      (cond
        loading?
        [:div {:class "flex items-center justify-center py-16"}
         [:div {:class "animate-spin rounded-full h-12 w-12 border-b-2 border-violet-500"}]]

        (empty? all-budgets)
        [empty-state]

        :else
        [:div
         (case view-mode
           :table [table-view]
           :grid [grid-view]
           :envelope [envelope-view]
           [grid-view])])]

     ;; Slide-in panel (outside space-y-6)
     [budgets-panel]]))
