(ns finance.views.wallets-panel
  "Slide-in panel for adding and editing wallets."
  (:require [re-frame.core :as rf]
            [clojure.string :as str]
            [finance.db :as db]
            [finance.utils.currency :as currency]
            [finance.components.icons :refer [icon]]))

;; =============================================================================
;; Wallet Type Configuration
;; =============================================================================

(def wallet-types
  [{:key :bank
    :label "Bank Account"
    :description "Checking or savings account"
    :icon :bank
    :bg "bg-blue-100 dark:bg-blue-900/30"
    :text "text-blue-600 dark:text-blue-400"}
   {:key :card
    :label "Credit Card"
    :description "Credit or debit card"
    :icon :credit-card
    :bg "bg-purple-100 dark:bg-purple-900/30"
    :text "text-purple-600 dark:text-purple-400"}
   {:key :crypto
    :label "Crypto Wallet"
    :description "Bitcoin, Ethereum, etc."
    :icon :bitcoin
    :bg "bg-orange-100 dark:bg-orange-900/30"
    :text "text-orange-600 dark:text-orange-400"}
   {:key :investment
    :label "Investment"
    :description "Brokerage or retirement"
    :icon :trending-up
    :bg "bg-green-100 dark:bg-green-900/30"
    :text "text-green-600 dark:text-green-400"}])

(def currencies
  [{:key :USD :label "US Dollar" :symbol "$"}
   {:key :COP :label "Colombian Peso" :symbol "$"}
   {:key :EUR :label "Euro" :symbol "€"}
   {:key :BTC :label "Bitcoin" :symbol "₿"}
   {:key :ETH :label "Ethereum" :symbol "Ξ"}])

(def preset-colors
  ["#6366f1" "#8b5cf6" "#ec4899" "#ef4444" "#f97316"
   "#eab308" "#22c55e" "#14b8a6" "#06b6d4" "#3b82f6"])

;; =============================================================================
;; Type Selector Component
;; =============================================================================

(defn type-card [{:keys [key label description icon-name bg text]} selected? on-click]
  [:label {:class "cursor-pointer group"}
   [:input {:type "radio"
            :name "wallet-type"
            :class "peer sr-only"
            :checked selected?
            :on-change on-click}]
   [:div {:class (str "flex items-center gap-3 p-4 rounded-xl border-2 transition-all duration-200 "
                      "group-hover:shadow-md group-hover:-translate-y-0.5 "
                      (if selected?
                        "border-violet-500 bg-violet-50 dark:bg-violet-900/20 shadow-md"
                        "border-neutral-200 dark:border-neutral-700 bg-white dark:bg-neutral-800 hover:border-neutral-300 dark:hover:border-neutral-600"))}
    [:div {:class (str "h-10 w-10 rounded-xl flex items-center justify-center " bg " " text)}
     [icon icon-name {:width 20 :height 20}]]
    [:div
     [:span {:class (str "block font-medium transition-colors "
                         (if selected?
                           "text-violet-600 dark:text-violet-400"
                           "text-neutral-900 dark:text-neutral-50"))}
      label]
     [:span {:class "text-xs text-neutral-500 dark:text-neutral-400"} description]]]])

(defn type-selector []
  (let [selected @(rf/subscribe [:wallets/form-field :type])]
    [:div {:class "space-y-2"}
     [:label {:class "block text-sm font-medium text-neutral-700 dark:text-neutral-300"} "Account Type"]
     [:div {:class "grid grid-cols-2 gap-3"}
      (for [{:keys [key label description icon bg text]} wallet-types]
        ^{:key key}
        [type-card {:key key :label label :description description :icon-name icon :bg bg :text text}
         (= key selected)
         #(rf/dispatch [:wallets/update-form-field :type key])])]]))

;; =============================================================================
;; Form Fields
;; =============================================================================

(defn text-field [{:keys [label field placeholder type value-key]}]
  (let [value @(rf/subscribe [:wallets/form-field (or value-key field)])]
    [:div {:class "space-y-2"}
     [:label {:class "block text-sm font-medium text-neutral-700 dark:text-neutral-300"} label]
     [:input {:class (str "block w-full px-4 py-3 border border-neutral-200 dark:border-neutral-700 rounded-xl "
                          "bg-neutral-50 dark:bg-neutral-800 text-neutral-900 dark:text-neutral-50 placeholder-neutral-400 "
                          "focus:outline-none focus:ring-2 focus:ring-violet-500 focus:border-transparent "
                          "transition-all sm:text-sm shadow-sm")
              :type (or type "text")
              :placeholder placeholder
              :value (or value "")
              :on-change #(rf/dispatch [:wallets/update-form-field field (-> % .-target .-value)])}]]))

(defn currency-balance-row []
  (let [balance @(rf/subscribe [:wallets/form-field :balance])
        curr @(rf/subscribe [:wallets/form-field :currency])]
    [:div {:class "grid grid-cols-2 gap-4"}
     ;; Currency
     [:div {:class "space-y-2"}
      [:label {:class "block text-sm font-medium text-neutral-700 dark:text-neutral-300"} "Currency"]
      [:div {:class "relative"}
       [:select {:class (str "block w-full pl-3 pr-8 py-3 border border-neutral-200 dark:border-neutral-700 rounded-xl "
                             "bg-neutral-50 dark:bg-neutral-800 text-neutral-900 dark:text-neutral-50 "
                             "appearance-none focus:outline-none focus:ring-2 focus:ring-violet-500 focus:border-transparent "
                             "transition-all sm:text-sm shadow-sm")
                 :value (name (or curr :USD))
                 :on-change #(rf/dispatch [:wallets/update-form-field :currency (keyword (-> % .-target .-value))])}
        (for [{:keys [key label symbol]} currencies]
          ^{:key key}
          [:option {:value (name key)} (str label " (" symbol ")")])]
       [:div {:class "absolute inset-y-0 right-0 pr-2 flex items-center pointer-events-none"}
        [icon :chevron-down {:width 16 :height 16 :class "text-neutral-400"}]]]]

     ;; Balance
     [:div {:class "space-y-2"}
      [:label {:class "block text-sm font-medium text-neutral-700 dark:text-neutral-300"} "Current Balance"]
      [:div {:class "relative"}
       [:div {:class "absolute inset-y-0 left-0 pl-4 flex items-center pointer-events-none"}
        [:span {:class "text-neutral-400 font-semibold"} "$"]]
       [:input {:class (str "block w-full pl-10 pr-3 py-3 border border-neutral-200 dark:border-neutral-700 rounded-xl "
                            "bg-neutral-50 dark:bg-neutral-800 text-neutral-900 dark:text-neutral-50 placeholder-neutral-400 "
                            "focus:outline-none focus:ring-2 focus:ring-violet-500 focus:border-transparent "
                            "transition-all sm:text-sm shadow-sm font-semibold")
                :type "number"
                :step "0.01"
                :placeholder "0.00"
                :value (or balance "")
                :on-change #(rf/dispatch [:wallets/update-form-field :balance (-> % .-target .-value)])}]]]]))

(defn color-picker []
  (let [selected @(rf/subscribe [:wallets/form-field :color])]
    [:div {:class "space-y-2"}
     [:label {:class "block text-sm font-medium text-neutral-700 dark:text-neutral-300"} "Accent Color (Optional)"]
     [:div {:class "flex flex-wrap gap-2"}
      (for [color preset-colors]
        ^{:key color}
        [:button {:class (str "h-8 w-8 rounded-lg transition-all "
                              (if (= color selected)
                                "ring-2 ring-offset-2 ring-violet-500 dark:ring-offset-neutral-800"
                                "hover:scale-110"))
                  :style {:background-color color}
                  :on-click #(rf/dispatch [:wallets/update-form-field :color (if (= color selected) nil color)])}])
      [:button {:class (str "h-8 w-8 rounded-lg border-2 border-dashed border-neutral-300 dark:border-neutral-600 "
                            "flex items-center justify-center hover:border-neutral-400 dark:hover:border-neutral-500 "
                            (when (nil? selected) "ring-2 ring-offset-2 ring-violet-500 dark:ring-offset-neutral-800"))
                :on-click #(rf/dispatch [:wallets/update-form-field :color nil])
                :title "No custom color"}
       [icon :x {:width 14 :height 14 :class "text-neutral-400"}]]]]))

(defn balance-label-field []
  (let [value @(rf/subscribe [:wallets/form-field :balance-label])
        wallet-type @(rf/subscribe [:wallets/form-field :type])
        default-label (case wallet-type
                        :card "Available Credit"
                        :crypto "Total Portfolio"
                        :investment "Total Value"
                        "Current Balance")]
    [:div {:class "space-y-2"}
     [:label {:class "block text-sm font-medium text-neutral-700 dark:text-neutral-300"} "Balance Label"]
     [:input {:class (str "block w-full px-4 py-3 border border-neutral-200 dark:border-neutral-700 rounded-xl "
                          "bg-neutral-50 dark:bg-neutral-800 text-neutral-900 dark:text-neutral-50 placeholder-neutral-400 "
                          "focus:outline-none focus:ring-2 focus:ring-violet-500 focus:border-transparent "
                          "transition-all sm:text-sm shadow-sm")
              :type "text"
              :placeholder default-label
              :value (or value "")
              :on-change #(rf/dispatch [:wallets/update-form-field :balance-label (-> % .-target .-value)])}]
     [:p {:class "text-xs text-neutral-500 dark:text-neutral-400"}
      "Customize how the balance is labeled (e.g., \"Current Balance\", \"Available Credit\")"]]))

;; =============================================================================
;; Preview Card
;; =============================================================================

(defn wallet-preview []
  (let [name @(rf/subscribe [:wallets/form-field :name])
        institution @(rf/subscribe [:wallets/form-field :institution])
        wallet-type @(rf/subscribe [:wallets/form-field :type])
        balance @(rf/subscribe [:wallets/form-field :balance])
        currency-val @(rf/subscribe [:wallets/form-field :currency])
        account-number @(rf/subscribe [:wallets/form-field :account-number])
        color @(rf/subscribe [:wallets/form-field :color])
        balance-label @(rf/subscribe [:wallets/form-field :balance-label])
        parsed-balance (if (and balance (not (str/blank? balance)))
                         (js/parseFloat balance)
                         0)
        type-config (first (filter #(= wallet-type (:key %)) wallet-types))
        initials (when institution
                   (->> (str/split institution #"\s+")
                        (take 2)
                        (map first)
                        (apply str)
                        str/upper-case))]
    [:div {:class "mt-6 pt-5 border-t border-neutral-200 dark:border-neutral-700"}
     [:h3 {:class "text-sm font-semibold text-neutral-900 dark:text-neutral-50 mb-3 flex items-center"}
      [icon :eye {:width 16 :height 16 :class "text-violet-500 mr-2"}]
      "Preview"]
     [:div {:class "relative bg-white dark:bg-neutral-800 rounded-2xl border border-neutral-200 dark:border-neutral-700 p-5 overflow-hidden"}
      ;; Decorative gradient
      [:div {:class "absolute top-0 right-0 w-32 h-32 bg-gradient-to-bl from-violet-500/10 to-transparent rounded-full blur-2xl -translate-y-1/2 translate-x-1/2"}]

      ;; Header
      [:div {:class "flex items-start justify-between mb-4 relative"}
       [:div {:class "h-10 w-10 rounded-xl flex items-center justify-center font-bold text-sm text-white"
              :style {:background-color (or color (:text type-config) "#6366f1")}}
        (or initials "?")]
       [:span {:class "inline-flex items-center gap-1 rounded-full font-semibold px-2 py-0.5 text-[10px] bg-green-100 dark:bg-green-900/30 text-green-600 dark:text-green-400"}
        [:span {:class "h-1.5 w-1.5 rounded-full bg-green-500"}]
        "ACTIVE"]]

      ;; Content
      [:div {:class "relative"}
       [:h3 {:class "font-semibold text-neutral-900 dark:text-neutral-50 mb-1 truncate"}
        (if (str/blank? name) "Account Name" name)]
       [:span {:class "font-mono text-sm text-neutral-500 dark:text-neutral-400"}
        (if (str/blank? account-number)
          "•••• •••• •••• ••••"
          (str "•••• •••• •••• " account-number))]
       [:p {:class "text-xs text-neutral-400 dark:text-neutral-500 mt-1"}
        (or (when-not (str/blank? balance-label) balance-label)
            (case wallet-type
              :card "Available Credit"
              :crypto "Total Portfolio"
              :investment "Total Value"
              "Current Balance"))]

       ;; Balance
       [:p {:class "text-xl font-bold text-neutral-900 dark:text-neutral-50 mt-4"}
        (currency/format-currency parsed-balance (or currency-val :USD))]]]]))

;; =============================================================================
;; Main Panel Component
;; =============================================================================

(defn wallets-panel []
  (let [is-open? @(rf/subscribe [:wallets/panel-open?])
        mode @(rf/subscribe [:wallets/panel-mode])
        form-valid? @(rf/subscribe [:wallets/form-valid?])
        loading? @(rf/subscribe [:wallets/loading?])
        selected-wallet-id @(rf/subscribe [:wallets/selected-wallet-id])]
    [:<>
     ;; Backdrop
     (when is-open?
       [:div {:class "fixed inset-0 bg-neutral-900/40 backdrop-blur-sm z-40 transition-opacity"
              :on-click #(rf/dispatch [:wallets/close-panel])}])

     ;; Panel
     [:div {:class (str "fixed top-0 right-0 h-full w-full max-w-md bg-white dark:bg-neutral-800 shadow-2xl z-50 "
                        "flex flex-col overflow-hidden transform transition-transform duration-300 ease-out "
                        (if is-open? "translate-x-0" "translate-x-full"))}
      ;; Header
      [:div {:class "flex-shrink-0 px-6 py-4 border-b border-neutral-200 dark:border-neutral-700 flex items-center justify-between"}
       [:h2 {:class "text-xl font-bold text-neutral-900 dark:text-neutral-50"}
        (if (= mode :edit) "Edit Wallet" "Add New Wallet")]
       [:button {:class "text-neutral-500 dark:text-neutral-400 hover:text-neutral-900 dark:hover:text-neutral-50 transition-colors focus:outline-none"
                 :on-click #(rf/dispatch [:wallets/close-panel])}
        [icon :x {:width 24 :height 24}]]]

      ;; Form Content
      [:div {:class "flex-1 overflow-y-auto px-6 pb-6"}
       [:form {:class "space-y-5 pt-4"}
        [type-selector]

        [text-field {:label "Account Name"
                     :field :name
                     :placeholder "e.g., Main Checking, Savings"}]

        [text-field {:label "Institution"
                     :field :institution
                     :placeholder "e.g., Chase, Bank of America, Coinbase"}]

        [text-field {:label "Last 4 Digits (Optional)"
                     :field :account-number
                     :placeholder "4422"}]

        [currency-balance-row]

        [balance-label-field]

        [color-picker]

        [wallet-preview]]]

      ;; Footer
      [:div {:class "flex-shrink-0 px-6 py-4 border-t border-neutral-200 dark:border-neutral-700 bg-neutral-50 dark:bg-neutral-800/50"}
       [:div {:class "flex gap-3"}
        [:button {:class (str "flex-1 py-3 px-4 rounded-xl border border-neutral-200 dark:border-neutral-700 "
                              "text-neutral-500 dark:text-neutral-400 font-medium "
                              "hover:bg-white dark:hover:bg-neutral-700 transition-colors shadow-sm")
                  :type "button"
                  :on-click #(rf/dispatch [:wallets/close-panel])}
         "Cancel"]
        [:button {:class (str "flex-1 py-3 px-4 rounded-xl bg-violet-500 hover:bg-violet-600 text-white font-medium "
                              "shadow-lg shadow-violet-500/30 transition-all "
                              "disabled:opacity-50 disabled:cursor-not-allowed")
                  :type "button"
                  :disabled (or (not form-valid?) loading?)
                  :on-click #(if (= mode :edit)
                               (rf/dispatch [:wallets/update selected-wallet-id
                                             (let [form @(rf/subscribe [:wallets/form])]
                                               {:name (:name form)
                                                :type (name (:type form))
                                                :institution (:institution form)
                                                :account-number (:account-number form)
                                                :balance (js/parseFloat (or (:balance form) "0"))
                                                :currency (name (:currency form))
                                                :color (:color form)
                                                :balance-label (:balance-label form)})])
                               (rf/dispatch [:wallets/create]))}
         (cond
           loading? (if (= mode :edit) "Updating..." "Creating...")
           (= mode :edit) "Update Wallet"
           :else "Create Wallet")]]]]]))
