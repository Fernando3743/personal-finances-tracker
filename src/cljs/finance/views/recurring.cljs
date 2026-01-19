(ns finance.views.recurring
  "Recurring transactions page with list, cards, and calendar views."
  (:require [re-frame.core :as rf]
            [clojure.string :as str]
            [finance.utils.currency :as currency]
            [finance.components.icons :refer [icon]]))

(defn format-date [date-str]
  (when date-str
    (let [date (js/Date. date-str)]
      (.toLocaleDateString date "en-US" #js {:month "short" :day "numeric"}))))

(defn format-month-year [date]
  (when date
    (.toLocaleDateString date "en-US" #js {:month "long" :year "numeric"})))

(defn days-until [date-str]
  (when date-str
    (let [target (js/Date. date-str)
          now (js/Date.)
          diff-ms (- (.getTime target) (.getTime now))]
      (max 0 (js/Math.ceil (/ diff-ms 86400000))))))

(defn frequency-label [freq]
  (case freq
    :daily "Daily"
    :weekly "Weekly"
    :monthly "Monthly"
    :yearly "Yearly"
    "Unknown"))

(def category-styles
  {:entertainment {:icon :film :bg "bg-red-100 dark:bg-red-900/30" :text "text-red-600 dark:text-red-400"}
   :utilities {:icon :zap :bg "bg-yellow-100 dark:bg-yellow-900/30" :text "text-yellow-600 dark:text-yellow-400"}
   :housing {:icon :home :bg "bg-blue-100 dark:bg-blue-900/30" :text "text-blue-600 dark:text-blue-400"}
   :transportation {:icon :car :bg "bg-indigo-100 dark:bg-indigo-900/30" :text "text-indigo-600 dark:text-indigo-400"}
   :restaurants {:icon :utensils :bg "bg-orange-100 dark:bg-orange-900/30" :text "text-orange-600 dark:text-orange-400"}
   :healthcare {:icon :dumbbell :bg "bg-teal-100 dark:bg-teal-900/30" :text "text-teal-600 dark:text-teal-400"}
   :groceries {:icon :utensils :bg "bg-green-100 dark:bg-green-900/30" :text "text-green-600 dark:text-green-400"}
   :shopping {:icon :tag :bg "bg-purple-100 dark:bg-purple-900/30" :text "text-purple-600 dark:text-purple-400"}
   :salary {:icon :dollar :bg "bg-emerald-100 dark:bg-emerald-900/30" :text "text-emerald-600 dark:text-emerald-400"}
   :other {:icon :tag :bg "bg-gray-100 dark:bg-gray-800" :text "text-gray-600 dark:text-gray-400"}})

(defn frequency-badge [freq]
  (let [is-yearly? (= freq :yearly)]
    [:span {:class (str "inline-flex items-center px-2.5 py-0.5 rounded-full text-xs font-medium "
                        (if is-yearly?
                          "bg-purple-100 dark:bg-purple-900/30 text-purple-800 dark:text-purple-400"
                          "bg-gray-100 dark:bg-gray-800 text-gray-800 dark:text-gray-300"))}
     (frequency-label freq)]))

(defn status-badge [active?]
  [:span {:class (str "inline-flex items-center px-2.5 py-0.5 rounded-full text-xs font-medium "
                      (if active?
                        "bg-green-100 dark:bg-green-900/30 text-green-800 dark:text-green-400"
                        "bg-yellow-100 dark:bg-yellow-900/30 text-yellow-800 dark:text-yellow-400"))}
   (if active? "Active" "Paused")])

(defn summary-card [{:keys [title value subtitle badge icon-name variant]}]
  (let [variant-styles {:primary {:bg "bg-violet-100 dark:bg-violet-900/30" :text "text-violet-600 dark:text-violet-400"}
                        :warning {:bg "bg-orange-100 dark:bg-orange-900/30" :text "text-orange-600 dark:text-orange-400"}
                        :success {:bg "bg-green-100 dark:bg-green-900/30" :text "text-green-600 dark:text-green-400"}}
        style (get variant-styles (or variant :primary))]
    [:div {:class "bg-white dark:bg-gray-800 rounded-2xl p-5 border border-gray-200 dark:border-gray-700"}
     [:div {:class "flex items-start justify-between"}
      [:div {:class (str "h-10 w-10 rounded-xl flex items-center justify-center " (:bg style) " " (:text style))}
       [icon icon-name {:width 20 :height 20}]]
      (when badge
        [:span {:class (str "px-2 py-0.5 rounded-full text-xs font-medium "
                            (if (str/starts-with? badge "+")
                              "bg-green-100 dark:bg-green-900/30 text-green-700 dark:text-green-400"
                              "bg-red-100 dark:bg-red-900/30 text-red-700 dark:text-red-400"))}
         badge])]
     [:div {:class "mt-4"}
      [:p {:class "text-sm text-gray-500 dark:text-gray-400"} title]
      [:p {:class "text-2xl font-bold text-gray-900 dark:text-gray-50 mt-1"}
       value
       (when subtitle
         [:span {:class "text-sm font-normal text-gray-400 dark:text-gray-500 ml-1"} subtitle])]]]))

(defn summary-cards-row []
  (let [total-monthly @(rf/subscribe [:recurring/total-monthly-cost])
        next-payment @(rf/subscribe [:recurring/next-payment])
        active-count @(rf/subscribe [:recurring/active-count])
        days-left (when next-payment (days-until (:recurring/next-occurrence next-payment)))]
    [:div {:class "grid grid-cols-1 sm:grid-cols-2 lg:grid-cols-3 gap-4 mb-6"}
     [summary-card {:title "Total Monthly Cost"
                    :value (currency/format-currency total-monthly :USD)
                    :subtitle "/mo"
                    :icon-name :dollar
                    :variant :primary}]
     [summary-card {:title "Next Payment"
                    :value (or (:recurring/description next-payment) "None")
                    :subtitle (when days-left (str "Due in " days-left " days"))
                    :icon-name :calendar
                    :variant :warning}]
     [summary-card {:title "Active Subscriptions"
                    :value (str active-count)
                    :icon-name :check-circle
                    :variant :success}]]))

(defn search-input []
  (let [query @(rf/subscribe [:recurring/search-query])]
    [:div {:class "relative flex-1 max-w-xs"}
     [:div {:class "absolute inset-y-0 left-0 pl-3 flex items-center pointer-events-none"}
      [icon :search {:width 18 :height 18 :class "text-gray-400"}]]
     [:input {:type "text"
              :class (str "w-full pl-10 pr-4 py-2 border border-gray-200 dark:border-gray-700 rounded-xl "
                          "bg-white dark:bg-gray-800 text-gray-900 dark:text-gray-100 "
                          "placeholder-gray-400 focus:ring-2 focus:ring-violet-500 focus:border-transparent "
                          "text-sm")
              :placeholder "Search payments..."
              :value query
              :on-change #(rf/dispatch [:recurring/set-search-query (-> % .-target .-value)])}]]))

(defn view-mode-toggle []
  (let [mode @(rf/subscribe [:recurring/view-mode])]
    [:div {:class "flex gap-1 p-1 bg-gray-100 dark:bg-gray-800 rounded-lg"}
     [:button {:class (str "p-2 rounded-md transition-colors "
                           (if (= mode :list) "bg-white dark:bg-gray-700 shadow-sm" "hover:bg-gray-200 dark:hover:bg-gray-700"))
               :on-click #(rf/dispatch [:recurring/set-view-mode :list])
               :title "List view"}
      [icon :list {:width 18 :height 18 :class (if (= mode :list) "text-violet-600" "text-gray-500")}]]
     [:button {:class (str "p-2 rounded-md transition-colors "
                           (if (= mode :cards) "bg-white dark:bg-gray-700 shadow-sm" "hover:bg-gray-200 dark:hover:bg-gray-700"))
               :on-click #(rf/dispatch [:recurring/set-view-mode :cards])
               :title "Cards view"}
      [icon :grid {:width 18 :height 18 :class (if (= mode :cards) "text-violet-600" "text-gray-500")}]]
     [:button {:class (str "p-2 rounded-md transition-colors "
                           (if (= mode :calendar) "bg-white dark:bg-gray-700 shadow-sm" "hover:bg-gray-200 dark:hover:bg-gray-700"))
               :on-click #(rf/dispatch [:recurring/set-view-mode :calendar])
               :title "Calendar view"}
      [icon :calendar {:width 18 :height 18 :class (if (= mode :calendar) "text-violet-600" "text-gray-500")}]]]))

(defn recurring-table-row [{:keys [recurring/id recurring/amount recurring/type
                                    recurring/category recurring/description
                                    recurring/currency recurring/frequency
                                    recurring/next-occurrence recurring/active?
                                    recurring/payment-method]}]
  (let [cat-style (get category-styles category (get category-styles :other))
        cat-icon (:icon cat-style)]
    [:tr {:class "group hover:bg-gray-50 dark:hover:bg-gray-800/50 transition-colors"}
     [:td {:class "px-6 py-4"}
      [:div {:class "flex items-center"}
       [:div {:class (str "h-10 w-10 rounded-xl flex items-center justify-center mr-4 " (:bg cat-style) " " (:text cat-style))}
        [icon cat-icon {:width 20 :height 20}]]
       [:div
        [:p {:class "font-semibold text-gray-900 dark:text-gray-50"}
         (or description "Unnamed")]
        [:p {:class "text-xs text-gray-500 dark:text-gray-400"}
         (or payment-method "Payment method")]]]]
     [:td {:class "px-6 py-4 text-right font-medium text-gray-900 dark:text-gray-50"}
      (currency/format-currency amount (or currency :USD))]
     [:td {:class "px-6 py-4 hidden md:table-cell"}
      [frequency-badge frequency]]
     [:td {:class "px-6 py-4 hidden md:table-cell text-gray-500 dark:text-gray-400"}
      (or (format-date next-occurrence) "N/A")]
     [:td {:class "px-6 py-4 text-right"}
      [:div {:class "flex items-center justify-end gap-1 opacity-0 group-hover:opacity-100 transition-opacity"}
       [:button {:class "p-1.5 rounded-lg text-gray-400 hover:text-violet-600 hover:bg-violet-50 dark:hover:bg-violet-900/20 transition-colors"
                 :title "Edit"}
        [icon :edit {:width 16 :height 16}]]
       [:button {:class "p-1.5 rounded-lg text-gray-400 hover:text-red-500 hover:bg-red-50 dark:hover:bg-red-900/20 transition-colors"
                 :on-click #(when (js/confirm "Delete this recurring transaction?")
                              (rf/dispatch [:recurring/delete id]))
                 :title "Delete"}
        [icon :trash {:width 16 :height 16}]]]]]))

(defn recurring-table [items]
  [:div {:class "bg-white dark:bg-gray-800 rounded-2xl shadow-sm border border-gray-200 dark:border-gray-700 overflow-hidden"}
   [:table {:class "w-full text-left border-collapse"}
    [:thead
     [:tr {:class "border-b border-gray-200 dark:border-gray-700 text-xs uppercase text-gray-500 dark:text-gray-400 bg-gray-50/50 dark:bg-gray-800/50"}
      [:th {:class "px-6 py-4 font-semibold"} "Service Name"]
      [:th {:class "px-6 py-4 font-semibold text-right"} "Amount"]
      [:th {:class "px-6 py-4 font-semibold hidden md:table-cell"} "Frequency"]
      [:th {:class "px-6 py-4 font-semibold hidden md:table-cell"} "Next Date"]
      [:th {:class "px-6 py-4 font-semibold"} ""]]]
    [:tbody {:class "divide-y divide-gray-200 dark:divide-gray-700 text-sm"}
     (for [item items]
       ^{:key (:recurring/id item)}
       [recurring-table-row item])]]])

(defn list-view []
  (let [items @(rf/subscribe [:recurring/filtered-items])]
    [recurring-table items]))

(defn recurring-card [{:keys [item]}]
  (let [{:recurring/keys [id amount category description frequency
                          next-occurrence currency payment-method]} item
        days-left (days-until next-occurrence)
        cat-style (get category-styles category (get category-styles :other))
        urgent? (and days-left (< days-left 3))]
    [:div {:class "group relative bg-white dark:bg-gray-800 rounded-2xl border border-gray-200 dark:border-gray-700 p-5 hover:shadow-lg transition-all"}
     [:div {:class (str "h-14 w-14 rounded-2xl flex items-center justify-center mb-4 "
                        "group-hover:scale-105 transition-transform " (:bg cat-style) " " (:text cat-style))}
      [icon (:icon cat-style) {:width 28 :height 28}]]
     [:div {:class "absolute top-4 right-4 flex gap-1 opacity-0 group-hover:opacity-100 transition-opacity"}
      [:button {:class "p-1.5 rounded-lg text-gray-400 hover:text-violet-600 hover:bg-violet-50 dark:hover:bg-violet-900/20 transition-colors"
                :title "Edit"}
       [icon :edit {:width 16 :height 16}]]
      [:button {:class "p-1.5 rounded-lg text-gray-400 hover:text-red-500 hover:bg-red-50 dark:hover:bg-red-900/20 transition-colors"
                :on-click #(when (js/confirm "Delete this recurring transaction?")
                             (rf/dispatch [:recurring/delete id]))
                :title "Delete"}
       [icon :trash {:width 16 :height 16}]]]
     [:div {:class "mb-3"}
      [:p {:class "font-semibold text-gray-900 dark:text-gray-50"} (or description "Unnamed")]
      [:p {:class "text-xs text-gray-500 dark:text-gray-400"} (or payment-method "Payment method")]]
     [:div {:class "flex items-baseline gap-2 mb-4"}
      [:span {:class "text-2xl font-bold text-gray-900 dark:text-gray-50"}
       (currency/format-currency amount (or currency :USD))]
      [frequency-badge frequency]]
     [:div {:class "space-y-2"}
      [:div {:class "flex justify-between text-xs text-gray-500 dark:text-gray-400"}
       [:span (str "Next: " (or (format-date next-occurrence) "N/A"))]
       [:span (when days-left (str days-left " days left"))]]
      [:div {:class "h-1.5 bg-gray-200 dark:bg-gray-700 rounded-full overflow-hidden"}
       [:div {:class (str "h-full rounded-full transition-all "
                          (if urgent? "bg-red-500" "bg-violet-500"))
              :style {:width (str (min 100 (max 5 (- 100 (* (or days-left 30) 3.33)))) "%")}}]]]]))

(defn add-new-card []
  [:button {:class (str "flex flex-col items-center justify-center min-h-[220px] "
                        "border-2 border-dashed border-gray-300 dark:border-gray-600 "
                        "rounded-2xl hover:border-violet-500 hover:bg-violet-50/50 "
                        "dark:hover:bg-violet-900/10 transition-all group")
            :on-click #(rf/dispatch [:recurring/open-panel])}
   [:div {:class (str "h-12 w-12 rounded-xl bg-gray-100 dark:bg-gray-800 "
                      "flex items-center justify-center mb-3 "
                      "group-hover:bg-violet-100 dark:group-hover:bg-violet-900/30 transition-colors")}
    [icon :plus {:width 24 :height 24 :class "text-gray-400 group-hover:text-violet-500"}]]
   [:span {:class (str "text-sm font-medium text-gray-500 dark:text-gray-400 "
                       "group-hover:text-violet-600 dark:group-hover:text-violet-400")}
    "Add New"]])

(defn cards-view []
  (let [items @(rf/subscribe [:recurring/filtered-items])]
    [:div {:class "grid grid-cols-1 md:grid-cols-2 xl:grid-cols-3 gap-4"}
     (for [item items]
       ^{:key (:recurring/id item)}
       [recurring-card {:item item}])
     [add-new-card]]))

(defn- same-day? [d1 d2]
  (and (= (.getDate d1) (.getDate d2))
       (= (.getMonth d1) (.getMonth d2))
       (= (.getFullYear d1) (.getFullYear d2))))

(defn- is-today? [date]
  (same-day? date (js/Date.)))

(defn generate-calendar-days [month items]
  (let [year (.getFullYear month)
        month-idx (.getMonth month)
        first-day (js/Date. year month-idx 1)
        last-day (js/Date. year (inc month-idx) 0)
        start-day-of-week (.getDay first-day)
        days-in-month (.getDate last-day)
        prev-month-last (js/Date. year month-idx 0)
        prev-days (.getDate prev-month-last)]
    (concat
     (for [i (range start-day-of-week)]
       {:date (js/Date. year (dec month-idx) (- prev-days (- start-day-of-week i 1)))
        :is-current-month? false
        :items []})
     (for [day (range 1 (inc days-in-month))]
       (let [date (js/Date. year month-idx day)]
         {:date date
          :is-current-month? true
          :items (filter #(when-let [next-occ (:recurring/next-occurrence %)]
                            (same-day? (js/Date. next-occ) date))
                         items)}))
     (let [remaining (- 42 (+ start-day-of-week days-in-month))]
       (for [i (range remaining)]
         {:date (js/Date. year (inc month-idx) (inc i))
          :is-current-month? false
          :items []})))))

(defn calendar-day-cell [{:keys [date items is-current-month?]}]
  (let [today? (is-today? date)]
    [:div {:class (str "min-h-[100px] p-2 border-b border-r border-gray-200 dark:border-gray-700 "
                       (if is-current-month? "" "bg-gray-50 dark:bg-gray-800/50"))}
     [:div {:class (str "w-7 h-7 rounded-full flex items-center justify-center text-sm font-medium mb-1 "
                        (cond
                          today? "bg-violet-500 text-white"
                          is-current-month? "text-gray-900 dark:text-gray-50"
                          :else "text-gray-400 dark:text-gray-600"))}
      (.getDate date)]
     [:div {:class "space-y-1"}
      (for [[idx item] (map-indexed vector (take 2 items))]
        (let [cat-style (get category-styles (:recurring/category item) (get category-styles :other))]
          ^{:key (str (:recurring/id item) "-" idx)}
          [:div {:class (str "px-1.5 py-0.5 rounded text-xs truncate " (:bg cat-style) " " (:text cat-style))}
           (:recurring/description item)]))
      (when (> (count items) 2)
        [:div {:class "text-xs text-gray-500 dark:text-gray-400 px-1.5"}
         (str "+" (- (count items) 2) " more")])]]))

(defn calendar-header []
  (let [month @(rf/subscribe [:recurring/calendar-month])
        count @(rf/subscribe [:recurring/active-count])]
    [:div {:class "flex items-center justify-between mb-4"}
     [:div {:class "flex items-center gap-4"}
      [:h3 {:class "text-xl font-bold text-gray-900 dark:text-gray-50"}
       (format-month-year month)]
      [:span {:class "px-2 py-1 rounded-lg bg-gray-100 dark:bg-gray-800 text-sm font-medium text-gray-600 dark:text-gray-400"}
       (str count " payments")]]
     [:div {:class "flex items-center gap-2"}
      [:button {:class "p-2 rounded-lg hover:bg-gray-100 dark:hover:bg-gray-800 transition-colors"
                :on-click #(rf/dispatch [:recurring/prev-month])}
       [icon :chevron-left {:width 20 :height 20}]]
      [:button {:class "px-3 py-1.5 rounded-lg bg-violet-500 hover:bg-violet-600 text-white text-sm font-medium"
                :on-click #(rf/dispatch [:recurring/go-to-today])}
       "Today"]
      [:button {:class "p-2 rounded-lg hover:bg-gray-100 dark:hover:bg-gray-800 transition-colors"
                :on-click #(rf/dispatch [:recurring/next-month])}
       [icon :chevron-right {:width 20 :height 20}]]]]))

(defn calendar-grid []
  (let [month @(rf/subscribe [:recurring/calendar-month])
        items @(rf/subscribe [:recurring/active-items])
        days-of-week ["Sun" "Mon" "Tue" "Wed" "Thu" "Fri" "Sat"]
        calendar-days (generate-calendar-days month items)]
    [:div {:class "bg-white dark:bg-gray-800 rounded-2xl border border-gray-200 dark:border-gray-700 overflow-hidden"}
     [:div {:class "grid grid-cols-7 border-b border-gray-200 dark:border-gray-700"}
      (for [day days-of-week]
        ^{:key day}
        [:div {:class "py-3 text-center text-xs font-semibold text-gray-500 dark:text-gray-400 uppercase"}
         day])]
     [:div {:class "grid grid-cols-7"}
      (for [[idx day-info] (map-indexed vector calendar-days)]
        ^{:key idx}
        [calendar-day-cell day-info])]]))

(defn upcoming-sidebar-item [{:keys [item is-today?]}]
  (let [{:recurring/keys [id description amount frequency category]} item
        cat-style (get category-styles category (get category-styles :other))]
    [:div {:class "flex gap-3"}
     [:div {:class "flex flex-col items-center"}
      [:div {:class (str "w-3 h-3 rounded-full "
                         (if is-today? "bg-violet-500" "bg-gray-300 dark:bg-gray-600"))}]
      [:div {:class "flex-1 w-px bg-gray-200 dark:bg-gray-700"}]]
     [:div {:class "flex-1 pb-4"}
      [:div {:class "bg-white dark:bg-gray-800 rounded-xl border border-gray-200 dark:border-gray-700 p-4"}
       [:div {:class "flex items-center gap-3 mb-3"}
        [:div {:class (str "h-10 w-10 rounded-xl flex items-center justify-center " (:bg cat-style) " " (:text cat-style))}
         [icon (:icon cat-style) {:width 20 :height 20}]]
        [:div {:class "flex-1"}
         [:p {:class "font-medium text-gray-900 dark:text-gray-50"} description]
         [:p {:class "text-xs text-gray-500 dark:text-gray-400"} (frequency-label frequency)]]]
       [:div {:class "flex items-center justify-between"}
        [:span {:class "text-lg font-bold text-gray-900 dark:text-gray-50"}
         (currency/format-currency amount :USD)]
        (when is-today?
          [:div {:class "flex gap-2"}
           [:button {:class "px-3 py-1.5 rounded-lg bg-violet-500 hover:bg-violet-600 text-white text-xs font-medium"
                     :on-click #(rf/dispatch [:recurring/pay-now id])}
            "Pay Now"]
           [:button {:class (str "px-3 py-1.5 rounded-lg border border-gray-200 dark:border-gray-700 "
                                 "text-gray-500 dark:text-gray-400 text-xs font-medium "
                                 "hover:bg-gray-50 dark:hover:bg-gray-700")
                     :on-click #(rf/dispatch [:recurring/skip-payment id])}
            "Skip"]])]]]]))

(defn calendar-sidebar []
  (let [upcoming @(rf/subscribe [:recurring/upcoming-this-week])
        weekly-total @(rf/subscribe [:recurring/weekly-total])]
    [:div {:class "w-full lg:w-80 flex-shrink-0"}
     [:div {:class "bg-white dark:bg-gray-800 rounded-2xl border border-gray-200 dark:border-gray-700 p-5"}
      [:h3 {:class "font-semibold text-gray-900 dark:text-gray-50 mb-4"} "Upcoming This Week"]
      (if (empty? upcoming)
        [:p {:class "text-gray-500 dark:text-gray-400 text-sm"} "No payments due this week"]
        [:div {:class "space-y-0"}
         (for [[idx item] (map-indexed vector upcoming)]
           ^{:key (:recurring/id item)}
           [upcoming-sidebar-item {:item item :is-today? (= idx 0)}])])
      [:div {:class "pt-4 border-t border-gray-200 dark:border-gray-700 mt-4"}
       [:div {:class "flex justify-between items-center mb-2"}
        [:span {:class "text-sm text-gray-500 dark:text-gray-400"} "Total this week"]
        [:span {:class "font-bold text-gray-900 dark:text-gray-50"}
         (currency/format-currency weekly-total :USD)]]
       [:div {:class "h-2 bg-gray-200 dark:bg-gray-700 rounded-full overflow-hidden"}
        [:div {:class "h-full bg-violet-500 rounded-full" :style {:width "45%"}}]]
       [:p {:class "text-xs text-gray-500 dark:text-gray-400 mt-2"}
        "Based on weekly budget"]]]]))

(defn calendar-view []
  [:div {:class "flex flex-col lg:flex-row gap-6"}
   [:div {:class "flex-1"}
    [calendar-header]
    [calendar-grid]]
   [calendar-sidebar]])

(defn empty-state []
  [:div {:class "bg-white dark:bg-gray-800 rounded-2xl shadow-sm border border-gray-200 dark:border-gray-700 p-16 text-center"}
   [:div {:class "flex justify-center mb-4"}
    [:div {:class "h-16 w-16 rounded-2xl bg-violet-100 dark:bg-violet-900/30 flex items-center justify-center"}
     [icon :refresh-cw {:width 32 :height 32 :class "text-violet-600 dark:text-violet-400"}]]]
   [:h3 {:class "text-lg font-semibold text-gray-900 dark:text-gray-50 mb-2"} "No recurring payments"]
   [:p {:class "text-gray-500 dark:text-gray-400 mb-6"} "Create a recurring payment to automate your finances"]
   [:button {:class (str "inline-flex items-center px-4 py-2 bg-violet-500 hover:bg-violet-600 text-white rounded-xl "
                         "font-medium shadow-lg shadow-violet-500/20 transition-colors")
             :on-click #(rf/dispatch [:recurring/open-panel])}
    [icon :plus {:width 16 :height 16 :class "mr-2"}]
    "New Payment"]])

(defn recurring-view []
  (let [items @(rf/subscribe [:recurring/items])
        loading? @(rf/subscribe [:recurring/loading?])
        view-mode @(rf/subscribe [:recurring/view-mode])]
    [:div {:class "space-y-6 max-w-7xl mx-auto"}
     [summary-cards-row]
     [:div {:class "flex flex-col md:flex-row md:items-center justify-between gap-4"}
      [:div
       [:h2 {:class "text-xl font-bold text-gray-900 dark:text-gray-50"} "Active Payments"]
       [:p {:class "text-gray-500 dark:text-gray-400 mt-1 text-sm"} "Manage your subscriptions and scheduled bills."]]
      [:div {:class "flex items-center gap-3"}
       [search-input]
       [:button {:class (str "p-2 rounded-lg border border-gray-200 dark:border-gray-700 "
                             "hover:bg-gray-50 dark:hover:bg-gray-700 transition-colors")}
        [icon :filter {:width 18 :height 18}]]
       [view-mode-toggle]
       [:button {:class (str "flex items-center px-4 py-2 bg-violet-500 hover:bg-violet-600 text-white rounded-xl "
                             "text-sm font-medium shadow-lg shadow-violet-500/20")
                 :on-click #(rf/dispatch [:recurring/open-panel])}
        [icon :plus {:width 18 :height 18 :class "mr-2"}]
        "New Payment"]]]
     (cond
       loading?
       [:div {:class "flex justify-center py-16"}
        [:div {:class "w-8 h-8 border-4 border-violet-600 border-t-transparent rounded-full animate-spin"}]]

       (empty? items)
       [empty-state]

       :else
       (case view-mode
         :list [list-view]
         :cards [cards-view]
         :calendar [calendar-view]
         [list-view]))]))
