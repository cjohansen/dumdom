(ns dumdom.inflate-test
  (:require [cljs.test :refer-macros [deftest is testing]]
            [dumdom.dom :as d]
            [dumdom.inflate :as sut]
            [dumdom.string :as string]))

(deftest inflate-rendering
  (testing "Does not remove existing valid DOM nodes"
    (let [el (js/document.createElement "div")]
      (set! (.-innerHTML el) "<h1>Hello</h1>")
      (set! (.. el -firstChild -marker) "marked")
      (sut/render (d/h1 {} "Hello") el)
      (is (= "marked" (.. el -firstChild -marker)))))

  (testing "Does not replace existing DOM nodes when elements have key"
    (let [el (js/document.createElement "div")
          component (d/h1 {:key "hello"} "Hello")]
      (set! (.-innerHTML el) (string/render component))
      (set! (.. el -firstChild -marker) "marked")
      (sut/render (d/h1 {:key "hello"} "Hello") el)
      (is (= "marked" (.. el -firstChild -marker))))))
