(ns finance.components.category-icon
  "Reusable component for displaying category icons in colored circles."
  (:require [finance.components.icons :refer [icon]]
            [finance.constants :as const]))

(def size-config
  {:xs {:container "w-6 h-6"   :icon {:width 12 :height 12}}
   :sm {:container "w-8 h-8"   :icon {:width 16 :height 16}}
   :md {:container "w-10 h-10" :icon {:width 20 :height 20}}
   :lg {:container "w-12 h-12" :icon {:width 24 :height 24}}
   :xl {:container "w-14 h-14" :icon {:width 28 :height 28}}})

(defn category-icon
  "Renders a category icon inside a colored circular background.

   Options:
   - :size - :xs (24px), :sm (32px), :md (40px), :lg (48px), :xl (56px)
   - :variant - :default (uses category colors), :income (green), :expense (red)

   Usage:
     [category-icon :groceries]
     [category-icon :groceries {:size :lg}]
     [category-icon :shopping {:size :md :variant :expense}]"
  ([category] (category-icon category {}))
  ([category {:keys [size variant] :or {size :md variant :default}}]
   (let [icon-key (const/get-category-icon category)
         colors (const/get-category-colors category)
         {:keys [container icon-size]} (get size-config size (:md size-config))
         color-classes (case variant
                         :income "bg-green-100 dark:bg-green-900/20 text-green-600 dark:text-green-500"
                         :expense "bg-red-100 dark:bg-red-900/20 text-red-600 dark:text-red-500"
                         (str (:bg colors) " " (:text colors)))]
     [:div {:class (str "rounded-full flex items-center justify-center flex-shrink-0 "
                        container " " color-classes)}
      [icon icon-key (:icon (get size-config size (:md size-config)))]])))

(defn category-badge
  "Renders a category name as a colored badge.

   Usage:
     [category-badge :groceries]
     [category-badge :entertainment]"
  [category]
  (let [colors (const/get-category-colors category)
        cat-name (name (or category :other))]
    [:span {:class (str "px-2 py-0.5 rounded-md text-xs font-medium " (:bg colors) " " (:text colors))}
     cat-name]))
