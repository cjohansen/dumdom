(ns dumdom.element
  (:require [clojure.set :as set]
            [clojure.string :as str]))

(defn- event-entry [attrs k]
  [(.toLowerCase (.slice (name k) 2)) (attrs k)])

(defn- pixelize-number [n]
  (if (number? n)
    (str n "px")
    n))

(def ^:private pixelized-styles
  [:width
   :height
   :padding
   :paddingLeft
   :paddingRight
   :paddingTop
   :paddingBottom
   :margin
   :marginLeft
   :marginRight
   :marginTop
   :marginBottom
   :top
   :left
   :right
   :bottom
   :borderWidth
   :borderRadius
   :borderTopWidth
   :borderRightWidth
   :borderBottomWidth
   :borderLeftWidth])

(defn- pixelize [styles]
  (reduce #(if (%2 %1) (update %1 %2 pixelize-number) %1)
          styles
          pixelized-styles))

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
    (cond-> {:attrs (apply dissoc attrs :style :enter-style :remove-style :destroy-style :component :value :key event-keys)
             :props (select-keys attrs [:value])
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
             :hook (merge
                    {}
                    (when-let [callback (:ref attrs)]
                      {:insert #(callback (.-elm %))
                       :destroy #(callback nil)}))}
      (:key attrs) (assoc :key (:key attrs)))))

(declare create)

(defn hiccup? [sexp]
  (and (vector? sexp) (or (keyword? (first sexp)) (fn? (first sexp)))))

(defn inflate-hiccup [el-fn sexp]
  (if-not (hiccup? sexp)
    sexp
    (let [el-type (first sexp)
          args (rest sexp)
          args (if (map? (first args)) args (concat [{}] args))]
      (if (fn? el-type)
        (apply el-type (rest sexp))
        (apply create el-fn (name el-type) args)))))

(defn create [el-fn type attrs & children]
  (fn [path k]
    (let [fullpath (conj path k)]
      (el-fn
       type
       (-> (prep-attrs attrs)
           (assoc-in [:hook :update]
                     (fn [old-vnode new-vnode]
                       (doseq [node (filter #(some-> % .-willEnter) (.-children new-vnode))]
                         ((.-willEnter node)))
                       (doseq [node (filter #(some-> % .-willAppear) (.-children new-vnode))]
                         ((.-willAppear node))))))
       (->> children
            (filter identity)
            (mapcat #(if (seq? %) % [%]))
            (map (partial inflate-hiccup el-fn))
            (map-indexed #(do
                            (if (fn? %2)
                             (%2 fullpath %1)
                             %2))))))))
