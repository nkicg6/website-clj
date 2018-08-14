;; main page for my clojure static website.
;; based on https://cjohansen.no/building-static-sites-in-clojure-with-stasis/

(ns website-clj.web
  "main namespace for building and exporting the website"
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


;; define page maps and link maps

(def programming-map
  (process/html-pages "/programming"
                      (stasis/slurp-directory "resources/programming" #".*\.(html|css|js)")))
(def programming-metadata
  (process/parse-edn "/programming" programming-map))

(def programming-links
  (process/format-html-links programming-metadata))

(def science-map
  (process/html-pages "/science"
                      (stasis/slurp-directory "resources/science" #".*\.(html|css|js)")))
(def science-metadata
  (process/parse-edn "/science" science-map))

(def science-links
  (process/format-html-links science-metadata))

;; load all assets
(defn get-assets
  "get all static assets from the public directory."
  []
  (assets/load-assets "public" [#".*"]))

;; main get pages function for render and export
(defn get-pages
  "Gathers all website pages and resources."
  []
  (stasis/merge-page-sources
   {:public (stasis/slurp-directory "resources/public" #".*\.(html|css|js)$") 
    :landing (process/home-page
              (stasis/slurp-directory "resources/home" #".*\.(html|css|js)$"))
    :programming  (zipmap (keys programming-map)
                          (map #(process/add-links % programming-links)
                               (vals programming-map)))
    :science (zipmap (keys science-map)
                     (map #(process/add-links % science-links)
                          (vals science-map)))}))

;; for test rendering
(def app
  "renders the website for experimentation"
  (optimus/wrap
   (stasis/serve-pages get-pages)
   get-assets
   optimizations/none
   serve-live-assets))

;; constants for exporting
(def export-dir "target/nickgeorge.net")
(def safe-dir "target")

;; main export function, called by lein build-site
(defn export
  "main export function for static site. See docs for functions included.
  `website-clj.helpers/save-git`
  `website-clj.helpers/cp-cname`
  `website-clj.helpers/cp-gitignore`
  `website-clj.helpers/replace-git`"
  []
  (helpers/save-git safe-dir export-dir)
  (let [assets (optimizations/all (get-assets) {})]
    (stasis/empty-directory! export-dir)
    (optimus.export/save-assets assets export-dir)
    (stasis/export-pages (get-pages) export-dir {:optimus-assets assets}))
  (helpers/cp-cname export-dir)
  (helpers/cp-gitignore export-dir)
  (helpers/replace-git safe-dir export-dir))
