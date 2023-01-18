(ns ded.controllers.ep
  (:require [ded.db :as db]
            [hiccup.page :as p]
            [selmer.parser :as tmpl]
            [ring.util.response :as resp]))

(defn default-handler [req]
  (assoc-in req [:params :message]
            (str "Is this fucking thing working?")))

(defn get-sites
  "Render the list view with all the sites in the database."
  [req]
  (let [sites (db/get-all (-> req :application/component :database))]
    (-> req
        (assoc-in [:params :sites] sites)
        (assoc :application/view "list")))
  )

(defn render-page
  "Each handler function here adds :application/view to the request
  data to indicate which view file they want displayed. This allows
  us to put the rendering logic in one place instead of repeating it
  for every handler."
  [req]
  (let [data {:sites (-> req :params :sites)}
        view (:application/view req "default")
        html (tmpl/render-file (str "views/sites/" view ".html") data)]
    (-> (resp/response (tmpl/render-file "layouts/default.html"
                                         (assoc data :body [:safe html])))
        (resp/content-type "text/html"))))


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
         (resp/content-type "text/html"))))
