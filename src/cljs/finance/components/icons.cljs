(ns finance.components.icons
  "Centralized SVG icon components with configurable props.")

(def icon-paths
  {:wallet
   [[:path {:d "M21 12V7H5a2 2 0 0 1 0-4h14v4"}]
    [:path {:d "M3 5v14a2 2 0 0 0 2 2h16v-5"}]
    [:path {:d "M18 12a2 2 0 0 0 0 4h4v-4h-4z"}]]

   :arrow-up
   [[:line {:x1 "12" :y1 "19" :x2 "12" :y2 "5"}]
    [:polyline {:points "5 12 12 5 19 12"}]]

   :arrow-down
   [[:line {:x1 "12" :y1 "5" :x2 "12" :y2 "19"}]
    [:polyline {:points "19 12 12 19 5 12"}]]

   :search
   [[:circle {:cx "11" :cy "11" :r "8"}]
    [:line {:x1 "21" :y1 "21" :x2 "16.65" :y2 "16.65"}]]

   :filter
   [[:polygon {:points "22 3 2 3 10 12.46 10 19 14 21 14 12.46 22 3"}]]

   :x
   [[:line {:x1 "18" :y1 "6" :x2 "6" :y2 "18"}]
    [:line {:x1 "6" :y1 "6" :x2 "18" :y2 "18"}]]

   :trash
   [[:polyline {:points "3 6 5 6 21 6"}]
    [:path {:d "m19 6-.867 12.142A2 2 0 0 1 16.138 20H7.862a2 2 0 0 1-1.995-1.858L5 6"}]
    [:path {:d "M10 11v6"}]
    [:path {:d "M14 11v6"}]
    [:path {:d "m9 6 1-3h4l1 3"}]]

   :check
   [[:polyline {:points "20 6 9 17 4 12"}]]

   :calendar
   [[:rect {:x "3" :y "4" :width "18" :height "18" :rx "2" :ry "2"}]
    [:line {:x1 "16" :y1 "2" :x2 "16" :y2 "6"}]
    [:line {:x1 "8" :y1 "2" :x2 "8" :y2 "6"}]
    [:line {:x1 "3" :y1 "10" :x2 "21" :y2 "10"}]]

   :chevron-down
   [[:polyline {:points "6 9 12 15 18 9"}]]

   :dashboard
   [[:rect {:x "3" :y "3" :width "7" :height "7"}]
    [:rect {:x "14" :y "3" :width "7" :height "7"}]
    [:rect {:x "14" :y "14" :width "7" :height "7"}]
    [:rect {:x "3" :y "14" :width "7" :height "7"}]]

   :list
   [[:line {:x1 "8" :y1 "6" :x2 "21" :y2 "6"}]
    [:line {:x1 "8" :y1 "12" :x2 "21" :y2 "12"}]
    [:line {:x1 "8" :y1 "18" :x2 "21" :y2 "18"}]
    [:line {:x1 "3" :y1 "6" :x2 "3.01" :y2 "6"}]
    [:line {:x1 "3" :y1 "12" :x2 "3.01" :y2 "12"}]
    [:line {:x1 "3" :y1 "18" :x2 "3.01" :y2 "18"}]]

   :plus
   [[:line {:x1 "12" :y1 "5" :x2 "12" :y2 "19"}]
    [:line {:x1 "5" :y1 "12" :x2 "19" :y2 "12"}]]

   :sun
   [[:circle {:cx "12" :cy "12" :r "5"}]
    [:line {:x1 "12" :y1 "1" :x2 "12" :y2 "3"}]
    [:line {:x1 "12" :y1 "21" :x2 "12" :y2 "23"}]
    [:line {:x1 "4.22" :y1 "4.22" :x2 "5.64" :y2 "5.64"}]
    [:line {:x1 "18.36" :y1 "18.36" :x2 "19.78" :y2 "19.78"}]
    [:line {:x1 "1" :y1 "12" :x2 "3" :y2 "12"}]
    [:line {:x1 "21" :y1 "12" :x2 "23" :y2 "12"}]
    [:line {:x1 "4.22" :y1 "19.78" :x2 "5.64" :y2 "18.36"}]
    [:line {:x1 "18.36" :y1 "5.64" :x2 "19.78" :y2 "4.22"}]]

   :moon
   [[:path {:d "M21 12.79A9 9 0 1 1 11.21 3 7 7 0 0 0 21 12.79z"}]]

   :alert-circle
   [[:circle {:cx "12" :cy "12" :r "10"}]
    [:line {:x1 "12" :y1 "8" :x2 "12" :y2 "12"}]
    [:line {:x1 "12" :y1 "16" :x2 "12.01" :y2 "16"}]]

   :dollar
   [[:line {:x1 "12" :y1 "1" :x2 "12" :y2 "23"}]
    [:path {:d "M17 5H9.5a3.5 3.5 0 0 0 0 7h5a3.5 3.5 0 0 1 0 7H6"}]]

   :logout
   [[:path {:d "M9 21H5a2 2 0 0 1-2-2V5a2 2 0 0 1 2-2h4"}]
    [:polyline {:points "16 17 21 12 16 7"}]
    [:line {:x1 "21" :y1 "12" :x2 "9" :y2 "12"}]]})

(def default-svg-attrs
  {:xmlns "http://www.w3.org/2000/svg"
   :viewBox "0 0 24 24"
   :fill "none"
   :stroke "currentColor"
   :stroke-width "2"
   :stroke-linecap "round"
   :stroke-linejoin "round"})

(defn icon
  "Renders an SVG icon by name with optional props.

   Usage:
     [icon :wallet]
     [icon :arrow-up {:width 20 :height 20}]
     [icon :check {:class \"my-class\" :stroke-width 3}]"
  ([icon-name] (icon icon-name {}))
  ([icon-name props]
   (if-let [paths (get icon-paths icon-name)]
     (let [width (or (:width props) 24)
           height (or (:height props) 24)
           svg-attrs (merge default-svg-attrs
                            {:width width :height height}
                            (dissoc props :width :height))]
       (into [:svg svg-attrs] paths))
     [:svg (merge default-svg-attrs
                  {:width (or (:width props) 24)
                   :height (or (:height props) 24)}
                  (dissoc props :width :height))
      [:circle {:cx "12" :cy "12" :r "10"}]
      [:text {:x "12" :y "16" :text-anchor "middle" :font-size "10"} "?"]])))