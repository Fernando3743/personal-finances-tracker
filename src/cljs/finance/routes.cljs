(ns finance.routes
  "Application route definitions using bidi."
  (:require [bidi.bidi :as bidi]))

(def routes
  ["/" {""             :dashboard
        "transactions" :transactions
        "add"          :add-transaction}])

(defn path-for [route]
  (bidi/path-for routes route))

(defn match-route [path]
  (bidi/match-route routes path))
