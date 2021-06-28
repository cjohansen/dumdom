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

(defn render
  "Render the virtual DOM node created by the component into the specified DOM
  element, and mount it for fast future re-renders."
  [component element]
  (let [current-node (or (root-node element) (init-node! element))
        element-id (.. element -dataset -dumdomId)
        component (e/inflate-hiccup component)
        vnode (component [element-id] 0)]
    (if vnode
      (let [vnode (clj->js vnode)]
        ;; If the root node does not have a key, Snabbdom will consider it the same
        ;; node as the node it is rendered into if they have the same tag name
        ;; (typically root nodes are divs, and typically they are rendered into
        ;; divs). When this happens, Snabbdom fires the update hook rather than the
        ;; insert hook, which breaks dumdom's contract. Forcing the root node to
        ;; have a key circumvents this problem and ensures the root node has its
        ;; insert hooks fired on initial render.
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
  [component element]
  (let [current-node (init-node! element)
        component (e/inflate-hiccup component)
        vnode (component [element-id] 0)]
    (when-let [vnode (some-> (component [element-id] 0) clj->js)]
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
