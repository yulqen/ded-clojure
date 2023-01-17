(ns ded.routes
  (:require [ring.middleware.defaults :as ring-defaults]
            [hiccup.page :as p]
            [ded.db :as db]
            [clojure.pprint :as pprint]
            [compojure.route :as route]
            [compojure.coercions :refer [as-int]]
            [compojure.core :refer [GET POST let-routes]]
            [ring.util.response :as resp]))

(defn default-handler [req]
  (assoc-in req [:params :message]
            (str "Is this fucking thing working?")))

(defn render-page [response]
    {:body "nonce!"})

(defn my-middleware
  [handler]
  (fn [request]
    (let [resp (handler request)]
      (if (resp/response? resp)
        resp
        (render-page resp)))))

(defn site-handler [req]
  (let [new-site1 {:xt/id         :mammoth-site
                   :site/name     "Mammoth Site"
                   :site/type     :submarine-base
                   :site/id       100
                   :site/location "Hopwirth Flogash"}
        new-site2 {:xt/id         :cloth-site
                   :site/name     "Cloth Site"
                   :site/type     :submarine-base
                   :site/id       101
                   :site/location "McSkelton Brimmy"}
        _         (db/add-site (-> req :application/component :database) new-site1)
        _         (db/add-site (-> req :application/component :database) new-site2)
        sites     (db/get-all (-> req :application/component :database))]
    (-> (resp/response
         (p/html5
          [:head
           [:title "SITES"]
           [:meta {:charset "UTF-8"}]
           [:meta {:name "Content-Type" :content "text/html"}]
           [:meta {:name    "viewport"
                   :content "width=device-width, initial-scale=1.0"}]]
          [:body
           [:ul
            [:li (for [x (map :site/name (map first sites))] [:li x])]]]))
         ;; (-> (resp/response
         ;;      (p/html5
         ;;       [:head
         ;;        [:title "Sites page!"]
         ;;        [:meta {:charset "UTF-8"}]
         ;;        [:meta {:name "Content-Type" :content "text/html"}]
         ;;        [:meta {:name "viewport"
         ;;                :content "width=device-width, initial-scale=1.0"}]
         ;;        [:body
         ;;         [:h1 "Wackatan Pobbles knoc!"]
         ;;         [:a {:href "http://nufc.com"} "nufc.com"]
         ;;         [:p "This is ROOMADOOM staplehurst paragraph of text that no one really cares about."]]]))
         (resp/content-type "text/html"))))

;; Helper for building the middleware:
(defn- add-app-component
  "Middleware to add your application component into the request. Use
  the same qualified keyword in your controller to retrieve it."
  [handler application]
  (fn [req]
    (handler (assoc req :application/component application))))

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
    (GET  "/" [] (wrap #'default-handler))
    (GET "/chorley" [] (wrap #'default-handler))
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
