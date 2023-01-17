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
         ;; it's important for your components to be idempotent: if you start
         ;; them more than once, only the first call to start should do anything
         ;; and subsequent calls should be an no-op -- the same applies to the
         ;; stop calls: only stop the system if it is running, else do nothing
         (if http-server
           this
           (assoc this
                  ;; start a Jetty web server -- use :join? false
                  ;; so that it does not block (we use a promise
                  ;; to block on in -main).
                  ;; to start an http-kit web server instead:
                  ;; 1. call run-server instead of run-jetty
                  ;; 2. omit :join? false since http-kit does
                  ;; not block when it starts
                  :http-server (run-jetty (handler-fn application)
                                          {:port port :join? false})
                  ;; this promise exists primarily so -main can
                  ;; wait on something, since we start the web
                  ;; server in a non-blocking way:
                  :shutdown (promise))))
  (stop  [this]
         (if http-server
           (do
             ;; shutdown Jetty: call .stop on the server object:
             (.stop http-server)
             ;; shutdown http-kit: invoke the server (as a function):
             ;; (http-server)
             ;; deliver the promise to indicate shutdown (this is
             ;; really just good housekeeping, since you're only
             ;; going to call stop via the REPL when you are not
             ;; waiting on the promise):
             (deliver shutdown true)
             (assoc this :http-server nil))
           this)))


(defrecord Database [datasource] ; state
  component/Lifecycle
  (start [this]
    (let [database (db/start-xtdb!)]
      database))
    ;; (if datasource
    ;;   this ; already initialized
    ;;   (let [database (assoc this :datasource {})]
    ;;     ;; set up database if necessary
    ;;     ;; LEMON R(populate database (:dbtype db-spec))
    ;;     database)))
  (stop [this]
    (db/stop-xtdb!)))
