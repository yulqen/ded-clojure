(ns ded.db
  (:require [xtdb.api :as xt]
            [clojure.java.io :as io]
            [clojure.string :as str])
  (:import [java.util UUID]))

(defn start-xtdb! []
  (letfn [(kv-store [dir]
            {:kv-store {:xtdb/module 'xtdb.rocksdb/->kv-store
                        :db-dir (io/file dir)
                        :sync? true}})]
    (xt/start-node
     {:xtdb/tx-log (kv-store "data/dev/tx-log")
      :xtdb/document-store (kv-store "data/dev/doc-store")
      :xtdb/index-store (kv-store "data/dev/index-store")})))


;; (defn start-xtdb! []
;;   (xt/start-node {}))

(def xtdb-node (start-xtdb!))
;; note that attempting to eval this expression more than once before first calling `stop-xtdb!` will throw a RocksDB locking error
;; this is because a node that depends on native libraries must be `.close`'d explicitly

(defn stop-xtdb! []
  (.close xtdb-node))

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


;; We need our xt/id fields to be automatically generated.
;; Schema ideas:
;; AA10000 - where AA is type identifier and 10000 automatically increments
;; Split UUID - can be generated easily but are unweildy presentationally
;; see below:

(str/join "-" (take 2 (str/split (.toString (UUID/randomUUID)) #"-")))

(defn add-site [component-node site]
  (xt/submit-tx component-node
                [[::xt/put
                  site]]))

(defn get-site-by-id
  [id component-node]
  (xt/q (xt/db component-node)
        '{:find [e]
          :in [?id]
          :where [[e :site/id ?id]]}
        id))

(defn get-site-ids [component-node]
  (xt/q (xt/db component-node)
        '{:find  [xtid id name]
          :where [[e :site/id id]
                  [e :xt/id xtid]
                  [e :site/name name]]}))

(defn get-all [component-node]
  (xt/q (xt/db component-node)
        '{:find [(pull e [*])]
          :where [[e :xt/id]]}))

(defn delete-entity [component-node xtid]
  (xt/submit-tx component-node
                [[::xt/delete xtid]]))

(comment
  (get-site-by-id 101 xtdb-node)
  (get-site-ids)
  ,)
