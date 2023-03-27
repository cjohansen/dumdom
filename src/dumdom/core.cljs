(ns dumdom.core
  (:require [dumdom.component :as component]
            [dumdom.dom :as d]
            [dumdom.element :as e]
            [dumdom.string :as string]
            [snabbdom :as snabbdom])
  (:require-macros [dumdom.core]))

(def ^:private current-nodes
  "A mapping from root DOM nodes to currently rendered virtual DOM trees. Used to
  reconcile (render component dom-node) to (patch old-vdom new-vdom)"
  (atom {}))

(def ^:private element-id
  "A counter used to assign unique ids to root elements"
  (atom -1))

(def patch
  "The snabbdom patch function used by render"
  (snabbdom/init #js [snabbdom/eventListenersModule
                      snabbdom/attributesModule
                      snabbdom/propsModule
                      snabbdom/styleModule
                      snabbdom/datasetModule]))

(defn set-root-id [element]
  (set! (.. element -dataset -dumdomId) (swap! element-id inc)))

(defn root-node [element]
  (@current-nodes (.. element -dataset -dumdomId)))

(defn register-vnode [element-id vnode]
  (swap! current-nodes assoc element-id vnode))

(defn unregister-vnode [element-id]
  (swap! current-nodes dissoc element-id))

(defn- init-node!
  "Snabbdom will replace the element provided as the original target for patch.
  When rendering into a new DOM node, we therefore create an intermediate in it
  and use that as Snabbdom's root, to avoid destroying the provided root node."
  [element]
  (set! (.-innerHTML element) "<div></div>")
  (set-root-id element)
  (.-firstElementChild element))

(defn purge! []
  (reset! current-nodes {}))

(defn- create-vdom [component element-id {:keys [handle-event]}]
  ;; The handle event function must be bound dynamically here
  ;; because the data structure containing event data to pass to
  ;; it was already built before render was called. This binding
  ;; avoids having to walk the entire vdom structure before
  ;; passing it to snabbdom.
  (when-let [component (e/inflate-hiccup component)]
    (binding [e/*handle-event* (or handle-event e/*handle-event*)]
      (some-> (component [element-id] {}) clj->js))))

(defn set-event-handler! [f]
  (when (and f (not (ifn? f)))
    (throw (ex-info "Event handler must be a function" {:f f})))
  (set! e/*handle-event* f))

(defn dispatch-event-data
  "Dispatch"
  [e data]
  (if e/*handle-event*
    (e/*handle-event* e data)
    (throw (js/Error. "Cannot dispatch custom event data without a global event handler. Call dumdom.core/set-event-handler!"))))

(defn render
  "Render the virtual DOM node created by the component into the specified DOM
  element, and mount it for fast future re-renders."
  [component element & [opt]]
  (when (and (:handle-event opt) (not (ifn? (:handle-event opt))))
    (throw (ex-info "Called dumdom.core/render with a handle-event that is not a function" opt)))
  (let [current-node (or (root-node element) (init-node! element))
        element-id (.. element -dataset -dumdomId)]
    (if-let [vnode (create-vdom component element-id opt)]
      (do
        ;; If the root node does not have a key, Snabbdom will consider it the
        ;; same node as the node it is rendered into if they have the same tag
        ;; name (typically root nodes are divs, and typically they are rendered
        ;; into divs). When this happens, Snabbdom fires the update hook rather
        ;; than the insert hook, which breaks dumdom's contract. Forcing the
        ;; root node to have a key circumvents this problem and ensures the root
        ;; node has its insert hooks fired on initial render.
        (when-not (.. vnode -key)
          (set! (.. vnode -key) "root-node"))
        (patch current-node vnode)
        (register-vnode element-id vnode))
      (do
        (set! (.-innerHTML element) "")
        (unregister-vnode element-id)))
    (when component/*render-eagerly?*
      (reset! component/eager-render-required? false))))

(defn render-once
  "Like render, but without mounting the element for future updates. This should
  only be used when you don't expect to re-render the component into the same
  element. Subsequent calls to render into the same element will always cause a
  full rebuild of the DOM. This function does not acumulate state."
  [component element & [opt]]
  (when (and (:handle-event opt) (not (ifn? (:handle-event opt))))
    (throw (ex-info "Called dumdom.core/render-once with a handle-event that is not a function" opt)))
  (let [current-node (init-node! element)
        element-id (.. element -dataset -dumdomId)]
    (when-let [vnode (create-vdom component element-id opt)]
      (patch current-node vnode))
    (when component/*render-eagerly?*
      (reset! component/eager-render-required? false))))

(defn unmount
  "Unmount an element previously mounted by dumdom.core/render"
  [element]
  (-> element .-dataset .-dumdomId unregister-vnode))

(def component component/component)
(def component? component/component?)
(def render-string string/render)

(defn TransitionGroup [opt children]
  (component/TransitionGroup d/el opt children))

(defn CSSTransitionGroup [opt children]
  (component/CSSTransitionGroup d/el opt children))
