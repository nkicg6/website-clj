(ns website-clj.website
  "main namespace for building and exporting the website"
  (:require [optimus.assets :as assets]
            [optimus.export]
            [optimus.optimizations :as optimizations]      
            [optimus.prime :as optimus]                    
            [optimus.strategies :refer [serve-live-assets]]
            [clojure.string :as str]
            [stasis.core :as stasis]
            [website-clj.export-helpers :as helpers]
            [website-clj.process-pages :as process]))

(def export-dir "target/nickgeorge.net")

(defn make-page-map
  "Reads all the html using `stasis/slurp-directory` and apply the html formatting
  and path formatting."
  [relative-path]
  (let [base (second (str/split relative-path #"/"))
        page-map (stasis/slurp-directory relative-path  #".*\.(html|css|js)")
        page-html (vals page-map)
        page-keys (map #(str "/" base %) (map #(str/replace % #"(?<!index)\.html$" "") (keys page-map)))]
    (->> page-html
         (map process/format-html)
         (zipmap page-keys))))

(defn home-page-header
  "Formatting for the landing/home page"
  []
  (let [hp-map (stasis/slurp-directory  "resources/home" #".*\.(html|css|js)$")
        hp-map-keys (keys hp-map)
        hp-html (vals hp-map)]
    (->> hp-html
         (map process/format-html)
         (zipmap hp-map-keys))))

(defn make-site-map
  "compile a list of all the pages for search engines."
  [sci-metadata prog-metadata]
  (apply str (for [x (keys (merge prog-metadata sci-metadata))]
               (str "http://nickgeorge.net" x "/\n" "https://nickgeorge.net" x "/\n"))))

(defn get-assets
  "get all static assets from the public directory."
  []
  (assets/load-assets "public" [#".*"]))

(defn get-pages
  "gets all pages and assets for testing and deployment"
  []
  (let [programming-map (make-page-map "resources/programming")
        prog-meta (process/make-edn-page-map programming-map)
        science-map (make-page-map "resources/science")
        sci-meta (process/make-edn-page-map science-map)
        homepage (home-page-header)
        all-links (process/merge-maps-sort-take-five prog-meta sci-meta)]
    (stasis/merge-page-sources
     {:public (stasis/slurp-directory  "resources/public" #".*\.(html|css|js)$")
      :landing (process/add-links-to-map all-links :#recentPosts homepage) 
      :programming (process/add-links-to-map (process/format-html-links prog-meta) :#pageListDiv programming-map)
      :science (process/add-links-to-map (process/format-html-links sci-meta) :#pageListDiv science-map)
      :robots (hash-map "/robots.txt" "User-agent: *\nDisallow:\nSITEMAP: http://nickgeorge.net/sitemap.txt")
      :sitemap (hash-map "/sitemap.txt" (make-site-map prog-meta sci-meta))})))

(def app
  "renders the website for experimentation"
  (optimus/wrap
   (stasis/serve-pages get-pages)
   get-assets
   optimizations/none
   serve-live-assets))

;; main export function, called by lein build-site
(defn export
  "main export function for static site. See docs for functions included."
  []
  (let [assets (optimizations/all (get-assets) {})]
    (helpers/clear-directory! export-dir)
    (optimus.export/save-assets assets export-dir)
    (stasis/export-pages (get-pages) export-dir {:optimus-assets assets})))
