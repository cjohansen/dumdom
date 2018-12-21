(ns dumdom.server-test
  (:require #?(:clj [clojure.test :refer [deftest testing is]]
               :cljs [cljs.test :refer [deftest testing is]])
            [dumdom.dom :as d]
            [dumdom.server :as sut]))

(deftest render-test
  (testing "Renders element to string"
    (is (= "<div>Hello</div>"
           (sut/render (d/div {} "Hello")))))

  (testing "Renders attributes to string"
    (is (= "<div title=\"Title\" width=\"10\">Hello</div>"
           (sut/render (d/div {:title "Title" :width 10} "Hello")))))

  (testing "Renders self-closing tag"
    (is (= "<input width=\"20\">"
           (sut/render (d/input {:width 20})))))

  (testing "Renders self-closing tag with prop"
    (is (= "<input width=\"20\" value=\"Hello\">"
           (sut/render (d/input {:width 20 :value "Hello"})))))

  (testing "Renders br"
    (is (= "<br>"
           (sut/render (d/br {})))))

  (testing "Renders element with styles"
    (is (= "<div style=\"border: 1px solid red; background: yellow\">Text</div>"
           (sut/render (d/div {:style {:border "1px solid red"
                                       :background "yellow"}} "Text")))))

  (testing "Renders element with properly cased styles"
    (is (= "<div style=\"padding-left: 10px\">Text</div>"
           (sut/render (d/div {:style {:paddingLeft "10px"}} "Text")))))

  (testing "Pixelizes style values"
    (is (= "<div style=\"padding-left: 10px\">Text</div>"
           (sut/render (d/div {:style {:paddingLeft 10}} "Text")))))

  (testing "Maps attribute names"
    (is (= "<label for=\"something\">Text</label>"
           (sut/render (d/label {:htmlFor "something"} "Text")))))

  (testing "Renders element with children"
    (is (= "<div><h1>Hello</h1><p>World</p></div>"
           (sut/render (d/div {}
                         (d/h1 {} "Hello")
                         (d/p {} "World")))))))

