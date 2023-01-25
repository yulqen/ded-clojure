(ns user
  (:require [ded.core :as ded]
            [ded.db :as db]
            [xtdb.api :as xt]
            [ded.routes :as routes]
            [com.stuartsierra.component :as component]))

(comment
  (def system (ded/new-system 8080))
  (alter-var-root #'system component/start)
  (alter-var-root #'system component/stop)
  ,)


(def system (ded/new-system 8080))

(defn start []
  (alter-var-root #'system component/start))

(defn stop []
  (alter-var-root #'system component/stop))

;; the correct way to close down from the repl:
 ;; (def mynode (-> system :application :database))
 ;; (stop)
 ;; (.close mynode)

 ;; this may also kill the repl, but if you restart it should restart clean

(def demo-sites [{:xt/id :rompers
                 :site/name "Rompers"
                 :site/location "Tiffenham"
                 :site/type "Garbage Dump"
                 :site/id 100}

                 {:xt/id :rompers2
                 :site/name "Rompers 2"
                 :site/location "Tiffenham"
                 :site/type "Garbage Dump"
                 :site/id 102}

                 {:xt/id :rompers3
                 :site/name "Rompers 3"
                 :site/location "Tiffenham"
                 :site/type "Garbage Dump"
                 :site/id 103}])

(defn prepare-tx [docs]
  (mapv (fn [doc] [::xt/put doc]) docs))

(defn get-node [](-> system :application :database))

(defn transact-test-data [node docs] (xt/submit-tx node docs))
