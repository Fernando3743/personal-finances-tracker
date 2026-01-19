(ns finance.views.recurring
  "Recurring transactions page view with table layout."
  (:require [re-frame.core :as rf]
            [clojure.string :as str]
            [finance.utils.currency :as currency]
            [finance.components.icons :refer [icon]]))

(defn format-date [date-str]
  (when date-str
    (let [date (js/Date. date-str)]
      (.toLocaleDateString date "en-US" #js {:month "short" :day "numeric"}))))

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

(defn recurring-table-row [{:keys [recurring/id recurring/amount recurring/type
                                    recurring/category recurring/description
                                    recurring/currency recurring/frequency
                                    recurring/next-occurrence recurring/active?]}]
  (let [cat-style (get category-styles category (get category-styles :other))
        cat-icon (:icon cat-style)
        is-income? (= type :income)]
    [:tr {:class "group hover:bg-gray-50 dark:hover:bg-gray-800/50 transition-colors"}
     [:td {:class "px-6 py-4"}
      [:div {:class "flex items-center"}
       [:div {:class (str "h-10 w-10 rounded-xl flex items-center justify-center mr-4 " (:bg cat-style) " " (:text cat-style))}
        [icon cat-icon {:width 20 :height 20}]]
       [:div
        [:p {:class "font-semibold text-gray-900 dark:text-gray-50"}
         (or description "Unnamed")]
        [:p {:class "text-xs text-gray-500 dark:text-gray-400"}
         (str "Next: " (or (format-date next-occurrence) "N/A"))]]]]
     [:td {:class "px-6 py-4 hidden md:table-cell text-gray-500 dark:text-gray-400"}
      (-> category name str/capitalize)]
     [:td {:class "px-6 py-4"}
      [frequency-badge frequency]]
     [:td {:class "px-6 py-4 text-right font-medium text-gray-900 dark:text-gray-50"}
      (currency/format-currency amount (or currency :USD))]
     [:td {:class "px-6 py-4 text-center"}
      [status-badge active?]]
     [:td {:class "px-6 py-4 text-right"}
      [:div {:class "flex items-center justify-end gap-1"}
       [:button {:class "p-1.5 rounded-lg text-gray-400 hover:text-violet-600 hover:bg-violet-50 dark:hover:bg-violet-900/20 transition-colors"
                 :on-click #(rf/dispatch [:recurring/toggle-active id])
                 :title (if active? "Pause" "Activate")}
        [icon (if active? :toggle-right :toggle-left) {:width 18 :height 18}]]
       [:button {:class "p-1.5 rounded-lg text-gray-400 hover:text-red-500 hover:bg-red-50 dark:hover:bg-red-900/20 transition-colors"
                 :on-click #(when (js/confirm "Delete this recurring transaction?")
                              (rf/dispatch [:recurring/delete id]))
                 :title "Delete"}
        [icon :trash {:width 18 :height 18}]]
       [:button {:class "p-1.5 rounded-lg text-gray-400 hover:text-gray-600 dark:hover:text-gray-300 transition-colors"
                 :title "More options"}
        [icon :more-vertical {:width 18 :height 18}]]]]]))

(defn recurring-table [items]
  [:div {:class "bg-white dark:bg-gray-800 rounded-2xl shadow-sm border border-gray-200 dark:border-gray-700 overflow-hidden"}
   [:table {:class "w-full text-left border-collapse"}
    [:thead
     [:tr {:class "border-b border-gray-200 dark:border-gray-700 text-xs uppercase text-gray-500 dark:text-gray-400 bg-gray-50/50 dark:bg-gray-800/50"}
      [:th {:class "px-6 py-4 font-semibold"} "Name"]
      [:th {:class "px-6 py-4 font-semibold hidden md:table-cell"} "Category"]
      [:th {:class "px-6 py-4 font-semibold"} "Frequency"]
      [:th {:class "px-6 py-4 font-semibold text-right"} "Amount"]
      [:th {:class "px-6 py-4 font-semibold text-center"} "Status"]
      [:th {:class "px-6 py-4 font-semibold"} ""]]]
    [:tbody {:class "divide-y divide-gray-200 dark:divide-gray-700 text-sm"}
     (for [item items]
       ^{:key (:recurring/id item)}
       [recurring-table-row item])]]])

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
        loading? @(rf/subscribe [:recurring/loading?])]
    [:div {:class "space-y-6 max-w-6xl mx-auto"}
     [:div {:class "flex flex-col md:flex-row md:items-center justify-between gap-4"}
      [:div
       [:h2 {:class "text-2xl font-bold text-gray-900 dark:text-gray-50"} "Recurring Payments"]
       [:p {:class "text-gray-500 dark:text-gray-400 mt-1"} "Manage your subscriptions and scheduled bills."]]
      [:div {:class "flex gap-3"}
       [:button {:class (str "flex items-center px-4 py-2 border border-gray-200 dark:border-gray-700 rounded-xl "
                             "bg-white dark:bg-gray-800 hover:bg-gray-50 dark:hover:bg-gray-700 "
                             "transition-colors text-sm font-medium text-gray-700 dark:text-gray-300")}
        [icon :filter {:width 18 :height 18 :class "mr-2"}]
        "Filter"]
       [:button {:class (str "flex items-center px-4 py-2 bg-violet-500 hover:bg-violet-600 text-white rounded-xl "
                             "transition-colors text-sm font-medium shadow-lg shadow-violet-500/20")
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
       [recurring-table items])]))
