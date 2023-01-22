(ns ded.db-test
  (:require [xtdb.api :as xt]
            [clojure.test :refer :all]
            [ded.db :as db]))

(def node (xt/start-node {}))

(def test-site {:xt/id :test-site-1
                :site/name "Test Site 1"
                :site/location "Test Location 1"
                :site/type "Test Type"
                :site/id 100})

;; TODO Test some basic middleware functionality
;; TODO Add some fixtures here: empty node, node containing sites, etc

(deftest set-up
  (db/add-site node test-site)
  (is (+ 1 1)))
