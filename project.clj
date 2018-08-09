(defproject dumdom "1"
  :description "The component library that's only interested in efficiently mapping data to DOM"
  :dependencies [[org.clojure/clojure "1.9.0"]
                 [org.clojure/clojurescript "1.10.238"]
                 [cljsjs/snabbdom "0.7.1-0"]]
  :jvm-opts ["-Djava.awt.headless=true"]
  :plugins [[lein-cljsbuild "1.1.5"]
            [lein-figwheel "0.5.15" :exclusions [org.clojure/clojure
                                                 org.clojure/tools.reader
                                                 joda-time
                                                 ring/ring-core]]]
  :clean-targets ^{:protect false} ["resources/public/js"
                                    "target"]
  :source-paths ["src"]
  :cljsbuild {:builds [{:id "tests"
                        :source-paths ["src" "test"]
                        :figwheel {:devcards true}
                        :compiler {:main "dumdom.devcards.test-runner"
                                   :parallel-build true
                                   :asset-path "js/test-cards-out"
                                   :output-to "resources/public/js/test-cards.js"
                                   :output-dir "resources/public/js/test-cards-out"
                                   :source-map-timestamp true}}
                       {:id "devcards"
                        :source-paths ["src" "devcards"]
                        :figwheel {:devcards true}
                        :compiler {:main "dumdom.cards"
                                   :parallel-build true
                                   :asset-path "js/devcards-out"
                                   :output-to "resources/public/js/devcards.js"
                                   :output-dir "resources/public/js/devcards-out"
                                   :source-map-timestamp true}}]}
  :figwheel {:css-dirs ["resources/public/css"]
             :server-port 8086}
  :profiles {:dev {:dependencies [[com.cemerick/piggieback "0.2.2"]
                                  [org.clojure/tools.nrepl "0.2.13"]
                                  [figwheel-sidecar "0.5.13"]
                                  [devcards "0.2.5"]]
                   :source-paths ["dev" "build"]
                   :main user
                   :repl-options {:nrepl-middleware [cemerick.piggieback/wrap-cljs-repl]}}})
