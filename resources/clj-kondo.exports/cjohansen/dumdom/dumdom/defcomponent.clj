(comment ; add-libs
  '{:deps {clj-kondo/clj-kondo {:mvn/version "2024.02.12"}}})

(ns dumdom.defcomponent
  (:require [clj-kondo.hooks-api :as api]))

(def valid-opts #{:on-mount
                  :on-update
                  :on-render
                  :on-unmount
                  :will-appear
                  :did-appear
                  :will-enter
                  :did-enter
                  :will-leave
                  :did-leave
                  :name})

(defn- extract-docstr
  [[docstr? & forms :as remaining-forms]]
  (if (api/string-node? docstr?)
    [docstr? forms]
    [(api/string-node "no docs") remaining-forms]))

(defn- extract-opts
  ([forms]
   (extract-opts forms []))
  ([[k v & forms :as remaining-forms] opts]
   (if (api/keyword-node? k)
     (do
       (when-not (valid-opts (api/sexpr k))
         (api/reg-finding! (assoc (meta k)
                                  :message (str "Invalid option: `" k "`")
                                  :type :dumdom/component-options)))
       (extract-opts forms (into opts [k v])))
     [(api/map-node opts) remaining-forms])))

(defn ^:export defcomponent [{:keys [node]}]
  (let [[name & forms] (rest (:children node))
        [docstr forms] (extract-docstr forms)
        [opts forms] (extract-opts forms)
        new-node (api/list-node
                  (list*
                   (api/token-node 'defn)
                   name
                   docstr
                   opts
                   forms))]
    {:node new-node}))

(comment
  (def code (str '(defcomponent heading
                    ""
                    :on-render (fn [dom-node val old-val])
                    :invalid (fn [])
                    [data]
                    (def data data)
                    [:h2 {:style {:background :black
                                  :color :white}}
                     (pr-str (:text data))])))

  (defcomponent {:node (api/parse-string code)})

  (require '[clj-kondo.core :as clj-kondo])
  (:findings (with-in-str (str "(require '[dumdom.core :refer [defcomponent]])"
                               " "
                               code)
               (clj-kondo/run! {:lint ["-"]})))
  :rcf)