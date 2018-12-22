(ns dumdom.test-helper
  (:require [dumdom.core :as dumdom]))

(defn render
  ([el]
   (render el [] 0))
  ([el path k]
   (el path k)))

(defn render-str [& args]
  (apply dumdom/render-string args))
