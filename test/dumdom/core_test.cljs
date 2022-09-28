(ns dumdom.core-test
  (:require [cljs.test :as t :refer-macros [async is testing]]
            [devcards.core :refer-macros [deftest]]
            dumdom.component
            [dumdom.core :as sut]
            [dumdom.dom :as d]
            [dumdom.test-helper :refer [render render-stripped-str render-str]]))

(sut/purge!)

(deftest component-render
  (testing "Renders component"
    (let [comp (sut/component (fn [data] (d/div {:className "lol"} data)))]
      (is (= "<div class=\"lol\">1</div>" (render-stripped-str (comp 1))))))

  (testing "Does not optimize away initial render when data is nil"
    (let [comp (sut/component (fn [data] (d/div {:className "lol"})))]
      (is (= "<div class=\"lol\"></div>" (render-stripped-str (comp nil))))))

  (testing "Does not re-render when immutable value hasn't changed"
    (let [mutable-state (atom [1 2 3])
          comp (sut/component (fn [data]
                                (let [v (first @mutable-state)]
                                  (swap! mutable-state rest)
                                  (d/div {} v))))]
      (is (= "<div>1</div>" (render-stripped-str (comp {:number 1}))))
      (is (= "<div>1</div>" (render-stripped-str (comp {:number 1}))))
      (is (= "<div>2</div>" (render-stripped-str (comp {:number 2}))))))

  (testing "Re-renders instance of component at different position in tree"
    (let [mutable-state (atom [1 2 3])
          comp (sut/component (fn [data]
                                (let [v (first @mutable-state)]
                                  (swap! mutable-state rest)
                                  (d/div {} v))))]
      (is (= "<div>1</div>" (render-stripped-str (comp {:number 1}) [] {"" 0})))
      (is (= "<div>2</div>" (render-stripped-str (comp {:number 1}) [] {"" 1})))
      (is (= "<div>1</div>" (render-stripped-str (comp {:number 1}) [] {"" 0})))
      (is (= "<div>2</div>" (render-stripped-str (comp {:number 1}) [] {"" 1})))
      (is (= "<div>3</div>" (render-stripped-str (comp {:number 2}) [] {"" 1})))))

  (testing "Ignores provided position when component has a keyfn"
    (let [mutable-state (atom [1 2 3])
          comp (sut/component
                (fn [data]
                  (let [v (first @mutable-state)]
                    (swap! mutable-state rest)
                    (d/div {} v)))
                {:keyfn :id
                 :name "comp"})]
      (is (= "<div data-dumdom-key=\"&quot;comp.c1.0&quot;\">1</div>" (render-str (comp {:id "c1" :number 1}) [] {"" 0})))
      (is (= "<div data-dumdom-key=\"&quot;comp.c1.0&quot;\">1</div>" (render-str (comp {:id "c1" :number 1}) [] {"" 1})))
      (is (= "<div data-dumdom-key=\"&quot;comp.c2.0&quot;\">2</div>" (render-str (comp {:id "c2" :number 1}) [] {"" 0})))
      (is (= "<div>3</div>" (render-stripped-str (comp {:number 1}) [] {"" 0})))
      (is (= "<div data-dumdom-key=\"&quot;comp.c2.0&quot;\">2</div>" (render-str (comp {:id "c2" :number 1}) [] {"" 1})))))

  (testing "Sets key on vdom node"
    (let [comp (sut/component
                (fn [data]
                  (d/div {} (:val data)))
                {:name "NamedComp"
                 :keyfn :id})]
      (is (= "NamedComp.c1.0" (:key (render (comp {:id "c1" :val 1})))))))

  (testing "keyfn combines with vdom node key"
    ;; Component and element keys MUST be combined in order to recognize that
    ;; one component replaces another at the same position with a
    ;; similar-looking DOM element in its root position.
    (let [comp (sut/component
                (fn [data]
                  (d/div {:key "key"} (:val data)))
                {:keyfn :id
                 :name "KeyedComp"})]
      (is (= "KeyedComp.c1.key.0" (:key (render (comp {:id "c1" :val 1})))))))

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
             (render-stripped-str (d/div {:className "wrapper"}
                                    (comp {:id "v1"} 1 2 3)))))))

  (testing "Does not freak out when rendering nil children"
    (let [comp (sut/component (fn [data]
                                (d/div {}
                                  (d/h1 {} "Hello")
                                  nil
                                  (d/p {} "Ok"))))]
      (is (= "<div class=\"wrapper\"><div><h1>Hello</h1><p>Ok</p></div><div>Meh</div></div>"
             (render-stripped-str (d/div {:className "wrapper"}
                                    (comp {})
                                    nil
                                    (d/div {} "Meh")))))))

  (testing "Allows components to return nil"
    (let [comp (sut/component (fn [data] nil))]
      (is (= "<div class=\"wrapper\"><div>Meh</div></div>"
             (render-str (d/div {:className "wrapper"}
                           (comp {})
                           (d/div {} "Meh"))))))))

(deftest render-test
  (testing "Renders vnode to DOM"
    (let [el (js/document.createElement "div")]
      (sut/render (d/div {} "Hello") el)
      (is (= "<div>Hello</div>" (.-innerHTML el)))))

  (testing "Renders hiccup"
    (let [el (js/document.createElement "div")]
      (sut/render [:div {}
                   [:h1 {:style {:border "1px solid cyan"}} "Hello"]
                   [:img {:border "2"}]] el)
      (is (= "<div><h1 style=\"border: 1px solid cyan;\">Hello</h1><img border=\"2\"></div>" (.-innerHTML el)))))

  (testing "Renders hiccup with comment placeholders for nils"
    (let [el (js/document.createElement "div")]
      (sut/render [:div {}
                   nil
                   [:img {:border "2"}]] el)
      (is (= "<div><!--nil--><img border=\"2\"></div>" (.-innerHTML el)))))

  (testing "Renders hiccup with mixed class sources"
    (let [el (js/document.createElement "div")]
      (sut/render [:div
                   [:div.lol {:class "hello"} "Hi"]
                   [:div.lol {:className "bubye"} "Hi"]] el)
      (is (= (str "<div>"
                  "<div class=\"hello lol\">Hi</div>"
                  "<div class=\"bubye lol\">Hi</div>"
                  "</div>") (.-innerHTML el)))))

  (testing "Renders hiccup with all the class sources"
    (let [el (js/document.createElement "div")]
      (sut/render [:div.lol {:class "hello"
                             :className "bubye"} "Hi"] el)
      (is (= "<div class=\"hello bubye lol\">Hi</div>" (.-innerHTML el)))))

  (testing "Supports list of classes"
    (let [el (js/document.createElement "div")]
      (sut/render [:div.lol {:class ["hello" :ahoi]
                             :className ["bubye" :ok]} "Hi"] el)
      (is (= "<div class=\"hello ahoi bubye ok lol\">Hi</div>"
             (.-innerHTML el)))))

  (testing "Supports keyword classes"
    (is (= (let [el (js/document.createElement "div")]
             (sut/render [:div {:class :ahoi
                                :className "bubye"} "Hi"] el)
             (.-innerHTML el))
           "<div class=\"ahoi bubye\">Hi</div>")))

  (testing "Renders custom elements in hiccup"
    (let [el (js/document.createElement "div")
          comp (sut/component (fn [data]
                                (d/a {} "Hi there " (:name data))))]
      (sut/render [comp {:name "world"}] el)
      (is (= "<a>Hi there world</a>" (.-innerHTML el)))))

  (testing "Allows custom elements to return hiccup"
    (let [el (js/document.createElement "div")
          comp (sut/component (fn [data]
                                [:a "Hi there " (:name data)]))]
      (sut/render [comp {:name "world"}] el)
      (is (= "<a>Hi there world</a>" (.-innerHTML el)))))

  (testing "Renders hiccup with a list of children"
    (let [el (js/document.createElement "div")]
      (sut/render [:ul (list [:li "One"] [:li "two"] [:li "three"])] el)
      (is (= "<ul><li>One</li><li>two</li><li>three</li></ul>" (.-innerHTML el)))))

  (testing "Allows components to return nil on first render"
    (let [el (js/document.createElement "div")
          comp (sut/component (fn [data] nil))]
      (sut/render [comp {}] el)
      (is (= "" (.-innerHTML el)))))

  (testing "Allows components to return nil, then something"
    (let [el (js/document.createElement "div")
          comp (sut/component (fn [{:keys [text]}] (if text [:p text] nil)))]
      (sut/render [comp {}] el)
      (sut/render [comp {:text "#1"}] el)
      (sut/render [comp {:text "#2"}] el)
      (is (= "<p>#2</p>" (.-innerHTML el)))))

  (testing "Allows components to return something, nil"
    (let [el (js/document.createElement "div")
          comp (sut/component (fn [{:keys [text]}] (if text [:p text] nil)))]
      (sut/render [comp {:text "#1"}] el)
      (sut/render [comp {}] el)
      (is (= "" (.-innerHTML el)))))

  (testing "Allows components to return something, nil, something"
    (let [el (js/document.createElement "div")
          comp (sut/component (fn [{:keys [text]}] (if text [:p text] nil)))]
      (sut/render [comp {:text "#1"}] el)
      (sut/render [comp {}] el)
      (sut/render [comp {:text "#2"}] el)
      (is (= "<p>#2</p>" (.-innerHTML el))))))

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

  (testing "Calls both on-mount and ref callback"
    (let [el (js/document.createElement "div")
          on-mount (atom nil)
          ref (atom nil)
          component (sut/component
                     (fn [data] (d/div {:ref #(reset! ref data)} (:text data)))
                     {:on-mount #(reset! on-mount %2)})]
      (sut/render (component {:text "Look ma!"}) el)
      (is (= {:text "Look ma!"} @on-mount))
      (is (= {:text "Look ma!"} @ref))))

  (testing "Does not call on-mount on update"
    (let [el (js/document.createElement "div")
          on-mount (atom [])
          component (sut/component
                     (fn [data] (d/div {} (:text data)))
                     {:on-mount (fn [node data]
                                  (swap! on-mount conj data))})]
      (sut/render (component {:text "LOL"}) el)
      (sut/render (component {:text "Hello"}) el)
      (is (= 1 (count @on-mount)))))

  (testing "Does not call on-mount when keyed component changes position"
    (let [el (js/document.createElement "div")
          on-mount (atom [])
          component (sut/component
                     (fn [data] (d/div {} (:text data)))
                     {:on-mount (fn [node data]
                                  (swap! on-mount conj data))
                      :keyfn #(str "key")})]
      (sut/render (d/div {} (component {:text "LOL"})) el)
      (sut/render (d/div {} (d/div {} "Look:") (component {:text "Hello"})) el)
      (is (= 1 (count @on-mount)))))

  (testing "Calls on-mount on nested components without dedicated elements"
    (let [el (js/document.createElement "div")
          on-mount (atom [])
          child-component (sut/component
                           (fn [_] [:div "Inner"])
                           {:on-mount (fn [node & args]
                                        (swap! on-mount conj :child))})
          parent-component (sut/component
                            (fn [data] (child-component data))
                            {:on-mount (fn [node & args]
                                         (swap! on-mount conj :parent))})]
      (sut/render (parent-component {}) el)
      (is (= [:child :parent]
             @on-mount))))

  (testing "Calls on-mount when nested component child is swapped out"
    (let [el (js/document.createElement "div")
          on-mount (atom [])
          child-1 (sut/component
                   (fn [_] [:div {:key "lol"} "Inner #1"])
                   {:name "Child1"
                    :on-mount (fn [node & args]
                                (swap! on-mount conj :child-1))})
          child-2 (sut/component
                   (fn [_] [:div "Inner #2"])
                   {:name "Child2"
                    :on-mount (fn [node & args]
                                (swap! on-mount conj :child-2))})
          parent-component (sut/component
                            (fn [data]
                              (if (:one? data)
                                (child-1 data)
                                (child-2 data)))
                            {:name "Parent"
                             :on-mount (fn [node & args]
                                         (swap! on-mount conj :parent))})]
      (sut/render (parent-component {:one? true}) el)
      (sut/render (parent-component {:one? false}) el)
      (is (= [:child-1 :parent :child-2 :parent]
             @on-mount)))))

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
      (is (= [{:text "Hello"} {:text "Aight"}] @on-update))))

  (testing "Does not call on-update when re-rendering without changes"
    (let [el (js/document.createElement "div")
          hooks (atom [])
          component (sut/component
                     (fn [data]
                       [:div (:text data)])
                     {:on-update (fn [node data]
                                   (swap! hooks conj [:on-update :component data]))})
          app (sut/component
               (fn [data]
                 [:div.app (component data)])
               {:on-update (fn [node data]
                             (swap! hooks conj [:on-update :app data]))})]
      (sut/render (app {:text "Hello, visitor"}) el)
      (sut/render (app {:text "Hello, visitor"}) el)
      (is (= @hooks []))))

  (testing "Only calls on-update once after each change"
    (let [el (js/document.createElement "div")
          hooks (atom [])
          component (sut/component
                     (fn [data]
                       [:div (:text data)])
                     {:on-update (fn [node data]
                                   (swap! hooks conj [:on-update :component data]))})
          app (sut/component
               (fn [data]
                 [:div.app (component data)])
               {:on-update (fn [node data]
                             (swap! hooks conj [:on-update :app data]))})]
      (sut/render (app {:text "Hello, visitor"}) el)
      (sut/render (app {:text "Hello, visitor"}) el)
      (sut/render (app {:text "Hello, friend"}) el)
      (sut/render (app {:text "Hello, friend"}) el)
      (is (= @hooks [[:on-update :app {:text "Hello, friend"}]
                     [:on-update :component {:text "Hello, friend"}]]))))

  (testing "Does not call on-update when parent changes"
    (let [el (js/document.createElement "div")
          hooks (atom [])
          component (sut/component
                     (fn [data]
                       [:div (:text data)])
                     {:on-update (fn [node data]
                                   (swap! hooks conj [:on-update :component data]))})
          app (sut/component
               (fn [data]
                 [:div.app
                  [:h1 (:title data)]
                  (component (:content data))])
               {:on-update (fn [node data]
                             (swap! hooks conj [:on-update :app data]))})]
      (sut/render (app {:title "#1" :content {:text "Hello, visitor"}}) el)
      (sut/render (app {:title "#2" :content {:text "Hello, visitor"}}) el)
      (is (= @hooks [[:on-update :app {:title "#2" :content {:text "Hello, visitor"}}]])))))

(deftest on-render-test
  (testing "Calls on-render when component first mounts"
    (let [el (js/document.createElement "div")
          on-render (atom nil)
          component (sut/component
                     (fn [_] (d/div {} "LOL"))
                     {:on-render (fn [node & args]
                                   (reset! on-render (apply vector node args)))})]
      (sut/render (component {:a 42} {:static "Prop"} {:another "Static"}) el)
      (is (= [(.-firstChild el) {:a 42} nil {:static "Prop"} {:another "Static"}]
             @on-render))))

  (testing "Calls on-render and on-mount when component first mounts"
    (let [el (js/document.createElement "div")
          on-render (atom nil)
          on-mount (atom nil)
          component (sut/component
                     (fn [_] (d/div {} "LOL"))
                     {:on-render (fn [node & args]
                                   (reset! on-render (apply vector node args)))
                      :on-mount (fn [node & args]
                                  (reset! on-mount (apply vector node args)))})]
      (sut/render (component {:a 42} {:static "Prop"} {:another "Static"}) el)
      (is (= [(.-firstChild el) {:a 42} nil {:static "Prop"} {:another "Static"}]
             @on-render))
      (is (= [(.-firstChild el) {:a 42} {:static "Prop"} {:another "Static"}]
             @on-mount))))

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
              {:text "Aight"}] @on-render))))

  (testing "Calls on-render and on-update on each update"
    (let [el (js/document.createElement "div")
          on-render (atom [])
          on-update (atom [])
          component (sut/component
                     (fn [data] (d/div {} (:text data)))
                     {:on-render (fn [node data]
                                   (swap! on-render conj data))
                      :on-update (fn [node data]
                                   (swap! on-update conj data))})]
      (sut/render (component {:text "LOL"}) el)
      (sut/render (component {:text "Hello"}) el)
      (sut/render (component {:text "Aight"}) el)
      (is (= [{:text "LOL"}
              {:text "Hello"}
              {:text "Aight"}] @on-render))
      (is (= [{:text "Hello"}
              {:text "Aight"}] @on-update))))

  (testing "Passes previous data to on-render"
    (let [el (js/document.createElement "div")
          on-render (atom [])
          component (sut/component
                     (fn [data] (d/div {} (:text data)))
                     {:on-render (fn [node data old-data statics]
                                   (swap! on-render conj [data old-data statics]))})]
      (sut/render (component {:text "LOL"} 2) el)
      (sut/render (component {:text "Hello"} 2) el)
      (sut/render (component {:text "Aight"} 2) el)
      (is (= [[{:text "LOL"} nil 2]
              [{:text "Hello"} {:text "LOL"} 2]
              [{:text "Aight"} {:text "Hello"} 2]] @on-render))))

  (testing "Does not call on-render when there's no change"
    (let [el (js/document.createElement "div")
          on-render (atom 0)
          component (sut/component
                     (fn [data] (d/div {} (:text data)))
                     {:on-render (fn [& args] (swap! on-render inc))})]
      (sut/render (component {:text "Hello"}) el)
      (sut/render (component {:text "Hello"}) el)
      (sut/render (component {:text "Hello"}) el)
      (is (= 1 @on-render))))

  (testing "Passes previous data to on-update"
    (let [el (js/document.createElement "div")
          on-update (atom [])
          component (sut/component
                     (fn [data] (d/div {} (:text data)))
                     {:on-update (fn [node data old-data statics]
                                   (swap! on-update conj [data old-data statics]))})]
      (sut/render (component {:text "LOL"} 2) el)
      (sut/render (component {:text "Hello"} 2) el)
      (sut/render (component {:text "Aight"} 2) el)
      (is (= [[{:text "Hello"} {:text "LOL"} 2]
              [{:text "Aight"} {:text "Hello"} 2]] @on-update)))))

(deftest on-unmount-test
  (testing "Does not call on-unmount when component first mounts"
    (let [el (js/document.createElement "div")
          on-unmount (atom nil)
          component (sut/component
                     (fn [_] (d/div {} "LOL"))
                     {:on-unmount #(reset! on-unmount %)})]
      (sut/render (component {:a 42}) el)
      (is (nil? @on-unmount))))

  (testing "Does not call on-unmount when updating component"
    (let [el (js/document.createElement "div")
          on-unmount (atom nil)
          component (sut/component
                     (fn [_] (d/div {} "LOL"))
                     {:on-unmount #(reset! on-unmount %)})]
      (sut/render (component {:a 42}) el)
      (sut/render (component {:a 13}) el)
      (is (nil? @on-unmount))))

  (testing "Calls on-unmount when removing component"
    (let [el (js/document.createElement "div")
          on-unmount (atom nil)
          component (sut/component
                     (fn [data] (d/div {} (:text data)))
                     {:on-unmount (fn [node & args]
                                    (reset! on-unmount (apply vector node args)))})]
      (sut/render (component {:text "LOL"}) el)
      (let [rendered (.-firstChild el)]
        (sut/render (d/h1 {} "Gone!") el)
        (is (= [rendered {:text "LOL"}] @on-unmount)))))

  (testing "Calls on-unmount when removing nested component"
    (let [el (js/document.createElement "div")
          on-unmount (atom [])
          inner-component (sut/component
                           (fn [data] (d/div {} (:text data)))
                           {:on-unmount (fn [node & args]
                                          (swap! on-unmount conj :inner))})
          outer-component (sut/component
                           (fn [data] (inner-component data))
                           {:on-unmount (fn [node & args]
                                          (swap! on-unmount conj :outer))})]
      (sut/render (outer-component {:text "LOL"}) el)
      (let [rendered (.-firstChild el)]
        (sut/render (d/h1 {} "Gone!") el)
        (is (= [:outer :inner] @on-unmount))))))

(deftest animation-callbacks-test
  (testing "Calls will-appear when parent mounts"
    (let [el (js/document.createElement "div")
          will-appear (atom nil)
          component (sut/component
                     (fn [data] (d/div {} (:text data)))
                     {:will-appear (fn [node callback & args]
                                     (reset! will-appear (apply vector node args)))})]
      (sut/render (d/div {} (component {:text "LOL"})) el)
      (is (= [(.. el -firstChild -firstChild) {:text "LOL"}] @will-appear))))

  (testing "Calls did-appear when the callback passed to will-appear is called"
    (let [el (js/document.createElement "div")
          will-appear (atom nil)
          did-appear (atom nil)
          component (sut/component
                     (fn [data] (d/div {} (:text data)))
                     {:will-appear (fn [node callback & args] (reset! will-appear callback))
                      :did-appear (fn [node & args]
                                    (reset! did-appear (apply vector node args)))})]
      (sut/render (d/div {} (component {:text "LOL"})) el)
      (is (nil? @did-appear))
      (@will-appear)
      (is (= [(.. el -firstChild -firstChild) {:text "LOL"}] @did-appear))))

  (testing "Does not call will-appear when parent updates"
    (let [el (js/document.createElement "div")
          will-appear (atom 0)
          component (sut/component
                     (fn [data] (d/div {} (:text data)))
                     {:will-appear #(swap! will-appear inc)})]
      (sut/render (d/div {} (component {:text "LOL"})) el)
      (sut/render (d/div {} (component {:text "LOL!!"})) el)
      (is (= 1 @will-appear))))

  (testing "Does not call will-enter when parent mounts"
    (let [el (js/document.createElement "div")
          will-enter (atom nil)
          component (sut/component
                     (fn [data] (d/div {} (:text data)))
                     {:will-enter (fn [node & args]
                                    (reset! will-enter (apply vector node args)))})]
      (sut/render (d/div {} (component {:text "LOL"})) el)
      (is (nil? @will-enter))))

  (testing "Calls will-enter when mounting element inside existing parent"
    (let [el (js/document.createElement "div")
          will-enter (atom nil)
          component (sut/component
                     (fn [data] (d/div {} (:text data)))
                     {:will-enter (fn [node callback & args]
                                    (reset! will-enter (apply vector node args)))})]
      (sut/render (d/div {}) el)
      (sut/render (d/div {} (component {:text "LOL"})) el)
      (is (= [(.. el -firstChild -firstChild) {:text "LOL"}] @will-enter))))

  (testing "Does not call will-enter when moving element inside existing parent"
    (let [el (js/document.createElement "div")
          will-enter (atom 0)
          component (sut/component
                     (fn [data] (d/div {} (:text data)))
                     {:will-enter #(swap! will-enter inc)
                      :keyfn #(str "comp")})]
      (sut/render (d/div {}) el)
      (sut/render (d/div {} (component {:text "LOL"})) el)
      (sut/render (d/div {} (d/div {} "Hmm") (component {:text "LOL"})) el)
      (is (= 1 @will-enter))))

  (testing "Calls did-enter when the callback passed to will-enter is called"
    (let [el (js/document.createElement "div")
          will-enter (atom nil)
          did-enter (atom nil)
          component (sut/component
                     (fn [data] (d/div {} (:text data)))
                     {:will-enter (fn [node callback & args] (reset! will-enter callback))
                      :did-enter (fn [node & args]
                                   (reset! did-enter (apply vector node args)))})]
      (sut/render (d/div {}) el)
      (sut/render (d/div {} (component {:text "LOL"})) el)
      (is (nil? @did-enter))
      (@will-enter)
      (is (= [(.. el -firstChild -firstChild) {:text "LOL"}] @did-enter))))

  (testing "Calls the will-leave callback when unmounting the DOM node"
    (let [el (js/document.createElement "div")
          will-leave (atom nil)
          component (sut/component
                     (fn [data] (d/div {} (:text data)))
                     {:will-leave (fn [node callback & args]
                                    (reset! will-leave (apply vector node args)))})]
      (sut/render (d/div {} (component {:text "LOL"})) el)
      (sut/render (d/div {}) el)
      (is (= [(.. el -firstChild -firstChild) {:text "LOL"}] @will-leave))))

  (testing "Does not call will-leave until enter callback is called"
    (let [el (js/document.createElement "div")
          will-enter (atom nil)
          will-leave (atom nil)
          component (sut/component
                     (fn [data] (d/div {} (:text data)))
                     {:will-enter (fn [node callback] (reset! will-enter callback))
                      :will-leave (fn [node callback & args]
                                    (reset! will-leave (apply vector node args)))})]
      (sut/render (d/div {}) el)
      (sut/render (d/div {} (component {:text "LOL"})) el)
      (sut/render (d/div {}) el)
      (is (nil? @will-leave))
      (@will-enter)
      (is (= [(.. el -firstChild -firstChild) {:text "LOL"}] @will-leave))))

  (testing "Calls did-leave when the callback passed to will-leave is called"
    (let [el (js/document.createElement "div")
          will-leave (atom nil)
          did-leave (atom nil)
          component (sut/component
                     (fn [data] (d/div {} (:text data)))
                     {:will-leave (fn [node callback & args] (reset! will-leave callback))
                      :did-leave (fn [node & args]
                                   (reset! did-leave (apply vector node args)))})]
      (sut/render (d/div {} (component {:text "LOL"})) el)
      (sut/render (d/div {}) el)
      (let [element (.. el -firstChild -firstChild)]
        (is (nil? @did-leave))
        (@will-leave)
        (is (= [element {:text "LOL"}] @did-leave)))))

  (testing "Does not call will-leave when parent element is removed"
    (let [el (js/document.createElement "div")
          will-leave (atom nil)
          component (sut/component
                     (fn [data] (d/div {} (:text data)))
                     {:will-leave (fn [node callback & args] (reset! will-leave callback))})]
      (sut/render (d/div {} (d/div {} (component {:text "LOL"}))) el)
      (sut/render (d/div {}) el)
      (is (nil? @will-leave))))

  (testing "Calls on-unmount when removing nested component"
    (let [el (js/document.createElement "div")
          on-unmount (atom [])
          child (sut/component
                 (fn [data] [:div "Child"])
                 {:on-unmount (fn [node & args]
                                (swap! on-unmount conj :child))})
          parent (sut/component
                  (fn [data]
                    (if (:child? data)
                      (child data)
                      [:div "Parent"]))
                  {:on-unmount (fn [node & args]
                                 (swap! on-unmount conj :parent))})]
      (sut/render (parent {:child? true}) el)
      (sut/render (parent {:child? false}) el)
      (is (= [:parent :child] @on-unmount)))))

(deftest TransitionGroup-test
  (testing "TransitionGroup creates span component"
    (let [el (js/document.createElement "div")]
      (sut/render (sut/TransitionGroup {} []) el)
      (is (= (.-innerHTML el) "<span></span>"))))

  (testing "Creates TransitionGroup with custom component tag"
    (let [el (js/document.createElement "div")]
      (sut/render (sut/TransitionGroup {:component "div"} []) el)
      (is (= (.-innerHTML el) "<div></div>"))))

  (testing "Creates TransitionGroup with custom attributes"
    (let [el (js/document.createElement "div")]
      (sut/render (sut/TransitionGroup {:component "div" :className "lol" :id "ok"} []) el)
      (is (= (.. el -firstChild -className) "lol"))
      (is (= (.. el -firstChild -id) "ok"))))

  (testing "Creates TransitionGroup with custom component"
    (let [el (js/document.createElement "div")
          component (sut/component
                     (fn [children]
                       (d/h1 {} children)))]
      (sut/render (sut/TransitionGroup {:component component}
                                       [(d/span {} "Hey") (d/span {} "there!")]) el)
      (is (= (.-innerHTML el) "<h1><span>Hey</span><span>there!</span></h1>")))))

(deftest CSSTransitionGroupEnterTest
  (testing "Adds enter class name according to the transition name"
    (let [el (js/document.createElement "div")]
      (sut/render (sut/CSSTransitionGroup {:transitionName "example"} []) el)
      (sut/render
       (sut/CSSTransitionGroup
        {:transitionName "example"}
        [(d/div {:key "#1"} "I will enter")])
       el)
      (is (= "example-enter" (.. el -firstChild -firstChild -className)))))

  (testing "Grabs enter class name from map argument"
    (let [el (js/document.createElement "div")]
      (sut/render (sut/CSSTransitionGroup {:transitionName {:enter "examplez-enter"}} []) el)
      (sut/render
       (sut/CSSTransitionGroup
        {:transitionName {:enter "examplez-enter"}}
        [(d/div {:key "#1"} "I will enter")])
       el)
      (is (= "examplez-enter" (.. el -firstChild -firstChild -className)))))

  (testing "Does not add enter class name when enter transition is disabled"
    (let [el (js/document.createElement "div")]
      (sut/render (sut/CSSTransitionGroup {:transitionName "example"
                                           :transitionEnter false} []) el)
      (sut/render
       (sut/CSSTransitionGroup
        {:transitionName "example"
         :transitionEnter false}
        [(d/div {:key "#1"} "I will enter")])
       el)
      (is (= "" (.. el -firstChild -firstChild -className)))))

  (testing "Adds enter class name to existing ones"
    (let [el (js/document.createElement "div")]
      (sut/render (sut/CSSTransitionGroup {:transitionName "example"} []) el)
      (sut/render
       (sut/CSSTransitionGroup
        {:transitionName "example"}
        [(d/div {:key "#2" :className "item"} "I will enter")])
       el)
      (is (= "item example-enter" (.. el -firstChild -firstChild -className)))))

  (testing "Adds enter-active class name on next tick"
    (async done
      (let [el (js/document.createElement "div")]
        (sut/render (sut/CSSTransitionGroup {:transitionName "example"} []) el)
        (sut/render
         (sut/CSSTransitionGroup
          {:transitionName "example"}
          [(d/div {:key "#3"} "I will enter")])
         el)
        (js/setTimeout
         (fn []
           (is (= "example-enter example-enter-active" (.. el -firstChild -firstChild -className)))
           (done))
         100))))

  (testing "Adds custom enter-active class name on next tick"
    (async done
      (let [el (js/document.createElement "div")
            props {:transitionName {:enter "swoosh" :enterActive "lol"}}]
        (sut/render (sut/CSSTransitionGroup props []) el)
        (sut/render (sut/CSSTransitionGroup props [(d/div {:key "#3"} "I will enter")])
                    el)
        (js/setTimeout
         (fn []
           (is (= "swoosh lol" (.. el -firstChild -firstChild -className)))
           (done))
         0))))

  (testing "Uses custom enter class name for missing enterActive class name"
    (async done
      (let [el (js/document.createElement "div")
            props {:transitionName {:enter "swoosh"}}]
        (sut/render (sut/CSSTransitionGroup props []) el)
        (sut/render (sut/CSSTransitionGroup props [(d/div {:key "#3"} "I will enter")])
                    el)
        (js/setTimeout
         (fn []
           (is (= "swoosh swoosh-active" (.. el -firstChild -firstChild -className)))
           (done))
         0))))

  (testing "Removes enter transition class names after timeout"
    (async done
      (let [el (js/document.createElement "div")]
        (sut/render (sut/CSSTransitionGroup {:transitionName "example"
                                             :transitionEnterTimeout 10} []) el)
        (sut/render
         (sut/CSSTransitionGroup
          {:transitionName "example"
           :transitionEnterTimeout 10}
          [(d/div {:key "#4" :className "do not remove"} "I will enter")])
         el)
        (js/setTimeout
         (fn []
           (is (= "do not remove" (.. el -firstChild -firstChild -className)))
           (done))
         10))))

  (testing "Removes enter transition class names after completed transition"
    (async done
      (let [el (js/document.createElement "div")
            style (or (js/document.getElementById "transition-css")
                      (let [style (js/document.createElement "style")]
                        (set! (.-type style) "text/css")
                        (set! (.-id style) "transition-css")
                        (js/document.head.appendChild style)
                        style))]
        (set! (.-innerHTML style) (str ".transition-example-enter {transition: color 10ms; color: #000;}"
                                       ".transition-example-enter-active {color: #f00;}"))
        (js/document.body.appendChild el)
        (js/setTimeout
         (fn []
           (sut/render (sut/CSSTransitionGroup {:transitionName "transition-example"} []) el)
           (js/setTimeout
            (fn []
              (sut/render
               (sut/CSSTransitionGroup
                {:transitionName "transition-example"}
                [(d/div {:key "#5"} "Check test document CSS")])
               el)
              (js/setTimeout
               (fn []
                 (is (= "" (.. el -firstChild -firstChild -className)))
                 (.removeChild (.-parentNode el) el)
                 (done))
               50))
            50))
         50)))))

(deftest CSSTransitionGroupAppearTest
  (testing "Adds appear class name according to the transition name"
    (let [el (js/document.createElement "div")]
      (sut/render
       (sut/CSSTransitionGroup
        {:transitionName "example"
         :transitionAppear true}
        [(d/div {:key "#1"} "I will appear")])
       el)
      (is (= "example-appear" (.. el -firstChild -firstChild -className)))))

  (testing "Does not add appear class name when appear transition is disabled"
    (let [el (js/document.createElement "div")]
      (sut/render
       (sut/CSSTransitionGroup
        {:transitionName "example"}
        [(d/div {:key "#1"} "I will appear")])
       el)
      (is (= "" (.. el -firstChild -firstChild -className)))))

  (testing "Adds appear class name to existing ones"
    (let [el (js/document.createElement "div")]
      (sut/render
       (sut/CSSTransitionGroup
        {:transitionName "example"
         :transitionAppear true}
        [(d/div {:key "#2" :className "item"} "I will appear")])
       el)
      (is (= "item example-appear" (.. el -firstChild -firstChild -className)))))

  (testing "Adds appear-active class name on next tick"
    (async done
      (let [el (js/document.createElement "div")]
        (sut/render
         (sut/CSSTransitionGroup
          {:transitionName "example"
           :transitionAppear true}
          [(d/div {:key "#3"} "I will appear")])
         el)
        (js/setTimeout
         (fn []
           (is (= "example-appear example-appear-active" (.. el -firstChild -firstChild -className)))
           (done))
         0))))

  (testing "Removes appear transition class names after timeout"
    (async done
      (let [el (js/document.createElement "div")]
        (sut/render
         (sut/CSSTransitionGroup
          {:transitionName "example"
           :transitionAppear true
           :transitionAppearTimeout 10}
          [(d/div {:key "#4" :className "do not remove"} "I will appear")])
         el)
        (js/setTimeout
         (fn []
           (is (= "do not remove" (.. el -firstChild -firstChild -className)))
           (done))
         10)))))

(deftest CSSTransitionGroupLeaveTest
  (testing "Adds leave class name according to the transition name"
    (let [el (js/document.createElement "div")]
      (sut/render
       (sut/CSSTransitionGroup
        {:transitionName "example"}
        [(d/div {:key "#1"} "I will leave")])
       el)
      (sut/render (sut/CSSTransitionGroup {:transitionName "example"} []) el)
      (is (= "example-leave" (.. el -firstChild -firstChild -className)))))

  (testing "Adds leave class name to existing ones"
    (let [el (js/document.createElement "div")]
      (sut/render
       (sut/CSSTransitionGroup
        {:transitionName "example"}
        [(d/div {:key "#2" :className "item"} "I will leave")])
       el)
      (sut/render (sut/CSSTransitionGroup {:transitionName "example"} []) el)
      (is (= "item example-leave" (.. el -firstChild -firstChild -className)))))

  (testing "Adds leave-active class name on next tick"
    (async done
      (let [el (js/document.createElement "div")]
        (sut/render
         (sut/CSSTransitionGroup
          {:transitionName "example"}
          [(d/div {:key "#3"} "I will leave")])
         el)
        (sut/render (sut/CSSTransitionGroup {:transitionName "example"} []) el)
        (js/setTimeout
         (fn []
           (is (= "example-leave example-leave-active" (.. el -firstChild -firstChild -className)))
           (done))
         0))))

  (testing "Removes node after leave timeout"
    (async done
      (let [el (js/document.createElement "div")]
        (sut/render
         (sut/CSSTransitionGroup
          {:transitionName "example"
           :transitionLeaveTimeout 10}
          [(d/div {:key "#4"} "I will leave")])
         el)
        (sut/render (sut/CSSTransitionGroup {:transitionName "example"
                                             :transitionLeaveTimeout 10} []) el)
        (js/setTimeout
         (fn []
           (is (nil? (.. el -firstChild -firstChild)))
           (done))
         10)))))

(deftest EagerRenderModeTest
  (testing "Does not re-render old components when new ones are defined by default"
    (let [el (js/document.createElement "div")
          on-render (atom [])
          comp (sut/component
                (fn [_] (d/div {} "Testing"))
                {:on-render (fn [node data & args]
                              (swap! on-render conj data))})]
      (sut/render (comp {:a 42}) el)
      (sut/component (fn [_] (d/div {} "Created another component")))
      (sut/render (comp {:a 42}) el)
      (is (= 1 (count @on-render)))))

  (testing "Re-renders old components when new ones are defined when configured to"
    (with-redefs [dumdom.component/*render-eagerly?* true]
      (let [el (js/document.createElement "div")
            on-render (atom [])
            comp (sut/component
                  (fn [_] (d/div {} "Testing"))
                  {:on-render (fn [node data & args]
                                (swap! on-render conj data))})]
        (sut/render (comp {:a 42}) el)
        (sut/component (fn [_] (d/div {} "Created another component")))
        (sut/render (comp {:a 42}) el)
        (is (= 2 (count @on-render))))))

  (testing "Only re-renders old components after new ones are defined"
    (with-redefs [dumdom.component/*render-eagerly?* true]
      (let [el (js/document.createElement "div")
            on-render (atom [])
            comp (sut/component
                  (fn [_] (d/div {} "Testing"))
                  {:on-render (fn [node data & args]
                                (swap! on-render conj data))})]
        (sut/render (comp {:a 42}) el)
        (sut/component (fn [_] (d/div {} "Created another component")))
        (sut/render (comp {:a 42}) el)
        (sut/render (comp {:a 42}) el)
        (is (= 2 (count @on-render)))))))

(deftest NestedComponentsTest
  (testing "Preserves key from nested component"
    (let [el (js/document.createElement "div")
          inner-component (sut/component
                           (fn [data] (d/div {} (:text data)))
                           {:keyfn :id
                            :name "Inner"})
          outer-component (sut/component
                           (fn [data] (inner-component data))
                           {:name "Outer"})]
      (is (= "Outer.Inner.42.0" (:key (render (outer-component {:text "Hello" :id 42}))))))))

(deftest KeyReuseTest
  (testing "Properly renders two elements with the same key"
    (let [el (js/document.createElement "div")
          on-render (atom [])
          comp (sut/component
                (fn [{:keys [text]}] (d/p {} text))
                {:keyfn (constantly "oops")
                 :on-mount (fn [node & args]
                             (set! (.-innerHTML node) "Mounted"))
                 :on-render (fn [node data & args]
                              (swap! on-render conj data))})]
      (sut/render [:div
                   (comp {:text "Same data!"})
                   (comp {:text "Same data!"})] el)
      (is (= "<div><p>Mounted</p><p>Mounted</p></div>" (.-innerHTML el)))
      (is (= [{:text "Same data!"}
              {:text "Same data!"}] @on-render))))

  (testing "Differentiates children of components with duplicate keys"
    (let [el (js/document.createElement "div")
          on-render (atom [])
          icon (sut/component
                (fn [data] [:div])
                {:on-mount (fn [node & args]
                             (set! (.-innerHTML node) "Mounted"))})
          comp (sut/component
                (fn [{:keys [text]}]
                  [:section
                   [:h1 text]
                   [icon]])
                {:keyfn (constantly "duplicate")})]
      (sut/render [:div
                   (comp {:text "First thing"})
                   (comp {:text "Second thing"})] el)
      (is (= (str "<div>"
                  "<section><h1>First thing</h1><div>Mounted</div></section>"
                  "<section><h1>Second thing</h1><div>Mounted</div></section>"
                  "</div>")
             (.-innerHTML el)))))

  (testing "Differentiates children of elements with duplicate keys"
    (let [el (js/document.createElement "div")
          on-render (atom [])
          icon (sut/component
                (fn [data] [:div])
                {:on-mount (fn [node & args]
                             (set! (.-innerHTML node) "Mounted"))})
          comp (sut/component
                (fn [{:keys [text]}]
                  [:div
                   [:h1 text]
                   [icon]]))]
      (sut/render [:div
                   [:section {:key "ohnoes!"}
                    (comp {:text "First thing"})]
                   [:section {:key "ohnoes!"}
                    (comp {:text "Second thing"})]] el)
      (is (= (.-innerHTML el)
             (str "<div>"
                  "<section><div><h1>First thing</h1><div>Mounted</div></div></section>"
                  "<section><div><h1>Second thing</h1><div>Mounted</div></div></section>"
                  "</div>"))))))

(deftest RenderNestedLists
  (testing "Renders nested lists without hiccups"
    (let [el (js/document.createElement "div")]
      (sut/render (d/div {}
                    (list (list (d/h1 {} (list (list "Hello")))))
                    (list (list [:p {} (list (list "Yup"))]))) el)
      (is (= (str "<div>"
                  "<h1>Hello</h1>"
                  "<p>Yup</p>"
                  "</div>")
             (.-innerHTML el))))))

(deftest component-comment-test
  (testing "Prepends component comment when *render-comments?* is true"
    (let [el (js/document.createElement "div")
          comp (sut/component (fn [{}] [:p "This is awesome!"])
                              {:name "Awesomesauce"})]
      (binding [dumdom.component/*render-comments?* true]
        (sut/render [:div [comp]] el)
        (is (= (str "<div>"
                    "<!--Awesomesauce-->"
                    "<p>This is awesome!</p>"
                    "</div>")
               (.-innerHTML el)))))))

(defn trigger-event [el event-name]
  (->> (doto (js/document.createEvent "HTMLEvents")
         (.initEvent event-name true false))
       (.dispatchEvent el)))

(deftest event-data-test
  (testing "Handles event data with a function"
    (let [el (js/document.createElement "div")
          event-data (atom nil)
          component (d/div {:onClick [:event "d/div onClick"]} "Click it")]
      (sut/render
       component
       el
       {:handle-event #(reset! event-data %&)})
      (trigger-event (.-firstChild el) "click")
      (is (= 2 (count @event-data)))
      (is (= [:event "d/div onClick"] (second @event-data)))))

  (testing "Handles event data with dashed event handler name"
    (let [el (js/document.createElement "div")
          event-data (atom nil)
          component [:div {:on-click [:event "d/div onClick"]} "Click it"]]
      (sut/render
       component
       el
       {:handle-event #(reset! event-data %&)})
      (trigger-event (.-firstChild el) "click")
      (is (= 2 (count @event-data)))
      (is (= [:event "d/div onClick"] (second @event-data)))))

  (testing "Handles event data with a global function"
    (let [el (js/document.createElement "div")
          event-data (atom nil)
          component (d/div {:onClick [:event "d/div onClick"]} "Click it")]
      (sut/set-event-handler! #(reset! event-data %&))
      (sut/render component el)
      (trigger-event (.-firstChild el) "click")
      (sut/set-event-handler! nil)
      (is (= 2 (count @event-data)))
      (is (= [:event "d/div onClick"] (second @event-data)))))

  (testing "Handles event data with a function on hiccup"
    (let [el (js/document.createElement "div")
          event-data (atom nil)
          component [:div {:onClick [:event "d/div onClick"]} "Click it"]]
      (sut/render
       component
       el
       {:handle-event #(reset! event-data %&)})
      (trigger-event (.-firstChild el) "click")
      (is (= 2 (count @event-data)))
      (is (= [:event "d/div onClick"] (second @event-data)))))

  (testing "Uses the passed in handle-event function with cached vdom"
    (let [el (js/document.createElement "div")
          event-fns (atom [])
          component [:div {:onClick [:event "d/div onClick"]} "Click it"]]
      (sut/render
       component
       el
       {:handle-event #(swap! event-fns conj :f1)})
      (trigger-event (.-firstChild el) "click")
      (sut/render
       component
       el
       {:handle-event #(swap! event-fns conj :f2)})
      (trigger-event (.-firstChild el) "click")
      (sut/render
       component
       el
       {:handle-event #(swap! event-fns conj :f3)})
      (trigger-event (.-firstChild el) "click")
      (is (= [:f1 :f2 :f3] @event-fns)))))
