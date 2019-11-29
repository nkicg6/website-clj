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

(defn make-page-map
  "makes page map for a topic"
  [base-name relative-path]
  (process/html-pages base-name
                      (stasis/slurp-directory relative-path  #".*\.(html|css|js)")))

(defn get-links-and-metadata
  [base-name page-map-output]
  (let [metad (process/make-edn-page-map base-name page-map-output) links (process/format-html-links metad)]
    {:metadata metad :links links}))

(defn get-first-five-links
  [sci-metadata prog-metadata]
  (process/format-html-links
   (process/merge-maps-sort-take-five sci-metadata prog-metadata)))

(defn make-site-map
  [sci-metadata prog-metadata]
  (apply str (for [x (keys (merge prog-metadata sci-metadata))]
               (str "http://nickgeorge.net" x "/\n" "https://nickgeorge.net" x "/\n"))))

(defn get-assets
  "get all static assets from the public directory."
  []
  (assets/load-assets "public" [#".*"]))

;; main get pages function for render and export
(defn get-pages
  "Gathers all website pages and resources, including sitemap and robots.txt."
  []
  ;; big let here, so I don't have to re-load for everything. will get rid of the other lets in the keys.
  (let [programming-map  (make-page-map "/programming" "resources/programming")
        programming-links-and-meta (get-links-and-metadata "/programming" "resources/programming")
        science-map (make-page-map "/science" "resources/science")
        science-links-and-meta (get-links-and-metadata "/science" "resources/science")]
   (stasis/merge-page-sources
    {:public (stasis/slurp-directory "resources/public" #".*\.(html|css|js)$") 

     :landing  (let [home-map (process/home-page (stasis/slurp-directory "resources/home" #".*\.(html|css|js)$"))]
                 (zipmap (keys home-map)
                         (map #(process/add-links % (get-first-five-links (:metadata programming-links-and-meta)
                                                                          (:metadata science-links-and-meta)) :#recentPosts)
                              (vals home-map))))
     
     :programming  (zipmap (keys programming-map)
                           (map #(process/add-links % (:links programming-links-and-meta) :#pageListDiv)
                                (vals programming-map)))

     :science (zipmap (keys science-map)
                      (map #(process/add-links % (:links science-links-and-meta) :#pageListDiv)
                           (vals science-map)))

     :robots (hash-map "/robots.txt" "User-agent: *\nDisallow:\nSITEMAP: http://nickgeorge.net/sitemap.txt")

     :sitemap (hash-map "/sitemap.txt" (make-site-map  (:metadata programming-links-and-meta) (:metadata science-links-and-meta)))})))

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
