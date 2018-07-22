;; main page for my clojure static website.
;; based on https://cjohansen.no/building-static-sites-in-clojure-with-stasis/

(ns website-clj.web
  (:require [optimus.assets :as assets]
            [optimus.export]
            [optimus.link :as link] 
            [optimus.optimizations :as optimizations]      
            [optimus.prime :as optimus]                    
            [optimus.strategies :refer [serve-live-assets]]
            [clojure.java.io :as io]
            [clojure.java.shell :as shell]
            [clojure.string :as str]
            [hiccup.page :refer [html5]]
            [hiccup.element :refer (link-to image)]
            [stasis.core :as stasis]))


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
       [:li {:class "inactive"} (link-to "/" "Science")]
       [:li {:class "inactive"} (link-to "/" "Programming")]
       [:li {:class "inactive"} (link-to "/" "About")]
       [:li {:class "inactive"} (link-to "/test_post" "Post")]
       [:li [:a {:href "https://github.com/nkicg6"}
             [:span {:class "fa fa-github" :style "font-size:24px"}]]]
       [:li [:a {:href "https://twitter.com/NicholasMG"}
             [:span {:class "fa fa-twitter-square" :style "font-size:24px"}]]]]]]
    [:div {:class "container"}
     [:div.body page]]
    ;;[:div.test [:img {:src "/img/test-img.png"}]] ; img test
    [:footer {:class "footer"}
     [:div {:class "text-center"}
      [:span {:class "text-muted"} "&copy 2018 Nick George"]]]]))

(defn prepare-page [page req]
  (if (string? page) page (page req)))

(defn get-assets []
  (assets/load-assets "public" [#".*"]))


(defn format-images [html]
  (str/replace html #"src=\"img" "src=\"/img"))

;; main pages function.
(defn html-pages [pages]
  (zipmap (map #(str/replace % #"\.html$" "") (keys pages))
          (map #(fn [req] (layout-base-header req %))
               (map format-images (vals pages)))))

(defn partial-pages [pages]
  (zipmap (keys pages)
          (map #(fn [req] (layout-base-header req %)) (vals pages))))

(defn home-page [pages]
  (zipmap (keys pages)
          (map #(fn [req] (layout-base-header req %)) (vals pages))))

(home-page
 (stasis/slurp-directory "resources/home" #".*\.(html|css|js)$"))


(defn get-pages []
  (stasis/merge-page-sources
   {:landing (home-page (stasis/slurp-directory "resources/home" #".*\.(html|css|js)$"))
    :posts  (html-pages (stasis/slurp-directory "resources/posts" #".*\.html$"))
    :partials (partial-pages (stasis/slurp-directory "resources/partials" #".*\.html$"))
    :public (stasis/slurp-directory "resources/public" #".*\.(html|css|js)$")}))




(def app
  (optimus/wrap
   (stasis/serve-pages get-pages)
   get-assets
   optimizations/none
   serve-live-assets))

(def export-dir "target/nickgeorge.net")

(def safe-dir "target")

(defn cp-cname [export-dir]
  (shell/sh "cp" "resources/CNAME" (str export-dir "/CNAME")))

(defn cp-gitignore [export-dir]
  (shell/sh "cp" "target/.gitignore" (str export-dir "/.gitignore")))

(defn save-git [safe-dir export-dir] 
  (shell/sh "mv" (str export-dir "/.git") (str safe-dir "/.git")))

(defn replace-git [safe-dir export-dir]
  (shell/sh "mv" (str safe-dir "/.git") (str export-dir "/.git")))


(defn export []
  (save-git safe-dir export-dir)
  (let [assets (optimizations/all (get-assets) {})]
    (stasis/empty-directory! export-dir)
    (optimus.export/save-assets assets export-dir)
    (stasis/export-pages (get-pages) export-dir {:optimus-assets assets}))
  (cp-cname export-dir)
  (cp-gitignore export-dir)
  (replace-git safe-dir export-dir))
