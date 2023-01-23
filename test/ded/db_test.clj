(ns ded.db-test
  (:require [xtdb.api :as xt]
            [clojure.test :refer [deftest use-fixtures is]]
            [clojure.edn :as edn]
            [ded.db :as db]))

;; TODO Test some basic middleware functionality
;; TODO Add some fixtures here: empty node, node containing sites, etc

(declare ^:dynamic *node*)

(defn with-node
  "Fixture to add data from initial.edn to test database."
  [f]
  (binding [*node* (xt/start-node {})]
    (xt/submit-tx *node* (db/prep-trx
                          (edn/read-string (slurp "test/ded/initial.edn"))))
    (xt/sync *node*)
    (f)
    (.close *node*)))

(use-fixtures :each with-node)

(deftest test-populate
  (is (= :test-site-1 (first (first (db/get-site-by-id 100 *node*)))))
  (is (= :test-site-2 (first (first (db/get-site-by-id 102 *node*)))))
  (is (= #{[:test-site-2 102 "Test Site 2"] [:test-site-1 100 "Test Site 1"]} (db/get-site-ids *node*))))


