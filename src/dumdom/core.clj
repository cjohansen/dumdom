(ns dumdom.core
  (:require [dumdom.component :as component]
            [dumdom.string :as string]))

(defn- extract-docstr
  [[docstr? & forms]]
  (if (string? docstr?)
    [docstr? forms]
    ["" (cons docstr? forms)]))

(defn- extract-opts
  ([forms] (extract-opts forms {}))
  ([[k v & forms] opts]
    (if (keyword? k)
      (extract-opts forms (assoc opts k v))
      [opts (concat [k v] forms)])))

(def component component/component)

(defmacro defcomponent
  "Creates a component with the given name, a docstring (optional), any number of
  option->value pairs (optional), an argument vector and any number of forms
  body, which will be used as the rendering function to
  dumdom.core/component.

  For example:

    (defcomponent Widget
      \"A Widget\"
      :on-mount #(...)
      :on-render #(...)
      [value constant-value]
      (some-child-components))

  Is shorthand for:

    (def Widget (dumdom.core/component
                  (fn [value constant-value] (some-child-components))
                  {:on-mount #(...)
                   :on-render #(...)}))"
  [name & forms]
  (let [[docstr forms] (extract-docstr forms)
        [options forms] (extract-opts forms)
        [argvec & body] forms
        options (merge {:name (str (:name (:ns &env)) "/" name)} options)]
    `(def ~name ~docstr (dumdom.core/component (fn ~argvec ~@body) ~options))))

(def render-string string/render)
