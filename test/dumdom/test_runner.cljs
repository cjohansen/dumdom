(ns ^:figwheel-hooks dumdom.test-runner
  (:require [cljs.test :as test]
            [cljs-test-display.core :as display]
            [dumdom.core-test]
            [dumdom.dom-test]
            [dumdom.string-test]))

(enable-console-print!)

(defn test-run []
  (test/run-tests 
   (display/init! "app-tests")
   'dumdom.core-test
   'dumdom.dom-test
   'dumdom.string-test))

(defn ^:after-load render-on-relaod []
  (test-run))

(test-run)
