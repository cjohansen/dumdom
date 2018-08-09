(ns dumdom.dom-test
  (:require [cljs.test :as t :refer-macros [is testing]]
            [devcards.core :refer-macros [deftest]]
            [dumdom.core :as dd]
            [dumdom.dom :as d]
            [dumdom.test-helper :refer [render-str]]))

(deftest dom-element-test
  (testing "Renders div element"
    (is (= "<div class=\"test\">Hello</div>"
           (render-str (d/div {:className "test"} "Hello")))))

  (testing "Renders nested div elements"
    (is (= "<div class=\"test\"><div id=\"yap\">Hello</div></div>"
           (render-str (d/div {:className "test"} (d/div {:id "yap"} "Hello"))))))

  (testing "Renders CSS number values as pixel values"
    (is (= "<div style=\"width: 100px; right: 30px; top: 20px; height: 50px; margin: 20px; padding: 50px; position: absolute; bottom: 40px; left: 10px\">Hello</div>"
           (render-str (d/div {:style {:width 100
                                       :height 50
                                       :position "absolute"
                                       :left 10
                                       :top 20
                                       :right 30
                                       :bottom 40
                                       :padding 50
                                       :margin 20}} "Hello"))))))

(deftest ref-test
  (testing "Invokes ref callback with DOM element"
    (let [node (atom nil)
          el (js/document.createElement "div")]
      (dd/render (d/div {:ref #(reset! node %)}) el)
      (is (= (.-firstChild el) @node))))

  (testing "Does not invoke ref function on update"
    (let [calls (atom 0)
          el (js/document.createElement "div")]
      (dd/render (d/div {:ref #(swap! calls inc)} "Allo") el)
      (dd/render (d/div {:ref #(swap! calls inc)} "Allo!") el)
      (is (= 1 @calls))))

  (testing "Invokes ref function with nil on unmount"
    (let [calls (atom [])
          el (js/document.createElement "div")]
      (dd/render (d/div {:ref #(swap! calls conj %)} "Allo") el)
      (dd/render (d/h1 {} "So long") el)
      (is (= 2 (count @calls)))
      (is (nil? (second @calls))))))
