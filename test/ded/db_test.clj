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

(deftest make-person
  (testing "Basic attributes"
    (is (= (:serial (db/make-person "Colin" "McLintock")) 1))
    (is (= (:primary-email (db/make-person "Colin" "McLintock"
                                           {:primary-email "colinm@example.com"})) "colinm@example.com"))
    (is (= (:secondary-email (db/make-person "Colin" "McLintock"
                                             {:secondary-email "colinm2@example.com"})) "colinm2@example.com"))
    (is (= (:serial (db/make-person "Colin" "McLintock"
                                    {:latest-id 120})) 121))
    (is (= (:title (db/make-person "Colin" "McLintock"
                                   {:title "Mr"})) "Mr"))
    (is (= (first (:phone-numbers
                   (db/make-person "Colin" "McLintock"
                                   {:title "Mr"
                                    :phone-numbers ["0700 303 2343" "0800 203 2434"]}))) "0700 303 2343")))

  ;; This is not a good test... 
  (testing "SiteOp relationship"
    (let [new-site (db/make-siteop "Test") ;; create a new test using make-siteop, which is bad...
          n {} ;; we need a mock xtdb node
          new-site-id (:xt/id new-site) ;; get the xtdb id of the new site
          person (db/make-person "Alan" "Titch" {:siteop-id new-site-id})] ;; create the new person and their relationship to the site
      (with-redefs-fn {#'db/get-people-for-siteop ;; here we are mocking this func...
                       (fn [n id] [{:first-name "Alan"}])} ;; ..to produce what we need for this test...
        #(is (= "Alan" (:first-name (first (db/get-people-for-siteop n new-site-id)))))))))

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

