(ns finance.views.dashboard
  "Dashboard view with financial overview and metrics."
  (:require [re-frame.core :as rf]
            [clojure.string :as str]
            [finance.utils.currency :as currency]
            [finance.components.icons :refer [icon]]))

(defn format-day [date]
  (when date
    (let [d (js/Date. date)]
      (.getDate d))))

(defn format-month-year [date]
  (when date
    (let [d (js/Date. date)]
      (.toLocaleDateString d "en-US" #js {:month "short" :year "numeric"}))))

(defn currency-card [{:keys [currency balance income expenses]}]
  (let [currency-name (case currency
                        :COP "Colombian Peso"
                        :USD "US Dollar"
                        (name currency))
        trend-percent (if (pos? balance) 2.5 -0.4)
        trend-positive? (pos? trend-percent)
        badge-colors (case currency
                       :COP {:bg "bg-blue-50 dark:bg-blue-900/20" :text "text-blue-600 dark:text-blue-400"}
                       :USD {:bg "bg-violet-50 dark:bg-violet-900/20" :text "text-violet-600 dark:text-violet-400"}
                       {:bg "bg-gray-50 dark:bg-gray-800" :text "text-gray-600 dark:text-gray-400"})]
    [:div {:class "bg-white dark:bg-neutral-800 rounded-xl shadow-sm p-6"}
     [:div {:class "flex justify-between items-start mb-8"}
      [:div {:class "flex gap-4"}
       [:div {:class (str "h-12 w-12 rounded-lg flex items-center justify-center font-bold "
                          (:bg badge-colors) " " (:text badge-colors))}
        (name currency)]
       [:div
        [:p {:class "text-sm text-gray-500 dark:text-gray-400"} currency-name]
        [:h3 {:class "text-3xl font-bold text-gray-900 dark:text-gray-50"}
         (currency/format-currency balance currency)]]]
      [:div {:class (str "px-2 py-1 rounded text-sm font-medium flex items-center gap-1 "
                         (if trend-positive?
                           "bg-emerald-50 text-emerald-600 dark:bg-emerald-900/20 dark:text-emerald-400"
                           "bg-red-50 text-red-600 dark:bg-red-900/20 dark:text-red-400"))}
       [icon (if trend-positive? :trending-up :trending-down) {:width 12 :height 12}]
       [:span (str (when trend-positive? "+") trend-percent "%")]]]
     [:div {:class "flex justify-between items-center"}
      [:div
       [:div {:class "flex items-center gap-2 mb-1"}
        [:div {:class "w-2 h-2 rounded-full bg-emerald-500"}]
        [:span {:class "text-sm text-gray-500 dark:text-gray-400"} "Income"]]
       [:p {:class "text-lg font-semibold"}
        (currency/format-currency income currency)]]
      [:div {:class "text-right"}
       [:div {:class "flex items-center gap-2 mb-1 justify-end"}
        [:div {:class "w-2 h-2 rounded-full bg-red-500"}]
        [:span {:class "text-sm text-gray-500 dark:text-gray-400"} "Expenses"]]
       [:p {:class "text-lg font-semibold"}
        (currency/format-currency expenses currency)]]]]))

(defn balances-section []
  (let [currency-balances @(rf/subscribe [:dashboard/all-currency-balances])
        currencies @(rf/subscribe [:dashboard/available-currencies])]
    [:<>
     [:div {:class "mb-6"}
      [:h1 {:class "text-2xl font-bold text-gray-900 dark:text-gray-50"} "Dashboard"]
      [:p {:class "text-gray-500 dark:text-gray-400"} "Your finances across all currencies"]]
     [:div {:class "grid grid-cols-1 md:grid-cols-2 gap-6 mb-6"}
      (if (seq currency-balances)
        (for [{:keys [currency balance income expenses]} currency-balances]
          ^{:key currency}
          [currency-card {:currency currency :balance balance :income income :expenses expenses}])
        (for [curr currencies]
          ^{:key curr}
          [currency-card {:currency curr :balance 0 :income 0 :expenses 0}]))]]))

(defn transaction-row [{:keys [transaction/id transaction/date transaction/description
                               transaction/amount transaction/type transaction/category
                               transaction/currency]}]
  (let [day (format-day date)
        month-year (format-month-year date)
        is-income? (= type :income)
        curr (or currency :COP)
        display-text (or description (str (name (or category :other)) " transaction"))]
    [:div {:class "flex items-center gap-4 p-4 hover:bg-gray-50 dark:hover:bg-neutral-800 rounded-lg cursor-pointer transition-colors"
           :on-click #(rf/dispatch [:app/navigate :transactions])}
     [:div {:class "text-center min-w-[3rem]"}
      [:div {:class "text-lg font-semibold text-gray-900 dark:text-gray-50"} day]
      [:div {:class "text-xs text-gray-500 dark:text-gray-400"} month-year]]
     [:div {:class "flex-1 min-w-0 text-sm text-gray-900 dark:text-gray-50 truncate"} display-text]
     [:div {:class "px-2 py-0.5 rounded bg-gray-100 dark:bg-neutral-800 text-xs font-medium text-gray-600 dark:text-gray-400"} (name curr)]
     [:div {:class (str "font-mono font-semibold text-sm "
                        (if is-income? "text-green-600 dark:text-green-500" "text-red-600 dark:text-red-500"))}
      (if is-income?
        (currency/format-currency amount curr)
        (str "-" (currency/format-currency amount curr)))]
     [:span {:class "text-gray-400 dark:text-gray-500"} ">"]]))

(defn transactions-panel []
  (let [transactions @(rf/subscribe [:tx/recent-transactions])]
    [:div {:class "bg-white dark:bg-neutral-800 rounded-xl shadow-sm p-5"}
     [:div {:class "flex items-center justify-between mb-4"}
      [:div
       [:h2 {:class "text-lg font-semibold text-gray-900 dark:text-gray-50"} "Transactions"]
       [:p {:class "text-sm text-gray-600 dark:text-gray-400"} "Recent activity"]]
      [:button {:class "text-sm text-violet-600 dark:text-violet-400 hover:text-violet-700 dark:hover:text-violet-300 font-medium transition-colors"
                :on-click #(rf/dispatch [:app/navigate :transactions])}
       "View all >"]]

     (if (empty? transactions)
       [:div {:class "text-center py-12"}
        [:p {:class "text-gray-600 dark:text-gray-400 mb-4"} "No transactions yet"]
        [:button {:class (str "inline-flex items-center gap-2 px-4 py-2 rounded-lg font-medium text-white "
                              "bg-violet-600 hover:bg-violet-700 "
                              "transition-all")
                  :on-click #(rf/dispatch [:app/navigate :add-transaction])}
         "Add Transaction"]]

       [:div {:class "divide-y divide-gray-200 dark:divide-neutral-700 -mx-5"}
        (for [t transactions]
          ^{:key (or (:transaction/id t) (random-uuid))}
          [:div {:class "px-5"}
           [transaction-row t]])])]))

(def category-colors
  {:housing "#7c3aed"
   :groceries "#22C55E"
   :food "#F59E0B"
   :restaurants "#F97316"
   :transportation "#10B981"
   :transport "#10B981"
   :utilities "#EAB308"
   :entertainment "#EC4899"
   :healthcare "#14B8A6"
   :shopping "#8B5CF6"
   :salary "#22C55E"
   :freelance "#06B6D4"
   :investments "#10B981"
   :gifts "#D946EF"
   :other "#EF4444"})

(defn generate-smooth-path [points width height max-val]
  (when (seq points)
    (let [n (count points)
          x-step (/ width (max 1 (dec n)))
          scale-y #(- height (* (/ % max-val) height))
          coords (map-indexed (fn [i v] [(* i x-step) (scale-y v)]) points)]
      (if (= n 1)
        (let [[x y] (first coords)]
          (str "M " x " " y))
        (str "M " (first (first coords)) " " (second (first coords))
             (apply str
                    (map (fn [[[x1 y1] [x2 y2]]]
                           (let [cp1x (+ x1 (* 0.3 (- x2 x1)))
                                 cp2x (- x2 (* 0.3 (- x2 x1)))]
                             (str " C " cp1x " " y1 " " cp2x " " y2 " " x2 " " y2)))
                         (partition 2 1 coords))))))))

(defn cash-flow-chart []
  (let [monthly-report @(rf/subscribe [:dashboard/monthly-report])
        months (or (:months monthly-report) [])
        income-data (if (seq months)
                      (mapv #(or (:income %) 0) months)
                      [800 1200 1800 2400 2800 3200])
        expense-data (if (seq months)
                       (mapv #(or (:expenses %) 0) months)
                       [600 800 1200 1600 1800 2000])
        max-val (max 1 (apply max (concat income-data expense-data)))
        rounded-max (* 1000 (js/Math.ceil (/ max-val 1000)))
        width 500
        height 200
        y-labels [4 3 2 1 0]]
    [:div {:class "flex gap-4"}
     [:div {:class "flex flex-col justify-between text-xs text-gray-500 dark:text-gray-400 py-2"}
      (for [label y-labels]
        ^{:key label}
        [:span (str label "k")])]
     [:div {:class "flex-1 relative"}
      [:svg {:viewBox (str "0 0 " width " " height)
             :preserveAspectRatio "none"
             :class "w-full h-48"}
       [:defs
        [:linearGradient {:id "incomeGradient" :x1 "0%" :y1 "0%" :x2 "0%" :y2 "100%"}
         [:stop {:offset "0%" :stop-color "#10b981" :stop-opacity "0.3"}]
         [:stop {:offset "100%" :stop-color "#10b981" :stop-opacity "0"}]]
        [:linearGradient {:id "expenseGradient" :x1 "0%" :y1 "0%" :x2 "0%" :y2 "100%"}
         [:stop {:offset "0%" :stop-color "#ef4444" :stop-opacity "0.3"}]
         [:stop {:offset "100%" :stop-color "#ef4444" :stop-opacity "0"}]]]
       (for [i (range 5)]
         ^{:key i}
         [:line {:x1 0 :y1 (* i (/ height 4)) :x2 width :y2 (* i (/ height 4))
                 :stroke "currentColor" :stroke-dasharray "4 4" :class "text-gray-200 dark:text-neutral-700"}])
       [:path {:d (str (generate-smooth-path income-data width height rounded-max)
                       " L " width " " height " L 0 " height " Z")
               :fill "url(#incomeGradient)"}]
       [:path {:d (generate-smooth-path income-data width height rounded-max)
               :fill "none" :stroke "#10b981" :stroke-width "2.5"}]
       [:path {:d (str (generate-smooth-path expense-data width height rounded-max)
                       " L " width " " height " L 0 " height " Z")
               :fill "url(#expenseGradient)"}]
       [:path {:d (generate-smooth-path expense-data width height rounded-max)
               :fill "none" :stroke "#ef4444" :stroke-width "2.5"}]]]]))

(defn cash-flow-widget []
  (let [time-range @(rf/subscribe [:dashboard/chart-time-range])]
    [:div {:class "bg-white dark:bg-neutral-800 rounded-xl shadow-sm p-5"}
     [:div {:class "flex items-center justify-between pb-8"}
      [:div
       [:h3 {:class "text-lg font-bold text-gray-900 dark:text-gray-50"} "Monthly Cash Flow"]
       [:p {:class "text-sm text-gray-500 dark:text-gray-400 mt-1"} "Income vs Expenses Analysis"]]
      [:div {:class "flex gap-1 p-1 bg-gray-100 dark:bg-neutral-800 rounded-lg"}
       (for [[key label] [[:week "W"] [:month "M"] [:year "Y"]]]
         ^{:key key}
         [:button {:class (str "px-3 py-1.5 rounded-md text-sm font-medium transition-colors "
                               (if (= time-range key)
                                 "bg-white dark:bg-neutral-700 text-gray-900 dark:text-gray-50 shadow-sm"
                                 "text-gray-600 dark:text-gray-400 hover:text-gray-900 dark:hover:text-gray-50"))
                   :on-click #(rf/dispatch [:dashboard/set-chart-range key])}
          label])]]
     [cash-flow-chart]]))

(defn donut-chart [{:keys [segments total]}]
  (let [size 160
        stroke-width 24
        radius (/ (- size stroke-width) 2)
        circumference (* 2 js/Math.PI radius)
        center (/ size 2)]
    [:div {:class "relative inline-flex items-center justify-center"}
     [:svg {:width size :height size :viewBox (str "0 0 " size " " size)}
      [:circle {:cx center :cy center :r radius
                :fill "none" :stroke "currentColor" :stroke-width stroke-width :class "text-gray-200 dark:text-neutral-700"}]
      (let [offset (atom 0)]
        (for [{:keys [percent color]} segments]
          (let [dash (* (/ percent 100) circumference)
                gap (- circumference dash)
                current-offset @offset]
            (swap! offset + dash)
            ^{:key color}
            [:circle {:cx center :cy center :r radius
                      :fill "none" :stroke color :stroke-width stroke-width
                      :stroke-dasharray (str dash " " gap)
                      :stroke-dashoffset (- (* 0.25 circumference) current-offset)
                      :style {:transform "rotate(-90deg)" :transform-origin "center"}}])))]
     [:div {:class "absolute inset-0 flex flex-col items-center justify-center"}
      [:span {:class "text-xs text-gray-500 dark:text-gray-400 font-medium uppercase tracking-wide"} "TOTAL"]
      [:span {:class "text-lg font-bold text-gray-900 dark:text-gray-50"} (currency/format-currency total :USD)]]]))

(defn category-donut-widget []
  (let [{:keys [categories]} @(rf/subscribe [:dashboard/category-totals])
        expense-cats (->> categories
                          (filter #(= (:type %) :expense))
                          (sort-by :total >)
                          (take 4))
        total-expenses (reduce + 0 (map :total expense-cats))
        segments (if (seq expense-cats)
                   (map (fn [{:keys [category total]}]
                          {:name (name category)
                           :percent (if (pos? total-expenses)
                                      (* 100 (/ total total-expenses))
                                      0)
                           :color (get category-colors category "#6B7280")})
                        expense-cats)
                   [{:name "Housing" :percent 45 :color "#7c3aed"}
                    {:name "Food" :percent 25 :color "#F59E0B"}
                    {:name "Transport" :percent 15 :color "#10B981"}
                    {:name "Others" :percent 15 :color "#EF4444"}])
        display-total (if (pos? total-expenses) total-expenses 2690)]
    [:div {:class "bg-white dark:bg-neutral-800 rounded-xl shadow-sm p-5"}
     [:h3 {:class "text-lg font-bold text-gray-900 dark:text-gray-50 mb-6"} "Spending by Category"]
     [:div {:class "flex flex-col items-center gap-6"}
      [donut-chart {:segments segments :total display-total}]
      [:div {:class "flex-1 space-y-3 w-full"}
       (for [{:keys [name percent color]} segments]
         ^{:key name}
         [:div {:class "flex items-center justify-between"}
          [:div {:class "flex items-center gap-2"}
           [:span {:class "w-3 h-3 rounded-full" :style {:background-color color}}]
           [:span {:class "text-sm text-gray-600 dark:text-gray-400"} (str/capitalize name)]]
          [:span {:class "text-sm font-bold text-gray-900 dark:text-gray-50"} (str (js/Math.round percent) "%")]])]]]))

(defn trend-chart-widget []
  [cash-flow-widget])

(defn category-summary-widget []
  [category-donut-widget])

(defn dashboard-skeleton []
  [:div {:class "space-y-8"}
   [:div {:class "grid grid-cols-1 md:grid-cols-2 xl:grid-cols-3 gap-4"}
    (for [i (range 3)]
      ^{:key i}
      [:div {:class "h-40 rounded-2xl bg-gray-100 dark:bg-neutral-800 animate-pulse"}])]
   [:div {:class "grid grid-cols-1 lg:grid-cols-2 gap-6"}
    [:div {:class "h-80 rounded-xl bg-gray-100 dark:bg-neutral-800 animate-pulse"}]
    [:div {:class "h-80 rounded-xl bg-gray-100 dark:bg-neutral-800 animate-pulse"}]]])

(defn dashboard-view []
  (let [loading? @(rf/subscribe [:app/loading?])]
    [:div {:class "max-w-7xl mx-auto space-y-8"}
     (if loading?
       [dashboard-skeleton]
       [:<>
        [balances-section]
        [:div {:class "grid grid-cols-1 lg:grid-cols-3 gap-6"}
         [:div {:class "lg:col-span-2"}
          [cash-flow-widget]]
         [category-donut-widget]]
        [transactions-panel]])]))
