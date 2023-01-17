(ns ded-xtb.web.core
  (:gen-class)
  (:require [hiccup.page :as p]
            [ring.adapter.jetty :as jetty]
            [clojure.pprint]
            [compojure.core :as compojure]
            [compojure.route :as route]
            [ring.middleware.reload :refer [wrap-reload]]))

(defn handler [request]
  (clojure.pprint/pprint request)
  {:status 200, :headers {"Content-Type" "text/html"} :body "hello world"})

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

(compojure/defroutes app
  (compojure/GET "/" [] "Hello world")
  (route/not-found "Page not found"))

(defn -main
  [& args]
  (jetty/run-jetty handler {:port 8080
                            :join? true}))
