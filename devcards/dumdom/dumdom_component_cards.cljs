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

(defcard dumdom-animated-component
  "This component has a child that should fade in and out"
  (fn [store]
    (when (< (:fades @store) 5)
      (js/setTimeout #(reset! store
                              (-> @store
                                  (update :visible? not)
                                  (update :fades inc))) 5000))
    [:div {}
     (when (:visible? @store)
       [:div {:style {:opacity "0"
                      :transition "opacity 0.25s"}
              :mounted-style {:opacity "1"}
              :leaving-style {:opacity "0"}}
        "I will fade both in and out"])])
  {:visible? false
   :fades 0})

(defcard dumdom-component-with-inner-html
  "This component uses dangerouslySetInnerHTML"
  [:div {:dangerouslySetInnerHTML
         {:__html "<p>I am pre-chewed <strong>markup</strong></p>"}}])

(defcard dumdom-component-with-nested-seqs
  "This component contains a nested seq"
  (do
    [:div {} (map identity (list (map identity (list (list "Ok")))))]))
