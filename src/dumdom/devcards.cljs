(ns dumdom.devcards
  (:require [devcards.core :refer [get-props ref->node]]
            [devcards.util.utils :refer-macros [define-react-class-once]]
            [dumdom.core :as q]))

(defn render [this]
  (q/render (get-props this :component) (ref->node this "dumdom-node")))

(define-react-class-once DumDomReactWrapper
  (componentDidUpdate
   [this _ _]
   (render this))
  (componentDidMount
   [this]
   (render this))
  (render
   [this]
   (js/React.createElement "div" #js {:ref "dumdom-node"})))

(defn as-react-element [component]
  (js/React.createElement DumDomReactWrapper #js {:component component}))

(defn reactify [renderer]
  (if (and (fn? renderer) (not (q/component? renderer)))
    (fn [& args]
      (let [res (apply renderer args)]
        (as-react-element res)))
    (as-react-element renderer)))
