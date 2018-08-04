(ns progess-pages
  (:require [clojure.string :as str]))



;; format images
(defn format-images [html]
  (str/replace html #"src=\"img" "src=\"/img"))

;; main pages formatting function
(defn html-pages [base pages]
  (zipmap (map #(str base %) (map #(str/replace % #"(?<!index)\.html$" "") (keys pages)))
          (map #(fn [req] (layout-base-header req %))
               (map format-images (vals pages)))))


(str/replace "index.html" #"(?<!index)\.html$" "")


