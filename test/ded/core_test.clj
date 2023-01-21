(ns ded.core-test
  (:require [clojure.test :refer :all]))

;; to run test via clojure -X:test or clojure -M:test at this point, you must stop the repl, until we reconfigure RocksDB implementation.

(deftest test-check
  (is (= 1 1)))
