(ns user
  (:require [clojure.pprint :as pprint]
            [ded.core :as ded]
            [ded.db :as db]
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
