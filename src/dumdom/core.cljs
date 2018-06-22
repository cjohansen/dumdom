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

(defn render
  "Render the virtual DOM node created by the compoennt into the specified DOM
  element"
  [component element]
  (let [current-node (or (@current-nodes element) (init-node! element))
        vnode (component [] 0)]
    (patch current-node vnode)
    (swap! current-nodes assoc element vnode)))

(defn- should-component-update? [component-state data]
  (not= (:data component-state) data))

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
  render function."
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
                             :path fullpath})]
           (if (should-component-update? instance data)
             (let [rendered ((apply render data args) fullpath 0)]
               (when key
                 (set! (.-key rendered) key))
               (when-let [on-mount (or (:on-mount opt) (:on-render opt))]
                 (let [insert-hook (.. rendered -data -hook -insert)]
                   (set! (.. rendered -data -hook -insert)
                         (fn [vnode]
                           (when insert-hook (insert-hook vnode))
                           (apply on-mount (.-elm vnode) data args)))))
               (when-let [on-update (or (:on-update opt) (:on-render opt))]
                 (set! (.. rendered -data -hook -update)
                       (fn [old-vnode vnode]
                         (apply on-update (.-elm vnode) data args))))
               (swap! instances assoc fullpath (assoc instance
                                                      :vdom rendered
                                                      :data data))
               rendered)
             (:vdom instance))))))))
