(ns website-clj.refactored-method
  "This ns is purely for testing refacted methods. All functions here will live in process-pages ns
  in the future"
  (:require [clojure.string :as str]
            [hiccup.core :refer [html]]
            [hiccup.page :use [html5 include-css include-js]]
            [hiccup.element :refer (link-to image)]
            [net.cgrand.enlive-html :as enlive]
            [clojure.edn :as edn] 
            [stasis.core :as stasis]
            [website-clj.process-pages :as process]))




