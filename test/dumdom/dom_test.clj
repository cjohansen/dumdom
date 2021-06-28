(ns dumdom.dom-test
  (:require [clojure.test :refer [deftest testing is]]
            [dumdom.dom :as sut]))

(defn remove-fns [x]
  (cond-> x
    (:data x) (update :data dissoc :on :hook)))

(defn render [comp]
  (-> (comp [] 0)
      remove-fns
      (update :children #(map remove-fns %))))

(deftest element-test
  (testing "Renders element"
    (is (= {:sel "div"
            :data {:attrs {}
                   :style nil
                   :props {}
                   :dataset {}}
            :children [{:text "Hello world"}]}
           (render (sut/div {} "Hello world")))))

  (testing "Renders element with attributes, props, and styles"
    (is (= {:sel "input"
            :data {:attrs {:width 10}
                   :style {:border "1px solid red"}
                   :dataset {}
                   :props {:value "Hello"}}
            :children [{:text "Hello world"}]}
           (render (sut/input {:width 10
                               :value "Hello"
                               :style {:border "1px solid red"}}
                              "Hello world")))))

  (testing "Renders element with children"
    (is (= {:sel "div"
            :data {:attrs {}
                   :style nil
                   :dataset {}
                   :props {}}
            :children [{:sel "h1"
                        :data {:style {:border "1px solid cyan"}
                               :dataset {}
                               :props {}
                               :attrs {}}
                        :children [{:text "Hello"}]}
                       {:sel "img"
                        :data {:style nil
                               :attrs {:border "2"}
                               :props {}
                               :dataset {}}
                        :children []}]}
           (render (sut/div {}
                            (sut/h1 {:style {:border "1px solid cyan"}} "Hello")
                            (sut/img {:border "2"}))))))

  (testing "Parses hiccup element name for classes"
    (is (= {:sel "div"
            :data {:attrs {}
                   :style nil
                   :dataset {}
                   :props {}}
            :children [{:sel "h1"
                        :data {:style nil
                               :attrs {:class "something nice and beautiful"}
                               :dataset {}
                               :props {}}
                        :children [{:text "Hello"}]}]}
           (render (sut/div {} [:h1.something.nice.and.beautiful "Hello"])))))

  (testing "Parses hiccup element name for id and classes, combines with existing"
    (is (= {:sel "div"
            :data {:attrs {}
                   :style nil
                   :dataset {}
                   :props {}}
            :children [{:sel "h1"
                        :data {:style nil
                               :attrs {:id "helau", :class "andhere here"}
                               :dataset {}
                               :props {}}
                        :children [{:text "Hello"}]}
                       {:sel "h1"
                        :data {:style nil
                               :attrs {:id "first", :class "lol"}
                               :dataset {}
                               :props {}}
                        :children [{:text "Hello"}]}]}
           (render (sut/div {}
                            [:h1.here#helau {:className "andhere"} "Hello"]
                            [:h1#first.lol {} "Hello"]))))))
