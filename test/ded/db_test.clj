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
(declare ^:dynamic *empty-node*)

(defn basic-populated-node
  "Fixture to add data from initial.edn to test database."
  [f]
  (binding [*node* (xt/start-node {})]
    (xt/submit-tx *node* (db/prep-trx
                          (edn/read-string (slurp "test/ded/initial.edn"))))
    (xt/sync *node*)
    (f)
    (.close *node*)))

(defn empty-node
  "Empty node for testing as an empty database."
  [f]
  (binding [*empty-node* (xt/start-node {})]
    (f)
    (.close *empty-node*)))

(use-fixtures :each empty-node basic-populated-node)

(deftest get-site-controllers
  (testing "Basic get functions"
    (is (= :test-site-1 (first (first (db/get-site-by-id 100 *node*)))))
    (is (= :test-site-2 (first (first (db/get-site-by-id 102 *node*)))))
    (is (= #{[:test-site-2 102 "Test Site 2"]
             [:test-site-1 100 "Test Site 1"]}
           (db/get-site-ids *node*)))))

(deftest adding-site-data-to-database
  (testing "Basic create"
    (let [first-site (db/add-site *empty-node* {:xt/id :SO1
                                                :site/name "Tothcubs"})]
      (is (= 0 (:xtdb.api/tx-id first-site)))
      (is (= :SO1 (xt/q (xt/db *empty-node*)
                        '{:find [xtid]
                          :where [[e :xt/id xtid]]}
                        ))))))


