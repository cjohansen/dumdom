(ns dumdom.string
  (:require [clojure.string :as str]
            [dumdom.element :as e]))

(defn- tag-name [node]
  (:sel node))

(defn- children [node]
  (:children node))

(defn- attributes [node]
  (merge (some-> node :data :attrs)
         (some-> node :data :props)
         (some->> node :data :dataset
                  (map (fn [[k v]] [(str "data-" (name k)) v]))
                  (into {}))
         (some->> node :data :on
                  (filter (comp string? second))
                  (map (fn [[k v]]
                         [(str "on" (str/capitalize k)) v]))
                  (into {}))))

(defn- el-key [node]
  (:key node))

(defn- style [node]
  (-> node :data :style))

(defn- text-node? [vnode]
  (nil? (:sel vnode)))

(defn- comment-node? [vnode]
  (= "!" (tag-name vnode)))

(defn- text [vnode]
  (:text vnode))

(defn- kebab-case [s]
  (str/lower-case (str/replace s #"([a-z])([A-Z])" "$1-$2")))

(defn- render-styles [styles]
  (if (string? styles)
    styles
    (->> styles
         (remove (comp nil? second))
         (map (fn [[k v]] (str (kebab-case (name k)) ": " v)))
         (str/join "; "))))

(defn- escape [s]
  (-> s
      (str/replace #"&(?!([a-z]+|#\d+);)" "&amp;")
      (str/replace #"\"" "&quot;")))

(defn- attrs [vnode]
  (let [k (el-key vnode)
        attributes (cond-> (dissoc (attributes vnode) :innerHTML)
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
    (or (nil? vnode)
        (comment-node? vnode)) ""
    (text-node? vnode) (text vnode)
    :default (str "<" (tag-name vnode) (attrs vnode) ">"
                  (let [attrs (attributes vnode)]
                    (if (contains? attrs :innerHTML)
                      (:innerHTML attrs)
                      (str/join "" (map dom-str (children vnode)))))
                  (closing-tag (tag-name vnode)))))

(defn render [component & [path kmap]]
  (let [component (e/inflate-hiccup component)]
    (dom-str (component (or path []) (or kmap {})))))
