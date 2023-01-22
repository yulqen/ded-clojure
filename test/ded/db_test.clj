(ns ded.db-test
  (:require [xtdb.api :as xt]))

(def node (xt/start-node {}))

(def test-site {:xt/id :test-site-1
                :site/name "Test Site 1"
                :site/location "Test Location 1"
                :site/type "Test Type"
                :site/id 100})


