(ns dumdom.dev
  (:require [dumdom.core :as dumdom :refer [defcomponent]]
            [dumdom.dom :as d]))

(enable-console-print!)

(def app (js/document.getElementById "app"))

(defonce store (atom {:things [{:text "Thing 1"
                                :id :t1}
                               {:text "Thing 2"
                                :id :t2}
                               {:text "Thing 3"
                                :id :t3}]}))

(defn mark-active [things id]
  (mapv #(assoc % :active? (= (:id %) id)) things))

(defcomponent Thing
  :keyfn :id
  [{:keys [id idx active? text]}]
  [:div {:style {:cursor "pointer"}
         :key (name id)
         :onClick (fn [e]
                    (swap! store update :things mark-active id))}
   (if active?
     [:strong text]
     text)])

(defcomponent App [data]
  [:div
   [:h1 "HELLO"]
   (map Thing (:things data))])

(defn render [state]
  (dumdom/render (App state) app))

(add-watch store :render (fn [_ _ _ state]
                           (println "Render" state)
                           (render state)))
(render @store)

(comment
  (swap! store assoc :things [])

  (swap! store assoc :things [{:text "Thing 1"
                               :id :t1}
                              {:text "Thing 2"
                               :id :t2}
                              {:text "Thing 3"
                               :id :t3}])

  (swap! store assoc :things [{:text "Thing 1"
                               :id :t1}
                              {:text "Thing 2"
                               :id :t2}
                              {:text "Thing 3"
                               :id :t3}
                              {:text "Thing 4"
                               :id :t4}
                              {:text "Thing 5"
                               :id :t5}])
)
