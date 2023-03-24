(ns dumdom.dev
  (:require [dumdom.core :as dumdom :refer [defcomponent]]
            [dumdom.snabbdom :as snabbdom]))

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

(def patch (snabbdom/init #js [snabbdom/styleModule]))


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

  (require '[quiescent.core :as q]
           '[quiescent.dom :as qd])

  (dumdom/render [:div {}
                  nil
                  [:div "Dumdom"]] app)
  (dumdom/render [:div {}
                  [:div {:style {:opacity 0.3 :transition "opacity 500ms"}} "Hello"]
                  [:div "Dumdom"]] app)
  (dumdom/render [:div {}
                  [:div {:style {:opacity 0.7 :transition "opacity 500ms"}} "Hello"]
                  [:div "Dumdom"]] app)

  (def qel (js/document.createElement "div"))
  (js/document.body.appendChild qel)

  (q/render (qd/div {}
                    nil
                    (qd/div {} "Quiescent")) qel)
  (q/render (qd/div {}
                    (qd/div {:style {:opacity 0.3 :transition "opacity 500ms"}}
                            "Hello!")
                    (qd/div {} "Quiescent")) qel)


  (def el (js/document.createElement "div"))
  (js/document.body.appendChild el)

  (js/console.log #js {:style #js {:opacity 0.3 :transition "opacity 500ms"}})

  (def vdom (patch el (snabbdom/h "!" #js {} "nil")))
  (def vdom (patch vdom (snabbdom/h "div" #js {} #js ["OK"])))

  (def vdom (patch el (snabbdom/vnode "" #js {} #js [])))
  (def vdom (patch vdom (snabbdom/h "div" #js {} #js ["OK"])))

  (def vdom (patch el (snabbdom/h "div" #js {} #js [(snabbdom/h "div" #js {} #js ["Hello from snabbdom"])])))
  (def vdom (patch vdom (snabbdom/h
                         "div"
                         #js {}
                         #js [(snabbdom/h
                               "div"
                               #js {:style #js {:opacity 0.3 :transition "opacity 500ms"}}
                               #js ["Hello from snabbdom"])])))

  (set! (.-innerHTML el) "Yo yoyo")
  (set! (.. el -style -transition) "opacity 0.5s")
  (set! (.. el -style -opacity) "0.3")
)
