(ns website-clj.website
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

(defn make-page-map
  "makes page map for a topic"
  [relative-path]
  (let [base (second (str/split relative-path #"/"))
        page-map (stasis/slurp-directory relative-path  #".*\.(html|css|js)")
        page-html (vals page-map)
        page-keys (map #(str "/" base %) (map #(str/replace % #"(?<!index)\.html$" "") (keys page-map)))]
    (->> page-html
         (map process/format-html)
        ;; other work on page-html would go here, likely one function which would be the
        ;; composed list from process-pages one.
        (zipmap page-keys))))

(defn home-page-header
  "layout and formatting for the home page"
  []
  (let [hp-map (stasis/slurp-directory  "resources/home" #".*\.(html|css|js)$")
        hp-map-keys (keys hp-map)
        hp-html (vals hp-map)]
    (->> hp-html
         (map process/format-html)
         (zipmap hp-map-keys))))

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

(defn add-links-to-map
  [links div page-map]
  (zipmap (keys page-map)
          (map #(process/add-links links div %) (vals page-map))))

(defn get-pages
  []
  (let [programming-map (make-page-map "resources/programming")
        prog-meta (process/make-edn-page-map programming-map)
        science-map (make-page-map "resources/science")
        sci-meta (process/make-edn-page-map science-map)
        homepage (home-page-header)
        all-links (process/merge-maps-sort-take-five prog-meta sci-meta)]
    (stasis/merge-page-sources
     {:public (stasis/slurp-directory  "resources/public" #".*\.(html|css|js)$")
      :landing (add-links-to-map all-links :#recentPosts homepage) 
      :programming (add-links-to-map (process/format-html-links prog-meta) :#pageListDiv programming-map)
      :science (add-links-to-map (process/format-html-links sci-meta) :#pageListDiv science-map)
      :robots (hash-map "/robots.txt" "User-agent: *\nDisallow:\nSITEMAP: http://nickgeorge.net/sitemap.txt")
      :sitemap (hash-map "/sitemap.txt" (make-site-map prog-meta sci-meta))})))

(def app
  "renders the website for experimentation"
  (optimus/wrap
   (stasis/serve-pages get-pages)
   get-assets
   optimizations/none
   serve-live-assets))

;; TODO! Write your own empty-dir! fn so you dont have to use this ugly copy hack. 
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
