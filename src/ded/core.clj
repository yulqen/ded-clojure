(ns ded.core
  (:gen-class)
  (:require [hiccup.page :as p]
            [ring.util.response :as resp]
            [ring.middleware.defaults :as ring-defaults]
            [clojure.pprint]
            [compojure.coercions :refer [as-int]]
            [compojure.core :refer [GET POST let-routes]]
            [compojure.route :as route]
            [ded.components :as my-components]
            [ded.db :as db]
            [com.stuartsierra.component :as component]))

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
          [:h1 "Bobbins woo"]
          [:p "This is RANDOM paragraph of text that no one really cares about."]]]))
      (resp/content-type "text/html")))
  
(defn my-application
  [config]
  (component/using (my-components/map->Application {:config config}) [:database]))

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


(defn web-server
  "Return a WebServer component that depends on the application.
  The handler-fn is a function that accepts the application (Component) and
  returns a fully configured Ring handler (with middeware)."
  [handler-fn port]
  (component/using (my-components/map->WebServer {:handler-fn handler-fn
                                    :port port})
                   [:application]))


(defn setup-database [] (my-components/map->Database db/xtdb-node))

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
