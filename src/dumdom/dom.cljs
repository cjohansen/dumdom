(ns dumdom.dom
  (:require [cljsjs.snabbdom]
            [clojure.set :as set]
            [clojure.string :as str])
  (:refer-clojure :exclude [time map meta mask])
  (:require-macros [dumdom.dom :as dm]))

(defn- event-entry [attrs k]
  [(.toLowerCase (.slice (name k) 2)) (attrs k)])

(defn- pixelize-number [n]
  (if (number? n)
    (str n "px")
    n))

(defn- pixelize [styles]
  (reduce #(if (%2 %1) (update %1 %2 pixelize-number) %1)
          styles
          [:width :height :padding :margin :top :left :right :bottom]))

(def ^:private attr-mappings
  {:acceptCharset :accept-charset
   :accessKey :accesskey
   :autoCapitalize :autocapitalize
   :autoComplete :autocomplete
   :autoFocus :autofocus
   :autoPlay :autoplay
   :bgColor :bgcolor
   :className :class
   :codeBase :codebase
   :colSpan :colspan
   :contentEditable :contenteditable
   :contextMenu :contextmenu
   :crossOrigin :crossorigin
   :dateTime :datetime
   :dirName :dirname
   :dropZone :dropzone
   :encType :enctype
   :htmlFor :for
   :formAction :formaction
   :hrefLang :hreflang
   :httpEquiv :http-equiv
   :isMap :ismap
   :itemProp :itemprop
   :keyType :keytype
   :maxLength :maxlength
   :minLength :minlength
   :noValidate :novalidate
   :placeHolder :placeholder
   :preLoad :preload
   :radioGroup :radiogroup
   :readOnly :readonly
   :rowSpan :rowspan
   :spellCheck :spellcheck
   :srcDoc :srcdoc
   :srcLang :srclang
   :srcSet :srcset
   :tabIndex :tabindex
   :useMap :usemap
   :accentHeight :accent-height
   :alignmentBaseline :alignment-baseline
   :arabicForm :arabic-form
   :baselineShift :baseline-shift
   :capHeight :cap-height
   :clipPath :clip-path
   :clipRule :clip-rule
   :colorInterpolation :color-interpolation
   :colorInterpolationFilters :color-interpolation-filters
   :colorProfile :color-profile
   :colorRendering :color-rendering
   :dominantBaseline :dominant-baseline
   :enableBackground :enable-background
   :fillOpacity :fill-opacity
   :fillRule :fill-rule
   :floodColor :flood-color
   :floodOpacity :flood-opacity
   :fontFamily :font-family
   :fontSize :font-size
   :fontSizeAdjust :font-size-adjust
   :fontStretch :font-stretch
   :fontStyle :font-style
   :fontVariant :font-variant
   :fontWeight :font-weight
   :glyphName :glyph-name
   :glyphOrientationHorizontal :glyph-orientation-horizontal
   :glyphOrientationVertical :glyph-orientation-vertical
   :horizAdvX :horiz-adv-x
   :horizOriginX :horiz-origin-x
   :imageRendering :image-rendering
   :letterSpacing :letter-spacing
   :lightingColor :lighting-color
   :markerEnd :marker-end
   :markerMid :marker-mid
   :markerStart :marker-start
   :overlinePosition :overline-position
   :overlineThickness :overline-thickness
   :panose1 :panose-1
   :paintOrder :paint-order
   :pointerEvents :pointer-events
   :renderingIntent :rendering-intent
   :shapeRendering :shape-rendering
   :stopColor :stop-color
   :stopOpacity :stop-opacity
   :strikethroughPosition :strikethrough-position
   :strikethroughThickness :strikethrough-thickness
   :strokeDasharray :stroke-dasharray
   :strokeDashoffset :stroke-dashoffset
   :strokeLinecap :stroke-linecap
   :strokeLinejoin :stroke-linejoin
   :strokeMiterlimit :stroke-miterlimit
   :strokeOpacity :stroke-opacity
   :strokeWidth :stroke-width
   :textAnchor :text-anchor
   :textDecoration :text-decoration
   :textRendering :text-rendering
   :underlinePosition :underline-position
   :underlineThickness :underline-thickness
   :unicodeBidi :unicode-bidi
   :unicodeRange :unicode-range
   :unitsPerEm :units-per-em
   :vAlphabetic :v-alphabetic
   :vHanging :v-hanging
   :vIdeographic :v-ideographic
   :vMathematical :v-mathematical
   :vectorEffect :vector-effect
   :vertAdvY :vert-adv-y
   :vertOriginX :vert-origin-x
   :vertOriginY :vert-origin-y
   :wordSpacing :word-spacing
   :writingMode :writing-mode
   :xHeight :x-height
   :xlinkActuate :xlink:actuate
   :xlinkArcrole :xlink:arcrole
   :xlinkHref :xlink:href
   :xlinkRole :xlink:role
   :xlinkShow :xlink:show
   :xlinkTitle :xlink:title
   :xlinkType :xlink:type
   :xmlBase :xml:base
   :xmlLang :xml:lang
   :xmlSpace :xml:space})

(defn- prep-attrs [attrs]
  (let [event-keys (filter #(and (str/starts-with? (name %) "on") (ifn? (attrs %))) (keys attrs))
        attrs (set/rename-keys attrs (select-keys attr-mappings (keys attrs)))]
    {:attrs (apply dissoc attrs :style :enter-style :remove-style :destroy-style :component event-keys)
     :style (merge (pixelize (:style attrs))
                   (when-let [enter (:enter-style attrs)]
                     {:delayed (pixelize enter)})
                   (when-let [remove (:remove-style attrs)]
                     {:remove (pixelize remove)})
                   (when-let [destroy (:destroy-style attrs)]
                     {:destroy (pixelize destroy)}))
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
  (let [el-fn
        (fn [path k]
          (let [fullpath (conj path k)]
            (js/snabbdom.h
             type
             (clj->js (-> (prep-attrs attrs)
                          (assoc-in [:hook :update]
                                    (fn [old-vnode new-vnode]
                                      (doseq [node (filter #(some-> % .-willEnter) (.-children new-vnode))]
                                        ((.-willEnter node)))
                                      (doseq [node (filter #(some-> % .-willAppear) (.-children new-vnode))]
                                        ((.-willAppear node)))))))
             (->> children
                  (filter identity)
                  (mapcat #(if (coll? %) % [%]))
                  (map-indexed #(if (fn? %2)
                                  (%2 fullpath %1)
                                  %2))
                  clj->js))))]
    (set! (.-dumdom el-fn) true)
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
