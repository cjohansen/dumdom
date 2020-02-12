(ns dumdom.string
  (:require [clojure.string :as str]
            [dumdom.element :as e]
            [dumdom.dom :as d]))

(defn- tag-name [node]
  #?(:cljs (.-sel node)
     :clj (:tag-name node)))

(defn- children [node]
  #?(:cljs (.-children node)
     :clj (:children node)))

(defn- attributes [node]
  #?(:cljs (merge (js->clj (.. node -data -attrs)) (js->clj (.. node -data -props)))
     :clj (:attributes node)))

(defn- el-key [node]
  #?(:cljs (.. node -key)
     :clj (:key node)))

(defn- style [node]
  #?(:cljs (js->clj (.. node -data -style))
     :clj (:style node)))

(defn- text-node? [vnode]
  #?(:cljs (nil? (.-sel vnode))
     :clj (not (map? vnode))))

(defn- text [vnode]
  #?(:cljs (.-text vnode)
     :clj vnode))

(defn- kebab-case [s]
  (str/lower-case (str/replace s #"([a-z])([A-Z])" "$1-$2")))

(defn- render-styles [styles]
  (str/join "; " (map (fn [[k v]] (str (kebab-case (name k)) ": " v)) styles)))

(defn- escape [s]
  (-> s
      (str/replace #"&(?!([a-z]+|#\d+);)" "&amp;")
      (str/replace #"\"" "&quot;")))

(defn- attrs [vnode]
  (let [k (el-key vnode)
        attributes (cond-> (dissoc (attributes vnode) :innerHTML "innerHTML")
                     k (assoc :data-dumdom-key (escape (pr-str k))))
        style (style vnode)]
    (->> (merge attributes
                (when style
                  {:style (render-styles style)}))
         (map (fn [[k v]] (str " " (name k) "=\"" v "\"")))
         (str/join ""))))

(def ^:private self-closing
  #{"area" "base" "br" "col" "embed" "hr" "img" "input"
    "link" "meta" "param" "source" "track" "wbr"})

(defn- closing-tag [tag-name]
  (when-not (self-closing tag-name)
    (str "</" tag-name ">")))

(defn- dom-str [vnode]
  (cond
    (nil? vnode) ""
    (text-node? vnode) (text vnode)
    :default (str "<" (tag-name vnode) (attrs vnode) ">"
                  (let [attrs (attributes vnode)]
                    (or (get attrs :innerHTML)
                        (get attrs "innerHTML")
                        (str/join "" (map dom-str (children vnode)))))
                  (closing-tag (tag-name vnode)))))

(defn render [component & [path k]]
  (let [component (e/inflate-hiccup d/render component)]
    (dom-str (component (or path []) (or k 0)))))
