(ns sites.core
  (:require [xtdb.api :as xt]))

(def node (xt/start-node {}))

(defn stop-xtdb! []
  (.close node))

(def site1 {:xt/id :mammoth-site
            :site/name "Mammoth Site"
            :site/type :submarine-base
            :site/id 100
            :site/location "Hopwirth Flogash"})

(def site2 {:xt/id :hockle-site
            :site/name "Hockle Site"
            :site/type :submarine-base
            :site/id 101
            :site/location "Grimthorpe Jyres"})

(defn add-sites [v]
  (map (fn [s]
         (xt/submit-tx node
                       [[::xt/put s]])) v))

(defn get-site-by-id
  [id]
  (xt/q (xt/db node)
        '{:find [e]
          :in [?id]
          :where [[e :site/id ?id]]}
        id))

(defn get-site-ids []
  (xt/q (xt/db node)
        '{:find  [xtid id name]
          :where [[e :site/id id]
                  [e :xt/id xtid]
                  [e :site/name name]]}))

(defn get-all [](xt/q (xt/db node)
                      '{:find [(pull e [*])]
                        :where [[e :xt/id]]}))

(comment
  (xt/sync node)
  (stop-xtdb!)
  (add-sites [site1 site2])
  (get-site-by-id 101)
  (get-all)
  (get-site-ids)
  )
