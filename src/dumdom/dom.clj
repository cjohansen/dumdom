(ns dumdom.dom
  (:require [dumdom.dom-macros :as dm]
            [dumdom.element :as element])
  (:refer-clojure :exclude [time map meta mask]))

(defn render [type attrs children]
  {:tag-name type
   :attributes (merge (:attrs attrs)
                      (:props attrs)
                      (->> (:dataset attrs)
                           (clojure.core/map (fn [[k v]]
                                               [(keyword (str "data-" (name k))) v]))
                           (into {})))
   :style (:style attrs)
   :key (:key attrs)
   :children children})

(defn el [type attrs & children]
  (apply element/create render type attrs children))

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
