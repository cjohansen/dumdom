(ns dumdom.component
  (:require [clojure.string :as str]
            [dumdom.element :as e]))

(def ^:dynamic *render-eagerly?*
  "When this var is set to `true`, every existing component will re-render on the
  next call after a new component has been created, even if the input data has
  not changed. This can be useful in development - if you have any level of
  indirection in your rendering code (e.g. passing a component function as the
  \"static arg\" to another component, multi-methods, etc), you are not
  guaranteed to have all changed components re-render after a compile and hot
  swap. With this var set to `true`, changing any code that defines a dumdom
  component will cause all components to re-render."
  false)

(def ^:dynamic *render-comments?*
  "When this var is set to `true`, an HTML comment block containing the
  component's name will be emitted for every named component. Useful
  during development to get an overview of which component is responsible
  for rendering a given fragment of the DOM."
  false)

(def eager-render-required? (atom false))

(defn- should-component-update? [component-state data]
  (or (not (contains? component-state :data))
      (not= (:data component-state) data)
      (and *render-eagerly?* @eager-render-required?)))

(defn setup-animation-hooks [rendered animation {:keys [will-enter will-appear]}]
  (when will-appear
    (swap! animation assoc :will-appear will-appear))
  (cond-> rendered
    will-enter (assoc :willEnter #(swap! animation assoc :will-enter will-enter))
    will-appear (assoc :willAppear #(swap! animation dissoc :will-appear))))

(defn- setup-mount-hook [rendered {:keys [on-mount on-render will-appear did-appear will-enter did-enter]} data args animation]
  (cond-> rendered
    (or on-mount on-render will-enter will-appear)
    (update-in
     [:data :hook :insert]
     (fn [insert-hook]
       (fn [vnode]
         (when insert-hook (insert-hook vnode))
         (when on-mount (apply on-mount (.-elm vnode) data args))
         (when on-render (apply on-render (.-elm vnode) data nil args))
         (let [{:keys [will-enter will-appear]} @animation]
           (when-let [callback (or will-enter will-appear)]
             (swap! animation assoc :ready? false)
             (apply callback
                    (.-elm vnode)
                    (fn []
                      (swap! animation assoc :ready? true)
                      (when-let [completion (if (= callback will-enter)
                                              did-enter
                                              did-appear)]
                        (apply completion (.-elm vnode) data args)))
                    data
                    args))))))))

(defn- setup-update-hook [rendered {:keys [on-update on-render]} data old-data args]
  (cond-> rendered
    (or on-update on-render)
    (assoc-in
     [:data :hook :update]
     (fn hook [old-vnode vnode]
       (when-not #?(:cljs (.-called hook)
                    :clj true)
         #?(:cljs (set! (.-called hook) true))
         (when on-update (apply on-update (.-elm vnode) data old-data args))
         (when on-render (apply on-render (.-elm vnode) data old-data args)))))))

(defn- setup-unmount-hook [rendered component data args animation on-destroy]
  (cond-> rendered
    :always
    (update-in
     [:data :hook :destroy]
     (fn [destroy-hook]
       (fn [vnode]
         (when-let [on-unmount (:on-unmount component)]
           (apply on-unmount (.-elm vnode) data args))
         (when destroy-hook
           (destroy-hook vnode))
         (on-destroy))))

    (:will-leave component)
    (assoc-in
     [:data :hook :remove]
     (fn [vnode snabbdom-callback]
       (let [callback (fn []
                        (when-let [did-leave (:did-leave component)]
                          (apply did-leave (.-elm vnode) data args))
                        (snabbdom-callback))]
         (if (:ready? @animation)
           (apply (:will-leave component) (.-elm vnode) callback data args)
           (add-watch animation :leave
             (fn [k r o n]
               (when (:ready? n)
                 (remove-watch animation :leave)
                 (apply (:will-leave component) (.-elm vnode) callback data args))))))))))

(defn resolve-key [rendered component-name keyfn-key kmap]
  (let [k (->> [component-name
                (str keyfn-key)
                (some-> (:dumdom/component-key rendered) first)]
               (remove empty?)
               (str/join ".")
               (e/enumerate-key kmap))]
    (assoc rendered
           :dumdom/component-key k
           :key (str/join "." k))))

(defn component
  "Returns a component function that uses the provided function for rendering. The
  resulting component will only call through to its rendering function when
  called with data that is different from the data that produced the currently
  rendered version of the component.

  The rendering function can be called with any number of arguments, but only
  the first one will influence rendering decisions. You should call the
  component with a single immutable value, followed by any number of other
  arguments, as desired. These additional constant arguments are suitable for
  passing messaging channels, configuration maps, and other utilities that are
  constant for the lifetime of the rendered element.

  The optional opts argument is a map with additional properties:

  :on-mount - A function invoked once, immediately after initial rendering. It
  is passed the rendered DOM node, and all arguments passed to the render
  function.

  :on-update - A function invoked immediately after an updated is flushed to the
  DOM, but not on the initial render. It is passed the underlying DOM node, the
  value, and any constant arguments passed to the render function.

  :on-render - A function invoked immediately after the DOM is updated, both on
  the initial render and subsequent updates. It is passed the underlying DOM
  node, the value, the old value, and any constant arguments passed to the
  render function.

  :on-unmount - A function invoked immediately before the component is unmounted
  from the DOM. It is passed the underlying DOM node, the most recent value and
  the most recent constant args passed to the render fn.

  :will-appear - A function invoked when this component is added to a mounting
  container component. Invoked at the same time as :on-mount. It is passed the
  underlying DOM node, a callback function, the most recent value and the most
  recent constant args passed to the render fn. The callback should be called to
  indicate that the element is done \"appearing\".

  :did-appear - A function invoked immediately after the callback passed
  to :will-appear is called. It is passed the underlying DOM node, the most
  recent value, and the most recent constant args passed to the render fn.

  :will-enter - A function invoked when this component is added to an already
  mounted container component. Invoked at the same time as :on.mount. It is
  passed the underlying DOM node, a callback function, the value and any
  constant args passed to the render fn. The callback function should be called
  to indicate that the element is done entering.

  :did-enter - A function invoked after the callback passed to :will-enter is
  called. It is passed the underlying DOM node, the value and any constant args
  passed to the render fn.

  :will-leave - A function invoked when this component is removed from its
  containing component. Is passed the underlying DOM node, a callback function,
  the most recent value and the most recent constant args passed to the render
  fn. The DOM node will not be removed until the callback is called.

  :did-leave - A function invoked after the callback passed to :will-leave is
  called (at the same time as :on-unmount). Is passed the underlying DOM node,
  the most recent value and the most recent constant args passed to the render
  fn."
  ([render] (component render {}))
  ([render opt]
   (when *render-eagerly?*
     (reset! eager-render-required? true))
   (let [instances (atom {})
         component-name (or (:name opt)
                            #?(:cljs (not-empty (.-name render)))
                            (str #?(:cljs (random-uuid)
                                    :clj (java.util.UUID/randomUUID))))]
     (fn [data & args]
       (let [comp-fn
             (fn [path kmap]
               (let [key (when-let [keyfn (:keyfn opt)] (keyfn data))
                     lookup-key (e/enumerate-key kmap key)
                     fullpath (conj path lookup-key)
                     instance (@instances fullpath)
                     animation (atom {:ready? true})]
                 (if (should-component-update? instance data)
                   (let [rendered
                         (some->
                          (when-let [vdom (apply render data args)]
                            ((e/inflate-hiccup vdom) fullpath {}))
                          (resolve-key component-name key kmap)
                          (assoc :dumdom/render-comments? *render-comments?*
                                 :dumdom/lookup-key lookup-key)
                          (update :dumdom/component-name
                                  (fn [s]
                                    (str component-name
                                         (when s (str "/" s)))))
                          #?(:cljs (setup-animation-hooks animation opt))
                          #?(:cljs (setup-unmount-hook opt data args animation #(swap! instances dissoc fullpath))))]
                     (swap! instances assoc fullpath {:vdom rendered :data data})
                     ;; The insert and update hooks are added after the instance
                     ;; is cached. When used from the cache, we never want
                     ;; insert or update hooks to be called. Snabbdom will
                     ;; occasionally call these even when there are no changes,
                     ;; because it uses identity to determine if a vdom node
                     ;; represents a change. Since dumdom always produces a new
                     ;; JavaScript object, Snabbdom's check will have false
                     ;; positives.
                     #?(:cljs (cond-> rendered
                                rendered
                                (setup-mount-hook opt data args animation)

                                ;; If the instance is nil, this is a new render,
                                ;; and we don't want to trigger any updates
                                ;; until it's been re-rendered with new data
                                (and rendered instance)
                                (setup-update-hook opt data (:data instance) args))
                        :clj rendered))
                   (:vdom instance))))]
         #?(:cljs (set! (.-dumdom comp-fn) true))
         comp-fn)))))

(defn single-child? [x]
  (or (fn? x) ;; component
      (and (vector? x)
           (keyword? (first x))) ;; hiccup
      ))

(defn TransitionGroup [el-fn opt children]
  ;; Vectors with a function in the head position are interpreted as hiccup data
  ;; - force children to be seqs to avoid them being parsed as hiccup.
  (let [children (if (single-child? children)
                   (list children)
                   (seq children))]
    (if (ifn? (:component opt))
      ((:component opt) children)
      (apply el-fn (or (:component opt) "span") opt children))))

(defn- complete-transition [node timeout callback]
  (if timeout
    #?(:cljs (js/setTimeout callback timeout))
    (let [callback-fn (atom nil)
          f (fn []
              (callback)
              (.removeEventListener node "transitionend" @callback-fn))]
      (reset! callback-fn f)
      (.addEventListener node "transitionend" f))))

(defn- transition-classes [transitionName transition]
  (if (string? transitionName)
    [(str transitionName "-" transition) (str transitionName "-" transition "-active")]
    (let [k (keyword transition)
          k-active (keyword (str transition "Active"))]
      [(k transitionName) (get transitionName k-active (str (k transitionName) "-active"))])))

(defn- animate [transition {:keys [enabled-by-default?]}]
  (let [timeout (keyword (str "transition" transition "Timeout"))]
    (fn [node callback {:keys [transitionName] :as props}]
      (if (get props (keyword (str "transition" transition)) enabled-by-default?)
        (let [[init-class active-class] (transition-classes transitionName (.toLowerCase transition))]
          (.add (.-classList node) init-class)
          (complete-transition node (get props timeout) callback)
          #?(:cljs (js/setTimeout #(.add (.-classList node) active-class) 0)))
        (callback)))))

(defn- cleanup-animation [transition]
  (fn [node {:keys [transitionName]}]
    (.remove (.-classList node) (str transitionName "-" transition))
    (.remove (.-classList node) (str transitionName "-" transition "-active"))))

(def TransitioningElement
  (component
   (fn [{:keys [child]}]
     child)
   {:will-appear (animate "Appear" {:enabled-by-default? false})
    :did-appear (cleanup-animation "appear")
    :will-enter (animate "Enter" {:enabled-by-default? true})
    :did-enter (cleanup-animation "enter")
    :will-leave (animate "Leave" {:enabled-by-default? true})}))

(defn CSSTransitionGroup [el-fn opt children]
  (let [children (if (single-child? children)
                   (list children)
                   (seq children))]
    (TransitionGroup el-fn opt (map #(TransitioningElement (assoc opt :child %)) children))))

(defn component? [x]
  (and x (.-dumdom x)))
