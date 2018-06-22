(ns dumdom.core-test
  (:require [cljs.test :as t :refer-macros [testing is]]
            [clojure.string :as str]
            [devcards.core :refer-macros [deftest]]
            [dumdom.core :as sut]
            [dumdom.dom :as d]
            [dumdom.test-helper :refer [render render-str]]))

(deftest component-render
  (testing "Renders component"
    (let [comp (sut/component (fn [data] (d/div {:className "lol"} data)))]
      (is (= "<div class=\"lol\">1</div>" (render-str (comp 1))))))

  (testing "Does not re-render when immutable value hasn't changed"
    (let [mutable-state (atom [1 2 3])
          comp (sut/component (fn [data]
                                 (let [v (first @mutable-state)]
                                   (swap! mutable-state rest)
                                   (d/div {} v))))]
      (is (= "<div>1</div>" (render-str (comp {:number 1}))))
      (is (= "<div>1</div>" (render-str (comp {:number 1}))))
      (is (= "<div>2</div>" (render-str (comp {:number 2}))))))

  (testing "Re-renders instance of component at different position in tree"
    (let [mutable-state (atom [1 2 3])
          comp (sut/component (fn [data]
                                 (let [v (first @mutable-state)]
                                   (swap! mutable-state rest)
                                   (d/div {} v))))]
      (is (= "<div>1</div>" (render-str (comp {:number 1}) [] 0)))
      (is (= "<div>2</div>" (render-str (comp {:number 1}) [] 1)))
      (is (= "<div>1</div>" (render-str (comp {:number 1}) [] 0)))
      (is (= "<div>2</div>" (render-str (comp {:number 1}) [] 1)))
      (is (= "<div>3</div>" (render-str (comp {:number 2}) [] 1)))))

  (testing "Ignores provided position when component has a keyfn"
    (let [mutable-state (atom [1 2 3])
          comp (sut/component (fn [data]
                                 (let [v (first @mutable-state)]
                                   (swap! mutable-state rest)
                                   (d/div {} v)))
                               {:keyfn :id})]
      (is (= "<div>1</div>" (render-str (comp {:id "c1" :number 1}) [] 0)))
      (is (= "<div>1</div>" (render-str (comp {:id "c1" :number 1}) [] 1)))
      (is (= "<div>2</div>" (render-str (comp {:id "c2" :number 1}) [] 0)))
      (is (= "<div>3</div>" (render-str (comp {:number 1}) [] 0)))
      (is (= "<div>2</div>" (render-str (comp {:id "c2" :number 1}) [] 1)))))

  (testing "Sets key on vdom node"
    (let [comp (sut/component (fn [data]
                                 (d/div {} (:val data)))
                               {:keyfn :id})]
      (is (= "c1" (.-key (render (comp {:id "c1" :val 1})))))))

  (testing "keyfn overrides vdom node key"
    (let [comp (sut/component (fn [data]
                                 (d/div {:key "key"} (:val data)))
                               {:keyfn :id})]
      (is (= "c1" (.-key (render (comp {:id "c1" :val 1})))))))

  (testing "Passes constant args to component, but does not re-render when they change"
    (let [calls (atom [])
          comp (sut/component (fn [& args]
                                 (swap! calls conj args)
                                 (d/div {:key "key"} "OK")))]
      (render (comp {:id "v1"} 1 2 3))
      (render (comp {:id "v1"} 2 3 4))
      (render (comp {:id "v2"} 3 4 5))
      (render (comp {:id "v2"} 4 5 6))
      (render (comp {:id "v3"} 5 6 7))
      (is (= [[{:id "v1"} 1 2 3]
              [{:id "v2"} 3 4 5]
              [{:id "v3"} 5 6 7]] @calls))))

  (testing "Renders component as child of DOM element"
      (let [comp (sut/component (fn [data]
                                 (d/p {} "From component")))]
      (is (= "<div class=\"wrapper\"><p>From component</p></div>"
             (render-str (d/div {:className "wrapper"}
                           (comp {:id "v1"} 1 2 3))))))))

(deftest render-test
  (testing "Renders vnode to DOM"
    (let [el (js/document.createElement "div")]
      (sut/render (d/div {} "Hello") el)
      (is (= "<div>Hello</div>" (.-innerHTML el))))))

(deftest on-mount-test
  (testing "Calls on-mount when component first mounts"
    (let [el (js/document.createElement "div")
          on-mount (atom nil)
          component (sut/component
                     (fn [_] (d/div {} "LOL"))
                     {:on-mount (fn [node & args]
                                  (reset! on-mount (apply vector node args)))})]
      (sut/render (component {:a 42} {:static "Prop"} {:another "Static"}) el)
      (is (= [(.-firstChild el) {:a 42} {:static "Prop"} {:another "Static"}]
             @on-mount))))

  (testing "Does not call on-mount on update"
    (let [el (js/document.createElement "div")
          on-mount (atom [])
          component (sut/component
                     (fn [data] (d/div {} (:text data)))
                     {:on-mount (fn [node data]
                                  (swap! on-mount conj data))})]
      (sut/render (component {:text "LOL"}) el)
      (sut/render (component {:text "Hello"}) el)
      (is (= 1 (count @on-mount))))))

(deftest on-update-test
  (testing "Does not call on-update when component first mounts"
    (let [el (js/document.createElement "div")
          on-update (atom nil)
          component (sut/component
                     (fn [_] (d/div {} "LOL"))
                     {:on-update (fn [node & args]
                                   (reset! on-update (apply vector node args)))})]
      (sut/render (component {:a 42} {:static "Prop"} {:another "Static"}) el)
      (is (nil? @on-update))))

  (testing "Calls on-update on each update"
    (let [el (js/document.createElement "div")
          on-update (atom [])
          component (sut/component
                     (fn [data] (d/div {} (:text data)))
                     {:on-update (fn [node data]
                                   (swap! on-update conj data))})]
      (sut/render (component {:text "LOL"}) el)
      (sut/render (component {:text "Hello"}) el)
      (sut/render (component {:text "Aight"}) el)
      (is (= [{:text "Hello"} {:text "Aight"}] @on-update)))))

(deftest on-render-test
  (testing "Calls on-render when component first mounts"
    (let [el (js/document.createElement "div")
          on-render (atom nil)
          component (sut/component
                     (fn [_] (d/div {} "LOL"))
                     {:on-render (fn [node & args]
                                  (reset! on-render (apply vector node args)))})]
      (sut/render (component {:a 42} {:static "Prop"} {:another "Static"}) el)
      (is (= [(.-firstChild el) {:a 42} {:static "Prop"} {:another "Static"}]
             @on-render))))

  (testing "Calls on-render on each update"
    (let [el (js/document.createElement "div")
          on-render (atom [])
          component (sut/component
                     (fn [data] (d/div {} (:text data)))
                     {:on-render (fn [node data]
                                   (swap! on-render conj data))})]
      (sut/render (component {:text "LOL"}) el)
      (sut/render (component {:text "Hello"}) el)
      (sut/render (component {:text "Aight"}) el)
      (is (= [{:text "LOL"}
              {:text "Hello"}
              {:text "Aight"}] @on-render)))))

