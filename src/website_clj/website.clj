;; Have to find a simpler way to handle links and adding links to the science and archive pages.
;; Adding those links to programming, science, and homepage are the main steps left.
;; and making the other pages are the only steps left
;; more room for improvement would be stopping the multiple parses on the title adding part...

(ns website-clj.website
  "main namespace for building and exporting the website"
  (:require [clojure.string :as str]
            [stasis.core :as stasis]
            [hiccup.core :refer [html]]
            [hiccup.page :refer [html5]]
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
  "md5 hash css text, rename file with first 8 digist of digest. Return a map of css-renmaed-path, css-val."
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
          page] ; put main content here
         [:footer
          [:p (str "&copy Nick George 2017-") (get-copyright-date)]]))

(defn parse-edn
  "returns edn metadata for page-text or nil"
  [page-text]
(edn/read-string
 (apply str
        (enlive/select (enlive/html-snippet page-text) [:#edn enlive/text-node]))))


(defn fmt-page-html [page]
  (-> page
      (str/replace #"<img src=.*/img" "<img src=\"/img")
      (str/replace #"<h2>Table of Contents</h2>" "<h1>&gt contents</h1>")))

(defn insert-page-title
  "insert-page-title parses edn metadata and return the html with a title inserted
  `page` is the raw HTML of a page including the header."
  [page]
  (let [meta-title (get (parse-edn page) :title "Nick's site")]
    (-> page
        (enlive/sniptest [:title]
                         (enlive/html-content meta-title)))))

(defn reverse-sort-by-date
  [metadata-map]
  (html [:ul (for [[k v] (reverse (sort-by #(get-in (val %) [:date]) metadata-map))] ;; reverse chrono order
               [:li (link-to k (get v :title)) (str "<em> Published: " (get v :date) "</em>")])]))

(defn metadata-to-links
  "build html list from a vec of metadata dicts"
  [metadata]
  (html [:ul (for [k metadata]
               [:li (link-to (get k :path) (get k :title))
                (str "<em> Published: " (get k :date) "</em>")])]))


(defn reverse-chrono
  [v-map]
  (reverse (sort-by :date v-map)))

(defn filter-metadata-topic
  "select only items from metadata vector `v` which match `topic`"
  [topic v]
  (filter #(= topic (:topic %)) v))

(defn get-pages!
  "read pages from disk and separate them into a map for further processing"
  [path]
  (let [all-pages-map (stasis/slurp-directory path #".*\.html$")
        landing-index (get all-pages-map "/index.html")
        science-index (get all-pages-map "/science/index.html")
        programming-index (get all-pages-map "/programming/index.html")
        no-index-map (apply dissoc all-pages-map ["/index.html" "/science/index.html"
                                                  "/programming/index.html"])
        no-index-metadata (reverse-chrono (map #(assoc %1 :path %2)
                                               (map parse-edn (vals no-index-map))
                                               (keys no-index-map)))]
    {:landing landing-index
     :sci-index science-index
     :prog-index programming-index
     :pages no-index-map
     :metadata no-index-metadata}))

(defn fmt-links
  "formats and adds links to homepages"
  [page-map]
  (let [{homepage :landing
         sci-home :sci-index
         prog-home :prog-index
         pages :pages
         metadata :metadata} page-map
        recent-five-links (metadata-to-links (take 5 metadata))
        sci-links (->> metadata
                       (filter-metadata-topic "science")
                       (metadata-to-links))
        prog-links (->> metadata
                       (filter-metadata-topic "programming")
                       (metadata-to-links))]))


#_(defn get-pages
  "gets all pages and assets for website"
  []
  (let [all-pages-map (stasis/slurp-directory "resources/" #".*\.html$")
        all-pages-keys (keys all-pages-map)
        css-hashed (cache-bust-css "resources/public") ;; keys needed for later
        css-keys (keys css-hashed)
        header-footer-partial (partial apply-header-footer css-keys) ;; apply arg for css vec first
        all-metadata-map (zipmap all-pages-keys (map parse-edn (vals all-pages-map)))
        homepage-links-partial (partial add-homepage-links (reverse-sort-by-date all-metadata-map))
        all-pages-vals (->> (vals all-pages-map)
                            (map header-footer-partial)
                            (map fmt-page-html)
                            (map insert-page-title)
                            (map homepage-links-partial))
]
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

;; TODO tasks for website (in no particular order)
;; - read all files with `slurp-directory`.
;; - apply header with renamed css
;; - fmt html
;; - parse edn add title to pages (make a map of path and edn content?)
;; - make list of links for science and programming page (use :topic = index and :title Programming archive orScience archive to sort/select)

(def test-page (slurp "resources/programming/index.html"))

;; I get it, everything is relative to resources/ for enlive?

(enlive/deftemplate index "programming/index.html" [p] [:div#pageListDiv] (enlive/content p))

(index "test stuff")
