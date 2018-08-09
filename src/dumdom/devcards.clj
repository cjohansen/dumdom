(ns dumdom.devcards
  "Devcards helpers for dumdom. dumdom.devcards/defcard is a drop-in replacement
  for devcards.core/defcard that supports dumdom components.

  The macro and helper function were lifted from devcards, and lightly adjusted
  to allow for dumdom components to automatically be wrapped in React elements,
  allowing them to be seamlessly rendered by devcards."
  (:require [devcards.core :as devcards]
            [devcards.util.utils :as utils]))

(defn card
  ([vname docu main-obj initial-data options]
   `(devcards.core/defcard* ~(symbol (name vname))
      (devcards.core/card-base
       {:name ~(name vname)
        :documentation ~docu
        :main-obj (dumdom.devcards/reactify ~main-obj)
        :initial-data ~initial-data
        :options ~options})))
  ([vname docu main-obj initial-data]
   (card vname docu main-obj initial-data {}))
  ([vname docu main-obj]
   (card vname docu main-obj {} {}))
  ([vname docu]
   (card vname docu nil {} {})))

(defmacro defcard [& expr]
  (when (utils/devcards-active?)
    (apply dumdom.devcards/card (devcards/parse-card-args expr 'card))))
