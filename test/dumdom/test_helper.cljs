(ns dumdom.test-helper
  (:require [dumdom.core :as dumdom]
            [dumdom.element :as e]))

(defn render
  ([el]
   (render el [] {}))
  ([el path kmap]
   (el path kmap)))

(defn render-str [& args]
  (apply dumdom/render-string args))

(defn to-vdom [sexp]
  (let [f (e/inflate-hiccup sexp)]
    (f [0] {})))

(defn walk-vdom [f vdom]
  (->> vdom
       (map (fn [[k v]]
              [k
               (cond
                 (map? v) (walk-vdom f v)
                 (coll? v) (map #(if (map? %)
                                   (walk-vdom f %)
                                   %) v)
                 :default v)]))
       f
       (into {})))

(defn strip-vdom [vdom]
  (walk-vdom
   #(remove (fn [[k v]]
              (or (nil? v)
                  (and (coll? v) (empty? v)))) %)
   vdom))

(defn strip-vdom-hooks [vdom]
  (walk-vdom #(remove (fn [[k v]] (= k :hook)) %) vdom))

(defn to-minimal-vdom [sexp]
  (->> (to-vdom sexp)
       strip-vdom-hooks
       strip-vdom))

(defn summarize-vdom [vdom]
  (if (:sel vdom)
    (into [(:sel vdom)
           (select-keys vdom [:key])]
          (map summarize-vdom (:children vdom)))
    (:text vdom)))
