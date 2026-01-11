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

   :chevron-right
   [[:polyline {:points "9 18 15 12 9 6"}]]

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
    [:line {:x1 "21" :y1 "12" :x2 "9" :y2 "12"}]]

   :bar-chart
   [[:line {:x1 "12" :y1 "20" :x2 "12" :y2 "10"}]
    [:line {:x1 "18" :y1 "20" :x2 "18" :y2 "4"}]
    [:line {:x1 "6" :y1 "20" :x2 "6" :y2 "16"}]]

   :trending-up
   [[:polyline {:points "23 6 13.5 15.5 8.5 10.5 1 18"}]
    [:polyline {:points "17 6 23 6 23 12"}]]

   :trending-down
   [[:polyline {:points "23 18 13.5 8.5 8.5 13.5 1 6"}]
    [:polyline {:points "17 18 23 18 23 12"}]]

   :upload
   [[:path {:d "M21 15v4a2 2 0 0 1-2 2H5a2 2 0 0 1-2-2v-4"}]
    [:polyline {:points "17 8 12 3 7 8"}]
    [:line {:x1 "12" :y1 "3" :x2 "12" :y2 "15"}]]

   :download
   [[:path {:d "M21 15v4a2 2 0 0 1-2 2H5a2 2 0 0 1-2-2v-4"}]
    [:polyline {:points "7 10 12 15 17 10"}]
    [:line {:x1 "12" :y1 "15" :x2 "12" :y2 "3"}]]

   :user
   [[:path {:d "M20 21v-2a4 4 0 0 0-4-4H8a4 4 0 0 0-4 4v2"}]
    [:circle {:cx "12" :cy "7" :r "4"}]]

   :shield
   [[:path {:d "M12 22s8-4 8-10V5l-8-3-8 3v7c0 6 8 10 8 10z"}]]

   :edit
   [[:path {:d "M11 4H4a2 2 0 0 0-2 2v14a2 2 0 0 0 2 2h14a2 2 0 0 0 2-2v-7"}]
    [:path {:d "M18.5 2.5a2.121 2.121 0 0 1 3 3L12 15l-4 1 1-4 9.5-9.5z"}]]

   :key
   [[:path {:d "M21 2l-2 2m-7.61 7.61a5.5 5.5 0 1 1-7.778 7.778 5.5 5.5 0 0 1 7.777-7.777zm0 0L15.5 7.5m0 0l3 3L22 7l-3-3m-3.5 3.5L19 4"}]]})

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