(ns ded.controllers.ep
  (:require [ded.db :as db]
            [hiccup.page :as p]
            [selmer.parser :as tmpl]
            [clojure.walk :refer [keywordize-keys]]
            [ring.util.response :as resp]))

(defn default-handler [req]
  (assoc-in req [:params :message]
            (str "Is this thing working?")))

(defn get-sites
  "Render the list view with all the sites in the database."
  [req]
  (let [sites (db/get-all (-> req :application/component :database))]
    (-> req
        (assoc-in [:params :sites] (vec (flatten (seq sites))))
        (assoc :application/view "list"))))

(defn edit
  "Display the add/edit form.
  If the :id parameter is present, Compojure will have coerced it to an
  int and we can use it to populate the edit form by loading that site's
  data from the the database."
  [req]
  (let [db   (-> req :application/component :database)
        site (when-let [id (get-in req [:params :site/id])]
               (db/get-site-by-id db id))]
    (-> req
        (update :params assoc
                :site site)
        (assoc :application/view "form"))))

(defn save-site
  "Save a new site into the database."
  [req]
  (let [data (-> req
                 :params
                 (keywordize-keys) ;; we have to do this as form fields come in as strings
                 (select-keys [:site/id :site/name :site/location :site/type :xt/id])
                 )]
    (db/add-site (-> req :application/component :database) data)
    (resp/redirect "/sites/list")))

(defn render-page
  [req]
  (let [data (:params req) ;; just passing through the data
        view (:application/view req "default")
        html (tmpl/render-file (str "views/sites/" view ".html") data)]
    (-> (resp/response (tmpl/render-file "layouts/default.html"
                                         (assoc data :body [:safe html])))
        (resp/content-type "text/html"))))
