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
    (is (= "<div style=\"width: 100px; right: 30px; top: 20px; height: 50px; margin: 20px; padding: 50px; position: absolute; bottom: 40px; flex: 1; opacity: 0; left: 10px\">Hello</div>"
           (render-str (d/div {:style {:width 100
                                       :height 50
                                       :position "absolute"
                                       :left 10
                                       :top 20
                                       :right 30
                                       :bottom 40
                                       :padding 50
                                       :margin 20
                                       :opacity 0
                                       :flex 1}} "Hello")))))

  (testing "Pixelizes snake-cased and camelCased CSS properties"
    (is (= "<div style=\"margin-left: 20px; margin-right: 20px\">Hello</div>"
           (render-str (d/div {:style {:margin-left 20
                                       :marginRight 20}} "Hello")))))

  (testing "Supports dashed attribute names"
    (is (= "<div class=\"hello\"></div>"
           (render-str (d/div {:class-name "hello"})))))

  (testing "Purges nil attribute values"
    (is (= "<img width=\"100\">"
           (render-str (d/img {:width 100 :height nil})))))

  (testing "Renders SVG element"
    (let [el (js/document.createElement "div")
          brush {:fill "none"
                 :strokeWidth "4"
                 :strokeLinecap "round"
                 :strokeLinejoin "round"
                 :strokeMiterlimit "10"}]
      (dd/render (d/svg {:viewBox "0 0 64 64"}
                   (d/circle (merge brush {:cx "32" :cy "32" :r "30"}))
                   (d/line (merge brush {:x1 "16" :y1 "32" :x2 "48" :y2 "32"}))
                   (d/line (merge brush {:x1 "32" :y1 "16" :x2 "32" :y2 "48"}))) el)
      (is (= (str "<svg viewBox=\"0 0 64 64\">"
                  "<circle fill=\"none\" cx=\"32\" cy=\"32\" r=\"30\" stroke-width=\"4\" "
                  "stroke-linejoin=\"round\" stroke-miterlimit=\"10\" stroke-linecap=\"round\">"
                  "</circle>"
                  "<line fill=\"none\" stroke-linejoin=\"round\" y1=\"32\" stroke-linecap=\"round\" "
                  "stroke-width=\"4\" stroke-miterlimit=\"10\" x1=\"16\" y2=\"32\" x2=\"48\">"
                  "</line>"
                  "<line fill=\"none\" stroke-linejoin=\"round\" y1=\"16\" stroke-linecap=\"round\" "
                  "stroke-width=\"4\" stroke-miterlimit=\"10\" x1=\"32\" y2=\"48\" x2=\"32\">"
                  "</line>"
                  "</svg>")
             (.-innerHTML el)))))

  (testing "Renders inner html"
    (is (= "<div><p>Ok</p></div>"
           (render-str (d/div {:dangerouslySetInnerHTML {:__html "<p>Ok</p>"}}))))))

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

(deftest input-test
  (testing "Renders values in inputs"
    (let [el (js/document.createElement "div")]
      (dd/render (d/input {:value "Yowsa" :width "100" :type "text"}) el)
      (is (= "Yowsa" (.. el -firstChild -value)))))

  (testing "Does not render explicit 'null' for input value"
    (let [el (js/document.createElement "div")]
      (dd/render (d/input {:value nil}) el)
      (is (= "" (.. el -firstChild -value)))))

  (testing "Sets value for text areas"
    (let [el (js/document.createElement "div")]
      (dd/render (d/textarea {:value "Aloha"}) el)
      (is (= "Aloha" (.. el -firstChild -value)))))

  (testing "Does not render explicit 'null' for text areas"
    (let [el (js/document.createElement "div")]
      (dd/render (d/textarea {:value nil}) el)
      (is (= "" (.. el -firstChild -value))))))

(deftest hiccup-test
  (testing "Renders hiccup-like"
    (let [el (js/document.createElement "div")]
      (dd/render [:div {} "Hello world"] el)
      (is (= "<div>Hello world</div>" (.-innerHTML el)))))

  (testing "Renders hiccup-like with children"
    (let [el (js/document.createElement "div")]
      (dd/render [:div {}
                  [:h1 {:style {:border "1px solid cyan"}} "Hello"]
                  [:img {:border "2"}]] el)
      (is (= "<div><h1 style=\"border: 1px solid cyan;\">Hello</h1><img border=\"2\"></div>" (.. el -innerHTML)))))

  (testing "Renders mixed hiccup and functions"
    (let [el (js/document.createElement "div")]
      (dd/render [:div {}
                  (d/h1 {:style {:border "1px solid cyan"}} [:a {} "Hello"])] el)
      (is (= "<div><h1 style=\"border: 1px solid cyan;\"><a>Hello</a></h1></div>" (.. el -innerHTML)))))

  (testing "Accepts omission of attribute map in hiccup syntax"
    (let [el (js/document.createElement "div")]
      (dd/render [:div
                  [:h1 "Hello"]
                  [:img {:border "2"}]] el)
      (is (= "<div><h1>Hello</h1><img border=\"2\"></div>" (.. el -innerHTML)))))

  (testing "Parses hiccup element name for classes"
    (let [el (js/document.createElement "div")]
      (dd/render [:div.something.nice.and.beautiful "Hello"] el)
      (is (= "<div class=\"something nice and beautiful\">Hello</div>" (.. el -innerHTML)))))

  (testing "Combines hiccup symbol classes with attribute classes"
    (let [el (js/document.createElement "div")]
      (dd/render [:div.something {:className "nice"} "Hello"] el)
      (is (= "<div class=\"nice something\">Hello</div>" (.. el -innerHTML)))))

  (testing "Sets inner HTML"
    (let [el (js/document.createElement "div")]
      (dd/render [:div {:dangerouslySetInnerHTML {:__html "<h2>LOL!</h2>"}}] el)
      (is (= "<div><h2>LOL!</h2></div>" (.. el -innerHTML))))))
