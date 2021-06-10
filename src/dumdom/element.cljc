(ns dumdom.element
  (:require [clojure.set :as set]
            [clojure.string :as str]))

(defn- event-entry [attrs k]
  [(.toLowerCase (.substring (name k) 2)) (attrs k)])

(defn- camelCase [s]
  (let [[f & rest] (str/split s #"-")]
    (str f (str/join "" (map str/capitalize rest)))))

(defn- camel-key [k]
  (keyword (camelCase (name k))))

(def ^:private skip-pixelize-attrs
  (->>
   [:animation-iteration-count
    :box-flex
    :box-flex-group
    :box-ordinal-group
    :column-count
    :fill-opacity
    :flex
    :flex-grow
    :flex-positive
    :flex-shrink
    :flex-negative
    :flex-order
    :font-weight
    :line-clamp
    :line-height
    :opacity
    :order
    :orphans
    :stop-opacity
    :stroke-dashoffset
    :stroke-opacity
    :stroke-width
    :tab-size
    :widows
    :z-index
    :zoom]
   (mapcat (fn [k] [k (camel-key k)]))
   set))

(defn- normalize-styles [styles]
  (reduce (fn [m [attr v]]
            (if (number? v)
              (if (skip-pixelize-attrs attr)
                (update m attr str)
                (update m attr str "px"))
              m))
          styles
          styles))

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
   :xmlSpace :xml:space
   :mountedStyle :mounted-style
   :leavingStyle :leaving-style
   :disappearingStyle :disappearing-style})

(defn- prep-attrs [attrs]
  (let [event-keys (filter #(and (str/starts-with? (name %) "on") (ifn? (attrs %))) (keys attrs))
        attrs (->> attrs
                   (map (fn [[k v]] [(camel-key k) v]))
                   (remove (fn [[k v]] (nil? v)))
                   (into {}))
        attrs (set/rename-keys attrs attr-mappings)]
    (cond-> {:attrs (apply dissoc attrs :style :mounted-style :leaving-style :disappearing-style
                           :component :value :key :ref :dangerouslySetInnerHTML event-keys)
             :props (merge (select-keys attrs [:value])
                           (when-let [html (-> attrs :dangerouslySetInnerHTML :__html)]
                             {:innerHTML html}))
             :style (merge (normalize-styles (:style attrs))
                           (when-let [enter (:mounted-style attrs)]
                             {:delayed (normalize-styles enter)})
                           (when-let [remove (:leaving-style attrs)]
                             {:remove (normalize-styles remove)})
                           (when-let [destroy (:disappearing-style attrs)]
                             {:destroy (normalize-styles destroy)}))
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
  (and (vector? sexp)
       (not (map-entry? sexp))
       (or (keyword? (first sexp)) (fn? (first sexp)))))

(defn parse-hiccup-symbol [sym attrs]
  (let [[_ id] (re-find #"#([^\.#]+)" sym)
        [el & classes] (-> (str/replace sym #"#([^#\.]+)" "")
                           (str/split #"\."))]
    [el
     (cond-> attrs
       id (assoc :id id)
       (seq classes) (update :className #(str/join " " (if % (conj classes %) classes))))]))

(defn explode-styles [s]
  (->> (str/split s #";")
       (map #(let [[k v] (map str/trim (str/split % #":"))]
               [k v]))
       (into {})))

(defn prep-hiccup-attrs [attrs]
  (cond-> attrs
    (string? (:style attrs)) (update :style explode-styles)))

(defn flatten-seqs [xs]
  (loop [res []
         [x & xs] xs]
    (cond
      (and (nil? xs) (nil? x)) (seq res)
      (nil? x) (recur res xs)
      (seq? x) (recur (into res (flatten-seqs x)) xs)
      :default (recur (conj res x) xs))))

(defn inflate-hiccup [el-fn sexp]
  (if-not (hiccup? sexp)
    sexp
    (let [el-type (first sexp)
          args (rest sexp)
          args (if (map? (first args)) args (concat [{}] args))]
      (if (fn? el-type)
        (apply el-type (rest sexp))
        (let [[element attrs] (parse-hiccup-symbol (name el-type) (first args))]
          (apply create el-fn element (prep-hiccup-attrs attrs) (flatten-seqs (rest args))))))))

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
