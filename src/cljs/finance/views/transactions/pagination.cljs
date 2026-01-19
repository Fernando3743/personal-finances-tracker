(ns finance.views.transactions.pagination
  "Pagination component for transaction lists."
  (:require [re-frame.core :as rf]
            [finance.components.icons :refer [icon]]))

(defn pagination []
  (let [current-page @(rf/subscribe [:tx/current-page])
        total-pages @(rf/subscribe [:tx/total-pages])
        pagination-info @(rf/subscribe [:tx/pagination-info])
        showing (:showing pagination-info)]

    [:div {:class "flex items-center justify-between mt-6 text-sm"}
     ;; Showing info
     [:span {:class "text-neutral-600 dark:text-neutral-400"}
      (str "Showing " showing " transactions")]

     ;; Pagination controls
     [:div {:class "flex items-center gap-2"}
      ;; Previous button
      [:button {:class (str "p-2 rounded-lg transition-colors "
                            (if (<= current-page 1)
                              "text-neutral-300 dark:text-neutral-700 cursor-not-allowed"
                              "text-neutral-600 dark:text-neutral-400 hover:bg-neutral-100 dark:hover:bg-neutral-800"))
                :disabled (<= current-page 1)
                :on-click #(when (> current-page 1)
                             (rf/dispatch [:tx/prev-page]))
                :title "Previous page"}
       [icon :chevron-left {:width 20 :height 20}]]

      ;; Page numbers
      (let [show-pages (cond
                         (<= total-pages 7) (range 1 (inc total-pages))
                         (<= current-page 3) (concat (range 1 5) [:ellipsis total-pages])
                         (>= current-page (- total-pages 2)) (concat [1 :ellipsis] (range (- total-pages 3) (inc total-pages)))
                         :else (concat [1 :ellipsis] (range (dec current-page) (+ current-page 2)) [:ellipsis total-pages]))]
        (for [[idx page-num] (map-indexed vector show-pages)]
          (if (= page-num :ellipsis)
            ^{:key (str "ellipsis-" idx)}
            [:span {:class "px-2 text-neutral-400 dark:text-neutral-600"} "..."]
            ^{:key page-num}
            [:button {:class (str "px-3 py-1.5 rounded-lg font-medium transition-colors "
                                  (if (= page-num current-page)
                                    "bg-violet-600 text-white shadow-sm"
                                    "text-neutral-600 dark:text-neutral-400 hover:bg-neutral-100 dark:hover:bg-neutral-800"))
                      :on-click #(when (not= page-num current-page)
                                   (rf/dispatch [:tx/set-page page-num]))}
             page-num])))

      ;; Next button
      [:button {:class (str "p-2 rounded-lg transition-colors "
                            (if (>= current-page total-pages)
                              "text-neutral-300 dark:text-neutral-700 cursor-not-allowed"
                              "text-neutral-600 dark:text-neutral-400 hover:bg-neutral-100 dark:hover:bg-neutral-800"))
                :disabled (>= current-page total-pages)
                :on-click #(when (< current-page total-pages)
                             (rf/dispatch [:tx/next-page]))
                :title "Next page"}
       [icon :chevron-right {:width 20 :height 20}]]]]))
