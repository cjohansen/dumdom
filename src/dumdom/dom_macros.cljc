(ns dumdom.dom-macros)

(defn- tag-definition
  "Return a form to define a wrapper function for a dumdom tag component"
  [tag]
  `(defn ~tag [& args#]
     (apply dumdom.dom/el ~(name tag) args#)))

(defmacro define-tags
  "Macros which expands to a do block that defines top-level constructor functions
  for each supported HTML and SVG tag, using dumdom.dom/el"
  [& tags]
  `(do (do ~@(clojure.core/map tag-definition tags))
       (def ~'defined-tags
         ~(zipmap tags
                  (map (comp symbol name) tags)))))
