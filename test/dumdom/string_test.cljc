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

  (testing "Does not render nil styles"
    (is (= "<div style=\"background: yellow\">Text</div>"
           (dumdom/render-string (d/div {:style {:border nil
                                                 :background "yellow"}} "Text")))))

  (testing "Does not trip on nil children"
    (is (= "<div style=\"background: yellow\"><div>Ok</div></div>"
           (dumdom/render-string (d/div {:style {:background "yellow"}} nil [:div "Ok"])))))

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
    (let [comp (dumdom/component
                (fn [data] (d/div {} (:text data)))
                {:name "NamedComp"})]
      (is (= "<div data-dumdom-key=\"&quot;NamedComp.0&quot;\">LOL</div>"
             (dumdom/render-string (comp {:text "LOL"}))))))

  (testing "Renders component key as data attribute"
    (is (= "<div data-dumdom-key=\"&quot;some-key.0&quot;\">LOL</div>"
           (dumdom/render-string (d/div {:key "some-key"} "LOL")))))

  (testing "Escapes ampersands in keys"
    (is (= "<div data-dumdom-key=\"&quot;some&amp;key.0&quot;\">LOL</div>"
           (dumdom/render-string (d/div {:key "some&key"} "LOL")))))

  (testing "Respects existing HTML entities in keys"
    (is (= "<div data-dumdom-key=\"&quot;some&#34;key.0&quot;\">LOL</div>"
           (dumdom/render-string (d/div {:key "some&#34;key"} "LOL")))))

  (testing "Renders hiccup to string"
    (is (= "<div style=\"color: red\"><h1>Hello</h1><p>World</p></div>"
           (dumdom/render-string [:div {:style {:color "red"}}
                                  [:h1 "Hello"]
                                  [:p "World"]]))))

  (testing "Renders block element with innerHTML"
    (is (= "<div data-dumdom-key=\"&quot;298141744.0&quot;\"><p style=\"color: red\">Hello</p></div>"
           (dumdom/render-string [:div {:dangerouslySetInnerHTML {:__html "<p style=\"color: red\">Hello</p>"}}]))))

  (testing "Renders inline element with innerHTML"
    (is (= "<span data-dumdom-key=\"&quot;-473860037.0&quot;\"><em>Hello</em></span>"
           (dumdom/render-string [:span {:dangerouslySetInnerHTML {:__html "<em>Hello</em>"}}]))))

  (testing "Renders multiple child elements, strings and numbers alike"
    (is (= "<span>These are 9 things</span>"
           (dumdom/render-string [:span "These are " 9 " things"]))))

  (testing "Ignores event handlers"
    (is (= "<a>Ok!</a>"
           (dumdom/render-string [:a {:onClick (fn [_])} "Ok!"])))))

(deftest renders-string-styles
  (is (= "<a style=\"text-decoration: none\">Ok</a>"
         (dumdom/render-string [:a {:style "text-decoration:none;"} "Ok"]))))

(deftest renders-nested-lists-and-lazy-seqs
  (is (= "<li>Text</li>"
         (dumdom/render-string
          [:li {} (list (map identity (list "Text")))]))))

(deftest does-not-stop-on-nil-children
  (is (= "<a href=\"/\" class=\"button text-m\">Click it</a>"
         (dumdom/render-string
          [:a {:className "button text-m" :href "/"}
           nil
           "Click it"]))))

(deftest renders-mixed-class-sources
  (is (= "<a href=\"/\" class=\"text-m button\">Click it</a>"
         (dumdom/render-string
          [:a.button {:class [:text-m] :href "/"}
           nil
           "Click it"]))))

(deftest renders-data-attributes
  (is (= "<div data-stuff=\"Yes\">Content</div>"
         (dumdom/render-string [:div {:data-stuff "Yes"} "Content"]))))

(deftest renders-string-event-handlers
  (is (= "<div onClick=\"alert(\"Most certainly!\")\">Content</div>"
         (dumdom/render-string [:div {:onClick "alert(\"Most certainly!\")"} "Content"]))))

(deftest renders-false-attributes-properly
  (is (= "<input>" (dumdom/render-string [:input {:checked false}]))))

(deftest renders-aria-attributes-properly
  (is (= "<div aria-hidden=\"true\"></div>" (dumdom/render-string [:div {:aria-hidden "true"}]))))

