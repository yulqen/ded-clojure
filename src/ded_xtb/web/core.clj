(ns ded-xtb.web.core
  (:gen-class)
  (:require [hiccup.page :as p]
            [ring.adapter.jetty :refer [run-jetty]]
            [ring.util.response :as resp]
            [ring.middleware.defaults :as ring-defaults]
            [clojure.pprint]
            [clojure.java.io :as io]
            [compojure.coercions :refer [as-int]]
            [compojure.core :refer [GET POST let-routes]]
            [compojure.route :as route]
            [xtdb.api :as xt]
            [com.stuartsierra.component :as component]))

;; (defn start-xtdb! []
;;   (letfn [(kv-store [dir]
;;             {:kv-store {:xtdb/module 'xtdb.rocksdb/->kv-store
;;                         :db-dir (io/file dir)
;;                         :sync? true}})]
;;     (xt/start-node
;;      {:xtdb/tx-log (kv-store "data/dev/tx-log")
;;       :xtdb/document-store (kv-store "data/dev/doc-store")
;;       :xtdb/index-store (kv-store "data/dev/index-store")})))
(defn start-xtdb! []
  (xt/start-node {}))

(def xtdb-node (start-xtdb!))
;; note that attempting to eval this expression more than once before first calling `stop-xtdb!` will throw a RocksDB locking error
;; this is because a node that depends on native libraries must be `.close`'d explicitly

(defn stop-xtdb! []
  (.close xtdb-node))

(defn handler [request]
  (clojure.pprint/pprint request)
  {:status 200, :headers {"Content-Type" "text/html"} :body "hello world"})

(defn site-handler [req]
  (-> (resp/response
       (p/html5
        [:head
         [:title "Sites page!"]
         [:meta {:charset "UTF-8"}]
         [:meta {:name "Content-Type" :content "text/html"}]
         [:meta {:name "viewport"
                 :content "width=device-width, initial-scale=1.0"}]
         [:body
          [:h1 "bobbins"]
          [:p "This is RANDOM paragraph of text that no one really cares about."]]]))
      (resp/content-type "text/html")))
  

(defrecord Application [config
                        database
                        state]
  component/Lifecycle
  (start [this]
    (assoc this :state "Running"))
  (stop [this]
    (assoc this :state "Stopped")))

(defn my-application
  [config]
  (component/using (map->Application {:config config}) [:database]))

(defn render-page [response]
    {:body "nonce!"})

(defn default-handler [req]
  (assoc-in req [:params :message]
            (str "Is this fucking thing working?")))

(defn my-middleware
  [handler]
  (fn [request]
    (let [resp (handler request)]
      (if (resp/response? resp)
        resp
        (render-page resp)))))

;; Helper for building the middleware:
(defn- add-app-component
  "Middleware to add your application component into the request. Use
  the same qualified keyword in your controller to retrieve it."
  [handler application]
  (fn [req]
    (handler (assoc req :application/component application))))

;; This is Ring-specific, the specific stack of middleware you need for your
;; application. This example uses a fairly standard stack of Ring middleware
;; with some tweaks for convenience

(defn middleware-stack
  "Given the application component and middleware, return a standard stack of
  Ring middleware for a web application."
  [app-component app-middleware]
  (fn [handler]
    (-> handler
        (app-middleware)
        (add-app-component app-component)
        (ring-defaults/wrap-defaults (-> ring-defaults/site-defaults
                                         ;; disable XSRF for now
                                         (assoc-in [:security :anti-forgery] false)
                                         ;; support load balancers
                                         (assoc-in [:proxy] true))))))

(defn my-handler
  "Given the application component, return middleware for routing.
  We use let-routes here rather than the more usual defroutes because
  Compojure assumes that if there's a match on the route, the entire
  request will be handled by the function specified for that route.
  Since we need to deal with page rendering after the handler runs,
  and we need to pass in the application component at start up, we
  need to define our route handlers so that they can be parameterized."
  [application]
  (let-routes [wrap (middleware-stack application #'my-middleware)]
    (GET  "/"                        []              (wrap #'default-handler))
    (GET "/sites" [] (wrap #'site-handler))
    ;; horrible: application should POST to this URL!
    ;;(GET  "/user/delete/:id{[0-9]+}" [id :<< as-int] (wrap #'user-ctl/delete-by-id))
    ;; add a new user:
    ;; (GET  "/user/form"               []              (wrap #'user-ctl/edit))
    ;; ;; edit an existing user:
    ;; (GET  "/user/form/:id{[0-9]+}"   [id :<< as-int] (wrap #'user-ctl/edit))
    ;; (GET  "/user/list"               []              (wrap #'user-ctl/get-users))
    ;; (POST "/user/save"               []              (wrap #'user-ctl/save))
    ;; ;; this just resets the change tracker but really should be a POST :)
    ;; (GET  "/reset"                   []              (wrap #'user-ctl/reset-changes))
    (route/resources "/")
    (route/not-found "Not Found")))

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

(defn web-server
  "Return a WebServer component that depends on the application.
  The handler-fn is a function that accepts the application (Component) and
  returns a fully configured Ring handler (with middeware)."
  [handler-fn port]
  (component/using (map->WebServer {:handler-fn handler-fn
                                    :port port})
                   [:application]))

()

;; (defrecord Database [db-spec     ; configuration
;;                      datasource] ; state
(defrecord Database [datasource] ; state
  component/Lifecycle
  (start [this]
    (let [database (start-xtdb!)]
      database))
    ;; (if datasource
    ;;   this ; already initialized
    ;;   (let [database (assoc this :datasource {})]
    ;;     ;; set up database if necessary
    ;;     ;; LEMON R(populate database (:dbtype db-spec))
    ;;     database)))
  (stop [this]
    (stop-xtdb!)))

  ;; allow the Database component to be "called" with no arguments
  ;; to produce the underlying datasource object
  ;; clojure.lang.IFn
  ;; (invoke [_] datasource))

;; (def ^:private my-db
;;   "SQLite database connection spec."
;;   {:dbtype "xtdb" :dbname "my-db"})

(defn setup-database [] (map->Database xtdb-node))

;; This is the piece that combines the generic web server component above with
;; your application-specific component defined at the top of the file, and
;; any dependencies your application has (in this case, the database):
;; Note that a Var is used -- the #' notation -- instead of a bare symbol
;; to make REPL-driven development easier. See the following for details:
;; https://clojure.org/guides/repl/enhancing_your_repl_workflow#writing-repl-friendly-programs
(defn new-system
  "Build a default system to run. In the REPL:
  (def system (new-system 8888))
  (alter-var-root #'system component/start)
  (alter-var-root #'system component/stop)
  See the Rich Comment Form below."
  ([port] (new-system port true))
  ([port repl]
   (component/system-map :application (my-application {:repl repl})
                         :database    (setup-database)
                         :web-server  (web-server #'my-handler port))))
(comment
  (def system (new-system 8080))
  (alter-var-root #'system component/start)
  (alter-var-root #'system component/stop)
  ;; the comma here just "anchors" the closing paren on this line,
  ;; which makes it easier to put you cursor at the end of the lines
  ;; above when you want to evaluate them into the REPL:
  ,)

(defonce ^:private
  ^{:doc "This exists so that if you run a socket REPL when
  you start the application, you can get at the running
  system easily.
  Assuming a socket REPL running on 50505:
  nc localhost 50505
  user=> (require 'usermanager.main)
  nil
  user=> (in-ns 'usermanager.main)
  ...
  usermanager.main=> (require '[next.jdbc :as jdbc])
  nil
  usermanager.main=> (def db (-> repl-system deref :application :database))
  #'usermanager.main/db
  usermanager.main=> (jdbc/execute! (db) [\"select * from addressbook\"])
  [#:addressbook{:id 1, :first_name \"Sean\", :last_name \"Corfield\", :email \"sean@worldsingles.com\", :department_id 4}]
  usermanager.main=>"}
  repl-system
  (atom nil))

(defn -main
  [& [port]]
  (let [port (or port (get (System/getenv) "PORT" 8080))
        port (cond-> port (string? port) Integer/parseInt)]
    (println "Starting up on port" port)
    ;; start the web server and application:
    (-> (component/start (new-system port false))
        ;; then put it into the atom so we can get at it from a REPL
        ;; connected to this application:
        (->> (reset! repl-system))
        ;; then wait "forever" on the promise created:
        :web-server :shutdown deref)))
