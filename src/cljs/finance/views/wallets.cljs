(ns finance.views.wallets
  "Wallets management view with multiple view modes."
  (:require [re-frame.core :as rf]
            [finance.components.icons :refer [icon]]
            [finance.views.wallets.components :as wc]
            [finance.utils.currency :as currency]
            [clojure.string :as str]))

(defn filter-wallets [wallets filter-type]
  (if (= filter-type :all)
    wallets
    (filter #(= (:wallet/type %) filter-type) wallets)))

(defn get-wallet-type-icon [wallet-type]
  (case wallet-type
    :bank :bank
    :card :credit-card
    :crypto :bitcoin
    :investment :trending-up
    :wallet))

(defn format-last-updated [date]
  (if date
    (let [now (js/Date.)
          diff (- (.getTime now) (.getTime (js/Date. date)))
          minutes (Math/floor (/ diff 60000))
          hours (Math/floor (/ minutes 60))]
      (cond
        (< minutes 1) "Just now"
        (< minutes 60) (str minutes " min ago")
        (< hours 24) (str hours " hours ago")
        :else "Yesterday"))
    "Unknown"))

(defn wallet-card
  "Renders a single wallet card for the grid view."
  [wallet]
  (let [{:keys [wallet/id wallet/name wallet/type wallet/institution
                wallet/account-number wallet/balance wallet/currency
                wallet/status wallet/balance-label trend-data change]} wallet
        positive? (>= (or change 0) 0)]
    [:div {:class "group relative bg-white dark:bg-neutral-800 rounded-2xl border border-neutral-200 dark:border-neutral-700 p-5 hover:shadow-xl hover:shadow-neutral-200/50 dark:hover:shadow-neutral-900/50 hover:-translate-y-1 transition-all duration-300 cursor-pointer overflow-hidden"
           :on-click #(rf/dispatch [:wallets/open-panel :edit id])}
     ;; Decorative gradient
     [:div {:class "absolute top-0 right-0 w-32 h-32 bg-gradient-to-bl from-violet-500/10 to-transparent rounded-full blur-2xl -translate-y-1/2 translate-x-1/2"}]

     ;; Header
     [:div {:class "flex items-start justify-between mb-4 relative"}
      [wc/wallet-icon {:wallet wallet :size :md}]
      [wc/status-badge {:status status}]]

     ;; Content
     [:div {:class "relative"}
      [:h3 {:class "font-semibold text-neutral-900 dark:text-neutral-50 mb-1 truncate"} name]
      [wc/masked-account-number {:account-number account-number :class "text-sm"}]
      [:p {:class "text-xs text-neutral-400 dark:text-neutral-500 mt-1"}
       (or balance-label "Current Balance")]

      ;; Balance
      [:div {:class "flex items-end justify-between mt-4"}
       [:p {:class "text-xl font-bold text-neutral-900 dark:text-neutral-50"}
        (currency/format-currency balance currency)]
       (when change
         [wc/trend-badge {:change change}])]]

     ;; Hover actions
     [:div {:class "absolute bottom-4 right-4 opacity-0 group-hover:opacity-100 transition-opacity flex gap-2"}
      [:button {:class "p-2 bg-neutral-100 dark:bg-neutral-700 rounded-lg hover:bg-neutral-200 dark:hover:bg-neutral-600 transition-colors"
                :on-click (fn [e]
                            (.stopPropagation e)
                            (rf/dispatch [:wallets/sync id]))
                :title "Sync wallet"}
       [icon :refresh-cw {:width 14 :height 14 :class "text-neutral-600 dark:text-neutral-300"}]]
      [:button {:class "p-2 bg-neutral-100 dark:bg-neutral-700 rounded-lg hover:bg-neutral-200 dark:hover:bg-neutral-600 transition-colors"
                :on-click (fn [e]
                            (.stopPropagation e)
                            (js/console.log "More options"))
                :title "More options"}
       [icon :more-vertical {:width 14 :height 14 :class "text-neutral-600 dark:text-neutral-300"}]]]]))

(defn add-wallet-card
  "Renders the 'Connect New Account' placeholder card."
  []
  [:div {:class "group flex flex-col items-center justify-center p-8 rounded-2xl border-2 border-dashed border-neutral-300 dark:border-neutral-600 hover:border-violet-500 dark:hover:border-violet-500 hover:bg-violet-50/50 dark:hover:bg-violet-900/10 transition-all cursor-pointer min-h-[220px]"
         :on-click #(rf/dispatch [:wallets/open-panel :create nil])}
   [:div {:class "h-12 w-12 rounded-2xl bg-neutral-100 dark:bg-neutral-700 group-hover:bg-violet-100 dark:group-hover:bg-violet-900/30 flex items-center justify-center mb-3 transition-colors"}
    [icon :plus {:width 24 :height 24 :class "text-neutral-400 dark:text-neutral-500 group-hover:text-violet-600 dark:group-hover:text-violet-400"}]]
   [:p {:class "font-medium text-neutral-600 dark:text-neutral-300 group-hover:text-violet-600 dark:group-hover:text-violet-400"} "Connect New Account"]
   [:p {:class "text-sm text-neutral-400 dark:text-neutral-500 mt-1"} "Link your bank or wallet"]])

(defn card-grid-view
  "Renders the wallet cards in a responsive grid."
  []
  (let [wallets @(rf/subscribe [:wallets/accounts])
        current-filter @(rf/subscribe [:wallets/filter])
        filtered (filter-wallets wallets current-filter)
        loading? @(rf/subscribe [:wallets/loading?])
        total-balance (reduce + 0 (map #(or (:wallet/balance %) 0) wallets))
        filter-counts {:all (count wallets)
                       :bank (count (filter #(= :bank (:wallet/type %)) wallets))
                       :card (count (filter #(= :card (:wallet/type %)) wallets))
                       :crypto (count (filter #(= :crypto (:wallet/type %)) wallets))
                       :investment (count (filter #(= :investment (:wallet/type %)) wallets))}]
    [:div {:class "space-y-6"}
     ;; Total Assets Header
     [wc/total-assets-header {:total total-balance :change 8.4 :currency :USD}]

     ;; Action Bar
     [:div {:class "flex flex-col sm:flex-row sm:items-center justify-between gap-4"}
      ;; Filter Tabs
      [wc/filter-tabs
       {:current-filter current-filter
        :on-change #(rf/dispatch [:wallets/set-filter %])
        :filters [{:key :all :label "All Accounts" :count (:all filter-counts)}
                  {:key :bank :label "Banks" :count (:bank filter-counts)}
                  {:key :card :label "Cards" :count (:card filter-counts)}
                  {:key :crypto :label "Crypto" :count (:crypto filter-counts)}]}]

      ;; Add Button
      [:button {:class "inline-flex items-center gap-2 px-4 py-2.5 bg-violet-600 hover:bg-violet-700 text-white font-medium rounded-xl transition-colors shadow-lg shadow-violet-500/30"
                :on-click #(rf/dispatch [:wallets/open-panel :create nil])}
       [icon :plus {:width 18 :height 18}]
       [:span "Add New Wallet"]]]

     ;; Wallet Grid
     (if loading?
       [:div {:class "grid grid-cols-1 md:grid-cols-2 lg:grid-cols-3 gap-5"}
        (for [i (range 6)]
          ^{:key i}
          [wc/wallet-card-skeleton])]
       (if (seq filtered)
         [:div {:class "grid grid-cols-1 md:grid-cols-2 lg:grid-cols-3 gap-5"}
          (for [wallet filtered]
            ^{:key (:wallet/id wallet)}
            [wallet-card wallet])
          [add-wallet-card]]
         [wc/empty-state
          {:icon-name :wallet
           :title "No Wallets Found"
           :description (if (= current-filter :all)
                          "You haven't added any wallets yet. Connect your bank accounts, credit cards, or crypto wallets to get started."
                          "No wallets match the current filter. Try selecting a different category.")
           :action-label "Add Your First Wallet"
           :on-action #(rf/dispatch [:wallets/open-panel :create nil])}]))]))

(defn account-list-row
  "Renders a single wallet row for the list view."
  [wallet]
  (let [{:keys [wallet/id wallet/name wallet/type wallet/institution
                wallet/account-number wallet/balance wallet/currency
                wallet/status wallet/last-updated trend-data change]} wallet
        positive? (>= (or change 0) 0)]
    [:div {:class "group flex items-center gap-4 p-4 bg-white dark:bg-neutral-800 rounded-xl border border-neutral-200 dark:border-neutral-700 hover:shadow-lg hover:shadow-neutral-200/50 dark:hover:shadow-neutral-900/50 transition-all cursor-pointer"
           :on-click #(rf/dispatch [:wallets/open-panel :edit id])}
     ;; Icon
     [wc/wallet-icon {:wallet wallet :size :md}]

     ;; Account Info
     [:div {:class "flex-1 min-w-0"}
      [:div {:class "flex items-center gap-2"}
       [:h3 {:class "font-semibold text-neutral-900 dark:text-neutral-50 truncate"} name]
       [wc/status-badge {:status status :size :sm}]]
      [:div {:class "flex items-center gap-2 mt-0.5"}
       [:span {:class "text-sm text-neutral-500 dark:text-neutral-400"} institution]
       (when (seq account-number)
         [:<>
          [:span {:class "text-neutral-300 dark:text-neutral-600"} "•"]
          [wc/masked-account-number {:account-number account-number :class "text-sm"}]])]]

     ;; Sparkline (hidden on mobile)
     [:div {:class "hidden lg:block w-32"}
      [wc/sparkline-chart {:data trend-data :width 120 :height 40 :positive? positive?}]]

     ;; Balance & Change
     [:div {:class "text-right"}
      [:p {:class "font-bold text-neutral-900 dark:text-neutral-50"}
       (currency/format-currency balance currency)]
      (when change
        [:div {:class "flex items-center justify-end gap-1 mt-0.5"}
         [wc/trend-badge {:change change :show-arrow false}]])]

     ;; Last Updated
     [:div {:class "hidden sm:block text-right w-20"}
      [:p {:class "text-xs text-neutral-400 dark:text-neutral-500"}
       (format-last-updated (:wallet/last-updated wallet))]]

     ;; Actions
     [:div {:class "flex items-center gap-1 opacity-0 group-hover:opacity-100 transition-opacity"}
      [:button {:class "p-2 hover:bg-neutral-100 dark:hover:bg-neutral-700 rounded-lg transition-colors"
                :on-click (fn [e]
                            (.stopPropagation e)
                            (rf/dispatch [:wallets/sync id]))
                :title "Sync"}
       [icon :refresh-cw {:width 16 :height 16 :class "text-neutral-500 dark:text-neutral-400"}]]
      [:button {:class "p-2 hover:bg-neutral-100 dark:hover:bg-neutral-700 rounded-lg transition-colors"
                :on-click (fn [e]
                            (.stopPropagation e))
                :title "More"}
       [icon :more-vertical {:width 16 :height 16 :class "text-neutral-500 dark:text-neutral-400"}]]]]))

(defn account-section
  "Renders a section of accounts grouped by type."
  [{:keys [title icon-name total-balance wallets]}]
  (when (seq wallets)
    [:div {:class "space-y-3"}
     ;; Section Header
     [:div {:class "flex items-center justify-between"}
      [:div {:class "flex items-center gap-2"}
       [:div {:class "h-8 w-8 rounded-lg bg-neutral-100 dark:bg-neutral-700 flex items-center justify-center"}
        [icon icon-name {:width 16 :height 16 :class "text-neutral-600 dark:text-neutral-400"}]]
       [:h3 {:class "font-semibold text-neutral-900 dark:text-neutral-50"} title]]
      [:span {:class "text-sm font-medium text-neutral-500 dark:text-neutral-400"}
       (currency/format-currency total-balance :USD)]]

     ;; Account Rows
     [:div {:class "space-y-2"}
      (for [wallet wallets]
        ^{:key (:wallet/id wallet)}
        [account-list-row wallet])]]))

(defn list-view
  "Renders the detailed asset performance list view."
  []
  (let [wallets @(rf/subscribe [:wallets/accounts])
        loading? @(rf/subscribe [:wallets/loading?])
        total-balance (reduce + 0 (map #(or (:wallet/balance %) 0) wallets))
        monthly-change 8.4
        grouped (group-by :wallet/type wallets)
        bank-total (reduce + 0 (map #(or (:wallet/balance %) 0) (:bank grouped)))
        card-total (reduce + 0 (map #(or (:wallet/balance %) 0) (:card grouped)))
        crypto-total (reduce + 0 (map #(or (:wallet/balance %) 0) (:crypto grouped)))
        investment-total (reduce + 0 (map #(or (:wallet/balance %) 0) (:investment grouped)))]
    [:div {:class "space-y-6"}
     ;; Breadcrumb
     [:nav {:class "flex items-center gap-2 text-sm text-neutral-500 dark:text-neutral-400"}
      [:span {:class "hover:text-neutral-700 dark:hover:text-neutral-300 cursor-pointer"} "Overview"]
      [icon :chevron-right {:width 14 :height 14}]
      [:span {:class "text-neutral-900 dark:text-neutral-50 font-medium"} "Wallets"]]

     ;; Header
     [:div {:class "flex flex-col sm:flex-row sm:items-center justify-between gap-4"}
      [:div
       [:h1 {:class "text-2xl font-bold text-neutral-900 dark:text-neutral-50"} "Wallets & Accounts"]
       [:p {:class "text-neutral-500 dark:text-neutral-400 mt-1"} "Track all your accounts in one place"]]
      [:div {:class "flex items-center gap-3"}
       [:button {:class "inline-flex items-center gap-2 px-4 py-2 bg-neutral-100 dark:bg-neutral-700 hover:bg-neutral-200 dark:hover:bg-neutral-600 text-neutral-700 dark:text-neutral-300 font-medium rounded-xl transition-colors"
                 :on-click #(rf/dispatch [:wallets/refresh-all])}
        [icon :refresh-cw {:width 16 :height 16}]
        [:span "Refresh All"]]
       [:button {:class "inline-flex items-center gap-2 px-4 py-2.5 bg-violet-600 hover:bg-violet-700 text-white font-medium rounded-xl transition-colors shadow-lg shadow-violet-500/30"
                 :on-click #(rf/dispatch [:wallets/open-panel :create nil])}
        [icon :plus {:width 18 :height 18}]
        [:span "Add New Account"]]]]

     ;; Summary Cards
     [:div {:class "grid grid-cols-1 sm:grid-cols-3 gap-4"}
      [wc/summary-stat-card
       {:title "Total Net Worth"
        :value (currency/format-currency total-balance :USD)
        :icon-name :wallet
        :icon-bg "bg-violet-100 dark:bg-violet-900/30"
        :icon-color "text-violet-600 dark:text-violet-400"
        :change monthly-change}]
      [wc/summary-stat-card
       {:title "Monthly Change"
        :value (str (if (>= monthly-change 0) "+" "") (.toFixed monthly-change 1) "%")
        :subtitle "vs last month"
        :icon-name :trending-up
        :icon-bg "bg-green-100 dark:bg-green-900/30"
        :icon-color "text-green-600 dark:text-green-400"}]
      [wc/summary-stat-card
       {:title "Total Assets"
        :value (str (count wallets) " accounts")
        :subtitle "across all institutions"
        :icon-name :layers
        :icon-bg "bg-blue-100 dark:bg-blue-900/30"
        :icon-color "text-blue-600 dark:text-blue-400"}]]

     ;; Account Sections
     (if loading?
       [wc/wallet-list-skeleton]
       [:div {:class "space-y-8"}
        [account-section
         {:title "Cash & Bank"
          :icon-name :bank
          :total-balance bank-total
          :wallets (:bank grouped)}]
        [account-section
         {:title "Credit Cards"
          :icon-name :credit-card
          :total-balance card-total
          :wallets (:card grouped)}]
        [account-section
         {:title "Crypto"
          :icon-name :bitcoin
          :total-balance crypto-total
          :wallets (:crypto grouped)}]
        [account-section
         {:title "Investments"
          :icon-name :trending-up
          :total-balance investment-total
          :wallets (:investment grouped)}]])]))

(defn net-worth-chart
  "Renders the portfolio net worth chart with time range toggles."
  []
  (let [time-range (atom :30d)
        data [120000 125000 130000 128000 135000 140000 142890]]
    [:div {:class "bg-white dark:bg-neutral-800 rounded-2xl border border-neutral-200 dark:border-neutral-700 p-6"}
     [:div {:class "flex items-center justify-between mb-6"}
      [:div
       [:p {:class "text-sm text-neutral-500 dark:text-neutral-400"} "Total Net Worth"]
       [:p {:class "text-3xl font-bold text-neutral-900 dark:text-neutral-50 mt-1"}
        (currency/format-currency 142890.55 :USD)]
       [:div {:class "flex items-center gap-2 mt-2"}
        [wc/trend-badge {:change 8.4}]
        [:span {:class "text-sm text-neutral-500 dark:text-neutral-400"} "vs last month"]]]
      [:div {:class "flex items-center gap-1 bg-neutral-100 dark:bg-neutral-700 rounded-xl p-1"}
       (for [[key label] [[:24h "24H"] [:7d "7D"] [:30d "30D"] [:1y "1Y"]]]
         ^{:key key}
         [:button {:class (str "px-3 py-1.5 text-sm font-medium rounded-lg transition-colors "
                               (if (= @time-range key)
                                 "bg-white dark:bg-neutral-600 text-neutral-900 dark:text-neutral-50 shadow-sm"
                                 "text-neutral-500 dark:text-neutral-400 hover:text-neutral-700 dark:hover:text-neutral-300"))
                   :on-click #(reset! time-range key)}
          label])]]

     ;; Chart area
     [:div {:class "h-48 flex items-end justify-between gap-1 mt-4"}
      (for [[i val] (map-indexed vector data)]
        ^{:key i}
        [:div {:class "flex-1 bg-gradient-to-t from-violet-500/20 to-violet-500/5 dark:from-violet-400/20 dark:to-violet-400/5 rounded-t-lg transition-all hover:from-violet-500/30 hover:to-violet-500/10"
               :style {:height (str (* (/ val 150000) 100) "%")}}])]]))

(defn currency-converter-widget
  "Renders the quick currency converter widget."
  []
  (let [from-amount (atom "1000")
        from-currency (atom "USD")
        to-currency (atom "BTC")
        result (atom nil)]
    (fn []
      [:div {:class "bg-white dark:bg-neutral-800 rounded-2xl border border-neutral-200 dark:border-neutral-700 p-6"}
       [:h3 {:class "font-semibold text-neutral-900 dark:text-neutral-50 mb-4"} "Quick Convert"]

       ;; From
       [:div {:class "space-y-2"}
        [:label {:class "text-sm text-neutral-500 dark:text-neutral-400"} "From"]
        [:div {:class "flex gap-2"}
         [:input {:class "flex-1 px-4 py-3 bg-neutral-50 dark:bg-neutral-700 border border-neutral-200 dark:border-neutral-600 rounded-xl text-neutral-900 dark:text-neutral-50 font-medium focus:outline-none focus:ring-2 focus:ring-violet-500"
                  :type "number"
                  :value @from-amount
                  :on-change #(reset! from-amount (-> % .-target .-value))}]
         [:select {:class "px-4 py-3 bg-neutral-50 dark:bg-neutral-700 border border-neutral-200 dark:border-neutral-600 rounded-xl text-neutral-900 dark:text-neutral-50 font-medium focus:outline-none focus:ring-2 focus:ring-violet-500"
                   :value @from-currency
                   :on-change #(reset! from-currency (-> % .-target .-value))}
          [:option {:value "USD"} "USD"]
          [:option {:value "EUR"} "EUR"]
          [:option {:value "COP"} "COP"]]]]

       ;; Swap button
       [:div {:class "flex justify-center my-3"}
        [:button {:class "p-2 bg-neutral-100 dark:bg-neutral-700 rounded-full hover:bg-neutral-200 dark:hover:bg-neutral-600 transition-colors"
                  :on-click #(let [temp @from-currency]
                               (reset! from-currency @to-currency)
                               (reset! to-currency temp))}
         [icon :repeat {:width 18 :height 18 :class "text-neutral-500 dark:text-neutral-400"}]]]

       ;; To
       [:div {:class "space-y-2"}
        [:label {:class "text-sm text-neutral-500 dark:text-neutral-400"} "To"]
        [:div {:class "flex gap-2"}
         [:div {:class "flex-1 px-4 py-3 bg-neutral-100 dark:bg-neutral-700 border border-neutral-200 dark:border-neutral-600 rounded-xl text-neutral-900 dark:text-neutral-50 font-medium"}
          (or @result "—")]
         [:select {:class "px-4 py-3 bg-neutral-50 dark:bg-neutral-700 border border-neutral-200 dark:border-neutral-600 rounded-xl text-neutral-900 dark:text-neutral-50 font-medium focus:outline-none focus:ring-2 focus:ring-violet-500"
                   :value @to-currency
                   :on-change #(reset! to-currency (-> % .-target .-value))}
          [:option {:value "BTC"} "BTC"]
          [:option {:value "ETH"} "ETH"]
          [:option {:value "USD"} "USD"]]]]

       ;; Convert button
       [:button {:class "w-full mt-4 py-3 bg-violet-600 hover:bg-violet-700 text-white font-medium rounded-xl transition-colors"
                 :on-click #(reset! result "0.0234 BTC")}
        "Preview Conversion"]])))

(defn fiat-accounts-table
  "Renders the fiat accounts table for portfolio view."
  []
  (let [wallets @(rf/subscribe [:wallets/accounts])
        fiat-wallets (filter #(#{:USD :COP :EUR :GBP} (:wallet/currency %)) wallets)]
    [:div {:class "bg-white dark:bg-neutral-800 rounded-2xl border border-neutral-200 dark:border-neutral-700 overflow-hidden"}
     [:div {:class "p-4 border-b border-neutral-200 dark:border-neutral-700 flex items-center justify-between"}
      [:h3 {:class "font-semibold text-neutral-900 dark:text-neutral-50"} "Fiat Accounts"]
      [:button {:class "text-sm text-violet-600 dark:text-violet-400 font-medium hover:underline"
                :on-click #(rf/dispatch [:wallets/open-panel :create nil])}
       "Add Account"]]
     [:div {:class "divide-y divide-neutral-200 dark:divide-neutral-700"}
      (for [wallet fiat-wallets]
        ^{:key (:wallet/id wallet)}
        [:div {:class "flex items-center justify-between p-4 hover:bg-neutral-50 dark:hover:bg-neutral-700/50 transition-colors cursor-pointer"
               :on-click #(rf/dispatch [:wallets/open-panel :edit (:wallet/id wallet)])}
         [:div {:class "flex items-center gap-3"}
          [wc/wallet-icon {:wallet wallet :size :sm}]
          [:div
           [:p {:class "font-medium text-neutral-900 dark:text-neutral-50"} (:wallet/institution wallet)]
           [:p {:class "text-sm text-neutral-500 dark:text-neutral-400"} (:wallet/name wallet)]]]
         [:div {:class "text-right"}
          [:p {:class "font-medium text-neutral-900 dark:text-neutral-50"}
           (currency/format-currency (:wallet/balance wallet) (:wallet/currency wallet))]
          [:p {:class "text-sm text-neutral-500 dark:text-neutral-400"}
           (currency/format-currency (:wallet/balance wallet) :USD)]]
         [icon :chevron-right {:width 16 :height 16 :class "text-neutral-400 dark:text-neutral-500 ml-2"}]])]]))

(defn crypto-assets-table
  "Renders the crypto assets table for portfolio view."
  []
  (let [wallets @(rf/subscribe [:wallets/accounts])
        crypto-wallets (filter #(#{:BTC :ETH :SOL} (:wallet/currency %)) wallets)]
    [:div {:class "bg-white dark:bg-neutral-800 rounded-2xl border border-neutral-200 dark:border-neutral-700 overflow-hidden"}
     [:div {:class "p-4 border-b border-neutral-200 dark:border-neutral-700 flex items-center justify-between"}
      [:h3 {:class "font-semibold text-neutral-900 dark:text-neutral-50"} "Crypto Assets"]
      [:button {:class "text-sm text-violet-600 dark:text-violet-400 font-medium hover:underline"
                :on-click #(rf/dispatch [:wallets/open-panel :create nil])}
       "Add Wallet"]]
     [:div {:class "divide-y divide-neutral-200 dark:divide-neutral-700"}
      (for [wallet crypto-wallets]
        ^{:key (:wallet/id wallet)}
        [:div {:class "flex items-center justify-between p-4 hover:bg-neutral-50 dark:hover:bg-neutral-700/50 transition-colors cursor-pointer"
               :on-click #(rf/dispatch [:wallets/open-panel :edit (:wallet/id wallet)])}
         [:div {:class "flex items-center gap-3"}
          [wc/wallet-icon {:wallet wallet :size :sm}]
          [:div
           [:p {:class "font-medium text-neutral-900 dark:text-neutral-50"} (name (:wallet/currency wallet))]
           [:p {:class "text-sm text-neutral-500 dark:text-neutral-400"} (:wallet/institution wallet)]]]
         [:div {:class "text-center"}
          [:p {:class "text-sm text-green-600 dark:text-green-400 font-medium"}
           (str "+" (.toFixed (or (:change wallet) 0) 1) "%")]
          [:p {:class "text-xs text-neutral-500 dark:text-neutral-400"} "24h"]]
         [:div {:class "text-right"}
          [:p {:class "font-medium text-neutral-900 dark:text-neutral-50"}
           (currency/format-currency (:wallet/balance wallet) :USD)]
          [:p {:class "text-sm text-neutral-500 dark:text-neutral-400"}
           (str "0.52 " (name (:wallet/currency wallet)))]]])]]))

(defn market-ticker
  "Renders the market ticker bar."
  []
  (let [tickers [{:symbol "BTC/USD" :price 97500 :change 2.4}
                 {:symbol "ETH/USD" :price 3450 :change -1.2}
                 {:symbol "EUR/USD" :price 1.085 :change 0.3}
                 {:symbol "SOL/USD" :price 185 :change 5.6}
                 {:symbol "GBP/USD" :price 1.27 :change -0.1}]]
    [:div {:class "bg-neutral-900 dark:bg-neutral-950 rounded-xl p-3 flex items-center gap-6 overflow-x-auto"}
     (for [{:keys [symbol price change]} tickers]
       ^{:key symbol}
       [:div {:class "flex items-center gap-3 flex-shrink-0"}
        [:span {:class "text-neutral-400 text-sm"} symbol]
        [:span {:class "text-white font-medium"}
         (if (< price 10) (.toFixed price 3) (.toLocaleString price))]
        [:span {:class (str "text-sm font-medium "
                            (if (>= change 0)
                              "text-green-400"
                              "text-red-400"))}
         (str (when (>= change 0) "+") (.toFixed change 1) "%")]])]))

(defn portfolio-view
  "Renders the multi-currency portfolio view."
  []
  [:div {:class "space-y-6"}
   ;; Header
   [:div {:class "flex flex-col sm:flex-row sm:items-center justify-between gap-4"}
    [:div
     [:h1 {:class "text-2xl font-bold text-neutral-900 dark:text-neutral-50"} "Portfolio Overview"]
     [:p {:class "text-neutral-500 dark:text-neutral-400 mt-1"} "Multi-currency portfolio tracker"]]
    [:div {:class "flex items-center gap-2"}
     [:span {:class "text-sm text-neutral-500 dark:text-neutral-400"} "Base currency:"]
     [:select {:class "px-3 py-1.5 bg-neutral-100 dark:bg-neutral-700 border-0 rounded-lg text-neutral-900 dark:text-neutral-50 font-medium text-sm focus:outline-none focus:ring-2 focus:ring-violet-500"}
      [:option {:value "USD"} "USD"]
      [:option {:value "EUR"} "EUR"]
      [:option {:value "COP"} "COP"]]]]

   ;; Main Content Grid
   [:div {:class "grid grid-cols-1 lg:grid-cols-3 gap-6"}
    [:div {:class "lg:col-span-2"}
     [net-worth-chart]]
    [:div
     [currency-converter-widget]]]

   ;; Accounts Grid
   [:div {:class "grid grid-cols-1 lg:grid-cols-2 gap-6"}
    [fiat-accounts-table]
    [crypto-assets-table]]

   ;; Market Ticker
   [market-ticker]])

(defn wallets-view
  "Main wallets page with view mode toggle."
  []
  (let [view-mode @(rf/subscribe [:wallets/view-mode])
        loading? @(rf/subscribe [:wallets/loading?])]
    [:div {:class "space-y-6"}
     ;; Page Header with View Toggle
     [:div {:class "flex flex-col sm:flex-row sm:items-center justify-between gap-4"}
      [:div
       [:h1 {:class "text-2xl font-bold text-neutral-900 dark:text-neutral-50"} "Wallets & Accounts"]
       [:p {:class "text-neutral-500 dark:text-neutral-400 mt-1"} "Manage all your accounts in one place"]]

      ;; View Mode Toggle
      [wc/view-mode-toggle
       {:current-mode view-mode
        :on-change #(rf/dispatch [:wallets/set-view-mode %])
        :modes [{:key :grid :icon-name :grid :label "Grid"}
                {:key :list :icon-name :list :label "List"}
                {:key :portfolio :icon-name :pie-chart :label "Portfolio"}]}]]

     ;; Conditional View Rendering
     (case view-mode
       :grid [card-grid-view]
       :list [list-view]
       :portfolio [portfolio-view]
       [card-grid-view])]))
