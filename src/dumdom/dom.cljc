(ns dumdom.dom
  (:require [dumdom.element :as element]
            #?(:clj [dumdom.dom-macros :as dm]))
  (:refer-clojure :exclude [time map meta mask])
  #?(:cljs (:require-macros [dumdom.dom-macros :as dm])))

(defn el
  "Creates a virtual DOM element component of the specified type with attributes
  and optional children. Returns a function that renders the virtual DOM. This
  function expects a vector path and a key that addresses the component."
  [type attrs & children]
  (let [el-fn (apply element/create type attrs (element/flatten-seqs children))]
    #?(:cljs (set! (.-dumdom el-fn) true))
    el-fn))

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
