(ns ^:figwheel-hooks dumdom.cards
  (:require [dumdom.dumdom-component-cards]
            [devcards.core :as devcards]))

(enable-console-print!)

(defn render []
  (devcards/start-devcard-ui! ))

(defn ^:after-load render-on-reload []
  (render))

(render)
