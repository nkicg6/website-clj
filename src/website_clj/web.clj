;; main page for my clojure static website.
;; based on https://cjohansen.no/building-static-sites-in-clojure-with-stasis/

(ns website-clj.web
  (:require [clojure.java.io :as io]
            [clojure.string :as str]
            [hiccup.page :refer [html5]]
            [hiccup.element :refer (link-to)]
            [me.raynes.cegdown :as md]
            [stasis.core :as stasis]))




(defn layout-base-header [page]
  (html5
   [:head
    [:meta {:charset "utf-8"}]
    [:meta {:name "viewport"
            :content "width=device-width, initial-scale=1.0"}]
    [:link {:rel "stylesheet" :href "https://maxcdn.bootstrapcdn.com/bootstrap/3.3.7/css/bootstrap.min.css" :integrity "sha384-BVYiiSIFeK1dGmJRAkycuHAHRg32OmUcww7on3RYdg4Va+PmSTsz/K68vbdEjh4u" :crossorigin "anonymous"}]
    [:link {:rel "stylesheet" :href "https://cdnjs.cloudflare.com/ajax/libs/font-awesome/4.7.0/css/font-awesome.min.css"}]
    [:script {:src "https://maxcdn.bootstrapcdn.com/bootstrap/3.3.7/js/bootstrap.min.js" :integrity "sha384-Tc5IQib027qvyjSMfHjOMaLkfuWVxZxUPnCJA7l2mCWNIpG9mGCD8wGNIcPD7Txa" :crossorigin "anonymous"}]]
   [:body
    [:nav {:class "navbar navbar-inverse"}
     [:div {:class "container-fluid"}
      [:div {:class "navbar-header"}
       (link-to  {:class "navbar-brand"} "/" "Nick George")]
      [:ul {:class "nav navbar-nav navbar-right"}
       [:li {:class "inactive"} (link-to "/" "Science")]
       [:li {:class "inactive"} (link-to "/" "Programming")]
       [:li {:class "inactive"} (link-to "/" "About")]
       [:li [:a {:href "https://github.com/nkicg6"}
             [:span {:class "fa fa-github" :style "font-size:24px"}]]]
       [:li [:a {:href "https://twitter.com/NicholasMG"}
             [:span {:class "fa fa-twitter-square" :style "font-size:24px"}]]]]]]
    [:div.logo "website-clj"]
    [:div.body page]
    [:footer {:class "footer"}
     [:div {:class "text-center"}
      [:span {:class "text-muted"} "&copy 2018 Nick George"]]]]))

(def pegdown-options ;; https://github.com/sirthias/pegdown
  [:autolinks :fenced-code-blocks :strikethrough])


(defn markdown-pages [pages]
  (zipmap (map #(str/replace % #"\.md$" "") (keys pages))
          (map #(layout-base-header (md/to-html %)) (vals pages))))


(defn html-pages [pages]
  (zipmap (map #(str/replace % #"\.html$" "") (keys pages))
          (map #(layout-base-header %) (vals pages))))


(defn partial-pages [pages]
  (zipmap (keys pages)
          (map layout-base-header (vals pages))))

(defn home-page [pages]
  (zipmap (keys pages)
          (map #(layout-base-header %) (vals pages))))

(home-page (stasis/slurp-directory "resources/home" #".*\.(html|css|js)$"))


(defn get-pages []
  (stasis/merge-page-sources
   {:landing (home-page (stasis/slurp-directory "resources/home" #".*\.(html|css|js)$"))
    :posts  (html-pages (stasis/slurp-directory "resources/posts" #".*\.html$"))
    :partials (partial-pages (stasis/slurp-directory "resources/partials" #".*\.html$"))
    :markdown (markdown-pages (stasis/slurp-directory "resources/md" #".*\.md$"))}))

(def app (stasis/serve-pages get-pages))


