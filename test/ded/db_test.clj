(ns ded.db-test
  (:require [clojure.test :refer [deftest use-fixtures is testing]]
            [clojure.edn :as edn]
            [clojure.inspector :as inspector]
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

;; DESIGN JOURNAL
#_{:clj-kondo/ignore [:redefined-var]}
(comment
  ;; create basic populated node from edn file

  (declare ^:dynamic *node*)
  (declare ^:dynamic *empty-node*)

  (defn basic-populated-node
    "Fixture to add data from initial.edn to test database."
    [f]

    (binding [*node* (xt/start-node {})] ;; start-node from xtd docs
      (xt/submit-tx *node* (db/prep-trx
                            (edn/read-string (slurp "test/ded/initial.edn"))))
      (xt/sync *node*)
      (f)
      (.close *node*)))
  #_())

