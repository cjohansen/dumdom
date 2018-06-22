(ns user
  (:require [figwheel-sidecar.repl-api :as r]))

(defn cljs []
  (r/start-figwheel!)
  (r/cljs-repl))
