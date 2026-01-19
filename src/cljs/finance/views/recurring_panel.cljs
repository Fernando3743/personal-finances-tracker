(ns finance.views.recurring-panel
  "Slide-in panel for adding recurring payments."
  (:require [re-frame.core :as rf]
            [clojure.string :as str]
            [finance.utils.currency :as currency]
            [finance.components.icons :refer [icon]]))

(def panel-categories
  [{:key :entertainment :label "Entertainment" :icon-name :film
    :bg "bg-red-100 dark:bg-red-900/30" :text "text-red-600 dark:text-red-400"}
   {:key :utilities :label "Utilities" :icon-name :zap
    :bg "bg-yellow-100 dark:bg-yellow-900/30" :text "text-yellow-600 dark:text-yellow-400"}
   {:key :housing :label "Housing" :icon-name :home
    :bg "bg-blue-100 dark:bg-blue-900/30" :text "text-blue-600 dark:text-blue-400"}
   {:key :transportation :label "Transport" :icon-name :car
    :bg "bg-indigo-100 dark:bg-indigo-900/30" :text "text-indigo-600 dark:text-indigo-400"}
   {:key :restaurants :label "Food" :icon-name :utensils
    :bg "bg-orange-100 dark:bg-orange-900/30" :text "text-orange-600 dark:text-orange-400"}
   {:key :healthcare :label "Health" :icon-name :dumbbell
    :bg "bg-teal-100 dark:bg-teal-900/30" :text "text-teal-600 dark:text-teal-400"}])

(defn category-card [{:keys [key label icon-name bg text]} selected? on-click]
  [:label {:class "cursor-pointer"}
   [:input {:type "radio"
            :name "category"
            :class "peer sr-only"
            :checked selected?
            :on-change on-click}]
   [:div {:class (str "flex flex-col items-center justify-center p-3 rounded-xl border h-24 transition-all shadow-sm hover:shadow "
                      (if selected?
                        "border-violet-500 bg-violet-50 dark:bg-violet-900/20"
                        "border-gray-200 dark:border-gray-700 bg-white dark:bg-gray-800 hover:bg-gray-50 dark:hover:bg-gray-700"))}
    [:div {:class (str "h-8 w-8 rounded-full flex items-center justify-center mb-2 " bg " " text)}
     [icon icon-name {:width 16 :height 16}]]
    [:span {:class (str "text-xs font-medium transition-colors "
                        (if selected?
                          "text-violet-600 dark:text-violet-400"
                          "text-gray-500 dark:text-gray-400"))}
     label]]])

(defn category-grid []
  (let [selected @(rf/subscribe [:recurring/form-field :category])]
    [:div {:class "space-y-2"}
     [:label {:class "block text-sm font-medium text-gray-500 dark:text-gray-400"} "Category"]
     [:div {:class "grid grid-cols-3 gap-3"}
      (for [{:keys [key] :as cat} panel-categories]
        ^{:key key}
        [category-card cat (= key selected)
         #(rf/dispatch [:recurring/update-form-field :category key])])]]))

(defn frequency-selector []
  (let [selected @(rf/subscribe [:recurring/form-field :frequency])
        frequencies [{:key :weekly :label "Weekly"}
                     {:key :monthly :label "Monthly"}
                     {:key :yearly :label "Yearly"}]]
    [:div {:class "space-y-2"}
     [:label {:class "block text-sm font-medium text-gray-500 dark:text-gray-400"} "Frequency"]
     [:div {:class "grid grid-cols-3 gap-2"}
      (for [{:keys [key label]} frequencies]
        ^{:key key}
        [:label {:class "cursor-pointer"}
         [:input {:type "radio"
                  :name "frequency"
                  :class "peer sr-only"
                  :checked (= key selected)
                  :on-change #(rf/dispatch [:recurring/update-form-field :frequency key])}]
         [:div {:class (str "rounded-xl border py-2.5 text-center text-xs font-medium transition-all shadow-sm "
                            (if (= key selected)
                              "border-violet-500 bg-violet-500 text-white"
                              "border-gray-200 dark:border-gray-700 bg-white dark:bg-gray-800 text-gray-500 dark:text-gray-400 hover:bg-gray-50 dark:hover:bg-gray-700"))}
          label]])]]))

(defn payment-name-input []
  (let [name @(rf/subscribe [:recurring/form-field :name])]
    [:div {:class "space-y-2"}
     [:label {:class "block text-sm font-medium text-gray-500 dark:text-gray-400"} "Payment Name"]
     [:div {:class "relative"}
      [:div {:class "absolute inset-y-0 left-0 pl-3 flex items-center pointer-events-none"}
       [icon :tag {:width 18 :height 18 :class "text-gray-400"}]]
      [:input {:class (str "block w-full pl-10 pr-3 py-3 border border-gray-200 dark:border-gray-700 rounded-xl "
                           "bg-gray-50 dark:bg-gray-800 text-gray-900 dark:text-gray-50 placeholder-gray-400 "
                           "focus:outline-none focus:ring-2 focus:ring-violet-500 focus:border-transparent "
                           "transition-all sm:text-sm shadow-sm")
               :type "text"
               :placeholder "e.g. Netflix Subscription"
               :value (or name "")
               :on-change #(rf/dispatch [:recurring/update-form-field :name (-> % .-target .-value)])}]]]))

(defn currency-amount-row []
  (let [amount @(rf/subscribe [:recurring/form-field :amount])
        curr @(rf/subscribe [:recurring/form-field :currency])
        currencies [:USD :COP :EUR]]
    [:div {:class "grid grid-cols-2 gap-4"}
     [:div {:class "space-y-2"}
      [:label {:class "block text-sm font-medium text-gray-500 dark:text-gray-400"} "Currency"]
      [:div {:class "relative"}
       [:select {:class (str "block w-full pl-3 pr-8 py-3 border border-gray-200 dark:border-gray-700 rounded-xl "
                             "bg-gray-50 dark:bg-gray-800 text-gray-900 dark:text-gray-50 "
                             "appearance-none focus:outline-none focus:ring-2 focus:ring-violet-500 focus:border-transparent "
                             "transition-all sm:text-sm shadow-sm")
                 :value (name (or curr :USD))
                 :on-change #(rf/dispatch [:recurring/update-form-field :currency (keyword (-> % .-target .-value))])}
        (for [c currencies]
          ^{:key c}
          [:option {:value (name c)} (name c)])]
       [:div {:class "absolute inset-y-0 right-0 pr-2 flex items-center pointer-events-none"}
        [icon :chevron-down {:width 16 :height 16 :class "text-gray-400"}]]]]
     [:div {:class "space-y-2"}
      [:label {:class "block text-sm font-medium text-gray-500 dark:text-gray-400"} "Amount"]
      [:div {:class "relative"}
       [:div {:class "absolute inset-y-0 left-0 pl-4 flex items-center pointer-events-none"}
        [:span {:class "text-gray-400 font-semibold"} "$"]]
       [:input {:class (str "block w-full pl-10 pr-3 py-3 border border-gray-200 dark:border-gray-700 rounded-xl "
                            "bg-gray-50 dark:bg-gray-800 text-gray-900 dark:text-gray-50 placeholder-gray-400 "
                            "focus:outline-none focus:ring-2 focus:ring-violet-500 focus:border-transparent "
                            "transition-all sm:text-sm shadow-sm font-semibold")
                :type "number"
                :step "0.01"
                :placeholder "0.00"
                :value (or amount "")
                :on-change #(rf/dispatch [:recurring/update-form-field :amount (-> % .-target .-value)])}]]]]))

(defn start-date-picker []
  (let [date @(rf/subscribe [:recurring/form-field :start-date])]
    [:div {:class "space-y-2"}
     [:label {:class "block text-sm font-medium text-gray-500 dark:text-gray-400"} "Start Date"]
     [:div {:class "relative"}
      [:div {:class "absolute inset-y-0 left-0 pl-3 flex items-center pointer-events-none"}
       [icon :calendar {:width 18 :height 18 :class "text-gray-400"}]]
      [:input {:class (str "block w-full pl-10 pr-3 py-3 border border-gray-200 dark:border-gray-700 rounded-xl "
                           "bg-gray-50 dark:bg-gray-800 text-gray-900 dark:text-gray-50 "
                           "focus:outline-none focus:ring-2 focus:ring-violet-500 focus:border-transparent "
                           "transition-all sm:text-sm shadow-sm")
               :type "date"
               :value (or date "")
               :on-change #(rf/dispatch [:recurring/update-form-field :start-date (-> % .-target .-value)])}]]]))

(defn wallet-dropdown []
  [:div {:class "space-y-2"}
   [:label {:class "block text-sm font-medium text-gray-500 dark:text-gray-400"} "Source Account / Wallet"]
   [:div {:class "relative"}
    [:div {:class "absolute inset-y-0 left-0 pl-3 flex items-center pointer-events-none"}
     [icon :wallet {:width 18 :height 18 :class "text-gray-400"}]]
    [:select {:class (str "block w-full pl-10 pr-10 py-3 border border-gray-200 dark:border-gray-700 rounded-xl "
                          "bg-gray-50 dark:bg-gray-800 text-gray-900 dark:text-gray-50 "
                          "appearance-none focus:outline-none focus:ring-2 focus:ring-violet-500 focus:border-transparent "
                          "transition-all sm:text-sm shadow-sm")}
     [:option "Main Checking (**** 4532)"]
     [:option "Credit Card (**** 9921)"]
     [:option "PayPal Balance"]]
    [:div {:class "absolute inset-y-0 right-0 pr-3 flex items-center pointer-events-none"}
     [icon :chevron-down {:width 16 :height 16 :class "text-gray-400"}]]]])

(defn- frequency-multiplier [freq]
  (case freq
    :weekly 52
    :monthly 12
    :yearly 1
    12))

(defn- frequency-label [freq]
  (case freq
    :weekly "Weekly"
    :monthly "Monthly"
    :yearly "Yearly"
    "Monthly"))

(defn impact-preview []
  (let [amount @(rf/subscribe [:recurring/form-field :amount])
        frequency @(rf/subscribe [:recurring/form-field :frequency])
        currency @(rf/subscribe [:recurring/form-field :currency])
        category @(rf/subscribe [:recurring/form-field :category])
        parsed-amount (if (and amount (not (str/blank? amount)))
                        (js/parseFloat amount)
                        0)
        annual-cost (* parsed-amount (frequency-multiplier frequency))
        cat-label (-> (some #(when (= (:key %) category) %) panel-categories)
                      :label
                      (or "selected category"))]
    [:div {:class "mt-8 pt-6 border-t border-gray-200 dark:border-gray-700"}
     [:h3 {:class "text-sm font-semibold text-gray-900 dark:text-gray-50 mb-4 flex items-center"}
      [icon :bar-chart {:width 18 :height 18 :class "text-violet-500 mr-2"}]
      "Impact Preview"]
     [:div {:class "bg-violet-50 dark:bg-violet-900/20 rounded-2xl p-5 border border-violet-100 dark:border-violet-800 space-y-5"}
      [:div {:class "flex justify-between items-end"}
       [:div
        [:p {:class "text-xs text-gray-500 dark:text-gray-400 mb-1 uppercase tracking-wide font-semibold"} "Annual Cost"]
        [:p {:class "text-3xl font-bold text-violet-600 dark:text-violet-400"}
         (currency/format-currency annual-cost (or currency :USD))]]
       [:div {:class "text-right"}
        [:span {:class "inline-flex items-center px-2 py-1 rounded-lg bg-white dark:bg-gray-800 text-xs font-medium text-gray-500 dark:text-gray-400 shadow-sm border border-gray-200 dark:border-gray-700"}
         (str "based on " (frequency-label frequency))]]]
      [:div
       [:div {:class "flex justify-between text-xs mb-2"}
        [:span {:class "text-gray-500 dark:text-gray-400"} "Monthly Budget Impact"]
        [:span {:class "font-medium text-gray-900 dark:text-gray-50"} "1.2%"]]
       [:div {:class "h-2.5 w-full bg-gray-200 dark:bg-gray-700 rounded-full overflow-hidden flex"}
        [:div {:class "h-full bg-gray-400 dark:bg-gray-500" :style {:width "65%"}}]
        [:div {:class "h-full bg-violet-500 animate-pulse" :style {:width "5%"}}]]
       [:div {:class "mt-3 flex items-start gap-2 text-xs text-gray-500 dark:text-gray-400 bg-white dark:bg-gray-800 p-2 rounded-lg border border-gray-200 dark:border-gray-700 shadow-sm"}
        [icon :check-circle {:width 14 :height 14 :class "text-green-500 mt-0.5 flex-shrink-0"}]
        [:span (str "Safe! You are still within your ")
         [:strong cat-label]
         " budget limit."]]]]]))

(defn recurring-panel []
  (let [is-open? @(rf/subscribe [:recurring/panel-open?])
        form-valid? @(rf/subscribe [:recurring/form-valid?])
        loading? @(rf/subscribe [:recurring/loading?])]
    [:<>
     (when is-open?
       [:div {:class "fixed inset-0 bg-gray-900/40 backdrop-blur-sm z-40 transition-opacity"
              :on-click #(rf/dispatch [:recurring/close-panel])}])
     [:div {:class (str "fixed top-0 right-0 h-full w-full max-w-md bg-white dark:bg-gray-800 shadow-2xl z-50 "
                        "flex flex-col overflow-hidden transform transition-transform duration-300 ease-out "
                        (if is-open? "translate-x-0" "translate-x-full"))}
      [:div {:class "flex-shrink-0 px-6 py-4 border-b border-gray-200 dark:border-gray-700 flex items-center justify-between"}
       [:h2 {:class "text-xl font-bold text-gray-900 dark:text-gray-50"} "New Recurring Payment"]
       [:button {:class "text-gray-500 dark:text-gray-400 hover:text-gray-900 dark:hover:text-gray-50 transition-colors focus:outline-none"
                 :on-click #(rf/dispatch [:recurring/close-panel])}
        [icon :x {:width 24 :height 24}]]]

      [:div {:class "flex-1 overflow-y-auto p-6"}
       [:form {:class "space-y-6"}
        [payment-name-input]
        [category-grid]
        [currency-amount-row]
        [frequency-selector]
        [start-date-picker]
        [wallet-dropdown]
        [impact-preview]]]

      [:div {:class "flex-shrink-0 px-6 py-4 border-t border-gray-200 dark:border-gray-700 bg-gray-50 dark:bg-gray-800/50"}
       [:div {:class "flex gap-3"}
        [:button {:class (str "flex-1 py-3 px-4 rounded-xl border border-gray-200 dark:border-gray-700 "
                              "text-gray-500 dark:text-gray-400 font-medium "
                              "hover:bg-white dark:hover:bg-gray-700 transition-colors shadow-sm")
                  :type "button"
                  :on-click #(rf/dispatch [:recurring/close-panel])}
         "Cancel"]
        [:button {:class (str "flex-1 py-3 px-4 rounded-xl bg-violet-500 hover:bg-violet-600 text-white font-medium "
                              "shadow-lg shadow-violet-500/30 transition-all "
                              "disabled:opacity-50 disabled:cursor-not-allowed")
                  :type "button"
                  :disabled (or (not form-valid?) loading?)
                  :on-click #(rf/dispatch [:recurring/create])}
         (if loading? "Adding..." "Add Payment")]]]]]))
