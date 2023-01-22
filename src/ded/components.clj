(ns ded.components
  (:require [com.stuartsierra.component :as component]
            [ring.adapter.jetty :refer [run-jetty]]
            [ded.db :as db]))

(defrecord Application [config
                        database
                        state]
  component/Lifecycle
  (start [this]
    (assoc this :state "Running"))
  (stop [this]
    (assoc this :state "Stopped")))


;; Standard web server component -- knows how to stop and start your chosen
;; web server... uses Jetty but explains how to use http-kit instead:
;; lifecycle for the specified web server in which we run
(defrecord WebServer [handler-fn server port ; parameters
                      application            ; dependencies
                      http-server shutdown]  ; state
  component/Lifecycle
  (start [this]
         (if http-server
           this
           (assoc this
                  :http-server (run-jetty (handler-fn application)
                                          {:port port :join? false})
                  :shutdown (promise))))
  (stop  [this]
         (if http-server
           (do
             (.stop http-server)
             (deliver shutdown true)
             (assoc this :http-server nil))
           this)))


(defrecord Database [datasource] ; state
  component/Lifecycle
  (start [this]
    (if datasource
      this
      (let [node (db/start-xtdb!)]
        node)))
  (stop [this]
    (.close datasource)))
