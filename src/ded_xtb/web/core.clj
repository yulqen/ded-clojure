(ns ded-xtb.web.core
  (:gen-class)
  (:require [reitit.ring :as ring]
            [org.httpkit.server :as s]
            [hiccup.page :as p]
            [ring.middleware.reload :refer [wrap-reload]]))

(defn handler [_]
  {:status 200, :body "ok"})

(defn site-handler [_]
  {:body (p/html5
          [:head
           [:title "Sites page!"]
           [:meta {:charset "UTF-8"}]
           [:meta {:name "viewport"
                   :content "width=device-width, initial-scale=1.0"}]
           [:body
            [:h1 "bobbins"]
            [:p "This is RANDOM paragraph of text that no one really cares about."]]])})

(defn wrap [handler id]
  (fn [request]
    (update (handler request) :wrap (fnil conj '()) id)))

(def app
  (ring/ring-handler
    (ring/router
     ["/api" {:middleware [[wrap :admin]]}
       ["/ping" {:get handler
                 :name ::ping}]
       ["/admin" {:middleware [[wrap :admin]]}
        ["/users" {:get handler
                   :post handler}]]
      ["/sites" {:get site-handler}]])))

(def reloadable-app
  (wrap-reload #'app))

(defonce server (atom nil))

(defn stop-server []
  (when-not (nil? @server)
    ;; graceful shutdown: wait 100ms for existing requests to be finished
    ;; :timeout is optional, when no timeout, stop immediately
    (@server :timeout 100)
    (reset! server nil)))

(defn -main [& args]
  ;; The #' is useful when you want to hot-reload code
  ;; You may want to take a look: https://github.com/clojure/tools.namespace
  ;; and https://http-kit.github.io/migration.html#reload
  (reset! server (s/run-server reloadable-app {:port 8080})))
