(ns ded.db-test
  (:require [xtdb.api :as xt]
            [clojure.test :refer [deftest use-fixtures is testing]]
            [clojure.edn :as edn]
            [ded.db :as db]))

;; TODO Test some basic middleware functionality
;; TODO Add some fixtures here: empty node, node containing sites, etc

;; we wish to work with an in-memory node
;; we use binding in the fixture below to assign this Var to the xtdb node
;; and then release it afterwards
(declare ^:dynamic *node*)

(defn basic-populated-node
  "Fixture to add data from initial.edn to test database."
  [f]
  (binding [*node* (xt/start-node {})]
    (xt/submit-tx *node* (db/prep-trx
                          (edn/read-string (slurp "test/ded/initial.edn"))))
    (xt/sync *node*)
    (f)
    (.close *node*)))

(use-fixtures :each basic-populated-node)

(deftest get-site-controllers
  (testing "Basic get functions"
    (is (= :test-site-1 (first (first (db/get-site-by-id 100 *node*)))))
    (is (= :test-site-2 (first (first (db/get-site-by-id 102 *node*)))))
    (is (= #{[:test-site-2 102 "Test Site 2"]
             [:test-site-1 100 "Test Site 1"]}
           (db/get-site-ids *node*)))))


