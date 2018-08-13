(ns website-clj.process-pages
  (:require [clojure.string :as str]
            [hiccup.core :refer [html]]
            [hiccup.page :use [html5 include-css include-js]]
            [hiccup.element :refer (link-to image)]
            [net.cgrand.enlive-html :as enlive]
            [clojure.edn :as edn] 
            [stasis.core :as stasis]))  ;; only for testing?


(defn layout-base-header
  "Applies a header and footer to html strings."
  [request page]
  (html5
   [:head
    [:meta {:charset "utf-8"}]
    [:meta {:name "viewport"
            :content "width=device-width, initial-scale=1.0"}]
    [:link {:rel "stylesheet" :href "https://maxcdn.bootstrapcdn.com/bootstrap/3.3.7/css/bootstrap.min.css"}]
    [:link {:rel "stylesheet" :href "https://cdnjs.cloudflare.com/ajax/libs/font-awesome/4.7.0/css/font-awesome.min.css"}]
    (include-css "/css/hide.css")
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
       [:li {:class "inactive"} (link-to "/" "About")]
       [:li [:a {:href "https://github.com/nkicg6"}
             [:span {:class "fa fa-github" :style "font-size:24px"}]]]
       [:li [:a {:href "https://twitter.com/NicholasMG"}
             [:span {:class "fa fa-twitter-square" :style "font-size:24px"}]]]]]]
    [:div {:class "container"}
     [:div.body page]]
    [:footer {:class "footer"}
     [:div {:class "text-center"}
      [:span {:class "text-muted"} "&copy 2018 Nick George"]]]]))

(defn prepare-page [page]
  "Force the evaluation of lazy pages.
  `page` is a function that takes no arguments and 
  returns an html string, or an html string."
  (if (string? page) page (page "")))

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
  `base` is whatever base name you want the string to have prepended to it. 
  `name` is a string."
  [base name]
  (str base
       (str/replace name #"(?<!index)\.html$" "")))

;; main pages formatting function
(defn html-pages 
  "Composed function that performs formatting to a map of strings
  The argument `base` is a new string that will be prepended to all keys in the 
  `pages` map argument. `pages` is typically a map created by the function `stasis/slurp-directory`. 
  The overall purpose of `html-pages` is to apply formatting to html pages meant for different sections
  of my website. For instance, calling `html-pages` with 'programming' and the a map of pages will prepend 
  'programming' to every key in the map and strip the html end off all non-index pages. "
  [base pages]
  (zipmap (map #(fmt-page-names base %) (keys pages)) ;; initial keys manipulation
          (map #(fn [req] (layout-base-header req %))  ;; apply main header/footer 
               (map format-html (vals pages)))))  ;; all html formatting 


(defn home-page
  "Applies `labout-base-header` to a map of `:page-names page-html`
  `pages` is typically a map created by the function `stasis/slurp-directory`"
  [pages]
  (zipmap (keys pages)
          (map #(fn [req] (layout-base-header req %)) (vals pages))))

(defn parse-edn
  "Takes raw html and returns keys from edn metadata under the <div id='edn'> html tag
  `html` is raw html"
  [html]
  (-> html
      (prepare-page)
      (enlive/html-snippet)
      (enlive/select [:#edn enlive/text-node])
      (->> (apply str)) ;; I know this is bad form, but it is the best way I know how to do it..
      (edn/read-string)
      ;;(select-keys [:title :date])
      ;;(vals)
      (get :title)))

(defn remove-index2
  "Filters out pages containing index from a map.
  `values` are strings."
  [values]
  (if (seq? values)
    (remove #(re-matches #"(/.*/)?index(.html)?" %) (first values))
    (remove #(re-matches #"(/.*/)?index(.html)?" %) values)))

(defn remove-index
  "Filters out pages containing index from a map.
  `values` are strings."
  [values]
  (remove #(re-matches #"(/.*/)?index(.html)?" %) values))

(defn link-map 
  "applies `remove-index` to a map.
  `stasis-map` is a map created by the function `stasis/slurp-directory`. The purpose is 
  to filter out index pages from a list of all pages in order to make a list of page links
  to insert into my index pages"
  [stasis-map]
  (zipmap (remove-index (keys stasis-map))
          (remove-index (map parse-edn (vals stasis-map)))))

(defn link-list
  "makes a list of links using hiccup markup
  `links` come from the output of `link-map`"
  [links]
  (html [:ul (for [[k v] links]
               [:li (link-to k v)])]))

(defn make-links
  "pipeline function to create the list of links to insert into the index page.
  `stasis-map` is a map created by the function `stasis/slurp-directory`. This function 
  provides the second argument to the `add-links` function"
  [stasis-map]
  (-> stasis-map
      (link-map)
      (link-list)))

(defn add-links
  "adds links of all pages to the index.html page and un-escapes html characters. 
  The `page` argument is the html for a page. 
  The `links` argument is an html string, typically generated with the `make-links` function 
  This returns the modified html"
  [page links]
  (-> page
      (prepare-page) ;; forse eval of lazy pages
      (enlive/sniptest
       [:#pageListDiv] ;; exists only in index pages. 
       (enlive/content links))
      (str/replace #"&gt;" ">")
      (str/replace #"&lt;" "<"))) ;; add the links


;; playing below


;; this will be used in the future for getting the other metadata. 
(def test-map2 {:title "test-title", :date "2018-08-06", :tags '("tag1" "clojure")})
(keys test-map2)

(let [title (get test-map2 :title) date (get test-map2 :date) tags (get test-map2 :tags)]
  (list title (list date tags)))

(def test-html-page
  (second (vals (html-pages "/programming"
                            (stasis/slurp-directory "resources/programming" #".*\.(html|css|js)")))))

(def testmapval
  (second (html-pages "/programming"
                      (stasis/slurp-directory "resources/programming" #".*\.(html|css|js)"))))
(def test-map1
  (hash-map (first testmapval)
            (second testmapval)))

(def testseq (parse-edn test-html-page))

(vals test-map1)
(parse-edn (prepare-page (vals test-map1)))


(link-map (html-pages "/programming"
                      (stasis/slurp-directory "resources/programming" #".*\.(html|css|js)")))
(hash-map (first testmapval) (parse-edn (prepare-page (second testmapval))))
