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
  "constant for all links holding programming pages"
  (process/html-pages "/programming"
                      (stasis/slurp-directory "resources/programming" #".*\.(html|css|js)")))
(def programming-metadata
  "constant for all programming-metadata"
  (process/make-edn-page-map "/programming" programming-map))

(def programming-links
  "constant for all programming links"
  (process/format-html-links programming-metadata))

(def science-map
  "constant for all science pages"
  (process/html-pages "/science"
                      (stasis/slurp-directory "resources/science" #".*\.(html|css|js)")))
(def science-metadata
  "constant for all science metadata"
  (process/make-edn-page-map "/science" science-map))

(def science-links
  "constant for all science links"
  (process/format-html-links science-metadata))

(def first-five-links
  "first five links to put on home page"
  (process/format-html-links (process/merge-maps-sort-take-five science-metadata programming-metadata)))

(def site-map
  "Generate site map urls"
  (apply str (for [x (keys (merge programming-metadata science-metadata))] (str "http://nickgeorge.net" x "/\n" "https://nickgeorge.net" x "/\n"))))

;; load all assets
(defn get-assets
  "get all static assets from the public directory."
  []
  (assets/load-assets "public" [#".*"]))

;; main get pages function for render and export
(defn get-pages
  "Gathers all website pages and resources, including sitemap and robots.txt."
  []
  (stasis/merge-page-sources
   {:public (stasis/slurp-directory "resources/public" #".*\.(html|css|js)$") 

    :landing  (let [home-map (process/home-page (stasis/slurp-directory "resources/home" #".*\.(html|css|js)$"))]
                (zipmap (keys home-map)
                        (map #(process/add-links % first-five-links :#recentPosts)
                             (vals home-map))))
    
    :programming  (zipmap (keys programming-map)
                          (map #(process/add-links % programming-links :#pageListDiv)
                               (vals programming-map)))

    :science (zipmap (keys science-map)
                     (map #(process/add-links % science-links :#pageListDiv)
                          (vals science-map)))

    :robots (hash-map "/robots.txt" "User-agent: *\nDisallow:\nSITEMAP: http://nickgeorge.net/sitemap.txt")

    :sitemap (hash-map "/sitemap.txt" site-map)}))

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
