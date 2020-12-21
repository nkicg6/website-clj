(ns website-clj.website
  "main namespace for building and exporting the website"
  (:require [clojure.string :as str]
            [me.raynes.fs :as fs]
            [stasis.core :as stasis]
            [optimus.assets :as assets]
            [hiccup.core :refer [html]]
            [hiccup.page :refer [html5]]
            [hiccup.element :refer [link-to]]
            [digest :as digest]
            [net.cgrand.enlive-html :as enlive]
            [clojure.edn :as edn]
            [clygments.core :as clygments]))

(def export-dir "/Users/nick/personal_projects/nkicg6.gitlab.io/public")

(defn highlight
  [enlive-node]
  (let [code (->> enlive-node :content (apply str))
        lang (keyword (str/replace (->> enlive-node :attrs :class) "src src-" ""))
        hl-code (clygments/highlight code lang :html)]
    (enlive/html-snippet hl-code)))

(defn highlight-code [page]
  (enlive/sniptest page
                   [:div.org-src-container :pre] highlight
                   [:div.org-src-container :pre] #(assoc-in % [:attrs :class] "codehilite")))

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
          [:title "Nick's site"]
          [:script "var clicky_site_ids = clicky_site_ids || []; clicky_site_ids.push(101287299);"]
          [:script {:src "//static.getclicky.com/js"}]
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
          (highlight-code page)] ; put main content here
         [:footer
          [:p (str "&copy Nick George 2017-") (get-copyright-date)]]))

(defn get-assets []
  (assets/load-assets "public/"))

(defn parse-edn
  "returns edn metadata for page-text or nil"
  [page-text]
(edn/read-string
 (apply str
        (enlive/select (enlive/html-snippet page-text) [:#edn enlive/text-node]))))

(defn enlive-insert-links
  [page links]
  (-> page
      (enlive/sniptest [:div#pageListDiv]
                       (enlive/html-content links))))

(defn insert-page-title
  "insert-page-title parses edn metadata and return the html with a title inserted
  `page` is the raw HTML of a page including the header."
  [page]
  (let [meta-title (get (parse-edn page) :title "Nick's site")]
    (-> page
        (enlive/sniptest [:title]
                         (enlive/html-content meta-title)))))

(defn fmt-page-html [page]
  (-> page
      (str/replace #"<img src=.*/img" "<img src=\"/img")
      (str/replace #"<h2>Table of Contents</h2>" "<h1>&gt contents</h1>")))

(defn metadata-to-links
  "build html list from a vec of metadata dicts"
  [m]
  (html [:ul (for [k m]
               [:li (link-to (get k :path) (get k :title))
                (str "<em> Published: " (get k :date) "</em>")])]))

(defn reverse-chrono
  [v-map]
  (reverse (sort-by :date v-map)))

(defn filter-metadata-topic
  "select only items from metadata vector `v` which match `topic`"
  [topic v]
  (filter #(= topic (:topic %)) v))

(defn make-site-map
  "compile a list of all the pages for search engines."
  [v]
  (apply str (for [x v]
               (str "https://nickgeorge.net" x "\n"))))

(defn get-pages!
  "read pages from disk and separate them into a map for further processing"
  [path]
  (let [all-pages-map (stasis/slurp-directory path #".*\.html$")
        landing-index (get all-pages-map "/index.html")
        science-index (get all-pages-map "/science/index.html")
        programming-index (get all-pages-map "/programming/index.html")
        no-index-map (apply dissoc all-pages-map ["/index.html" "/science/index.html"
                                                  "/programming/index.html"])
        fmt-keys-no-index (map #(str/replace % #"(?<!index)\.html$" "/") (keys no-index-map))
        no-index-metadata (reverse-chrono (map #(assoc %1 :path %2)
                                               (map parse-edn (vals no-index-map))
                                               fmt-keys-no-index))]
    {:landing landing-index
     :sci-index science-index
     :prog-index programming-index
     :pages (zipmap fmt-keys-no-index (vals no-index-map))
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
                       (metadata-to-links))
        home (enlive-insert-links homepage recent-five-links)
        sci (enlive-insert-links sci-home sci-links)
        prog (enlive-insert-links prog-home prog-links)]
    (stasis/merge-page-sources {:pages pages 
                                :home {"/index.html" home}
                                :prog-home {"/programming/index.html" prog}
                                :sci-home {"/science/index.html" sci}})))

(defn fmt-pages
  "applies header/footer and css, returns site"
  [m]
  (let [css-hashed (cache-bust-css "resources/public")
        css-keys (keys css-hashed)
        header-footer-partial (partial apply-header-footer css-keys) ;; apply css vec arg first
        all-page-keys (keys m)
        all-pages (->> (vals m)
                      (map header-footer-partial)
                      (map fmt-page-html)
                      (map insert-page-title)
                      (zipmap all-page-keys))]
    (stasis/merge-page-sources
     {:pages all-pages
      :css css-hashed
      :robots {"/robots.txt" "User-agent: *\nDisallow:\nSITEMAP: http://nickgeorge.net/sitemap.txt"}
      :sitemap {"/sitemap.txt" (make-site-map all-page-keys)}})))

(defn make-site!
  "main site building"
  []
  (-> (get-pages! "resources")
     (fmt-links)
     (fmt-pages)))

(def app
  "preview app. images are not rendering..."
  (stasis/serve-pages make-site!))

;; main export function, called by lein build-site

(defn export
  "main export function for static site."
  []
  (stasis/export-pages (make-site!) export-dir))
