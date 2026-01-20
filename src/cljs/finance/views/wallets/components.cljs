(ns finance.views.wallets.components
  "Reusable wallet UI components."
  (:require [finance.components.icons :refer [icon]]
            [finance.utils.currency :as currency]
            [clojure.string :as str]))

;; =============================================================================
;; Institution Colors and Icons
;; =============================================================================

(def institution-colors
  {"chase" {:bg "#1a56db" :text "white"}
   "bank of america" {:bg "#012169" :text "white"}
   "wells fargo" {:bg "#d71e28" :text "white"}
   "citi" {:bg "#003B70" :text "white"}
   "capital one" {:bg "#004977" :text "white"}
   "coinbase" {:bg "#0052ff" :text "white"}
   "robinhood" {:bg "#00c805" :text "white"}
   "fidelity" {:bg "#367F2E" :text "white"}
   "vanguard" {:bg "#8B0000" :text "white"}
   "schwab" {:bg "#00A0DF" :text "white"}
   "default" {:bg "#6366f1" :text "white"}})

(defn get-institution-color [institution]
  (get institution-colors (str/lower-case (or institution "")) (get institution-colors "default")))

(defn institution-initials [institution]
  (when institution
    (->> (str/split institution #"\s+")
         (take 2)
         (map #(first %))
         (apply str)
         str/upper-case)))

;; =============================================================================
;; Wallet Icon Component
;; =============================================================================

(defn wallet-icon
  "Renders a wallet/institution icon with initials as fallback.

   Props:
   - wallet: The wallet map
   - size: :sm (24px), :md (40px), :lg (48px) - default :md"
  [{:keys [wallet size] :or {size :md}}]
  (let [institution (:wallet/institution wallet)
        wallet-type (:wallet/type wallet)
        custom-color (:wallet/color wallet)
        {:keys [bg text]} (if custom-color
                            {:bg custom-color :text "white"}
                            (get-institution-color institution))
        sizes {:sm "h-6 w-6 text-xs"
               :md "h-10 w-10 text-sm"
               :lg "h-12 w-12 text-base"}
        size-class (get sizes size "h-10 w-10 text-sm")]
    [:div {:class (str "rounded-xl flex items-center justify-center font-bold " size-class)
           :style {:background-color bg :color text}}
     (if-let [icon-url (:wallet/icon-url wallet)]
       [:img {:src icon-url
              :alt institution
              :class "w-full h-full object-cover rounded-xl"}]
       [:span (institution-initials institution)])]))

;; =============================================================================
;; Status Badge Component
;; =============================================================================

(defn status-badge
  "Renders a status badge (ACTIVE, SYNCED, UPDATING, ERROR).

   Props:
   - status: :active, :synced, :updating, :error
   - size: :sm, :md - default :sm"
  [{:keys [status size] :or {size :sm}}]
  (let [status-styles {:active {:bg "bg-green-100 dark:bg-green-900/30"
                                :text "text-green-600 dark:text-green-400"
                                :dot "bg-green-500"
                                :label "ACTIVE"}
                       :synced {:bg "bg-blue-100 dark:bg-blue-900/30"
                                :text "text-blue-600 dark:text-blue-400"
                                :dot "bg-blue-500"
                                :label "SYNCED"}
                       :updating {:bg "bg-yellow-100 dark:bg-yellow-900/30"
                                  :text "text-yellow-600 dark:text-yellow-400"
                                  :dot "bg-yellow-500 animate-pulse"
                                  :label "UPDATING"}
                       :error {:bg "bg-red-100 dark:bg-red-900/30"
                               :text "text-red-600 dark:text-red-400"
                               :dot "bg-red-500"
                               :label "ERROR"}}
        {:keys [bg text dot label]} (get status-styles (keyword status) (get status-styles :active))
        size-class (if (= size :sm)
                     "px-2 py-0.5 text-[10px]"
                     "px-2.5 py-1 text-xs")]
    [:span {:class (str "inline-flex items-center gap-1 rounded-full font-semibold " bg " " text " " size-class)}
     [:span {:class (str "h-1.5 w-1.5 rounded-full " dot)}]
     label]))

;; =============================================================================
;; Masked Account Number
;; =============================================================================

(defn masked-account-number
  "Renders masked account number (e.g., •••• •••• •••• 4422)."
  [{:keys [account-number class]}]
  (let [last-four (or account-number "")]
    [:span {:class (str "font-mono text-neutral-500 dark:text-neutral-400 " class)}
     (if (seq last-four)
       (str "•••• •••• •••• " last-four)
       "•••• •••• •••• ••••")]))

;; =============================================================================
;; Trend Badge
;; =============================================================================

(defn trend-badge
  "Renders a trend indicator with percentage change.

   Props:
   - change: Percentage change (positive or negative)
   - show-arrow: Whether to show arrow icon - default true"
  [{:keys [change show-arrow] :or {show-arrow true}}]
  (let [positive? (>= (or change 0) 0)
        change-value (Math/abs (or change 0))]
    [:span {:class (str "inline-flex items-center gap-0.5 text-xs font-medium "
                        (if positive?
                          "text-green-600 dark:text-green-400"
                          "text-red-600 dark:text-red-400"))}
     (when show-arrow
       [icon (if positive? :trending-up :trending-down) {:width 14 :height 14}])
     (str (when positive? "+") (.toFixed change-value 1) "%")]))

;; =============================================================================
;; Sparkline Chart
;; =============================================================================

(defn sparkline-chart
  "Renders a small SVG sparkline chart.

   Props:
   - data: Vector of numbers
   - width: SVG width (default 120)
   - height: SVG height (default 40)
   - positive?: Whether trend is positive (affects color)"
  [{:keys [data width height positive?]
    :or {width 120 height 40}}]
  (when (and data (seq data))
    (let [min-val (apply min data)
          max-val (apply max data)
          range-val (- max-val min-val)
          normalized (if (zero? range-val)
                       (repeat (count data) 0.5)
                       (map #(/ (- % min-val) range-val) data))
          padding 4
          chart-width (- width (* 2 padding))
          chart-height (- height (* 2 padding))
          points (map-indexed
                  (fn [i val]
                    (let [x (+ padding (* (/ i (max 1 (dec (count data)))) chart-width))
                          y (+ padding (* (- 1 val) chart-height))]
                      (str x "," y)))
                  normalized)
          path-d (str "M " (str/join " L " points))
          color (if positive? "#10b981" "#ef4444")]
      [:svg {:width width
             :height height
             :viewBox (str "0 0 " width " " height)
             :class "overflow-visible"}
       ;; Gradient fill area
       [:defs
        [:linearGradient {:id (str "sparkline-gradient-" (if positive? "green" "red"))
                          :x1 "0%" :y1 "0%" :x2 "0%" :y2 "100%"}
         [:stop {:offset "0%" :stop-color color :stop-opacity "0.3"}]
         [:stop {:offset "100%" :stop-color color :stop-opacity "0"}]]]
       ;; Fill area
       [:path {:d (str path-d " L " (- width padding) "," (- height padding) " L " padding "," (- height padding) " Z")
               :fill (str "url(#sparkline-gradient-" (if positive? "green" "red") ")")}]
       ;; Line
       [:path {:d path-d
               :fill "none"
               :stroke color
               :stroke-width "2"
               :stroke-linecap "round"
               :stroke-linejoin "round"}]
       ;; End dot
       (let [[x y] (str/split (last points) #",")]
         [:circle {:cx x :cy y :r "3" :fill color}])])))

;; =============================================================================
;; Summary Stat Card
;; =============================================================================

(defn summary-stat-card
  "Renders a summary statistic card.

   Props:
   - title: Card title
   - value: Main value to display
   - subtitle: Optional subtitle
   - icon-name: Icon key
   - icon-bg: Background color class for icon
   - icon-color: Color class for icon
   - change: Optional percentage change
   - class: Additional CSS classes"
  [{:keys [title value subtitle icon-name icon-bg icon-color change class]}]
  [:div {:class (str "bg-white dark:bg-neutral-800 rounded-xl border border-neutral-200 dark:border-neutral-700 p-5 " class)}
   [:div {:class "flex items-start justify-between mb-3"}
    [:div {:class (str "h-10 w-10 rounded-xl flex items-center justify-center " (or icon-bg "bg-violet-100 dark:bg-violet-900/30"))}
     [icon icon-name {:width 20 :height 20 :class (or icon-color "text-violet-600 dark:text-violet-400")}]]
    (when change
      [trend-badge {:change change}])]
   [:div
    [:p {:class "text-2xl font-bold text-neutral-900 dark:text-neutral-50"} value]
    [:p {:class "text-sm text-neutral-500 dark:text-neutral-400"} title]
    (when subtitle
      [:p {:class "text-xs text-neutral-400 dark:text-neutral-500 mt-1"} subtitle])]])

;; =============================================================================
;; Total Assets Header
;; =============================================================================

(defn total-assets-header
  "Renders the total liquid assets header display.

   Props:
   - total: Total amount
   - change: Percentage change
   - currency: Currency symbol"
  [{:keys [total change currency] :or {currency :USD}}]
  [:div {:class "bg-gradient-to-br from-violet-600 to-purple-600 rounded-2xl p-6 text-white mb-6"}
   [:div {:class "flex items-center justify-between"}
    [:div
     [:p {:class "text-violet-200 text-sm font-medium mb-1"} "Total Liquid Assets"]
     [:p {:class "text-3xl font-bold"} (currency/format-currency total currency)]
     (when change
       [:div {:class "flex items-center gap-1 mt-2"}
        [:span {:class (str "inline-flex items-center gap-1 px-2 py-0.5 rounded-full text-xs font-medium "
                            (if (>= change 0)
                              "bg-white/20 text-white"
                              "bg-red-400/20 text-red-200"))}
         [icon (if (>= change 0) :trending-up :trending-down) {:width 12 :height 12}]
         (str (when (>= change 0) "+") (.toFixed change 2) "%")]
        [:span {:class "text-violet-200 text-xs"} "vs last month"]])]
    [:div {:class "h-16 w-16 rounded-2xl bg-white/10 flex items-center justify-center"}
     [icon :wallet {:width 32 :height 32}]]]])

;; =============================================================================
;; View Mode Toggle
;; =============================================================================

(defn view-mode-toggle
  "Renders view mode toggle buttons.

   Props:
   - current-mode: Currently selected mode
   - on-change: Callback function (mode) => void
   - modes: Vector of {:key :mode-key :icon :icon-name :label \"Label\"}"
  [{:keys [current-mode on-change modes]}]
  [:div {:class "flex items-center bg-neutral-100 dark:bg-neutral-700 rounded-xl p-1"}
   (for [{:keys [key icon-name label]} modes]
     ^{:key key}
     [:button {:class (str "flex items-center gap-1.5 px-3 py-1.5 rounded-lg text-sm font-medium transition-all "
                           (if (= current-mode key)
                             "bg-white dark:bg-neutral-600 text-neutral-900 dark:text-neutral-50 shadow-sm"
                             "text-neutral-500 dark:text-neutral-400 hover:text-neutral-700 dark:hover:text-neutral-300"))
               :on-click #(on-change key)
               :title label}
      [icon icon-name {:width 16 :height 16}]
      [:span {:class "hidden sm:inline"} label]])])

;; =============================================================================
;; Filter Tabs
;; =============================================================================

(defn filter-tabs
  "Renders filter tab buttons.

   Props:
   - current-filter: Currently selected filter
   - on-change: Callback function (filter) => void
   - filters: Vector of {:key :filter-key :label \"Label\" :count optional-count}"
  [{:keys [current-filter on-change filters]}]
  [:div {:class "flex flex-wrap gap-2"}
   (for [{:keys [key label count]} filters]
     ^{:key key}
     [:button {:class (str "px-4 py-2 rounded-xl text-sm font-medium transition-all "
                           (if (= current-filter key)
                             "bg-violet-600 text-white shadow-lg shadow-violet-500/30"
                             "bg-neutral-100 dark:bg-neutral-700 text-neutral-600 dark:text-neutral-300 hover:bg-neutral-200 dark:hover:bg-neutral-600"))
               :on-click #(on-change key)}
      label
      (when count
        [:span {:class "ml-1.5 text-xs opacity-70"} (str "(" count ")")])])])

;; =============================================================================
;; Empty State
;; =============================================================================

(defn empty-state
  "Renders an empty state message.

   Props:
   - icon-name: Icon to display
   - title: Title text
   - description: Description text
   - action-label: Optional button label
   - on-action: Optional button callback"
  [{:keys [icon-name title description action-label on-action]}]
  [:div {:class "flex flex-col items-center justify-center text-center py-16 px-6"}
   [:div {:class "mb-4 h-16 w-16 rounded-2xl bg-neutral-100 dark:bg-neutral-700 flex items-center justify-center"}
    [icon icon-name {:width 32 :height 32 :class "text-neutral-400 dark:text-neutral-500"}]]
   [:h3 {:class "text-lg font-semibold text-neutral-600 dark:text-neutral-300 mb-2"} title]
   [:p {:class "text-neutral-500 dark:text-neutral-400 max-w-md mb-6"} description]
   (when (and action-label on-action)
     [:button {:class "px-6 py-3 bg-violet-600 hover:bg-violet-700 text-white font-medium rounded-xl transition-colors shadow-lg shadow-violet-500/30"
               :on-click on-action}
      action-label])])

;; =============================================================================
;; Loading Skeleton
;; =============================================================================

(defn wallet-card-skeleton
  "Renders a loading skeleton for a wallet card."
  []
  [:div {:class "bg-white dark:bg-neutral-800 rounded-2xl border border-neutral-200 dark:border-neutral-700 p-5 animate-pulse"}
   [:div {:class "flex items-start justify-between mb-4"}
    [:div {:class "h-10 w-10 bg-neutral-200 dark:bg-neutral-700 rounded-xl"}]
    [:div {:class "h-5 w-16 bg-neutral-200 dark:bg-neutral-700 rounded-full"}]]
   [:div {:class "space-y-3"}
    [:div {:class "h-5 w-32 bg-neutral-200 dark:bg-neutral-700 rounded"}]
    [:div {:class "h-4 w-40 bg-neutral-200 dark:bg-neutral-700 rounded"}]
    [:div {:class "h-3 w-24 bg-neutral-200 dark:bg-neutral-700 rounded"}]
    [:div {:class "h-6 w-28 bg-neutral-200 dark:bg-neutral-700 rounded mt-4"}]]])

(defn wallet-list-skeleton
  "Renders a loading skeleton for wallet list rows."
  []
  [:div {:class "animate-pulse space-y-3"}
   (for [i (range 3)]
     ^{:key i}
     [:div {:class "flex items-center gap-4 p-4 bg-white dark:bg-neutral-800 rounded-xl border border-neutral-200 dark:border-neutral-700"}
      [:div {:class "h-10 w-10 bg-neutral-200 dark:bg-neutral-700 rounded-xl"}]
      [:div {:class "flex-1 space-y-2"}
       [:div {:class "h-4 w-32 bg-neutral-200 dark:bg-neutral-700 rounded"}]
       [:div {:class "h-3 w-40 bg-neutral-200 dark:bg-neutral-700 rounded"}]]
      [:div {:class "h-10 w-24 bg-neutral-200 dark:bg-neutral-700 rounded"}]
      [:div {:class "h-6 w-20 bg-neutral-200 dark:bg-neutral-700 rounded"}]])])
