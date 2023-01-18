(ns ded.controllers.ep
  (:require [ded.db :as db]
            [hiccup.page :as p]
            [ring.util.response :as resp]))

(defn default-handler [req]
  (assoc-in req [:params :message]
            (str "Is this fucking thing working?")))

(defn render-page [response]
  {:body "nonce!"})

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
