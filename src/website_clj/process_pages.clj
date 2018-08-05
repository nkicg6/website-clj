(ns website-clj.process-pages
  (:require [clojure.string :as str]
            [hiccup.page :refer [html5]]
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


;; playing below

;; get the test of your first page

(def test-html ((first (vals (html-pages "/test"
                                         (stasis/slurp-directory "resources/test" #".*\.html$")))) "" ))
(defn parse-edn
  [html]
  (-> html
      (enlive/html-snippet)
      (enlive/select [:#edn enlive/text-node])
      (->> (apply str)) ;; I know this is bad form, but it is the best way I know how to do it..
      (edn/read-string)
      ))

(get (parse-edn test-html) :title)

(defn prepare-page [page]
  (if (string? page) page (page "")))

(map parse-edn (map prepare-page (vals (html-pages "/test"
                                                   (stasis/slurp-directory "resources/test" #".*\.html$")))))

