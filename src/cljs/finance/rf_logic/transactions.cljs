(ns finance.rf-logic.transactions
  "Transactions page logic - CRUD, filtering, form management."
  (:require [re-frame.core :as rf]
            [clojure.string :as str]
            [finance.db :as db]
            [day8.re-frame.http-fx]
            [ajax.core :as ajax]
            [finance.utils.errors :as errors]
            [finance.utils.filters :as filters]
            [finance.utils.date :as date-utils]
            [finance.constants :as const]))

(rf/reg-event-fx
 :transactions/init
 (fn [_ _]
   {:dispatch [:tx/fetch-transactions]}))

(rf/reg-event-fx
 :tx/fetch-transactions
 (fn [{:keys [db]} _]
   {:db (-> db
            (assoc :loading? true)
            (assoc-in [:transactions-state :error] nil))
    :http-xhrio {:method :get
                 :uri "/api/transactions"
                 :with-credentials true
                 :response-format (ajax/json-response-format {:keywords? true})
                 :on-success [:tx/fetch-transactions-success]
                 :on-failure [:tx/fetch-transactions-failure]}}))

(rf/reg-event-fx
 :tx/fetch-transactions-failure
 (fn [{:keys [db]} [_ error]]
   (errors/log-error "Transactions fetch" error)
   {:db (-> db
            (assoc :loading? false)
            (assoc-in [:transactions-state :error]
                      (errors/extract-error-message error "Failed to load transactions")))
    :dispatch [:app/show-toast
               {:type :error
                :title "Error"
                :message "Failed to load transactions. Please try again."}]}))

(rf/reg-event-db
 :tx/fetch-transactions-success
 (fn [db [_ response]]
   (-> db
       (assoc :loading? false)
       (assoc :error nil)
       (assoc :transactions (:transactions response)))))

(rf/reg-event-fx
 :tx/create-transaction
 (fn [{:keys [db]} _]
   (let [form (:transaction-form db)
         is-recurring? (:is-recurring form)
         payload (cond-> {:amount (js/parseFloat (:amount form))
                          :type (name (:type form))
                          :category (name (:category form))
                          :description (:description form)
                          :tags (mapv name (:tags form))
                          :currency (name (or (:currency form) :COP))}
                   (:exchange-rate form)
                   (assoc :exchange-rate (:exchange-rate form))
                   (not (str/blank? (:notes form)))
                   (assoc :notes (:notes form)))]
     {:db (-> db
              (assoc :loading? true)
              (assoc-in [:transaction-form :pending-recurring?] is-recurring?))
      :http-xhrio {:method :post
                   :uri "/api/transactions"
                   :params payload
                   :format (ajax/json-request-format)
                   :with-credentials true
                   :response-format (ajax/json-response-format {:keywords? true})
                   :on-success [:tx/create-transaction-success]
                   :on-failure [:app/api-error]}})))

(rf/reg-event-fx
 :tx/create-transaction-success
 (fn [{:keys [db]} [_ _response]]
   (let [was-recurring? (get-in db [:transaction-form :pending-recurring?])
         form (:transaction-form db)]
     {:db (-> db
              (assoc :loading? false)
              (assoc :error nil)
              (assoc :transaction-form (:transaction-form db/default-db))
              (assoc-in [:panel :open?] false))
      :dispatch-n (cond-> [[:tx/fetch-transactions]
                           [:dashboard/fetch-summary]
                           [:app/show-toast
                            {:type :success
                             :title "Transaction Added"
                             :message "Your transaction was saved successfully."}]]
                    was-recurring?
                    (conj [:recurring/create-from-transaction form]))})))

(rf/reg-event-fx
 :tx/delete-transaction
 (fn [{:keys [db]} [_ id]]
   {:db (assoc db :loading? true)
    :http-xhrio {:method :delete
                 :uri (str "/api/transactions/" id)
                 :format (ajax/json-request-format)
                 :with-credentials true
                 :response-format (ajax/json-response-format {:keywords? true})
                 :on-success [:tx/delete-transaction-success]
                 :on-failure [:app/api-error]}}))

(rf/reg-event-fx
 :tx/delete-transaction-success
 (fn [{:keys [db]} _]
   {:db (assoc db :loading? false)
    :dispatch-n [[:tx/fetch-transactions]
                 [:dashboard/fetch-summary]
                 [:app/show-toast
                  {:type :success
                   :title "Transaction Deleted"
                   :message "The transaction was removed."}]]}))

(rf/reg-event-db
 :tx/update-filter
 (fn [db [_ field value]]
   (assoc-in db [:filter field] value)))

(rf/reg-event-db
 :tx/clear-filters
 (fn [db _]
   (assoc db :filter (:filter db/default-db))))

(rf/reg-event-db
 :tx/update-form-field
 (fn [db [_ field value]]
   (assoc-in db [:transaction-form field] value)))

(rf/reg-event-db
 :tx/reset-form
 (fn [db _]
   (assoc db :transaction-form (:transaction-form db/default-db))))

(rf/reg-sub
 :tx/transactions
 (fn [db _]
   (:transactions db)))

(rf/reg-sub
 :tx/error
 (fn [db _]
   (get-in db [:transactions-state :error])))

(rf/reg-sub
 :tx/transaction-count
 :<- [:tx/transactions]
 (fn [transactions _]
   (count transactions)))

(rf/reg-sub
 :tx/recent-transactions
 :<- [:tx/transactions]
 (fn [transactions _]
   (take const/recent-transactions-limit transactions)))

(rf/reg-sub
 :tx/filter
 (fn [db _]
   (:filter db)))

(rf/reg-sub
 :tx/filter-search
 :<- [:tx/filter]
 (fn [fltr _]
   (:search fltr)))

(rf/reg-sub
 :tx/filter-type
 :<- [:tx/filter]
 (fn [fltr _]
   (:type fltr)))

(rf/reg-sub
 :tx/filter-category
 :<- [:tx/filter]
 (fn [fltr _]
   (:category fltr)))

(rf/reg-sub
 :tx/filter-currency
 :<- [:tx/filter]
 (fn [fltr _]
   (:currency fltr)))

(rf/reg-sub
 :tx/has-active-filters?
 :<- [:tx/filter]
 (fn [fltr _]
   (filters/has-active-filters? fltr)))

(rf/reg-sub
 :tx/filtered-transactions
 :<- [:tx/transactions]
 :<- [:tx/filter]
 (fn [[transactions fltr] _]
   (filters/apply-filters transactions fltr)))

(rf/reg-sub
 :tx/transactions-by-date
 :<- [:tx/filtered-transactions]
 (fn [transactions _]
   (group-by (fn [tx]
               (date-utils/format-date (:transaction/date tx)))
             transactions)))

(rf/reg-sub
 :tx/transaction-form
 (fn [db _]
   (:transaction-form db)))

(rf/reg-sub
 :tx/form-field
 :<- [:tx/transaction-form]
 (fn [form [_ field]]
   (get form field)))

(rf/reg-sub
 :tx/categories
 (fn [db _]
   (:categories db)))

(rf/reg-sub
 :tx/form-valid?
 :<- [:tx/transaction-form]
 (fn [form _]
   (let [{:keys [amount type category]} form]
     (and (not (empty? (str amount)))
          (some? type)
          (some? category)
          (pos? (js/parseFloat amount))))))

(rf/reg-sub
 :tx/form-amount-display
 :<- [:tx/transaction-form]
 (fn [form _]
   (let [amount (:amount form)]
     (if (str/blank? (str amount))
       "0.00"
       (let [parsed (js/parseFloat amount)]
         (if (js/isNaN parsed)
           "0.00"
           (.toFixed parsed 2)))))))

(rf/reg-sub
 :tx/form-currency
 :<- [:tx/transaction-form]
 (fn [form _]
   (or (:currency form) :COP)))

(rf/reg-sub
 :tx/form-preview
 :<- [:tx/transaction-form]
 (fn [form _]
   (let [amount-str (:amount form)
         amount (when (and amount-str (not (str/blank? (str amount-str))))
                  (let [parsed (js/parseFloat amount-str)]
                    (when-not (js/isNaN parsed) parsed)))
         today (js/Date.)]
     {:amount (or amount 0)
      :type (or (:type form) :expense)
      :category (or (:category form) :other)
      :description (or (:description form) "")
      :currency (or (:currency form) :COP)
      :date (or (:date form)
                (.toLocaleDateString today "en-US" #js {:month "short" :day "numeric"}))})))

(rf/reg-sub
 :tx/form-notes
 :<- [:tx/transaction-form]
 (fn [form _]
   (or (:notes form) "")))

(rf/reg-sub
 :tx/form-is-recurring
 :<- [:tx/transaction-form]
 (fn [form _]
   (boolean (:is-recurring form))))

(rf/reg-sub
 :tx/grouped-by-date-feed
 :<- [:tx/filtered-transactions]
 (fn [transactions _]
   (let [grouped (group-by #(date-utils/format-date (:transaction/date %)) transactions)
         sorted-dates (sort > (keys grouped))]
     (mapv (fn [date]
             {:label date
              :date date
              :transactions (get grouped date [])})
           sorted-dates))))

(rf/reg-sub
 :tx/daily-totals
 :<- [:tx/filtered-transactions]
 (fn [transactions _]
   (reduce (fn [acc tx]
             (let [date (date-utils/format-date (:transaction/date tx))
                   amount (:transaction/amount tx)
                   signed-amount (if (= :income (:transaction/type tx))
                                   amount
                                   (- amount))]
               (update acc date (fnil + 0) signed-amount)))
           {}
           transactions)))

(def category-colors
  {:food "#ef4444"
   :transport "#f97316"
   :entertainment "#eab308"
   :shopping "#22c55e"
   :health "#14b8a6"
   :education "#3b82f6"
   :bills "#8b5cf6"
   :salary "#10b981"
   :freelance "#06b6d4"
   :investment "#6366f1"
   :other "#94a3b8"})

(rf/reg-sub
 :tx/top-categories-data
 :<- [:tx/filtered-transactions]
 (fn [transactions _]
   (let [expenses (filter #(= :expense (:transaction/type %)) transactions)
         by-category (group-by :transaction/category expenses)
         totals (map (fn [[cat txs]]
                       {:category cat
                        :amount (reduce + 0 (map :transaction/amount txs))})
                     by-category)
         sorted (sort-by :amount > totals)
         total (reduce + 0 (map :amount sorted))
         with-percent (mapv (fn [{:keys [category amount]}]
                              {:category category
                               :amount amount
                               :percent (if (pos? total)
                                          (* 100 (/ amount total))
                                          0)
                               :color (get category-colors category "#94a3b8")})
                            sorted)]
     {:segments with-percent
      :total total})))

(def page-size 10)

(rf/reg-sub
 :tx/current-page
 (fn [db _]
   (get-in db [:pagination :current-page] 1)))

(rf/reg-sub
 :tx/total-pages
 :<- [:tx/filtered-transactions]
 (fn [transactions _]
   (let [total (count transactions)]
     (max 1 (js/Math.ceil (/ total page-size))))))

(rf/reg-sub
 :tx/pagination-info
 :<- [:tx/filtered-transactions]
 :<- [:tx/current-page]
 (fn [[transactions current-page] _]
   (let [total (count transactions)
         start (* (dec current-page) page-size)
         end (min total (* current-page page-size))]
     {:showing (if (zero? total)
                 "0"
                 (str (inc start) "-" end " of " total))
      :start start
      :end end
      :total total})))

(rf/reg-event-db
 :tx/set-page
 (fn [db [_ page]]
   (assoc-in db [:pagination :current-page] page)))

(rf/reg-event-db
 :tx/next-page
 (fn [db _]
   (update-in db [:pagination :current-page] (fnil inc 1))))

(rf/reg-event-db
 :tx/prev-page
 (fn [db _]
   (update-in db [:pagination :current-page] (fn [p] (max 1 (dec (or p 1)))))))

(rf/reg-sub
 :tx/paginated-transactions
 :<- [:tx/filtered-transactions]
 :<- [:tx/current-page]
 (fn [[transactions current-page] _]
   (let [start (* (dec current-page) page-size)]
     (->> transactions
          (drop start)
          (take page-size)))))

;; JS .getDay() returns 0=Sunday, 1=Monday, etc.
(def day-labels ["S" "M" "T" "W" "T" "F" "S"])

(rf/reg-sub
 :tx/daily-spending-data
 :<- [:tx/filtered-transactions]
 (fn [transactions _]
   (let [expenses (filter #(= :expense (:transaction/type %)) transactions)
         by-day (group-by #(-> (:transaction/date %)
                               js/Date.
                               .getDay)
                          expenses)
         daily-totals (mapv (fn [day-idx]
                              {:label (nth day-labels day-idx)
                               :amount (reduce + 0 (map :transaction/amount (get by-day day-idx [])))
                               :day day-idx})
                            (range 7))]
     (if (every? #(zero? (:amount %)) daily-totals)
       [{:label "M" :amount 0 :day 0}
        {:label "T" :amount 0 :day 1}
        {:label "W" :amount 0 :day 2}
        {:label "T" :amount 0 :day 3}
        {:label "F" :amount 0 :day 4}
        {:label "S" :amount 0 :day 5}
        {:label "S" :amount 0 :day 6}]
       daily-totals))))

(rf/reg-sub
 :tx/net-cash-flow-data
 :<- [:tx/filtered-transactions]
 (fn [transactions _]
   (let [by-week (group-by #(let [date (js/Date. (:transaction/date %))
                                  week-num (js/Math.ceil (/ (.getDate date) 7))]
                              week-num)
                           transactions)
         weeks (sort (keys by-week))
         week-totals (mapv (fn [week]
                             (let [txs (get by-week week [])
                                   income (reduce + 0 (map :transaction/amount
                                                          (filter #(= :income (:transaction/type %)) txs)))
                                   expenses (reduce + 0 (map :transaction/amount
                                                            (filter #(= :expense (:transaction/type %)) txs)))]
                               (- income expenses)))
                           (or (seq weeks) [1 2 3 4]))
         labels (mapv #(str "Week " %) (or (seq weeks) [1 2 3 4]))]
     {:data (if (empty? week-totals) [0 0 0 0] week-totals)
      :labels (if (empty? labels) ["Week 1" "Week 2" "Week 3" "Week 4"] labels)})))

;; View mode: :table or :feed
(rf/reg-sub
 :tx/view-mode
 (fn [db _]
   (get db :tx-view-mode :table)))

(rf/reg-event-db
 :tx/set-view-mode
 (fn [db [_ mode]]
   (assoc db :tx-view-mode mode)))

(rf/reg-event-db
 :tx/toggle-view-mode
 (fn [db _]
   (update db :tx-view-mode #(if (= % :feed) :table :feed))))
