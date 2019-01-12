(ns dumdom.inflate
  (:require [cljs.reader :as reader]
            [dumdom.core :as dumdom]
            [snabbdom]))

(defn- init-node! [element]
  (dumdom/set-root-id element)
  (.-firstElementChild element))

(extend-type js/NodeList
  ISeqable
  (-seq [array] (array-seq array 0)))

(defn- vnode [sel data children text elm]
  (cond-> {}
    (:key data) (assoc :key (reader/read-string (:key data)))
    sel (assoc :sel sel)
    data (assoc :data data)
    children (assoc :children children)
    text (assoc :text text)
    elm (assoc :elm elm)))

(defn- props [node]
  (let [attributes (.-attributes node)
        len (.-length attributes)]
    (loop [props {}
           i 0]
      (if (< i len)
        (let [attr-name (.-nodeName (aget attributes i))
              attr-val (.-nodeValue (aget attributes i))]
          (recur
           (if (= attr-name "data-dumdom-key")
             (assoc props :key (reader/read-string attr-val))
             (assoc-in props [:attrs (keyword attr-name)] attr-val))
           (inc i)))
        props))))

(declare to-vnode)

(defn- element-vnode [node]
  (vnode (.toLowerCase (.-tagName node)) (props node) (map to-vnode (.-childNodes node)) nil node))

(defn- to-vnode [node]
  (cond
    (= 1 (.-nodeType node)) (element-vnode node)
    (= 3 (.-nodeType node)) (vnode nil nil nil (.-textContent node) node)
    (= 8 (.-nodeType node)) (vnode "!" {} [] (.-textContent node) node)
    :default (vnode "" {} [] nil node)))

(defn render [component element]
  (let [current-node (or (dumdom/root-node element) (init-node! element))
        element-id (.. element -dataset -dumdomId)
        vnode (component [element-id] 0)]
    (dumdom/patch (clj->js (to-vnode current-node)) vnode)
    (dumdom/register-vnode element-id vnode)))
