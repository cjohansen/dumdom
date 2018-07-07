(ns dumdom.test-helper
  (:require [clojure.string :as str]))

(def attr-names
  {"className" "class"})

(defn- attrs [vnode]
  (->> (js->clj (-> vnode .-data .-props))
       (map (fn [[k v]] (str " " (or (attr-names k) k) "=\"" v "\"")))
       (str/join "")))

(defn dom-str [vnode]
  (cond
    (nil? vnode) ""
    (nil? (.-sel vnode)) (.-text vnode)
    :default (str "<" (.-sel vnode) (attrs vnode) ">"
                  (str/join "" (map dom-str (.-children vnode)))
                  "</" (.-sel vnode) ">")))

(defn render
  ([el]
   (render el [] 0))
  ([el path k]
   (el path k)))

(defn render-str [& args]
  (dom-str (apply render args)))
