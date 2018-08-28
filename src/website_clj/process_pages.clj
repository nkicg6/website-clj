;; also, add recent posts to home page! previous five?
;; another metadata thing could be the first 50 words of the post. very easy to work with this now. 
;; TODO! add enlive to add a title to the head of the doc using the :title metadata
(ns website-clj.process-pages
  (:require [clojure.string :as str]
            [hiccup.core :refer [html]]
            [hiccup.page :use [html5 include-css include-js]]
            [hiccup.element :refer [link-to image javascript-tag]]
            [net.cgrand.enlive-html :as enlive]
            [clojure.edn :as edn] 
            [stasis.core :as stasis]))  ;; only for testing?

;; --- google analytics ---
(def google-analytics (str "window.dataLayer = window.dataLayer || [];"
                           "function gtag(){dataLayer.push(arguments);}"
                           "gtag('js', new Date());"
                           "gtag('config', 'UA-124749948-1');"))

;; --- basic html formatting ---

;;header
(defn layout-base-header
  "Applies a header and footer to html strings."
  [page]
  (html5 {:lang "en"}
         [:head
          [:script {:src "https://www.googletagmanager.com/gtag/js?id=UA-124749948-1" :async "async"}]
          [:script google-analytics]
          [:title "Nick's site"]
          [:meta {:charset "utf-8"}]
          [:meta {:name "viewport"
                  :content "width=device-width, initial-scale=1.0"}]
          [:link {:rel "stylesheet" :href "https://maxcdn.bootstrapcdn.com/bootstrap/3.3.7/css/bootstrap.min.css"}]
          [:link {:rel "stylesheet" :href "https://cdnjs.cloudflare.com/ajax/libs/font-awesome/4.7.0/css/font-awesome.min.css"}] 
          (include-css "/css/custom.css") 
          [:script {:src "https://maxcdn.bootstrapcdn.com/bootstrap/3.3.7/js/bootstrap.min.js" :integrity "sha384-Tc5IQib027qvyjSMfHjOMaLkfuWVxZxUPnCJA7l2mCWNIpG9mGCD8wGNIcPD7Txa" :crossorigin "anonymous"}]
          ]
         [:body
          [:nav {:class "navbar navbar-inverse"}
           [:div {:class "container-fluid"}
            [:div {:class "navbar-header"}
             (link-to  {:class "navbar-brand"} "/" "Nick George")]
            [:ul {:class "nav navbar-nav navbar-right"}
             [:li {:class "inactive"} (link-to "/science" "Science")]
             [:li {:class "inactive"} (link-to "/programming" "Programming")]
             [:li [:a {:href "https://github.com/nkicg6"}
                   [:span {:class "fa fa-github" :style "font-size:24px"}]]]
             [:li [:a {:href "https://twitter.com/NicholasMG"}
                   [:span {:class "fa fa-twitter-square" :style "font-size:24px"}]]]]]]
          [:div {:class "container"}
           [:div.body {:style "font-size:18px"} page]]
          [:footer {:class "footer"}
           [:div {:class "text-center"}
            [:span {:class "text-muted"} "&copy; Nick George 2017-2018"]]]]))

(defn format-images [html]
  "formats html image link to appropriately link to static website image directory.
  `html` is a raw html string."
  (str/replace html #"src=\"img" "src=\"/img"))


(defn format-html 
  "Composed function to apply multiple html processing steps to raw html.
  `html` is a raw html string."
  [html]
  (-> html
      (format-images))) ;; other fns for html here

(defn fmt-page-names
  "removes .html from all non-index.html pages.
  `base-name` is whatever base name you want the string to have prepended to it. 
  `name` is a string."
  [base-name name]
  (str base-name
       (str/replace name #"(?<!index)\.html$" "")))

(defn home-page
  "Applies `labout-base-header` to a map of `:page-names page-html`
  `pages` is typically a map created by the function `stasis/slurp-directory`"
  [pages]
  (zipmap (keys pages)
          (map #(layout-base-header %) (map #(format-html %) (vals pages)))))

;; big fn
(defn html-pages
  "Composed function that performs html formatting to a map of strings for my blog.
  The argument `base-name` is a new string that will be prepended to all keys in the 
  `page-map` map argument. `page-map` is a map created by the function `stasis/slurp-directory`. 
  The purpose of `html-pages` is to apply formatting to html pages meant for different sections
  of my website. For instance, calling `html-pages` with '/programming' and the a map of pages will prepend 
  '/programming/<page-name>' to every key in the map and strip the html end off all non-index pages."
  [base-name page-map]
  (zipmap (map #(fmt-page-names base-name %) (keys page-map))
          (map #(layout-base-header %) (map #(format-html %) (vals page-map)))))


;; --- edn parsing for metadata---

;; remove index page
(defn remove-index
  "Removes /index.html from map that will be parsed for edn metadata.
  `base-name` is the name prepended to the index.html page. For programming pages it will be '/programming'
  `page-map` is the map returned by `html-pages`. returns `page-map` minus the index pages."
  [base-name page-map]
  (dissoc page-map (str base-name "/index.html")))

(defn parse-html
  "Takes raw html and returns keys from edn metadata under the <div id='edn'> html tag
  `html` is raw html"
  [html]
  (-> html
      (enlive/html-snippet)
      (enlive/select [:#edn enlive/text-node])
      (->> (apply str)) ;; I know this is bad form, but it is the best way I know how to do it..
      (edn/read-string)))

(defn make-edn-page-map
  "filters the `page-map` to remove index.html and returns a map of page names and edn metadata.
  `page-map` is returned by `stasis/slurp-directory`. 
  `base-name` provides the prepended base for the directory you are filtering by with `remove-index`"
  [base-name page-map]
  (let [filtered-page-map (remove-index base-name page-map)]
    (zipmap (keys filtered-page-map)
            (map parse-html (vals filtered-page-map)))))

;; --- make links to insert ---

(defn format-html-links
  "Makes a list of links in reverse chronological order using hiccup markup.
  `metadata-map`comes from the output of `make-edn-page-map`"
  [metadata-map]
  (html [:ul (for [[k v] (reverse (sort-by #(get-in (val %) [:date]) metadata-map))] ;; reverse chrono order
               [:li (link-to k (get v :title)) (str "<em> Published: " (get v :date) "</em>")])]))

(defn insert-page-title
  "`insert-page-title` parses edn metadata and return the html with a title inserted
  `page` is the raw HTML of a page including the header."
  [page]
  (let [meta-title (get (parse-html page) :title "Nick's site")]
    (-> page
        (enlive/sniptest [:title]
                         (enlive/html-content meta-title)))))

;; --- insert links ---

(defn add-links
  "Adds links of all pages to the index.html page and un-escapes html characters. 
  The `page` argument is the html for a page. 
  The `links` argument is an html string, typically generated with the `make-links` function 
  This returns the modified html"
  [page links]
  (-> page
      (enlive/sniptest
       [:#pageListDiv] ;; exists only in index pages. 
       (enlive/html-content links))))


;; -- TESTING BELOW --
;; first step is slurping a directory, applying the path prefix and formatting html.

(def slurped-raw
  "holds a map of formatted html pages for my website"
  (html-pages "/test" (stasis/slurp-directory "resources/test" #".*\.(html|css|js)")))
;; (keys slurped-raw)
(def test-html (layout-base-header (second (vals slurped-raw))))

(parse-html test-html)


;;(rename-page test-html)

;; ;; next step is parsing edn. I will use the already slurped directory for this.
(def metadata (make-edn-page-map "/test" slurped-raw))
metadata
(format-html-links metadata)
;; now I need to make the links. Sorted in reverse chrono order. 
(def links-to-put (format-html-links metadata))
;; now insert the links. 
(zipmap (keys slurped-raw) (map #(add-links % links-to-put) (vals slurped-raw)))
