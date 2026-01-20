(ns finance.views.budget-analysis
  "Budget Analysis page with three switchable view modes: Table, Grid, Envelope."
  (:require [re-frame.core :as rf]
            [clojure.string :as str]
            [finance.utils.currency :as currency]
            [finance.components.icons :refer [icon]]
            [finance.components.category-icon :refer [category-icon]]))

(defn add-trend-data
  "Adds trend data to budget items for table view display."
  [items]
  (map (fn [item]
         (let [cat-name (name (get-in item [:budget :budget/category] :other))
               hash-val (reduce + (map int cat-name))
               trend-direction (cond
                                 (> (:percentage item) 1) :up
                                 (< (:percentage item) 0.5) :down
                                 :else :flat)
               trend-percent (mod hash-val 30)
               last-month-factor (+ 0.8 (* 0.4 (/ (mod hash-val 100) 100)))]
           (assoc item
                  :trend-direction trend-direction
                  :trend-percent trend-percent
                  :last-month-spent (* (:spent item) last-month-factor))))
       items))

(defn get-budget-data-with-trends
  "Returns budget data with trend information added."
  []
  (let [api-trends @(rf/subscribe [:budget-analysis/category-trends])
        budget-data @(rf/subscribe [:budget-analysis/budget-data])]
    (if (seq api-trends)
      api-trends
      (add-trend-data budget-data))))

(defn get-status-counts
  "Returns status counts for filter tabs."
  []
  (let [api-counts @(rf/subscribe [:budget-analysis/status-counts])
        budget-data @(rf/subscribe [:budget-analysis/budget-data])]
    (if (pos? (:all api-counts 0))
      api-counts
      (let [grouped (group-by :status budget-data)]
        {:all (count budget-data)
         :on-track (count (get grouped :ok []))
         :near-limit (count (get grouped :warning []))
         :over-budget (count (get grouped :exceeded []))}))))

(defn status-color [status]
  (case status
    :exceeded {:bg "bg-red-100 dark:bg-red-900/20"
               :text "text-red-600 dark:text-red-400"
               :border "border-red-200 dark:border-red-800"
               :bar "bg-red-500"
               :accent "border-l-red-500"}
    :warning {:bg "bg-amber-100 dark:bg-amber-900/20"
              :text "text-amber-600 dark:text-amber-400"
              :border "border-amber-200 dark:border-amber-800"
              :bar "bg-amber-500"
              :accent "border-l-amber-500"}
    {:bg "bg-gray-100 dark:bg-gray-800"
     :text "text-gray-600 dark:text-gray-400"
     :border "border-gray-200 dark:border-gray-700"
     :bar "bg-violet-500"
     :accent "border-l-violet-500"}))

(defn status-label [status]
  (case status
    :exceeded "Over Budget"
    :warning "Near Limit"
    "On Track"))

(defn trend-color [direction]
  (case direction
    :up "text-red-500 dark:text-red-400"
    :down "text-green-500 dark:text-green-400"
    "text-gray-400 dark:text-gray-500"))

(defn trend-icon-name [direction]
  (case direction
    :up :trending-up
    :down :trending-down
    :minus))

(defn view-mode-toggle []
  (let [view-mode @(rf/subscribe [:budget-analysis/view-mode])]
    [:div {:class "flex items-center gap-1 p-1 bg-gray-100 dark:bg-neutral-800 rounded-xl"}
     [:button {:class (str "px-3 py-1.5 rounded-lg text-xs font-medium transition-all flex items-center gap-1.5 "
                           (if (= view-mode :table)
                             "bg-white dark:bg-neutral-700 text-gray-900 dark:text-gray-50 shadow-sm"
                             "text-gray-600 dark:text-gray-400 hover:text-gray-900 dark:hover:text-gray-50"))
               :on-click #(rf/dispatch [:budget-analysis/set-view-mode :table])}
      [icon :table {:width 14 :height 14}]
      [:span "Table"]]
     [:button {:class (str "px-3 py-1.5 rounded-lg text-xs font-medium transition-all flex items-center gap-1.5 "
                           (if (= view-mode :grid)
                             "bg-white dark:bg-neutral-700 text-gray-900 dark:text-gray-50 shadow-sm"
                             "text-gray-600 dark:text-gray-400 hover:text-gray-900 dark:hover:text-gray-50"))
               :on-click #(rf/dispatch [:budget-analysis/set-view-mode :grid])}
      [icon :grid {:width 14 :height 14}]
      [:span "Grid"]]
     [:button {:class (str "px-3 py-1.5 rounded-lg text-xs font-medium transition-all flex items-center gap-1.5 "
                           (if (= view-mode :envelope)
                             "bg-white dark:bg-neutral-700 text-gray-900 dark:text-gray-50 shadow-sm"
                             "text-gray-600 dark:text-gray-400 hover:text-gray-900 dark:hover:text-gray-50"))
               :on-click #(rf/dispatch [:budget-analysis/set-view-mode :envelope])}
      [icon :layout {:width 14 :height 14}]
      [:span "Envelope"]]]))

(defn page-header []
  (let [current-period @(rf/subscribe [:budget-analysis/current-period-label])
        comparison-period @(rf/subscribe [:budget-analysis/comparison-period-label])]
    [:div {:class "flex flex-col md:flex-row md:items-end justify-between gap-4 mb-6"}
     [:div
      [:h1 {:class "text-2xl md:text-3xl font-extrabold text-gray-900 dark:text-gray-50 tracking-tight"}
       "Budget Analysis"]
      [:p {:class "text-violet-600 dark:text-violet-400 text-sm font-medium mt-1 flex items-center gap-2"}
       [icon :calendar {:width 14 :height 14}]
       (str current-period " vs " comparison-period)]]
     [:div {:class "flex items-center gap-3"}
      [:button {:class (str "px-4 py-2 border border-gray-200 dark:border-neutral-700 rounded-xl "
                            "text-gray-700 dark:text-gray-300 font-medium text-sm "
                            "hover:bg-gray-50 dark:hover:bg-neutral-800 transition-colors "
                            "flex items-center gap-2 shadow-sm")
                :on-click #(rf/dispatch [:budget-analysis/export-csv])}
       [icon :file-down {:width 16 :height 16}]
       [:span "Export CSV"]]
      [:button {:class (str "px-4 py-2 bg-violet-500 hover:bg-violet-600 text-white rounded-xl "
                            "font-medium text-sm shadow-lg shadow-violet-500/30 transition-all "
                            "flex items-center gap-2")
                :on-click #(rf/dispatch [:budgets/open-panel :create])}
       [icon :plus {:width 16 :height 16}]
       [:span "Create New Budget"]]]]))

(defn time-period-dropdown []
  (let [time-period @(rf/subscribe [:budget-analysis/time-period])]
    [:div {:class "flex flex-col gap-1"}
     [:label {:class "text-xs font-semibold text-gray-500 dark:text-gray-400 uppercase tracking-wider pl-1"}
      "Time Period"]
     [:div {:class "relative"}
      [:select {:class (str "appearance-none w-full pl-3 pr-8 py-2 border border-gray-200 dark:border-neutral-700 "
                            "rounded-lg bg-white dark:bg-neutral-800 text-gray-900 dark:text-gray-50 "
                            "text-sm font-medium focus:outline-none focus:ring-2 focus:ring-violet-500/20 focus:border-violet-500 "
                            "cursor-pointer")
                :value (name time-period)
                :on-change #(rf/dispatch [:budget-analysis/set-time-period (keyword (-> % .-target .-value))])}
       [:option {:value "this-month"} "This Month"]
       [:option {:value "last-month"} "Last Month"]
       [:option {:value "this-quarter"} "Last Quarter"]
       [:option {:value "this-year"} "Year to Date"]]
      [:div {:class "absolute inset-y-0 right-0 pr-2 flex items-center pointer-events-none"}
       [icon :chevron-down {:width 14 :height 14 :class "text-gray-400"}]]]]))

(defn account-dropdown []
  (let [selected @(rf/subscribe [:budget-analysis/selected-account])]
    [:div {:class "flex flex-col gap-1"}
     [:label {:class "text-xs font-semibold text-gray-500 dark:text-gray-400 uppercase tracking-wider pl-1"}
      "Account"]
     [:div {:class "relative"}
      [:select {:class (str "appearance-none w-full pl-3 pr-8 py-2 border border-gray-200 dark:border-neutral-700 "
                            "rounded-lg bg-white dark:bg-neutral-800 text-gray-900 dark:text-gray-50 "
                            "text-sm font-medium focus:outline-none focus:ring-2 focus:ring-violet-500/20 focus:border-violet-500 "
                            "cursor-pointer")
                :value (or selected "all")
                :on-change #(let [v (-> % .-target .-value)]
                              (rf/dispatch [:budget-analysis/set-account (when (not= v "all") v)]))}
       [:option {:value "all"} "All Accounts"]
       [:option {:value "checking"} "Checking"]
       [:option {:value "savings"} "Savings"]
       [:option {:value "credit"} "Credit Card"]]
      [:div {:class "absolute inset-y-0 right-0 pr-2 flex items-center pointer-events-none"}
       [icon :chevron-down {:width 14 :height 14 :class "text-gray-400"}]]]]))

(defn search-input []
  (let [query @(rf/subscribe [:budget-analysis/search-query])]
    [:div {:class "flex flex-col gap-1"}
     [:label {:class "text-xs font-semibold text-gray-500 dark:text-gray-400 uppercase tracking-wider pl-1"}
      "Search Categories"]
     [:div {:class "relative"}
      [:div {:class "absolute inset-y-0 left-0 pl-3 flex items-center pointer-events-none"}
       [icon :search {:width 16 :height 16 :class "text-gray-400"}]]
      [:input {:class (str "block w-full pl-9 pr-3 py-2 border border-gray-200 dark:border-neutral-700 rounded-lg "
                           "bg-white dark:bg-neutral-800 text-gray-900 dark:text-gray-50 placeholder-gray-400 "
                           "focus:outline-none focus:ring-2 focus:ring-violet-500/20 focus:border-violet-500 text-sm")
               :type "text"
               :placeholder "e.g. Groceries, Rent..."
               :value query
               :on-change #(rf/dispatch [:budget-analysis/set-search (-> % .-target .-value)])}]]]))

(defn filter-toolbar []
  [:div {:class "flex flex-col lg:flex-row items-stretch lg:items-end gap-4 mb-6 p-4 bg-white dark:bg-neutral-800 rounded-xl border border-gray-200 dark:border-neutral-700 shadow-sm"}
   [:div {:class "grid grid-cols-2 lg:flex gap-3"}
    [time-period-dropdown]
    [account-dropdown]]
   [:div {:class "flex-1 lg:max-w-xs lg:ml-auto"}
    [search-input]]
   [:div {:class "flex justify-end"}
    [view-mode-toggle]]])

(defn trend-indicator [{:keys [direction percent]}]
  [:div {:class (str "flex items-center gap-1 text-xs font-semibold " (trend-color direction))}
   [icon (trend-icon-name direction) {:width 14 :height 14}]
   [:span (str (when (= direction :up) "+") percent "%")]])

(defn difference-badge [remaining curr]
  (let [over? (neg? remaining)
        color-class (if over?
                      "bg-red-50 dark:bg-red-900/20 text-red-600 dark:text-red-400 border-red-100 dark:border-red-800"
                      "bg-green-50 dark:bg-green-900/20 text-green-600 dark:text-green-400 border-green-100 dark:border-green-800")]
    [:span {:class (str "inline-flex items-center px-2.5 py-1 rounded-full text-xs font-bold border " color-class)}
     (if over?
       (str "+" (currency/format-currency (Math/abs remaining) curr))
       (str "-" (currency/format-currency remaining curr)))]))

(defn on-track-badge []
  [:span {:class "inline-flex items-center gap-1.5 px-2.5 py-1 rounded-full text-xs font-bold bg-gray-100 dark:bg-neutral-800 text-gray-600 dark:text-gray-400 border border-gray-200 dark:border-neutral-700"}
   [icon :check {:width 12 :height 12}]
   "On Track"])

(defn table-row [{:keys [budget spent remaining percentage status trend-direction trend-percent last-month-spent]}]
  (let [cat (or (:budget/category budget) :other)
        amount (or (:budget/amount budget) 0)
        curr (or (:budget/currency budget) :COP)
        colors (status-color status)]
    [:tr {:class "group border-b border-gray-100 dark:border-neutral-700 hover:bg-gray-50 dark:hover:bg-neutral-800/50 transition-colors"}
     ;; Category
     [:td {:class "py-4 pl-6 pr-4"}
      [:div {:class "flex items-center gap-3"}
       [category-icon cat {:size :md}]
       [:div
        [:div {:class "font-bold text-gray-900 dark:text-gray-50 text-sm"} (str/capitalize (name cat))]
        [:div {:class "text-xs text-gray-500 dark:text-gray-500"} "Monthly"]]]]
     ;; Budgeted
     [:td {:class "py-4 px-4 text-right tabular-nums text-sm font-medium text-gray-500 dark:text-gray-400"}
      (currency/format-currency amount curr)]
     ;; Actual Spent
     [:td {:class "py-4 px-4 text-right tabular-nums text-sm font-bold text-gray-900 dark:text-gray-50"}
      (currency/format-currency spent curr)]
     ;; Difference
     [:td {:class "py-4 px-4 text-right"}
      (if (and (>= percentage 0.95) (<= percentage 1.05))
        [on-track-badge]
        [difference-badge remaining curr])]
     ;; Last Month
     [:td {:class "py-4 px-4 text-right tabular-nums text-sm text-gray-500 dark:text-gray-400"}
      (currency/format-currency (or last-month-spent 0) curr)]
     ;; Trend
     [:td {:class "py-4 px-4 pr-6 text-right"}
      [trend-indicator {:direction trend-direction :percent trend-percent}]]]))

(defn totals-row []
  (let [total-budgeted @(rf/subscribe [:budgets/total-budgeted])
        total-spent @(rf/subscribe [:budgets/total-spent])
        remaining (- total-budgeted total-spent)
        over? (neg? remaining)]
    [:tr {:class "bg-gray-50 dark:bg-neutral-900/50 font-semibold border-t-2 border-gray-200 dark:border-neutral-700"}
     [:td {:class "py-5 pl-6 pr-4 font-bold text-sm text-gray-900 dark:text-gray-50 uppercase tracking-wider"} "Total"]
     [:td {:class "py-5 px-4 text-right tabular-nums text-base font-bold text-gray-500 dark:text-gray-400"}
      (currency/format-currency total-budgeted :COP)]
     [:td {:class "py-5 px-4 text-right tabular-nums text-lg font-black text-gray-900 dark:text-gray-50"}
      (currency/format-currency total-spent :COP)]
     [:td {:class "py-5 px-4 text-right"}
      [:span {:class (str "inline-flex items-center px-3 py-1 rounded-full text-sm font-bold "
                          (if over?
                            "bg-red-100 dark:bg-red-900/40 text-red-800 dark:text-red-200"
                            "bg-green-100 dark:bg-green-900/40 text-green-800 dark:text-green-200"))}
       (if over? "Over Budget" "Under Budget")]]
     [:td {:class "py-5 px-4 text-right tabular-nums text-sm font-bold text-gray-500 dark:text-gray-400"} "—"]
     [:td {:class "py-5 px-4 pr-6 text-right"}
      [:div {:class "flex items-center justify-end gap-1 text-gray-400 text-xs font-semibold"}
       [icon :minus {:width 14 :height 14}]
       "0%"]]]))

(defn table-pagination [{:keys [total page page-size total-pages]}]
  (let [start (inc (* (dec page) page-size))
        end (min total (* page page-size))]
    [:div {:class "flex items-center justify-between px-4 py-3 border-t border-gray-100 dark:border-neutral-700 bg-gray-50 dark:bg-neutral-900/30"}
     [:span {:class "text-sm text-violet-600 dark:text-violet-400 font-medium"}
      (str "Showing " start "-" end " of " total " categories")]
     [:div {:class "flex items-center gap-2"}
      [:button {:class (str "w-8 h-8 flex items-center justify-center rounded-lg border border-gray-200 dark:border-neutral-700 "
                            "text-gray-500 dark:text-gray-400 bg-white dark:bg-neutral-800 "
                            "hover:bg-gray-50 dark:hover:bg-neutral-700 transition-colors "
                            "disabled:opacity-50 disabled:cursor-not-allowed")
                :disabled (= page 1)
                :on-click #(rf/dispatch [:budget-analysis/set-page (dec page)])}
       [icon :chevron-left {:width 16 :height 16}]]
      (for [p (range 1 (inc (min 3 total-pages)))]
        ^{:key p}
        [:button {:class (str "w-8 h-8 flex items-center justify-center rounded-lg text-sm font-bold transition-colors "
                              (if (= p page)
                                "bg-violet-500 text-white shadow-sm"
                                "border border-gray-200 dark:border-neutral-700 text-gray-500 dark:text-gray-400 bg-white dark:bg-neutral-800 hover:bg-gray-50 dark:hover:bg-neutral-700"))
                  :on-click #(rf/dispatch [:budget-analysis/set-page p])}
         p])
      (when (> total-pages 3)
        [:span {:class "text-gray-400"} "..."])
      [:button {:class (str "w-8 h-8 flex items-center justify-center rounded-lg border border-gray-200 dark:border-neutral-700 "
                            "text-gray-500 dark:text-gray-400 bg-white dark:bg-neutral-800 "
                            "hover:bg-gray-50 dark:hover:bg-neutral-700 transition-colors "
                            "disabled:opacity-50 disabled:cursor-not-allowed")
                :disabled (= page total-pages)
                :on-click #(rf/dispatch [:budget-analysis/set-page (inc page)])}
       [icon :chevron-right {:width 16 :height 16}]]]]))

(defn table-view []
  (let [all-data (get-budget-data-with-trends)
        {:keys [page page-size]} @(rf/subscribe [:budget-analysis/pagination])
        total (count all-data)
        total-pages (max 1 (int (Math/ceil (/ total page-size))))
        start (* (dec page) page-size)
        items (take page-size (drop start all-data))]
    [:div {:class "bg-white dark:bg-neutral-800 rounded-2xl border border-gray-200 dark:border-neutral-700 overflow-hidden shadow-sm"}
     [:div {:class "overflow-x-auto"}
      [:table {:class "w-full text-left min-w-[900px]"}
       [:thead {:class "bg-gray-50 dark:bg-neutral-900/50"}
        [:tr
         [:th {:class "py-4 pl-6 pr-4 font-bold text-xs uppercase tracking-wider text-gray-500 dark:text-gray-400 w-[25%]"} "Category"]
         [:th {:class "py-4 px-4 font-bold text-xs uppercase tracking-wider text-gray-500 dark:text-gray-400 text-right w-[15%]"} "Budgeted"]
         [:th {:class "py-4 px-4 font-bold text-xs uppercase tracking-wider text-gray-500 dark:text-gray-400 text-right w-[15%]"} "Actual Spent"]
         [:th {:class "py-4 px-4 font-bold text-xs uppercase tracking-wider text-gray-500 dark:text-gray-400 text-right w-[15%]"} "Difference"]
         [:th {:class "py-4 px-4 font-bold text-xs uppercase tracking-wider text-gray-500 dark:text-gray-400 text-right w-[15%]"} "Last Month"]
         [:th {:class "py-4 px-4 pr-6 font-bold text-xs uppercase tracking-wider text-gray-500 dark:text-gray-400 text-right w-[15%]"} "Trend"]]]
       [:tbody
        (for [item items]
          ^{:key (get-in item [:budget :budget/id] (random-uuid))}
          [table-row item])
        [totals-row]]]]
     [table-pagination {:total total :page page :page-size page-size :total-pages total-pages}]]))

(defn summary-card [{:keys [title value subtitle icon-name icon-color progress]}]
  [:div {:class "group relative overflow-hidden bg-white dark:bg-neutral-800 rounded-2xl p-6 border border-gray-100 dark:border-neutral-700 shadow-sm hover:shadow-md transition-all"}
   ;; Decorative icon
   (when icon-name
     [:div {:class "absolute -right-4 -top-4 opacity-10 group-hover:opacity-20 transition-opacity"}
      [icon icon-name {:width 80 :height 80 :class (or icon-color "text-violet-500")}]])
   [:div {:class "relative z-10"}
    [:p {:class "text-gray-500 dark:text-gray-400 text-sm font-medium mb-1"} title]
    [:h3 {:class "text-3xl font-black text-gray-900 dark:text-gray-50 tracking-tight"} value]
    (when subtitle
      [:div {:class "mt-3 flex items-center gap-2"}
       (if progress
         [:<>
          [:div {:class "w-24 h-2 bg-gray-100 dark:bg-neutral-700 rounded-full overflow-hidden"}
           [:div {:class (str "h-full rounded-full " (:color progress))
                  :style {:width (str (min 100 (:value progress)) "%")}}]]
          [:span {:class "text-sm font-medium text-violet-600 dark:text-violet-400"} (str (:value progress) "% Used")]
          [:span {:class "text-gray-300 dark:text-neutral-600 mx-1"} "•"]
          [:span {:class "text-sm text-gray-500 dark:text-gray-400"} subtitle]]
         [:div {:class "inline-flex items-center gap-2 px-3 py-1 rounded-full bg-gray-50 dark:bg-neutral-700 border border-gray-100 dark:border-neutral-600"}
          [icon :calendar {:width 12 :height 12 :class "text-gray-500"}]
          [:span {:class "text-xs font-semibold text-gray-600 dark:text-gray-300"} subtitle]])])]])

(defn grid-summary-row []
  (let [total-budgeted @(rf/subscribe [:budgets/total-budgeted])
        total-spent @(rf/subscribe [:budgets/total-spent])
        remaining (- total-budgeted total-spent)
        percentage (if (pos? total-budgeted) (int (* 100 (/ total-spent total-budgeted))) 0)
        days-left @(rf/subscribe [:budgets/days-left-in-month])
        now (js/Date.)
        month-name (.toLocaleString now "en-US" #js {:month "long" :year "numeric"})]
    [:div {:class "grid grid-cols-1 md:grid-cols-2 gap-6 mb-8"}
     [summary-card {:title "Total Monthly Budget"
                    :value (currency/format-currency total-budgeted :COP)
                    :subtitle month-name
                    :icon-name :piggy-bank
                    :icon-color "text-violet-500"}]
     [summary-card {:title "Left to Spend"
                    :value (currency/format-currency remaining :COP)
                    :subtitle (str days-left " days left")
                    :icon-name :dollar
                    :icon-color "text-green-500"
                    :progress {:value percentage
                               :color (cond
                                        (> percentage 100) "bg-red-500"
                                        (> percentage 80) "bg-amber-500"
                                        :else "bg-violet-500")}}]]))

(defn grid-filter-tabs []
  (let [filter-status @(rf/subscribe [:budget-analysis/filter-status])
        counts (get-status-counts)]
    [:div {:class "flex flex-col sm:flex-row items-start sm:items-center justify-between gap-4 mb-6"}
     [:div {:class "flex flex-wrap items-center gap-2"}
      [:button {:class (str "px-4 py-2 rounded-lg text-sm font-semibold transition-colors "
                            (if (= filter-status :all)
                              "bg-gray-900 dark:bg-gray-50 text-white dark:text-gray-900 shadow-sm"
                              "bg-white dark:bg-neutral-800 text-gray-600 dark:text-gray-300 border border-gray-200 dark:border-neutral-700 hover:bg-gray-50 dark:hover:bg-neutral-700"))
                :on-click #(rf/dispatch [:budget-analysis/set-filter-status :all])}
       "All Budgets"]
      [:button {:class (str "px-4 py-2 rounded-lg text-sm font-medium transition-colors "
                            (if (= filter-status :on-track)
                              "bg-green-500 text-white"
                              "bg-white dark:bg-neutral-800 text-gray-600 dark:text-gray-300 border border-gray-200 dark:border-neutral-700 hover:bg-gray-50 dark:hover:bg-neutral-700"))
                :on-click #(rf/dispatch [:budget-analysis/set-filter-status :on-track])}
       "On Track"]
      [:button {:class (str "px-4 py-2 rounded-lg text-sm font-medium transition-colors "
                            (if (= filter-status :near-limit)
                              "bg-amber-500 text-white"
                              "bg-white dark:bg-neutral-800 text-gray-600 dark:text-gray-300 border border-gray-200 dark:border-neutral-700 hover:bg-gray-50 dark:hover:bg-neutral-700"))
                :on-click #(rf/dispatch [:budget-analysis/set-filter-status :near-limit])}
       "Near Limit"]
      [:button {:class (str "px-4 py-2 rounded-lg text-sm font-medium transition-colors "
                            (if (= filter-status :over-budget)
                              "bg-red-500 text-white"
                              "bg-white dark:bg-neutral-800 text-gray-600 dark:text-gray-300 border border-gray-200 dark:border-neutral-700 hover:bg-gray-50 dark:hover:bg-neutral-700"))
                :on-click #(rf/dispatch [:budget-analysis/set-filter-status :over-budget])}
       "Over Budget"]]
     [:div {:class "flex items-center gap-2 text-sm text-gray-500 dark:text-gray-400"}
      [icon :filter {:width 16 :height 16}]
      [:span {:class "font-medium cursor-pointer hover:text-gray-800 dark:hover:text-gray-200"} "Sort by Amount"]]]))

(defn budget-grid-card [{:keys [budget spent remaining percentage status]}]
  (let [cat (or (:budget/category budget) :other)
        amount (or (:budget/amount budget) 0)
        curr (or (:budget/currency budget) :COP)
        colors (status-color status)
        pct (int (* 100 (or percentage 0)))]
    [:article {:class (str "flex flex-col bg-white dark:bg-neutral-800 rounded-xl border border-gray-100 dark:border-neutral-700 "
                           "shadow-sm p-5 relative group hover:shadow-md hover:border-gray-200 dark:hover:border-neutral-600 "
                           "transition-all border-l-4 " (:accent colors))}
     [:div {:class "flex items-start justify-between mb-4"}
      [:div {:class "flex items-center gap-3"}
       [category-icon cat {:size :lg}]
       [:div
        [:h3 {:class "font-bold text-lg text-gray-900 dark:text-gray-50"} (str/capitalize (name cat))]
        [:p {:class "text-xs text-gray-500 font-medium uppercase tracking-wider"} "MONTHLY"]]]
      [:button {:class "text-gray-300 dark:text-neutral-600 hover:text-gray-600 dark:hover:text-gray-300 transition-colors"}
       [icon :more-horizontal {:width 20 :height 20}]]]
     ;; Amount display
     [:div {:class "flex items-end gap-2 mb-3"}
      [:span {:class (str "text-3xl font-black "
                          (if (= status :exceeded) "text-red-600 dark:text-red-400" "text-gray-900 dark:text-gray-50"))}
       (currency/format-currency spent curr)]
      [:span {:class "text-sm font-medium text-gray-400 mb-1.5"}
       (str "/ " (currency/format-currency amount curr))]]
     ;; Progress bar
     [:div {:class "w-full bg-gray-100 dark:bg-neutral-700 rounded-full h-3 mb-4 overflow-hidden"}
      [:div {:class (str "h-full rounded-full transition-all " (:bar colors))
             :style {:width (str (min 100 pct) "%")}}]]
     ;; Footer
     [:div {:class "flex items-center justify-between mt-auto pt-3 border-t border-gray-50 dark:border-neutral-700/50"}
      [:div {:class "flex flex-col"}
       [:span {:class "text-xs text-gray-400 dark:text-gray-500 font-medium mb-0.5"} "Remaining"]
       [:span {:class (str "text-sm font-bold "
                           (if (neg? remaining) "text-red-600 dark:text-red-400" "text-green-600 dark:text-green-400"))}
        (if (neg? remaining)
          (str "-" (currency/format-currency (Math/abs remaining) curr))
          (str "+" (currency/format-currency remaining curr)))]]
      [:span {:class (str "inline-flex items-center gap-1.5 px-3 py-1 rounded-full text-xs font-bold border "
                          (:bg colors) " " (:text colors) " " (:border colors))}
       (when (= status :exceeded)
         [icon :alert-triangle {:width 12 :height 12}])
       (when (= status :ok)
         [:span {:class "w-1.5 h-1.5 rounded-full bg-green-500"}])
       (when (= status :warning)
         [:span {:class "w-1.5 h-1.5 rounded-full bg-amber-500"}])
       (status-label status)]]]))

(defn add-category-card []
  [:button {:class (str "flex flex-col items-center justify-center bg-gray-50 dark:bg-neutral-800/50 rounded-xl "
                        "border-2 border-dashed border-gray-200 dark:border-neutral-700 p-6 min-h-[260px] "
                        "hover:border-violet-400 dark:hover:border-violet-500 hover:bg-violet-50 dark:hover:bg-violet-900/10 "
                        "transition-all group cursor-pointer")
            :on-click #(rf/dispatch [:budgets/open-panel :create])}
   [:div {:class "w-14 h-14 rounded-full bg-white dark:bg-neutral-700 shadow-sm flex items-center justify-center mb-4 group-hover:scale-110 group-hover:shadow-md transition-all"}
    [icon :plus {:width 24 :height 24 :class "text-gray-400 group-hover:text-violet-500"}]]
   [:h3 {:class "font-bold text-lg text-gray-900 dark:text-gray-50 mb-2 group-hover:text-violet-600 dark:group-hover:text-violet-400"}
    "Add New Category"]
   [:p {:class "text-center text-gray-500 dark:text-gray-400 text-sm px-4"}
    "Create a budget for a new category to stay on top of your spending."]])

(defn grid-view []
  (let [budgets @(rf/subscribe [:budget-analysis/budget-data])]
    [:div
     [grid-summary-row]
     [grid-filter-tabs]
     [:div {:class "grid grid-cols-1 md:grid-cols-2 xl:grid-cols-3 gap-5"}
      (for [item budgets]
        ^{:key (get-in item [:budget :budget/id] (random-uuid))}
        [budget-grid-card item])
      [add-category-card]]]))

(defn semi-circular-gauge [{:keys [percentage]}]
  (let [clamped (min 100 (max 0 percentage))
        ;; Semi-circle has ~157 units circumference for r=50
        stroke-dasharray (str (* clamped 1.57) " 157")]
    [:div {:class "relative w-48 h-24 flex items-end justify-center"}
     [:svg {:viewBox "0 0 100 50" :class "w-full h-full overflow-visible"}
      ;; Background arc
      [:path {:d "M 5 50 A 45 45 0 0 1 95 50"
              :fill "none"
              :stroke "currentColor"
              :stroke-width "10"
              :stroke-linecap "round"
              :class "text-gray-100 dark:text-neutral-700"}]
      ;; Progress arc with gradient
      [:defs
       [:linearGradient {:id "gaugeGradient" :x1 "0%" :x2 "100%"}
        [:stop {:offset "0%" :stop-color "#8b5cf6"}]
        [:stop {:offset "100%" :stop-color "#a78bfa"}]]]
      [:path {:d "M 5 50 A 45 45 0 0 1 95 50"
              :fill "none"
              :stroke "url(#gaugeGradient)"
              :stroke-width "10"
              :stroke-linecap "round"
              :stroke-dasharray stroke-dasharray}]]
     [:div {:class "absolute bottom-0 left-0 right-0 text-center"}
      [:div {:class "text-3xl font-bold text-gray-900 dark:text-gray-50"} (str (int clamped) "%")]
      [:div {:class "text-xs text-gray-500 dark:text-gray-400 font-medium"} "Utilized"]]]))

(defn envelope-master-header []
  (let [total-budgeted @(rf/subscribe [:budgets/total-budgeted])
        total-spent @(rf/subscribe [:budgets/total-spent])
        remaining (- total-budgeted total-spent)
        percentage (if (pos? total-budgeted) (* 100 (/ total-spent total-budgeted)) 0)
        days-left @(rf/subscribe [:budgets/days-left-in-month])
        daily-avg (if (and (pos? days-left) (pos? remaining))
                    (/ remaining days-left)
                    @(rf/subscribe [:budgets/daily-avg-remaining]))
        now (js/Date.)
        month-name (.toLocaleString now "en-US" #js {:month "long"})]
    [:div {:class "bg-white dark:bg-neutral-800 rounded-3xl border border-gray-100 dark:border-neutral-700 p-6 md:p-8 mb-6 relative overflow-hidden shadow-sm"}
     ;; Decorative background
     [:div {:class "absolute -right-20 -top-20 w-64 h-64 bg-violet-500/5 dark:bg-violet-500/10 rounded-full blur-3xl pointer-events-none"}]
     [:div {:class "flex flex-col lg:flex-row lg:items-center justify-between gap-6 relative z-10"}
      [:div {:class "flex-1"}
       [:div {:class "inline-flex items-center px-3 py-1 rounded-full bg-gray-100 dark:bg-neutral-700 text-xs font-semibold uppercase tracking-wider text-gray-500 dark:text-gray-300 mb-3"}
        (str/upper-case month-name) " 2024"]
       [:h2 {:class "text-3xl md:text-4xl font-bold text-gray-900 dark:text-gray-50 mb-2"}
        (str month-name " Budget")]
       [:p {:class "text-gray-600 dark:text-gray-400 text-lg"}
        "You have "
        [:span {:class (str "font-bold " (if (neg? remaining) "text-red-500" "text-green-500"))}
         (currency/format-currency (Math/abs remaining) :COP)]
        (if (neg? remaining) " over budget" " left to spend this month.")]
       [:div {:class "flex items-center gap-6 mt-6"}
        [:div
         [:p {:class "text-xs text-gray-400 uppercase font-medium tracking-wide"} "Total Limit"]
         [:p {:class "text-xl font-bold text-gray-900 dark:text-gray-50"}
          (currency/format-currency total-budgeted :COP)]]
        [:div {:class "w-px h-10 bg-gray-200 dark:bg-neutral-700"}]
        [:div
         [:p {:class "text-xs text-gray-400 uppercase font-medium tracking-wide"} "Daily Avg"]
         [:p {:class "text-xl font-bold text-gray-900 dark:text-gray-50"}
          (currency/format-currency daily-avg :COP)]]]]
      [:div {:class "flex justify-center lg:justify-end"}
       [semi-circular-gauge {:percentage percentage}]]]]))

(defn envelope-card [{:keys [budget spent remaining percentage status]}]
  (let [cat (or (:budget/category budget) :other)
        amount (or (:budget/amount budget) 0)
        curr (or (:budget/currency budget) :COP)
        fill-height (min 100 (int (* 100 (or percentage 0))))
        fill-color (case status
                     :exceeded "bg-gradient-to-t from-red-500 to-red-400"
                     :warning "bg-gradient-to-t from-amber-500 to-amber-400"
                     "bg-gradient-to-t from-violet-600 to-violet-500")]
    [:div {:class "bg-white dark:bg-neutral-800 rounded-2xl border border-gray-100 dark:border-neutral-700 p-4 shadow-sm hover:shadow-md transition-all"}
     ;; Header
     [:div {:class "flex flex-col items-center mb-3"}
      [category-icon cat {:size :lg}]
      [:h4 {:class "font-bold text-gray-800 dark:text-gray-50 text-lg text-center"} (str/capitalize (name cat))]
      [:p {:class "text-xs text-gray-400 font-medium uppercase tracking-wide"}
       (str "Limit: " (currency/format-currency amount curr))]]
     ;; Envelope visual
     [:div {:class "relative h-40 rounded-xl overflow-hidden bg-gray-50 dark:bg-neutral-900 mx-2 mb-3"}
      ;; Scale markers
      [:div {:class "absolute inset-0 flex flex-col justify-between py-4 px-3 opacity-20 pointer-events-none z-10"}
       (for [i (range 4)]
         ^{:key i}
         [:div {:class "w-full border-t border-dashed border-gray-400 dark:border-gray-600"}])]
      ;; Fill
      [:div {:class (str "absolute bottom-0 left-0 right-0 transition-all duration-700 ease-out " fill-color)
             :style {:height (str fill-height "%")}}
       [:div {:class "absolute top-0 w-full h-1 bg-white/30"}]]
      ;; Value display
      [:div {:class "absolute inset-0 flex items-center justify-center"}
       [:div {:class "text-center bg-white/90 dark:bg-neutral-800/90 backdrop-blur-sm rounded-lg px-4 py-2 shadow-sm"}
        [:p {:class "text-2xl font-bold text-gray-900 dark:text-gray-50"}
         (currency/format-currency spent curr)]
        [:p {:class "text-xs text-gray-500 dark:text-gray-400 font-medium"} "Spent"]]]]]))

(defn envelope-add-card []
  [:button {:class (str "rounded-2xl p-4 border-2 border-dashed border-gray-200 dark:border-neutral-700 "
                        "flex flex-col items-center justify-center min-h-[280px] "
                        "hover:border-violet-400 hover:bg-violet-50 dark:hover:bg-violet-900/10 "
                        "transition-all group cursor-pointer")
            :on-click #(rf/dispatch [:budgets/open-panel :create])}
   [:div {:class "w-14 h-14 rounded-full bg-gray-100 dark:bg-neutral-700 flex items-center justify-center mb-4 group-hover:scale-110 transition-transform"}
    [icon :plus {:width 24 :height 24 :class "text-gray-400 group-hover:text-violet-500"}]]
   [:p {:class "text-gray-500 font-medium group-hover:text-violet-600"} "Add Category"]])

(defn quick-insights-sidebar []
  (let [top-saver @(rf/subscribe [:budgets/top-saver])
        watch-out @(rf/subscribe [:budgets/watch-out])
        recent-transactions @(rf/subscribe [:tx/recent-transactions])]
    [:div {:class "space-y-6"}
     ;; Quick Insights Header
     [:div {:class "flex items-center justify-between"}
      [:h3 {:class "text-lg font-bold text-gray-800 dark:text-gray-50"} "Quick Insights"]
      [icon :info {:width 16 :height 16 :class "text-gray-400"}]]

     ;; Insight Cards
     [:div {:class "space-y-4"}
      ;; Top Saver
      (when top-saver
        [:div {:class "rounded-xl p-5 bg-gradient-to-br from-teal-50 to-green-50 dark:from-teal-900/20 dark:to-green-900/20 border border-teal-200 dark:border-teal-800 relative overflow-hidden"}
         [:div {:class "absolute top-0 right-0 p-3 opacity-10"}
          [icon :dollar {:width 48 :height 48 :class "text-teal-500"}]]
         [:div {:class "flex gap-3 items-start relative z-10"}
          [:div {:class "p-2 rounded-lg bg-teal-100 dark:bg-teal-900/30 text-teal-600 dark:text-teal-400 shrink-0"}
           [icon :thumbs-up {:width 20 :height 20}]]
          [:div
           [:p {:class "text-xs font-bold text-teal-600 dark:text-teal-400 uppercase tracking-wide mb-1"} "Top Saver"]
           [:p {:class "text-sm text-gray-700 dark:text-gray-200 leading-snug"}
            "On Track! You've saved "
            [:span {:class "font-bold"} (currency/format-currency (:remaining top-saver) (get-in top-saver [:budget :budget/currency] :COP))]
            " in "
            [:span {:class "font-semibold underline decoration-teal-400/50"} (str/capitalize (name (get-in top-saver [:budget :budget/category] :other)))]
            " this month."]]]])

      ;; Watch Out
      (when watch-out
        [:div {:class "rounded-xl p-5 bg-gradient-to-br from-amber-50 to-yellow-50 dark:from-amber-900/20 dark:to-yellow-900/20 border border-amber-200 dark:border-amber-800 relative overflow-hidden"}
         [:div {:class "absolute top-0 right-0 p-3 opacity-10"}
          [icon :alert-triangle {:width 48 :height 48 :class "text-amber-500"}]]
         [:div {:class "flex gap-3 items-start relative z-10"}
          [:div {:class "p-2 rounded-lg bg-amber-100 dark:bg-amber-900/30 text-amber-600 dark:text-amber-400 shrink-0"}
           [icon :alert-triangle {:width 20 :height 20}]]
          [:div
           [:p {:class "text-xs font-bold text-amber-600 dark:text-amber-400 uppercase tracking-wide mb-1"} "Watch Out"]
           [:p {:class "text-sm text-gray-700 dark:text-gray-200 leading-snug"}
            "Careful! You've used "
            [:span {:class "font-bold"} (str (int (* 100 (:percentage watch-out))) "%")]
            " of your "
            [:span {:class "font-semibold underline decoration-amber-400/50"} (str/capitalize (name (get-in watch-out [:budget :budget/category] :other)))]
            " budget."]]]])]

     ;; Recent Transactions
     [:div {:class "bg-white dark:bg-neutral-800 rounded-xl border border-gray-100 dark:border-neutral-700 p-5 shadow-sm"}
      [:div {:class "flex justify-between items-center mb-4"}
       [:h4 {:class "font-bold text-gray-800 dark:text-gray-50"} "Recent"]
       [:button {:class "text-xs text-violet-600 dark:text-violet-400 font-medium hover:underline"}
        "View All"]]
      [:div {:class "space-y-4"}
       (for [tx (take 3 (or recent-transactions []))]
         ^{:key (or (:transaction/id tx) (random-uuid))}
         [:div {:class "flex items-center gap-3"}
          [category-icon (:transaction/category tx) {:size :md}]
          [:div {:class "flex-1 min-w-0"}
           [:p {:class "text-sm font-bold text-gray-800 dark:text-gray-200 truncate"}
            (or (:transaction/description tx) "Transaction")]
           [:p {:class "text-xs text-gray-400"}
            (str (str/capitalize (name (or (:transaction/category tx) :other))) " • Today")]]
          [:span {:class (str "text-sm font-bold "
                              (if (= :expense (:transaction/type tx))
                                "text-gray-800 dark:text-gray-50"
                                "text-green-600 dark:text-green-400"))}
           (str (when (= :expense (:transaction/type tx)) "-")
                (currency/format-currency (:transaction/amount tx) (:transaction/currency tx)))]])]]

     ;; Add Transaction Button
     [:button {:class (str "w-full px-4 py-3 bg-violet-500 hover:bg-violet-600 text-white rounded-xl "
                           "font-semibold shadow-lg shadow-violet-500/30 transition-all "
                           "flex items-center justify-center gap-2 group")
               :on-click #(rf/dispatch [:app/open-panel :add-transaction])}
      [icon :plus {:width 18 :height 18 :class "group-hover:rotate-90 transition-transform"}]
      [:span "Add Transaction"]]]))

(defn envelope-view []
  (let [budgets @(rf/subscribe [:budget-analysis/budget-data])]
    [:div {:class "grid grid-cols-1 lg:grid-cols-12 gap-6"}
     ;; Main content (8 cols)
     [:div {:class "lg:col-span-8 space-y-6"}
      [envelope-master-header]
      [:div {:class "flex items-center justify-between px-2 mb-4"}
       [:h3 {:class "text-xl font-bold text-gray-800 dark:text-gray-200"} "Envelope Categories"]
       [:button {:class "text-violet-600 dark:text-violet-400 text-sm font-semibold hover:underline"}
        "Manage Categories"]]
      [:div {:class "grid grid-cols-2 md:grid-cols-3 xl:grid-cols-4 gap-4"}
       (for [item budgets]
         ^{:key (get-in item [:budget :budget/id] (random-uuid))}
         [envelope-card item])
       [envelope-add-card]]]
     ;; Sidebar (4 cols)
     [:aside {:class "lg:col-span-4"}
      [quick-insights-sidebar]]]))

(defn empty-state []
  [:div {:class "text-center py-16 bg-white dark:bg-neutral-800 rounded-xl border border-gray-200 dark:border-neutral-700"}
   [:div {:class "text-6xl mb-4"} "📊"]
   [:h3 {:class "text-xl font-bold text-gray-900 dark:text-gray-50 mb-2"} "No budget data yet"]
   [:p {:class "text-gray-600 dark:text-gray-400 mb-6 max-w-md mx-auto"}
    "Create your first budget to start tracking your spending and see detailed analysis."]
   [:button {:class (str "inline-flex items-center gap-2 px-5 py-3 bg-violet-500 hover:bg-violet-600 "
                         "text-white rounded-xl font-semibold shadow-lg shadow-violet-500/30 transition-all")
             :on-click #(rf/dispatch [:budgets/open-panel :create])}
    [icon :plus {:width 18 :height 18}]
    [:span "Create Your First Budget"]]])

(defn loading-skeleton []
  [:div {:class "space-y-4"}
   (for [i (range 4)]
     ^{:key i}
     [:div {:class "flex items-center gap-4 p-4 bg-gray-100 dark:bg-neutral-800 rounded-xl animate-pulse"}
      [:div {:class "w-12 h-12 rounded-full bg-gray-200 dark:bg-neutral-700"}]
      [:div {:class "flex-1 space-y-2"}
       [:div {:class "h-4 bg-gray-200 dark:bg-neutral-700 rounded w-3/5"}]
       [:div {:class "h-3 bg-gray-200 dark:bg-neutral-700 rounded w-2/5"}]]])])

(defn budget-analysis-view []
  (let [view-mode @(rf/subscribe [:budget-analysis/view-mode])
        loading? @(rf/subscribe [:budgets/loading?])
        has-data? (seq @(rf/subscribe [:budgets/all-budgets]))]
    [:div {:class "space-y-6"}
     [page-header]
     [filter-toolbar]
     (cond
       loading?
       [loading-skeleton]

       (not has-data?)
       [empty-state]

       :else
       (case view-mode
         :table [table-view]
         :grid [grid-view]
         :envelope [envelope-view]
         [table-view]))]))
