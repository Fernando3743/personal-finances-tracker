(ns finance.rf-logic.budget-analysis
  "Budget Analysis page logic - view modes, filters, and data transformations."
  (:require [re-frame.core :as rf]
            [finance.db :as db]
            [clojure.string :as str]))

(rf/reg-event-fx
 :budget-analysis/init
 (fn [_ _]
   {:dispatch-n [[:budgets/fetch]
                 [:budgets/fetch-status]
                 [:tx/fetch-transactions]]}))

(rf/reg-event-db
 :budget-analysis/set-view-mode
 (fn [db [_ mode]]
   ;; Persist to local storage for preference
   (.setItem js/localStorage "budget-analysis-view-mode" (name mode))
   (assoc-in db [:budget-analysis :view-mode] mode)))

(rf/reg-event-db
 :budget-analysis/set-time-period
 (fn [db [_ period]]
   (assoc-in db [:budget-analysis :time-period] period)))

(rf/reg-event-db
 :budget-analysis/set-comparison-period
 (fn [db [_ period]]
   (assoc-in db [:budget-analysis :comparison-period] period)))

(rf/reg-event-db
 :budget-analysis/set-account
 (fn [db [_ account-id]]
   (assoc-in db [:budget-analysis :selected-account] account-id)))

(rf/reg-event-db
 :budget-analysis/set-search
 (fn [db [_ query]]
   (-> db
       (assoc-in [:budget-analysis :search-query] query)
       (assoc-in [:budget-analysis :page] 1))))

(rf/reg-event-db
 :budget-analysis/set-sort
 (fn [db [_ field]]
   (let [current-field (get-in db [:budget-analysis :sort-by])
         current-dir (get-in db [:budget-analysis :sort-dir])
         new-dir (if (= current-field field)
                   (if (= current-dir :asc) :desc :asc)
                   :asc)]
     (-> db
         (assoc-in [:budget-analysis :sort-by] field)
         (assoc-in [:budget-analysis :sort-dir] new-dir)))))

(rf/reg-event-db
 :budget-analysis/set-page
 (fn [db [_ page]]
   (assoc-in db [:budget-analysis :page] page)))

(rf/reg-event-db
 :budget-analysis/set-filter-status
 (fn [db [_ status]]
   (-> db
       (assoc-in [:budget-analysis :filters :status] status)
       (assoc-in [:budget-analysis :page] 1))))

(rf/reg-event-fx
 :budget-analysis/export-csv
 (fn [{:keys [db]} _]
   ;; Export CSV logic - trigger download
   (let [data @(rf/subscribe [:budget-analysis/budget-data])]
     {:dispatch [:app/show-toast
                 {:type :info
                  :title "Export Started"
                  :message "Your CSV file is being generated..."}]})))

(rf/reg-sub
 :budget-analysis/view-mode
 (fn [db _]
   (let [stored (keyword (.getItem js/localStorage "budget-analysis-view-mode"))]
     (or (get-in db [:budget-analysis :view-mode])
         (when (#{:table :grid :envelope} stored) stored)
         :table))))

(rf/reg-sub
 :budget-analysis/time-period
 (fn [db _]
   (get-in db [:budget-analysis :time-period] :this-month)))

(rf/reg-sub
 :budget-analysis/comparison-period
 (fn [db _]
   (get-in db [:budget-analysis :comparison-period] :last-month)))

(rf/reg-sub
 :budget-analysis/selected-account
 (fn [db _]
   (get-in db [:budget-analysis :selected-account])))

(rf/reg-sub
 :budget-analysis/search-query
 (fn [db _]
   (get-in db [:budget-analysis :search-query] "")))

(rf/reg-sub
 :budget-analysis/sort-config
 (fn [db _]
   {:field (get-in db [:budget-analysis :sort-by] :category)
    :dir (get-in db [:budget-analysis :sort-dir] :asc)}))

(rf/reg-sub
 :budget-analysis/pagination
 (fn [db _]
   {:page (get-in db [:budget-analysis :page] 1)
    :page-size (get-in db [:budget-analysis :page-size] 6)}))

(rf/reg-sub
 :budget-analysis/filter-status
 (fn [db _]
   (get-in db [:budget-analysis :filters :status] :all)))

(rf/reg-sub
 :budget-analysis/loading?
 (fn [db _]
   (get-in db [:budget-analysis :loading?] false)))

(rf/reg-sub
 :budget-analysis/current-period-label
 :<- [:budget-analysis/time-period]
 (fn [period _]
   (let [now (js/Date.)
         month (.toLocaleString now "en-US" #js {:month "long"})
         year (.getFullYear now)]
     (case period
       :this-month (str month " " year)
       :last-month (let [d (js/Date. year (dec (.getMonth now)) 1)]
                     (str (.toLocaleString d "en-US" #js {:month "long"}) " " (.getFullYear d)))
       :this-quarter (str "Q" (inc (quot (.getMonth now) 3)) " " year)
       :this-year (str year)
       (str month " " year)))))

(rf/reg-sub
 :budget-analysis/comparison-period-label
 :<- [:budget-analysis/comparison-period]
 (fn [period _]
   (let [now (js/Date.)
         year (.getFullYear now)]
     (case period
       :last-month (let [d (js/Date. year (dec (.getMonth now)) 1)]
                     (str (.toLocaleString d "en-US" #js {:month "long"}) " " (.getFullYear d)))
       :two-months-ago (let [d (js/Date. year (- (.getMonth now) 2) 1)]
                         (str (.toLocaleString d "en-US" #js {:month "long"}) " " (.getFullYear d)))
       "Previous Period"))))

(rf/reg-sub
 :budget-analysis/budget-data
 :<- [:budgets/budget-summaries]
 :<- [:budget-analysis/search-query]
 :<- [:budget-analysis/filter-status]
 :<- [:budget-analysis/sort-config]
 (fn [[summaries query filter-status sort-config] _]
   (let [{:keys [field dir]} sort-config
         ;; Filter by search
         searched (if (str/blank? query)
                    summaries
                    (filter #(str/includes?
                              (str/lower-case (name (get-in % [:budget :budget/category])))
                              (str/lower-case query))
                            summaries))
         ;; Filter by status
         status-filtered (case filter-status
                           :on-track (filter #(= :ok (:status %)) searched)
                           :near-limit (filter #(= :warning (:status %)) searched)
                           :over-budget (filter #(= :exceeded (:status %)) searched)
                           searched)
         ;; Sort
         sorted (sort-by (case field
                           :category #(name (get-in % [:budget :budget/category]))
                           :budgeted #(get-in % [:budget :budget/amount])
                           :spent :spent
                           :difference :remaining
                           :percentage :percentage
                           #(name (get-in % [:budget :budget/category])))
                         (if (= dir :desc) > <)
                         status-filtered)]
     sorted)))

(rf/reg-sub
 :budget-analysis/paginated-data
 :<- [:budget-analysis/budget-data]
 :<- [:budget-analysis/pagination]
 (fn [[data {:keys [page page-size]}] _]
   (let [start (* (dec page) page-size)
         total (count data)]
     {:items (take page-size (drop start data))
      :total total
      :page page
      :page-size page-size
      :total-pages (max 1 (int (Math/ceil (/ total page-size))))})))

(rf/reg-sub
 :budget-analysis/status-counts
 :<- [:budgets/budget-summaries]
 (fn [summaries _]
   (let [grouped (group-by :status summaries)]
     {:all (count summaries)
      :on-track (count (get grouped :ok []))
      :near-limit (count (get grouped :warning []))
      :over-budget (count (get grouped :exceeded []))})))

(rf/reg-sub
 :budget-analysis/category-trends
 :<- [:budget-analysis/budget-data]
 (fn [data _]
   ;; Generate mock trend data - in production, this would come from API
   ;; For demo purposes, we'll calculate a pseudo-random but consistent trend
   (map (fn [item]
          (let [cat-name (name (get-in item [:budget :budget/category] :other))
                ;; Use category name hash for consistent but varied trends
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
        data)))

(rf/reg-sub
 :tx/recent-transactions
 :<- [:tx/transactions]
 (fn [transactions _]
   (->> transactions
        (sort-by :transaction/date >)
        (take 5))))
