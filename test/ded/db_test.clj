(ns ded.db-test
  (:require [clojure.test :refer [deftest use-fixtures is testing]]
            [clojure.edn :as edn]
            [ded.db :as db]))

;; TODO Test some basic middleware functionality
;; TODO Add some fixtures here: empty node, node containing sites, etc

;; we wish to work with an in-memory node
;; we use binding in the fixture below to assign this Var to the xtdb node
;; and then release it afterwards

;; TODO start testing the record constructors

(deftest make-siteop
  (is (= (:serial (db/make-siteop "Test Site 2" {:latest-id 10})) 11))
  (is (= (:typecode (db/make-siteop "Test Site 3")) :SOP))
  (is (= (:serial (db/make-siteop "Test Site 1")) 1))
  (is (= (:desc (db/make-siteop "Test Site 1" {:desc "A test"})) "A test")))

