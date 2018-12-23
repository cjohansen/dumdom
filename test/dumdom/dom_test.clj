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
                     (sut/img {:border "2"})) [] 0)))))
