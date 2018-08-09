(ns dumdom.dumdom-component-cards
  (:require [dumdom.core :as q]
            [dumdom.devcards :refer-macros [defcard]]
            [dumdom.dom :as d]))

(q/defcomponent SomeStuff
  :on-mount (fn [] (println "Watch out, Imma mount"))
  :on-update (fn [] (println "Watch out, Imma update"))
  [title]
  (d/div {}
    (d/h1 {} title)
    (d/p {} "I am a dumdom component")))

(defcard dumdom-component
  (SomeStuff "Hello world!!"))

(defcard dumdom-component-card-fn
  "This component was created with a devcard function and initial state"
  (fn [store]
    (SomeStuff (:title @store)))
  {:title "Lol"})
