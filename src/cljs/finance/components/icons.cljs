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

   :chevron-left
   [[:polyline {:points "15 18 9 12 15 6"}]]

   :grid
   [[:rect {:x "3" :y "3" :width "7" :height "7"}]
    [:rect {:x "14" :y "3" :width "7" :height "7"}]
    [:rect {:x "14" :y "14" :width "7" :height "7"}]
    [:rect {:x "3" :y "14" :width "7" :height "7"}]]

   :credit-card
   [[:rect {:x "1" :y "4" :width "22" :height "16" :rx "2" :ry "2"}]
    [:line {:x1 "1" :y1 "10" :x2 "23" :y2 "10"}]]

   :clock
   [[:circle {:cx "12" :cy "12" :r "10"}]
    [:polyline {:points "12 6 12 12 16 14"}]]

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
   [[:path {:d "M21 2l-2 2m-7.61 7.61a5.5 5.5 0 1 1-7.778 7.778 5.5 5.5 0 0 1 7.777-7.777zm0 0L15.5 7.5m0 0l3 3L22 7l-3-3m-3.5 3.5L19 4"}]]

   :mail
   [[:path {:d "M4 4h16c1.1 0 2 .9 2 2v12c0 1.1-.9 2-2 2H4c-1.1 0-2-.9-2-2V6c0-1.1.9-2 2-2z"}]
    [:polyline {:points "22,6 12,13 2,6"}]]

   :lock
   [[:rect {:x "3" :y "11" :width "18" :height "11" :rx "2" :ry "2"}]
    [:path {:d "M7 11V7a5 5 0 0 1 10 0v4"}]]

   :github
   [[:path {:d "M9 19c-5 1.5-5-2.5-7-3m14 6v-3.87a3.37 3.37 0 0 0-.94-2.61c3.14-.35 6.44-1.54 6.44-7A5.44 5.44 0 0 0 20 4.77 5.07 5.07 0 0 0 19.91 1S18.73.65 16 2.48a13.38 13.38 0 0 0-7 0C6.27.65 5.09 1 5.09 1A5.07 5.07 0 0 0 5 4.77a5.44 5.44 0 0 0-1.5 3.78c0 5.42 3.3 6.61 6.44 7A3.37 3.37 0 0 0 9 18.13V22"}]]

   :google
   [[:path {:d "M22.56 12.25c0-.78-.07-1.53-.2-2.25H12v4.26h5.92c-.26 1.37-1.04 2.53-2.21 3.31v2.77h3.57c2.08-1.92 3.28-4.74 3.28-8.09z" :fill "#4285F4" :stroke "none"}]
    [:path {:d "M12 23c2.97 0 5.46-.98 7.28-2.66l-3.57-2.77c-.98.66-2.23 1.06-3.71 1.06-2.86 0-5.29-1.93-6.16-4.53H2.18v2.84C3.99 20.53 7.7 23 12 23z" :fill "#34A853" :stroke "none"}]
    [:path {:d "M5.84 14.09c-.22-.66-.35-1.36-.35-2.09s.13-1.43.35-2.09V7.07H2.18C1.43 8.55 1 10.22 1 12s.43 3.45 1.18 4.93l2.85-2.22.81-.62z" :fill "#FBBC05" :stroke "none"}]
    [:path {:d "M12 5.38c1.62 0 3.06.56 4.21 1.64l3.15-3.15C17.45 2.09 14.97 1 12 1 7.7 1 3.99 3.47 2.18 7.07l3.66 2.84c.87-2.6 3.3-4.53 6.16-4.53z" :fill "#EA4335" :stroke "none"}]]

   :refresh-cw
   [[:polyline {:points "23 4 23 10 17 10"}]
    [:polyline {:points "1 20 1 14 7 14"}]
    [:path {:d "M3.51 9a9 9 0 0 1 14.85-3.36L23 10M1 14l4.64 4.36A9 9 0 0 0 20.49 15"}]]

   :piggy-bank
   [[:path {:d "M19 5c-1.5 0-2.8 1.4-3 2-3.5-1.5-11-.3-11 5 0 1.8 0 3 2 4.5V20h4v-2h3v2h4v-4c1-.5 1.7-1 2-2h2v-4h-2c0-1-.5-1.5-1-2V5z"}]
    [:path {:d "M2 9v1c0 1.1.9 2 2 2h1"}]
    [:path {:d "M16 11h.01"}]]

   :toggle-right
   [[:rect {:x "1" :y "5" :width "22" :height "14" :rx "7" :ry "7"}]
    [:circle {:cx "16" :cy "12" :r "3"}]]

   :toggle-left
   [[:rect {:x "1" :y "5" :width "22" :height "14" :rx "7" :ry "7"}]
    [:circle {:cx "8" :cy "12" :r "3"}]]

   :star
   [[:polygon {:points "12 2 15.09 8.26 22 9.27 17 14.14 18.18 21.02 12 17.77 5.82 21.02 7 14.14 2 9.27 8.91 8.26 12 2"}]]

   :more-vertical
   [[:circle {:cx "12" :cy "12" :r "1"}]
    [:circle {:cx "12" :cy "5" :r "1"}]
    [:circle {:cx "12" :cy "19" :r "1"}]]

   :tag
   [[:path {:d "M20.59 13.41l-7.17 7.17a2 2 0 0 1-2.83 0L2 12V2h10l8.59 8.59a2 2 0 0 1 0 2.82z"}]
    [:line {:x1 "7" :y1 "7" :x2 "7.01" :y2 "7"}]]

   :home
   [[:path {:d "M3 9l9-7 9 7v11a2 2 0 0 1-2 2H5a2 2 0 0 1-2-2z"}]
    [:polyline {:points "9 22 9 12 15 12 15 22"}]]

   :car
   [[:path {:d "M19 17h2c.6 0 1-.4 1-1v-3c0-.9-.7-1.7-1.5-1.9C18.7 10.6 16 10 16 10s-1.3-1.4-2.2-2.3c-.5-.4-1.1-.7-1.8-.7H5c-.6 0-1.1.4-1.4.9l-1.4 2.9A3.7 3.7 0 0 0 2 12v4c0 .6.4 1 1 1h2"}]
    [:circle {:cx "7" :cy "17" :r "2"}]
    [:circle {:cx "17" :cy "17" :r "2"}]]

   :utensils
   [[:path {:d "M3 2v7c0 1.1.9 2 2 2h4a2 2 0 0 0 2-2V2"}]
    [:path {:d "M7 2v20"}]
    [:path {:d "M21 15V2a5 5 0 0 0-5 5v6c0 1.1.9 2 2 2h3zm0 0v7"}]]

   :dumbbell
   [[:path {:d "M14.4 14.4 9.6 9.6"}]
    [:path {:d "M18.657 21.485a2 2 0 1 1-2.829-2.828l-1.767 1.768a2 2 0 1 1-2.829-2.829l6.364-6.364a2 2 0 1 1 2.829 2.829l-1.768 1.767a2 2 0 1 1 2.828 2.829z"}]
    [:path {:d "m2.515 18.657 1.414-1.414"}]
    [:path {:d "M5.343 21.485a2 2 0 0 1 0-2.828l1.768-1.768"}]
    [:path {:d "m2.515 5.343 1.414 1.414"}]
    [:path {:d "M5.343 2.515a2 2 0 0 1 2.828 0l1.768 1.768"}]
    [:path {:d "m18.657 2.515-1.414 1.414"}]
    [:path {:d "M21.485 5.343a2 2 0 0 1 0 2.828l-1.768 1.768"}]]

   :zap
   [[:polygon {:points "13 2 3 14 12 14 11 22 21 10 12 10 13 2"}]]

   :film
   [[:rect {:x "2" :y "2" :width "20" :height "20" :rx "2.18" :ry "2.18"}]
    [:line {:x1 "7" :y1 "2" :x2 "7" :y2 "22"}]
    [:line {:x1 "17" :y1 "2" :x2 "17" :y2 "22"}]
    [:line {:x1 "2" :y1 "12" :x2 "22" :y2 "12"}]
    [:line {:x1 "2" :y1 "7" :x2 "7" :y2 "7"}]
    [:line {:x1 "2" :y1 "17" :x2 "7" :y2 "17"}]
    [:line {:x1 "17" :y1 "17" :x2 "22" :y2 "17"}]
    [:line {:x1 "17" :y1 "7" :x2 "22" :y2 "7"}]]

   :check-circle
   [[:path {:d "M22 11.08V12a10 10 0 1 1-5.93-9.14"}]
    [:polyline {:points "22 4 12 14.01 9 11.01"}]]

   :music
   [[:path {:d "M9 18V5l12-2v13"}]
    [:circle {:cx "6" :cy "18" :r "3"}]
    [:circle {:cx "18" :cy "16" :r "3"}]]

   :cloud
   [[:path {:d "M18 10h-1.26A8 8 0 1 0 9 20h9a5 5 0 0 0 0-10z"}]]

   :shopping-cart
   [[:circle {:cx "9" :cy "21" :r "1"}]
    [:circle {:cx "20" :cy "21" :r "1"}]
    [:path {:d "M1 1h4l2.68 13.39a2 2 0 0 0 2 1.61h9.72a2 2 0 0 0 2-1.61L23 6H6"}]]

   :activity
   [[:polyline {:points "22 12 18 12 15 21 9 3 6 12 2 12"}]]

   :shopping-bag
   [[:path {:d "M6 2L3 6v14a2 2 0 0 0 2 2h14a2 2 0 0 0 2-2V6l-3-4z"}]
    [:line {:x1 "3" :y1 "6" :x2 "21" :y2 "6"}]
    [:path {:d "M16 10a4 4 0 0 1-8 0"}]]

   :dollar-sign
   [[:line {:x1 "12" :y1 "1" :x2 "12" :y2 "23"}]
    [:path {:d "M17 5H9.5a3.5 3.5 0 0 0 0 7h5a3.5 3.5 0 0 1 0 7H6"}]]

   :briefcase
   [[:rect {:x "2" :y "7" :width "20" :height "14" :rx "2" :ry "2"}]
    [:path {:d "M16 21V5a2 2 0 0 0-2-2h-4a2 2 0 0 0-2 2v16"}]]

   :gift
   [[:polyline {:points "20 12 20 22 4 22 4 12"}]
    [:rect {:x "2" :y "7" :width "20" :height "5"}]
    [:line {:x1 "12" :y1 "22" :x2 "12" :y2 "7"}]
    [:path {:d "M12 7H7.5a2.5 2.5 0 0 1 0-5C11 2 12 7 12 7z"}]
    [:path {:d "M12 7h4.5a2.5 2.5 0 0 0 0-5C13 2 12 7 12 7z"}]]

   :package
   [[:line {:x1 "16.5" :y1 "9.4" :x2 "7.5" :y2 "4.21"}]
    [:path {:d "M21 16V8a2 2 0 0 0-1-1.73l-7-4a2 2 0 0 0-2 0l-7 4A2 2 0 0 0 3 8v8a2 2 0 0 0 1 1.73l7 4a2 2 0 0 0 2 0l7-4A2 2 0 0 0 21 16z"}]
    [:polyline {:points "3.27 6.96 12 12.01 20.73 6.96"}]
    [:line {:x1 "12" :y1 "22.08" :x2 "12" :y2 "12"}]]

   :info
   [[:circle {:cx "12" :cy "12" :r "10"}]
    [:line {:x1 "12" :y1 "16" :x2 "12" :y2 "12"}]
    [:line {:x1 "12" :y1 "8" :x2 "12.01" :y2 "8"}]]})

(def default-svg-attrs
  {:xmlns "http://www.w3.org/2000/svg"
   :viewBox "0 0 24 24"
   :fill "none"
   :stroke "currentColor"
   :stroke-width "2"
   :stroke-linecap "round"
   :stroke-linejoin "round"
   :aria-hidden "true"
   :focusable "false"})

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