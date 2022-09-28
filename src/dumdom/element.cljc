(ns dumdom.element
  (:require [clojure.set :as set]
            [clojure.string :as str]))

(def ^:dynamic *handle-event* nil)

(defn event-handler [h]
  (if (fn? h)
    h
    (if-let [f *handle-event*]
      ;; At this point we need to "early" bind the late bound var. It made it
      ;; this far, but now it must be frozen in time, so it sticks around for
      ;; the asynchronous events it will handle after being rendered to the DOM.
      (fn [e]
        (f e h))
      (when (string? h)
        ;; Strings could be inline JavaScript, so will be allowed when there is
        ;; no global event handler.
        h))))

(defn- event-entry [attrs k]
  [(.toLowerCase (str/replace (name k) #"^on" ""))
   (event-handler (attrs k))])

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

(defn data-attr? [[k v]]
  (re-find #"^data-" (name k)))

(defn event-handler? [attr-name]
  (re-find #"on[A-Z]" (name attr-name)))

(defn- prep-attrs [attrs]
  (let [dataset (->> attrs
                     (filter data-attr?)
                     (map (fn [[k v]] [(str/replace (name k) #"^data-" "") v]))
                     (into {}))
        attrs (->> attrs
                   (remove data-attr?)
                   (map (fn [[k v]] [(camel-key k) v]))
                   (remove (fn [[k v]] (nil? v)))
                   (into {}))
        attrs (set/rename-keys attrs attr-mappings)
        event-keys (filter event-handler? (keys attrs))
        el-key (or (:key attrs)
                   (when (contains? attrs :dangerouslySetInnerHTML)
                     (hash (:dangerouslySetInnerHTML attrs))))]
    (cond-> {:attrs (apply dissoc attrs :style :mounted-style :leaving-style :disappearing-style
                           :component :value :key :ref :dangerouslySetInnerHTML event-keys)
             :props (cond-> {}
                      (:value attrs) (assoc :value (:value attrs))

                      (contains? (:dangerouslySetInnerHTML attrs) :__html)
                      (assoc :innerHTML (-> attrs :dangerouslySetInnerHTML :__html)))
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
                       :destroy #(callback nil)}))
             :dataset dataset}
      el-key (assoc :key el-key))))

(declare create)

(defn hiccup? [sexp]
  (and (vector? sexp)
       (not (map-entry? sexp))
       (or (keyword? (first sexp)) (fn? (first sexp)))))

(defn class-list [classes]
  (cond
    (keyword? classes) [(name classes)]
    (string? classes) [classes]
    (empty? classes) []
    (coll? classes) (map #(if (keyword? %) (name %) %) classes)
    (nil? classes) []
    :else (throw (ex-info "Received class name that is neither string, keyword, or a collection of those"
                          {:classes classes}))))

(defn parse-hiccup-symbol [sym attrs]
  (let [[_ id] (re-find #"#([^\.#]+)" sym)
        [el & classes] (-> (str/replace sym #"#([^#\.]+)" "")
                           (str/split #"\."))
        classes (->> (concat
                      (class-list (:class attrs))
                      (class-list (:className attrs))
                      classes)
                     (remove empty?))]
    [el
     (cond-> (dissoc attrs :class :className)
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
      (seq? x) (recur (into res (flatten-seqs x)) xs)
      :default (recur (conj res x) xs))))

(defn add-namespace [vnode]
  (cond-> vnode
    (not= "foreignObject" (:sel vnode))
    (assoc-in [:data :ns] "http://www.w3.org/2000/svg")

    (:children vnode)
    (update :children #(map add-namespace %))))

(defn svg? [sel]
  (and (= "s" (nth sel 0))
       (= "v" (nth sel 1))
       (= "g" (nth sel 2))
       (or (= 3 (count sel))
           (= "." (nth sel 3))
           (= "#" (nth sel 3)))))

(defn primitive? [x]
  (or (string? x) (number? x)))

(defn convert-primitive-children [children]
  (for [c children]
    (if (primitive? c)
      {:text c}
      c)))

;; This is a port of Snabbdom's `h` function, but without the varargs support.
(defn create-vdom-node [sel attrs children]
  (let [cmap? (map? children)]
    (cond-> {:sel sel
             :data (dissoc attrs :key :dumdom/component-key)
             :dumdom/component-key (:dumdom/component-key attrs)}
      (primitive? children)
      (assoc :text children)

      cmap?
      (assoc :children [children])

      (and (seq? children) (not cmap?))
      (assoc :children children)

      :always (update :children convert-primitive-children)

      (svg? sel)
      add-namespace

      (:key attrs)
      (assoc :key (:key attrs)))))

(defn inflate-hiccup [sexp]
  (cond
    (nil? sexp) (create-vdom-node "!" {} "nil")

    (not (hiccup? sexp)) sexp

    :default
    (let [tag-name (first sexp)
          args (rest sexp)
          args (if (map? (first args)) args (concat [{}] args))]
      (if (fn? tag-name)
        (apply tag-name (rest sexp))
        (let [[element attrs] (parse-hiccup-symbol (name tag-name) (first args))]
          (apply create element (prep-hiccup-attrs attrs) (flatten-seqs (rest args))))))))

(defn enumerate-key [kmap k]
  (let [k (if (string? k) k (str k))]
    [k (get kmap k 0)]))

(defn realize-children [path xs]
  (loop [xs (seq xs)
         res []
         ks {}]
    (if (nil? xs)
      (seq res)
      (let [x (first xs)
            child (if (fn? x)
                    (x path ks)
                    x)]
        (recur
         (next xs)
         (conj res child)
         (let [[k n] (:dumdom/component-key child)
               [lk lk-n] (:dumdom/lookup-key child)]
           (cond-> (assoc ks k (some-> (or n 0) inc))
             lk (assoc lk (some-> (or lk-n 0) inc)))))))))

(defn add-comment-node [{:dumdom/keys [component-name render-comments?] :as component}]
  (cond->> [component]
    (and render-comments? component-name)
    (cons (create-vdom-node "!" {} component-name))))

(defn create [tag-name attrs & children]
  (fn [path kmap]
    (let [attrs (prep-attrs attrs)
          k (enumerate-key kmap (:key attrs))
          fullpath (conj path k)]
      (create-vdom-node
       tag-name
       (cond-> attrs
         (:key attrs) (assoc :key (str/join "." k))

         k (assoc :dumdom/component-key k)

         :always
         (assoc-in [:hook :update]
                   (fn [old-vnode new-vnode]
                     (doseq [node (filter #(some-> % .-willEnter) (.-children new-vnode))]
                       ((.-willEnter node)))
                     (doseq [node (filter #(some-> % .-willAppear) (.-children new-vnode))]
                       ((.-willAppear node))))))
       (->> children
            (mapcat #(if (seq? %) % [%]))
            (map inflate-hiccup)
            (realize-children fullpath)
            (mapcat add-comment-node))))))
