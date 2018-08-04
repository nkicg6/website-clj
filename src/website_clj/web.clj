;; main page for my clojure static website.
;; based on https://cjohansen.no/building-static-sites-in-clojure-with-stasis/

(ns website-clj.web
  (:require [optimus.assets :as assets]
            [optimus.export]
            [optimus.link :as link] 
            [optimus.optimizations :as optimizations]      
            [optimus.prime :as optimus]                    
            [optimus.strategies :refer [serve-live-assets]]
            [clojure.java.io :as io]
            [clojure.string :as str]
            [stasis.core :as stasis]
            [website-clj.export-helpers :as helpers]
            [website-clj.process-pages :as process]))



(defn prepare-page [page req]
  (if (string? page) page (page req)))

(defn get-assets []
  (assets/load-assets "public" [#".*"]))


(defn get-pages []
  (stasis/merge-page-sources
   {:landing (process/home-page (stasis/slurp-directory "resources/home" #".*\.(html|css|js)$"))
    :programming  (process/html-pages "/programming" (stasis/slurp-directory "resources/programming" #".*\.html$"))
    :science (process/html-pages "/science" (stasis/slurp-directory "resources/science" #".*\.html$"))
    :partials (process/partial-pages (stasis/slurp-directory "resources/partials" #".*\.html$"))
    :public (stasis/slurp-directory "resources/public" #".*\.(html|css|js)$")}))


(def app
  (optimus/wrap
   (stasis/serve-pages get-pages)
   get-assets
   optimizations/none
   serve-live-assets))

(def export-dir "target/nickgeorge.net")

(def safe-dir "target")

(defn export []
  (helpers/save-git safe-dir export-dir)
  (let [assets (optimizations/all (get-assets) {})]
    (stasis/empty-directory! export-dir)
    (optimus.export/save-assets assets export-dir)
    (stasis/export-pages (get-pages) export-dir {:optimus-assets assets}))
  (helpers/cp-cname export-dir)
  (helpers/cp-gitignore export-dir)
  (helpers/replace-git safe-dir export-dir))

