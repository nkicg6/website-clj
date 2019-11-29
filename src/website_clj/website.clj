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
        page-keys (map #(str "/" base %) (keys page-map))]
    (->> page-html
         (map process/layout-base-header)
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
         (map process/layout-base-header)
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

(defn get-pages
  []
  (let [programming-map (make-page-map "resources/programming")
        science-map (make-page-map "resources/science")]
    (stasis/merge-page-sources
     {:public (stasis/slurp-directory  "resources/public" #".*\.(html|css|js)$")
      :landing (home-page-header)
      :programming programming-map
      :science science-map})))

(def app
  "renders the website for experimentation"
  (optimus/wrap
   (stasis/serve-pages get-pages)
   get-assets
   optimizations/none
   serve-live-assets))
