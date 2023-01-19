(ns user
  (:require [clojure.pprint :as pprint]
            [ded.core :as ded]
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
