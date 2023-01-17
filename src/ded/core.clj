(ns ded.core
  (:gen-class)
  (:require [clojure.pprint]
            [ded.components :as my-components]
            [ded.db :as db]
            [ded.routes :as my-routes]
            [com.stuartsierra.component :as component]))

(defn my-application
  [config]
  (component/using (my-components/map->Application {:config config}) [:database]))


(defn my-web-server
  "Return a WebServer component that depends on the application.
  The handler-fn is a function that accepts the application (Component) and
  returns a fully configured Ring handler (with middeware)."
  [handler-fn port]
  (component/using (my-components/map->WebServer {:handler-fn handler-fn
                                    :port port})
                   [:application]))


(defn my-database [] (my-components/map->Database db/xtdb-node))

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
                         :database    (my-database)
                         :web-server  (my-web-server #'my-routes/my-handler port))))


(defonce ^:private repl-system
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
