(ns dumdom.core
  (:require [dumdom.dom :as d]
            [snabbdom :as sd]
            ["snabbdom/modules/eventlisteners" :as events]
            ["snabbdom/modules/props" :as props]
            ["snabbdom/modules/style" :as style]))

(def ^:private current-nodes
  "A mapping from root DOM nodes to currently rendered virtual DOM trees. Used to
  reconcile (render component dom-node) to (patch old-vdom new-vdom)"
  (atom {}))

(def ^:private patch
  "The snabbdom patch function used by render"
  (sd/init (clj->js [(.-default events)
                     (.-default props)
                     (.-default style)])))

(defn- init-node!
  "Snabbdom will replace the element provided as the original target for patch.
  When rendering into a new DOM node, we therefore create an intermediate in it
  and use that as Snabbdom's root, to avoid destroying the provided root node."
  [element]
  (set! (.-innerHTML element) "<div></div>")
  (.-firstChild element))

(defn purge! []
  (reset! current-nodes {}))

(defn render
  "Render the virtual DOM node created by the compoennt into the specified DOM
  element"
  [component element]
  (let [current-node (or (@current-nodes element) (init-node! element))
        vnode (component [(count @current-nodes)] 0)]
    (patch current-node vnode)
    (swap! current-nodes assoc element vnode)))

(defn- should-component-update? [component-state data]
  (not= (:data component-state) data))

(defn- setup-mount-hook [rendered {:keys [on-mount on-render will-enter did-enter]} data args animation]
  (when (or on-mount on-render will-enter)
    (let [insert-hook (.. rendered -data -hook -insert)]
      (set! (.. rendered -data -hook -insert)
            (fn [vnode]
              (when insert-hook (insert-hook vnode))
              (when on-mount (apply on-mount (.-elm vnode) data args))
              (when on-render (apply on-render (.-elm vnode) data args))
              (let [{:keys [will-enter]} @animation]
                (when will-enter
                  (swap! animation assoc :ready? false)
                  (apply will-enter
                         (.-elm vnode)
                         (fn []
                           (swap! animation assoc :ready? true)
                           (when did-enter (apply did-enter (.-elm vnode) data args)))
                         data
                         args))))))))

(defn- setup-update-hook [rendered {:keys [on-update on-render]} data args]
  (when (or on-update on-render)
    (set! (.. rendered -data -hook -update)
          (fn [old-vnode vnode]
            (when on-update (apply on-update (.-elm vnode) data args))
            (when on-render (apply on-render (.-elm vnode) data args))))))

(defn- setup-unmount-hook [rendered component data args animation]
  (when-let [on-unmount (:on-unmount component)]
    (set! (.. rendered -data -hook -destroy)
          (fn [vnode]
            (apply on-unmount (.-elm vnode) data args))))
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

  :on-mount - A function which will be invoked once, immediately after initial
  rendering. It is passed the rendered DOM node, and all arguments passed to the
  render function.

  :on-update - A function which will be invoked immediately after an updated is
  flushed to the DOM, but not on the initial render. It is passed the underlying
  DOM node, the value, and any constant arguments passed to the render function.

  :on-render - A function which will be invoked immediately after an updated is
  flushed to the DOM, but not on the initial render. It is passed the underlying
  DOM node, the value, the old value, and any constant arguments passed to the
  render function.

  :on-unmount - A function which will be invoked immediately before the
  component is unmounted from the DOM. It is passed the underlying DOM node, the
  most recent value and the most recent constant args passed to the render fn."
  ([render] (component render {}))
  ([render opt]
   (let [instances (atom {})]
     (fn [data & args]
       (fn [path k]
         (let [key (when-let [keyfn (:keyfn opt)] (keyfn data))
               fullpath (conj path (or key k))
               instance (or (@instances fullpath)
                            {:render render
                             :opt opt
                             :path fullpath})
               animation (atom {:ready? true})]
           (if (should-component-update? instance data)
             (let [rendered ((apply render data args) fullpath 0)]
               (when key
                 (set! (.-key rendered) key))
               (when-let [will-enter (:will-enter opt)]
                 (set! (.-willEnter rendered) #(swap! animation assoc :will-enter will-enter)))
               (setup-mount-hook rendered opt data args animation)
               (setup-update-hook rendered opt data args)
               (setup-unmount-hook rendered opt data args animation)
               (swap! instances assoc fullpath (assoc instance
                                                      :vdom rendered
                                                      :data data))
               rendered)
             (:vdom instance))))))))

(defn TransitionGroup [opt children]
  (apply d/el (or (:component opt) "span") opt children))

(defn- add-class [el class-name]
  (.add (.-classList el) class-name))

(def TransitioningElement
  (component
   (fn [{:keys [child]}]
     child)
   {:will-enter (fn [node callback {:keys [transitionName transitionEnterTimeout]}]
                  (add-class node (str transitionName "-enter"))
                  (if transitionEnterTimeout
                    (js/setTimeout callback transitionEnterTimeout)
                    (let [callback-fn (atom nil)
                          f (fn []
                              (callback)
                              (.removeEventListener node "transitionend" @callback-fn))]
                      (reset! callback-fn f)
                      (.addEventListener node "transitionend" f)))
                  (js/setTimeout #(add-class node (str transitionName "-enter-active")) 0))
    :did-enter (fn [node {:keys [transitionName]}]
                 (.remove (.-classList node) (str transitionName "-enter"))
                 (.remove (.-classList node) (str transitionName "-enter-active")))}))

(defn CSSTransitionGroup [opt children]
  (TransitionGroup opt (map #(TransitioningElement (assoc opt :child %)) children)))
