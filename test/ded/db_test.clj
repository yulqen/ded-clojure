(ns ded.db-test
  (:require [xtdb.api :as xt]
            [clojure.test :refer :all]
            [ded.db :as db]))

(def test-site {:xt/id :test-site-1
                :site/name "Test Site 1"
                :site/location "Test Location 1"
                :site/type "Test Type"
                :site/id 100})

(def test-site2 {:xt/id :test-site2
                :site/name "Test Site 2"
                :site/location "Test Location 2"
                :site/type "Test Type"
                :site/id 102})

;; TODO Test some basic middleware functionality
;; TODO Add some fixtures here: empty node, node containing sites, etc

(def ^:dynamic *node*)

(defn with-node [f]
  (binding [*node* (xt/start-node {})]
    (xt/submit-tx *node* [[::xt/put test-site2]])
    (try (f)
         (finally
           (.close *node*)))))

(use-fixtures :each with-node)

(deftest test-1
  (is (= "Nonce" (db/get-site-by-id 100 *node*))))
