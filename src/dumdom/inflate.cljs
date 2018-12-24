(ns dumdom.inflate
  (:require [dumdom.core :as dumdom]
            [snabbdom]))

(defn- init-node! [element]
  (dumdom/set-root-id element)
  (.-firstElementChild element))

(defn render [component element]
  (let [current-node (or (dumdom/root-node element) (init-node! element))
        element-id (.. element -dataset -dumdomId)
        vnode (component [element-id] 0)]
    (dumdom/patch (js/snabbdom.tovnode current-node) vnode)
    (dumdom/register-vnode element-id vnode)))
