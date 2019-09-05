(ns dumdom.dom-test
  (:require [clojure.test :refer [deftest testing is]]
            [dumdom.dom :as sut]))

(deftest element-test
  (testing "Renders element"
    (is (= {:tag-name "div" :attributes {} :style nil :children ["Hello world"] :key nil}
           ((sut/div {} "Hello world") [] 0))))

  (testing "Renders element with attributes, props, and styles"
    (is (= {:tag-name "input"
            :attributes {:width 10 :value "Hello"}
            :style {:border "1px solid red"}
            :children ["Hello world"]
            :key nil}
           ((sut/input {:width 10
                        :value "Hello"
                        :style {:border "1px solid red"}}
                       "Hello world") [] 0))))

  (testing "Renders element with children"
    (is (= {:tag-name "div"
            :attributes {}
            :style nil
            :key nil
            :children [{:tag-name "h1"
                        :key nil
                        :style {:border "1px solid cyan"}
                        :attributes {}
                        :children ["Hello"]}
                       {:tag-name "img"
                        :style nil
                        :key nil
                        :attributes {:border "2"}
                        :children []}]}
           ((sut/div {}
                     (sut/h1 {:style {:border "1px solid cyan"}} "Hello")
                     (sut/img {:border "2"})) [] 0))))

  (testing "Parses hiccup element name for classes"
    (is (= {:tag-name "div"
            :attributes {}
            :style nil
            :key nil
            :children [{:tag-name "h1"
                        :key nil
                        :style nil
                        :attributes {:class "something nice and beautiful"}
                        :children ["Hello"]}]}
           ((sut/div {} [:h1.something.nice.and.beautiful "Hello"]) [] 0))))

  (testing "Parses hiccup element name for id and classes, combines with existing"
    (is (= {:tag-name "div"
            :attributes {}
            :style nil
            :key nil
            :children [{:tag-name "h1"
                        :key nil
                        :style nil
                        :attributes {:id "helau", :class "andhere here"}
                        :children ["Hello"]}
                       {:tag-name "h1"
                        :key nil
                        :style nil
                        :attributes {:id "first", :class "lol"}
                        :children ["Hello"]}]}
           ((sut/div {}
                     [:h1.here#helau {:className "andhere"} "Hello"]
                     [:h1#first.lol {} "Hello"]) [] 0)))))
