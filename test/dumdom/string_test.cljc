(ns dumdom.string-test
  (:require #?(:clj [clojure.test :refer [deftest testing is]]
               :cljs [cljs.test :refer [deftest testing is]])
            [dumdom.dom :as d]
            [dumdom.core :as dumdom]))

(deftest render-test
  (testing "Renders element to string"
    (is (= "<div>Hello</div>"
           (dumdom/render-string (d/div {} "Hello")))))

  (testing "Renders attributes to string"
    (is (= "<div title=\"Title\" width=\"10\">Hello</div>"
           (dumdom/render-string (d/div {:title "Title" :width 10} "Hello")))))

  (testing "Renders self-closing tag"
    (is (= "<input width=\"20\">"
           (dumdom/render-string (d/input {:width 20})))))

  (testing "Renders self-closing tag with prop"
    (is (= "<input width=\"20\" value=\"Hello\">"
           (dumdom/render-string (d/input {:width 20 :value "Hello"})))))

  (testing "Renders br"
    (is (= "<br>"
           (dumdom/render-string (d/br {})))))

  (testing "Renders element with styles"
    (is (= "<div style=\"border: 1px solid red; background: yellow\">Text</div>"
           (dumdom/render-string (d/div {:style {:border "1px solid red"
                                                 :background "yellow"}} "Text")))))

  (testing "Renders element with properly cased styles"
    (is (= "<div style=\"padding-left: 10px\">Text</div>"
           (dumdom/render-string (d/div {:style {:paddingLeft "10px"}} "Text")))))

  (testing "Pixelizes style values"
    (is (= "<div style=\"padding-left: 10px\">Text</div>"
           (dumdom/render-string (d/div {:style {:paddingLeft 10}} "Text")))))

  (testing "Maps attribute names"
    (is (= "<label for=\"something\">Text</label>"
           (dumdom/render-string (d/label {:htmlFor "something"} "Text")))))

  (testing "Renders element with children"
    (is (= "<div><h1>Hello</h1><p>World</p></div>"
           (dumdom/render-string (d/div {}
                                   (d/h1 {} "Hello")
                                   (d/p {} "World"))))))

  (testing "Renders custom component to string"
    (let [comp (dumdom/component (fn [data] (d/div {} (:text data))))]
      (is (= "<div>LOL</div>"
             (dumdom/render-string (comp {:text "LOL"}))))))

  (testing "Renders component key as data attribute"
    (is (= "<div data-dumdom-key=\"&quot;some-key&quot;\">LOL</div>"
           (dumdom/render-string (d/div {:key "some-key"} "LOL")))))

  (testing "Escapes ampersands in keys"
    (is (= "<div data-dumdom-key=\"&quot;some&amp;key&quot;\">LOL</div>"
           (dumdom/render-string (d/div {:key "some&key"} "LOL")))))

  (testing "Respects existing HTML entities in keys"
    (is (= "<div data-dumdom-key=\"&quot;some&#34;key&quot;\">LOL</div>"
           (dumdom/render-string (d/div {:key "some&#34;key"} "LOL")))))

  (testing "Renders hiccup to string"
    (is (= "<div style=\"color: red\"><h1>Hello</h1><p>World</p></div>"
           (dumdom/render-string [:div {:style {:color "red"}}
                                  [:h1 "Hello"]
                                  [:p "World"]])))))
