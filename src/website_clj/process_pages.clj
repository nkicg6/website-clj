;; also, add recent posts to home page! previous five?
;; another metadata thing could be the first 50 words of the post. very easy to work with this now. 
(ns website-clj.process-pages
  (:require [clojure.string :as str]
            [hiccup.core :refer [html]]
            [hiccup.page :refer [html5 include-css include-js]]
            [hiccup.element :refer [link-to]]
            [net.cgrand.enlive-html :as enlive]
            [clojure.edn :as edn]))

;; --- google analytics ---
(def google-analytics (str "window.dataLayer = window.dataLayer || [];"
                           "function gtag(){dataLayer.push(arguments);}"
                           "gtag('js', new Date());"
                           "gtag('config', 'UA-124749948-1');"))

;; --- basic html formatting ---
(defn get-copyright-date []
  (.format (java.text.SimpleDateFormat. "yyyy")
           (new java.util.Date)))

;;header
(defn layout-base-header
  "Applies a header and footer to html strings."
  [page]
  (html5 {:lang "en"}
         [:head
          [:script {:src "https://www.googletagmanager.com/gtag/js?id=UA-124749948-1" :async "async"}]
          [:script google-analytics]
          [:script {:src  "https://cdnjs.cloudflare.com/ajax/libs/mathjax/2.7.5/latest.js?config=TeX-MML-AM_CHTML"
                    :async "async"}]
          [:title "Nick's site"]
          [:meta {:charset "utf-8"}]
          [:meta {:name "viewport"
                  :content "width=device-width, initial-scale=1.0"}]
          #_[:link {:rel "stylesheet" :href "https://maxcdn.bootstrapcdn.com/bootstrap/3.3.7/css/bootstrap.min.css"}]
          #_[:link {:rel "stylesheet" :href "https://cdnjs.cloudflare.com/ajax/libs/font-awesome/4.7.0/css/font-awesome.min.css"}] 
          #_(include-css "/css/custom.css")
          (include-css "/css/style.css")
          #_[:script {:src "https://maxcdn.bootstrapcdn.com/bootstrap/3.3.7/js/bootstrap.min.js"
                    :integrity "sha384-Tc5IQib027qvyjSMfHjOMaLkfuWVxZxUPnCJA7l2mCWNIpG9mGCD8wGNIcPD7Txa"
                    :crossorigin "anonymous"}]]
         [:body [:div {:class "header"}]
          page
          #_[:nav {:class "navbar navbar-inverse"}
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
          [:footer {:class "footer"}
           [:div {:class "text-center"}
            [:span #_{:class "text-muted"} (str "&copy; Nick George 2017-" (get-copyright-date))]]]]))

(defn format-images [html]
  "formats html image link to appropriately link to static website image directory.
  `html` is a raw html string."
  (-> html
   (str/replace #"<img src=.*/img" "<img src=\"/img")
   (str/replace #"../public" "")))

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
  "`insert-page-title` parses edn metadata and return the html with a title inserted
  `page` is the raw HTML of a page including the header."
  [page]
  (let [meta-title (get (parse-html page) :title "Nick's site")]
    (-> page
        (enlive/sniptest [:title]
                         (enlive/html-content meta-title)))))

(defn format-html 
  "Composed function to apply multiple html processing steps to raw html.
  `html` is a raw html string."
  [html]
  (-> html
      format-images
      layout-base-header
      insert-page-title))

(defn remove-index
  "hard coded, removes the two index.html pages for building the site's list of links"
  [page-map]
  (dissoc page-map "/programming/index.html" "/science/index.html"))

(defn make-edn-page-map
  "filters the `page-map` to remove index.html keys and returns a map of page names and edn metadata.
  `page-map` is returned by `website/make-page-map`"
  [page-map]
  (let [filtered-page-map (remove-index page-map)
        page-keys (keys filtered-page-map)
        parsed-page-vals (map parse-html (vals filtered-page-map))]
    (zipmap page-keys parsed-page-vals)))

;; --- make links to insert ---

(defn format-html-links
  "Makes a list of links in reverse chronological order using hiccup markup.
  `metadata-map`comes from the output of `make-edn-page-map`"
  [metadata-map]
  (html [:ul (for [[k v] (reverse (sort-by #(get-in (val %) [:date]) metadata-map))] ;; reverse chrono order
               [:li (link-to k (get v :title)) (str "<em> Published: " (get v :date) "</em>")])]))

;; --- merge and sort ---

(defn merge-maps-sort-take-five
  "Merges two lists,of maps sorts reverse chronologically and takes the first five.
  `metadata1` and `metadata2` are generated by `make-edn-page-map`"
  [metadata1 metadata2]
  (let [merged (merge metadata1 metadata2)]
    (format-html-links (take 5 (reverse (sort-by #(get-in (val %) [:date]) merged))))))

;; --- insert links ---

(defn add-links
  "Adds links of all pages to the index.html page and un-escapes html characters. 
  The `page` argument is the html for a page. 
  The `links` argument is an html string, typically generated with the `make-links` function 
  This returns the modified html"
  [links div page]
  (-> page
      (enlive/sniptest
       [div] ;; exists only in index pages. 
       (enlive/html-content links))))

(defn add-links-to-map
  "Function to add the links to the pagemap returned by `website/make-page-map`"
  [links div page-map]
  (zipmap (keys page-map)
          (map #(add-links links div %) (vals page-map))))

;; -- TESTING BELOW --
;; first step is slurping a directory, applying the path prefix and formatting html.

;; (def slurped-raw
;;   "holds a map of formatted html pages for my website"
;;   (html-pages "/test" (stasis/slurp-directory "resources/test" #".*\.(html|css|js)")))
;; ;; (keys slurped-raw)
;; (def test-html (layout-base-header (second (vals slurped-raw))))

;; (parse-html test-html)


;; ;;(rename-page test-html)

;; ;; ;; next step is parsing edn. I will use the already slurped directory for this.
;; (def metadata (make-edn-page-map "/test" slurped-raw))
;; (format-html-links metadata)
;; ;; now I need to make the links. Sorted in reverse chrono order. 
;; (def links-to-put (format-html-links metadata))
;; ;; now insert the links. 
;;                                         ;(zipmap (keys slurped-raw) (map #(add-links % links-to-put) (vals slurped-raw)))
