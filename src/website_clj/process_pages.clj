(ns website-clj.process-pages
  (:require [clojure.string :as str]
            [hiccup.core :refer [html]]
            [hiccup.page :use [html5 include-css include-js]]
            [hiccup.element :refer (link-to image)]
            [net.cgrand.enlive-html :as enlive]
            [clojure.edn :as edn] 
            [stasis.core :as stasis] ;; only for testing?
            ))


;; header formatting goes on every page
(defn layout-base-header [request page]
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

;; force rendering of pages
(defn prepare-page [page]
  (if (string? page) page (page "")))

;; format images
(defn format-images [html]
  (str/replace html #"src=\"img" "src=\"/img"))

;; anything that formats html will be added here. 
(defn format-html [html]
  (-> html
      (format-images))
  ;; other fns for html here
  )

;; fix up page names. 
(defn fmt-page-names [base name]
  (str base
       (str/replace name #"(?<!index)\.html$" "")))

;; main pages formatting function
(defn html-pages [base pages]
  (zipmap (map #(fmt-page-names base %) (keys pages)) ;; initial keys manipulation
          (map #(fn [req] (layout-base-header req %))  ;; apply main header/footer 
               (map format-html (vals pages))) ;; all html formatting 
          ))

;; will likely be removed. 
(defn partial-pages [pages]
  (zipmap (keys pages)
          (map #(fn [req] (layout-base-header req %)) (vals pages))))

;; likely not needed?
(defn home-page [pages]
  (zipmap (keys pages)
          (map #(fn [req] (layout-base-header req %)) (vals pages))))


;; parse edn takes values, returns the title part.  
(defn parse-edn
  [html]
  (-> html
      (prepare-page)
      (enlive/html-snippet)
      (enlive/select [:#edn enlive/text-node])
      (->> (apply str)) ;; I know this is bad form, but it is the best way I know how to do it..
      (edn/read-string)
      (get :title)))

;; don't want to use links from index pages. 
(defn remove-index
  [values]
  (remove #(re-matches #"(/.*/)?index(.html)?" %) values))

;; make a list of links.
(defn link-map [stasis-map]
  (zipmap (remove-index (keys stasis-map))
          (remove-index (map parse-edn (vals stasis-map)))))



;; this makes a list of links with Hiccup. enlive will then insert it.
(defn link-list [links]
  (html [:ul (for [[k v] links]
               [:li (link-to k v)])]))

;; make-links provides the second argument to add-links, with the first being the raw html. 
(defn make-links [stasis-map]
  (-> stasis-map
      (link-map)
      (link-list)))


;; main workhorse function that adds the links and returns the modified html

(defn add-links [page links]
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
(str/replace test-html #"&gt" "")
