(ns finance.routes
  "Application route definitions using bidi."
  (:require [bidi.bidi :as bidi]))

(def routes
  ["/" {""             :dashboard
        "transactions" :transactions
        "wallets"      :wallets
        "reports"      :reports
        "add"          :add-transaction
        "login"        :login
        "register"     :register}])

(def public-routes
  "Routes that don't require authentication."
  #{:login :register})

(defn path-for [route]
  (bidi/path-for routes route))

(defn match-route [path]
  (bidi/match-route routes path))
