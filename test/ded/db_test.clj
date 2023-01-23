(ns ded.db-test
  (:require [xtdb.api :as xt]
            [clojure.test :refer :all]
            [clojure.edn :as edn]
            [ded.db :as db]))

;; TODO Test some basic middleware functionality
;; TODO Add some fixtures here: empty node, node containing sites, etc

(declare ^:dynamic *node*)

(defn with-node [f]
  (letfn [(prep-trx [docs]
            (mapv (fn [doc] [::xt/put doc]) docs))]
    (binding [*node* (xt/start-node {})]
      (xt/submit-tx *node* (prep-trx (edn/read-string (slurp "test/ded/initial.edn"))))
      (xt/sync *node*)
      (f)
      (.close *node*))))

(use-fixtures :each with-node)

(deftest test-1
  (is (= :test-site-1 (first (first (db/get-site-by-id 100 *node*)))))
  (is (= :test-site-2 (first (first (db/get-site-by-id 102 *node*))))))


