;; too complicated. Read all the pages in as raw html, apply all formatting once merged.q
;; last step should be applying the header to ALL pages.

(ns website-clj.website
  "main namespace for building and exporting the website"
  (:require [clojure.string :as str]
            [stasis.core :as stasis]
            [hiccup.core :refer [html]]
            [hiccup.page :refer [html5 include-css include-js]]
            [hiccup.element :refer [link-to]]
            [digest :as digest]
            [net.cgrand.enlive-html :as enlive]
            [clojure.edn :as edn]
            [website-clj.export-helpers :as helpers]))

(def export-dir "target/nickgeorge.net")


(defn get-copyright-date []
  (.format (java.text.SimpleDateFormat. "yyyy")
           (new java.util.Date)))

(defn cache-bust-css [path]
  "hash css file value, rename file with first 8 digist of digest. Return a map of css-renmaed-path, css-val"
  (let [css-map (stasis/slurp-directory path #".*\.(css)")
        css-vals (vals css-map)
        css-keys (keys css-map)
        md5-hashes (map digest/md5 css-vals)
        short-hash  (map #(apply str (take 8 %)) md5-hashes)
        new-keys (map #(str/replace %1 ".css" (str "-" %2 ".css")) css-keys short-hash)]
    (zipmap new-keys css-vals)))

(defn apply-header-footer
  "Applies a header and footer and css to an html strings."
  [css-seq page]
  (html5 {:lang "en"}
         [:head
          #_[:script {:src "https://www.googletagmanager.com/gtag/js?id=UA-124749948-1" :async "async"}]
          #_[:script google-analytics]
          [:title "Nick's site"]
          [:meta {:charset "utf-8"}]
          [:meta {:name "viewport"
                  :content "width=device-width, initial-scale=1.0"}]
          (for [css css-seq] [:link {:type "text/css", :href css :rel "stylesheet"}])]
         [:body
          [:div {:class "header"}
           [:div {:class "name"}
            [:a {:class "name" :href "/"} "Nick George"]
            [:div {:class "header-right"}
             [:a {:href "/science"} "Science"]
             [:a {:href "/programming"} "Programming"]]]]
          page]
         [:footer
          [:p (str "&copy Nick George 2017-") (get-copyright-date)]]))


(defn parse-edn
  "returns edn metadata for page-text or nil"
  [page-text]
(edn/read-string
 (apply str
        (enlive/select (enlive/html-snippet page-text) [:#edn enlive/text-node]))))



(defn get-pages
  "gets all pages and assets for website"
  []
  (let [all-pages-map (stasis/slurp-directory "resources/" #".*\.html$")
        all-pages-keys (keys all-pages-map)
        css-hashed (cache-bust-css "resources/public") ;; keys needed for later
        css-keys (keys css-hashed)
        header-footer-partial (partial apply-header-footer css-keys) ;; apply first arg for hashed css names
        all-pages-vals (map header-footer-partial (vals all-pages-map)) ;; map over pages
        edn-all (map parse-edn all-pages-vals)]
    
    (stasis/merge-page-sources
     {:pages
      (zipmap all-pages-keys all-pages-vals)
      :css css-hashed
      :img (stasis/slurp-directory "resources/public" #".*\.(png|jpg)$")})))


(defn make-site-map
  "compile a list of all the pages for search engines."
  [sci-metadata prog-metadata]
  (apply str (for [x (keys (merge prog-metadata sci-metadata))]
               (str "http://nickgeorge.net" x "/\n" "https://nickgeorge.net" x "/\n"))))


;; main export function, called by lein build-site
(defn export
  "main export function for static site. See docs for functions included."
  []
  (helpers/clear-directory! export-dir)
  (stasis/export-pages (get-pages) export-dir))

;;;; Scratch/repl play ;;;;


(def app
  "preview app"
  (stasis/serve-pages get-pages))
;; TODO apply html formatting, title adding, etc.
;; TODO fix fns that add links to landing pages
;; In make-page-map you only need a :pages, :css, and :images
