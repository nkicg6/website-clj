;; too complicated. Read all the pages in as raw html, apply all formatting once merged.q
;; last step should be applying the header to ALL pages.

(ns website-clj.website
  "main namespace for building and exporting the website"
  (:require [clojure.string :as str]
            [stasis.core :as stasis]
            [digest :as digest]
            [net.cgrand.enlive-html :as enlive]
            [clojure.edn :as edn]
            [website-clj.export-helpers :as helpers]
            [website-clj.process-pages :as process]))

(def export-dir "target/nickgeorge.net")

(defn remove-temps [m]
  (apply dissoc m (filter #(re-matches #".*\.html~" %) (keys m))))

(defn make-page-map
  "Reads all the html using `stasis/slurp-directory` and apply the html formatting
  and path formatting."
  [relative-path]
  (let [base (second (str/split relative-path #"/"))
        page-map (remove-temps (stasis/slurp-directory relative-path  #".*\.(html|css|js)"))
        page-html (vals page-map)
        page-keys (map #(str "/" base %) (map #(str/replace % #"(?<!index)\.html$" "") (keys page-map)))]
    (->> page-html
         (map process/format-html)
         (zipmap page-keys))))

(defn cache-bust-css [path]
  "hash css file value, rename file with first 8 digist of digest. Return a map of css-renmaed-path, css-val"
  (let [css-map (stasis/slurp-directory path #".*\.(css)")
        css-vals (vals css-map)
        css-keys (keys css-map)
        md5-hashes (map digest/md5 css-vals)
        short-hash  (map #(apply str (take 8 %)) md5-hashes)
        new-keys (map #(str/replace %1 ".css" (str "-" %2 ".css")) css-keys short-hash)]
    (zipmap new-keys css-vals)))

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
     {:public (stasis/slurp-directory  "resources/public" #".*\.(html|css|js|png|jpg)$")
      :landing (process/add-links-to-map all-links :#recentPosts homepage) 
      :programming (process/add-links-to-map (process/format-html-links prog-meta) :#pageListDiv programming-map)
      :science (process/add-links-to-map (process/format-html-links sci-meta) :#pageListDiv science-map)
      :robots (hash-map "/robots.txt" "User-agent: *\nDisallow:\nSITEMAP: http://nickgeorge.net/sitemap.txt")
      :sitemap (hash-map "/sitemap.txt" (make-site-map prog-meta sci-meta))})))


;; main export function, called by lein build-site
(defn export
  "main export function for static site. See docs for functions included."
  []
  (helpers/clear-directory! export-dir)
  (stasis/export-pages (get-pages) export-dir))

#_(def app
  "preview app"
  (stasis/serve-pages get-pages))

;;;; Scratch/repl play ;;;;

(defn parse-edn
  "returns edn metadata for page-text or nil"
  [page-text]
(edn/read-string
 (apply str
        (enlive/select (enlive/html-snippet page-text) [:#edn enlive/text-node]))))



(defn get-pages-simple
  "gets all pages and assets for website"
  []
  (let [all-pages-map (stasis/slurp-directory "resources/" #".*\.html$")
        all-pages-keys (keys all-pages-map)
        all-pages-vals (map process/apply-header-footer (vals all-pages-map))
        edn-all (map parse-edn all-pages-vals)]
    (zipmap all-pages-keys all-pages-vals)
    ))


(get-pages-simple)

(def app
  "preview app"
  (stasis/serve-pages get-pages-simple))
;; TODO apply html formatting, title adding, etc.
;; TODO fix fns that add links to landing pages
;; In make-page-map you only need a :pages, :css, and :images
