(ns dumdom.core
  (:require [cljs.core.async :refer [timeout]]
            [cljsjs.snabbdom]
            [dumdom.dom :as d])
  (:require-macros [dumdom.core]))

(def ^:private current-nodes
  "A mapping from root DOM nodes to currently rendered virtual DOM trees. Used to
  reconcile (render component dom-node) to (patch old-vdom new-vdom)"
  (atom {}))

(def ^:private element-id
  "A counter used to assign unique ids to root elements"
  (atom -1))

(def ^:private patch
  "The snabbdom patch function used by render"
  (js/snabbdom.init (clj->js [(.-eventlisteners js/snabbdom)
                              (.-attributes js/snabbdom)
                              (.-style js/snabbdom)])))

(defn- init-node!
  "Snabbdom will replace the element provided as the original target for patch.
  When rendering into a new DOM node, we therefore create an intermediate in it
  and use that as Snabbdom's root, to avoid destroying the provided root node."
  [element]
  (set! (.-innerHTML element) "<div></div>")
  (set! (.. element -dataset -dumdomId) (swap! element-id inc))
  (.-firstChild element))

(defn purge! []
  (reset! current-nodes {}))

(defn render
  "Render the virtual DOM node created by the compoennt into the specified DOM
  element"
  [component element]
  (let [current-node (or (@current-nodes (.. element -dataset -dumdomId)) (init-node! element))
        element-id (.. element -dataset -dumdomId)
        vnode (component [element-id] 0)]
    (patch current-node vnode)
    (swap! current-nodes assoc element-id vnode)))

(defn- should-component-update? [component-state data]
  (or (not (contains? component-state :data))
      (not= (:data component-state) data)))

(defn- setup-mount-hook [rendered {:keys [on-mount on-render will-appear did-appear will-enter did-enter]} data args animation]
  (when (or on-mount on-render will-enter will-appear)
    (let [insert-hook (.. rendered -data -hook -insert)]
      (set! (.. rendered -data -hook -insert)
            (fn [vnode]
              (when insert-hook (insert-hook vnode))
              (when on-mount (apply on-mount (.-elm vnode) data args))
              (when on-render (apply on-render (.-elm vnode) data args))
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

(defn- setup-update-hook [rendered {:keys [on-update on-render]} data args]
  (when (or on-update on-render)
    (set! (.. rendered -data -hook -update)
          (fn [old-vnode vnode]
            (when on-update (apply on-update (.-elm vnode) data args))
            (when on-render (apply on-render (.-elm vnode) data args))))))

(defn- setup-unmount-hook [rendered component data args animation on-destroy]
  (set! (.. rendered -data -hook -destroy)
        (fn [vnode]
          (when-let [on-unmount (:on-unmount component)]
            (apply on-unmount (.-elm vnode) data args))
          (on-destroy)))
  (when-let [will-leave (:will-leave component)]
    (set! (.. rendered -data -hook -remove)
          (fn [vnode snabbdom-callback]
            (let [callback (fn []
                             (when-let [did-leave (:did-leave component)]
                               (apply did-leave (.-elm vnode) data args))
                             (snabbdom-callback))]
              (if (:ready? @animation)
                (apply will-leave (.-elm vnode) callback data args)
                (add-watch animation :leave
                  (fn [k r o n]
                    (when (:ready? n)
                      (remove-watch animation :leave)
                      (apply will-leave (.-elm vnode) callback data args))))))))))

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
   (let [instances (atom {})]
     (fn [data & args]
       (let [comp-fn
             (fn [path k]
               (let [key (when-let [keyfn (:keyfn opt)] (keyfn data))
                     fullpath (conj path (or key k))
                     instance (or (@instances fullpath)
                                  {:render render
                                   :opt opt
                                   :path fullpath})
                     animation (atom {:ready? true})]
                 (if (should-component-update? instance data)
                   (when-let [rendered (when-let [renderer (apply render data args)]
                                         (renderer fullpath 0))]
                     (when key
                       (set! (.-key rendered) key))
                     (when-let [will-enter (:will-enter opt)]
                       (set! (.-willEnter rendered) #(swap! animation assoc :will-enter will-enter)))
                     (when-let [will-appear (:will-appear opt)]
                       (swap! animation assoc :will-appear will-appear)
                       (set! (.-willAppear rendered) #(swap! animation dissoc :will-appear)))
                     (setup-mount-hook rendered opt data args animation)
                     (setup-update-hook rendered opt data args)
                     (setup-unmount-hook rendered opt data args animation #(swap! instances dissoc fullpath))
                     (swap! instances assoc fullpath (assoc instance
                                                            :vdom rendered
                                                            :data data))
                     rendered)
                   (:vdom instance))))]
         (set! (.-dumdom comp-fn) true)
         comp-fn)))))

(defn TransitionGroup [opt children]
  (if (ifn? (:component opt))
    ((:component opt) children)
    (apply d/el (or (:component opt) "span") opt children)))

(defn- add-class [el class-name]
  (.add (.-classList el) class-name))

(defn- complete-transition [node timeout callback]
  (if timeout
    (js/setTimeout callback timeout)
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
          (js/setTimeout #(.add (.-classList node) active-class) 0))
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

(defn CSSTransitionGroup [opt children]
  (TransitionGroup opt (map #(TransitioningElement (assoc opt :child %)) children)))

(defn component? [x]
  (and x (.-dumdom x)))
