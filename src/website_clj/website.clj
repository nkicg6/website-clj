;; Have to find a simpler way to handle links and adding links to the science and archive pages.
;; Adding those links to programming, science, and homepage are the main steps left.
;; and making the other pages are the only steps left
;; more room for improvement would be stopping the multiple parses on the title adding part...

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


(get {"science" "science-page" "programming" "programming-page"} "science")


(defn fmt-page-html [page]
  (-> page
      (str/replace #"<img src=.*/img" "<img src=\"/img")
      (str/replace #"<h2>Table of Contents</h2>" "<h1>&gt contents</h1>")))

(defn parse-html
  "Takes raw html and returns keys from edn metadata under the <div id='edn'> html tag
  `html` is raw html"
  [html]
  (as-> html raw-text
    (enlive/html-snippet raw-text)
    (enlive/select raw-text [:#edn enlive/text-node])
    (apply str raw-text)
    (edn/read-string raw-text)))

(defn insert-page-title
  "insert-page-title parses edn metadata and return the html with a title inserted
  `page` is the raw HTML of a page including the header."
  [page]
  (let [meta-title (get (parse-html page) :title "Nick's site")]
    (-> page
        (enlive/sniptest [:title]
                         (enlive/html-content meta-title)))))

(defn get-pages
  "gets all pages and assets for website"
  []
  (let [all-pages-map (stasis/slurp-directory "resources/" #".*\.html$")
        all-pages-keys (keys all-pages-map)
        css-hashed (cache-bust-css "resources/public") ;; keys needed for later
        css-keys (keys css-hashed)
        header-footer-partial (partial apply-header-footer css-keys) ;; apply first arg for hashed css names
        all-pages-vals (->> (vals all-pages-map)
                            (map header-footer-partial)
                            (map fmt-page-html)
                            (map insert-page-title)) ;; all html formatting done here
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
;; TODO fix fns that add links to landing pages
