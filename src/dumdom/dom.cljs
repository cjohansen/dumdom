(ns dumdom.dom
  (:require [clojure.set :as set]
            [clojure.string :as str]
            [snabbdom :as sd])
  (:refer-clojure :exclude [time map meta mask])
  (:require-macros [dumdom.dom :as dm]))

(defn- process-classes [attrs]
  (update-in attrs [:className]
             #(if (map? %)
                %
                (->> (if (string? %) (str/split % #" ") %)
                     (filter identity)
                     (mapv (fn [cn] [cn true]))
                     (into {})))))

(defn- event-entry [attrs k]
  [(.toLowerCase (.slice (name k) 2)) (attrs k)])

(defn- prep-props [attrs]
  (let [event-keys (filter #(and (str/starts-with? (name %) "on") (ifn? (attrs %))) (keys attrs))]
    {:props (apply dissoc attrs :style :enter-style :remove-style :destroy-style event-keys)
     :style (merge (:style attrs)
                   (when-let [enter (:enter-style attrs)]
                     {:delayed enter})
                   (when-let [remove (:remove-style attrs)]
                     {:remove remove})
                   (when-let [destroy (:destroy-style attrs)]
                     {:destroy destroy}))
     :on (->> event-keys
              (mapv #(event-entry attrs %))
              (into {}))
     :key (:key attrs)
     :hook (merge
            {}
            (when-let [callback (:ref attrs)]
              {:insert #(callback (.-elm %))
               :destroy #(callback nil)}))}))

(defn el
  "Creates a virtual DOM element component of the specified type with attributes
  and optional children. Returns a function that renders the virtual DOM. This
  function expects a vector path and a key that addresses the component."
  [type attrs & children]
  (fn [path k]
    (let [fullpath (conj path k)]
      (sd/h type
            (clj->js (prep-props attrs))
            (->> children
                 (mapcat #(if (coll? %) % [%]))
                 (map-indexed #(if (fn? %2)
                                 (%2 fullpath %1)
                                 %2))
                 clj->js)))))

(dm/define-tags
  a abbr address area article aside audio b base bdi bdo big blockquote body br
  button canvas caption cite code col colgroup data datalist dd del details dfn
  div dl dt em embed fieldset figcaption figure footer form h1 h2 h3 h4 h5 h6
  head header hr html i iframe img input ins kbd keygen label legend li link main
  map mark menu menuitem meta meter nav noscript object ol optgroup option output
  p param pre progress q rp rt ruby s samp script section select small source
  span strong style sub summary sup table tbody td textarea tfoot th thead time
  title tr track u ul var video wbr circle clipPath defs ellipse g image line
  linearGradient mask path pattern polygon polyline radialGradient rect stop svg
  text tspan)
