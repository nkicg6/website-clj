(ns website-clj.export-helpers
  "helper functions for saving the git directory, cname, and gitignore from `stasis/empty-directory!`
  This exists to help with rendering static sites on github." 
  (:require [clojure.string :as str]))

(defn make-filtered-list
  ;; this would be a good use for a macro. pass in the vector of exclusions, and generate an arbitrary len of
  ;; exclude predicates.
  [base]
  (let [flist  (mapv str (filter #(.isFile %) (file-seq (clojure.java.io/file base))))]
     (filter (apply every-pred [#(not (str/includes? % ".git")) #(not (str/includes? % "CNAME"))]) flist)))

(defn clear-directory!
  [base]
  (map clojure.java.io/delete-file (make-filtered-list  base pat1 pat2)))

;;(clear-directory! "/Users/nick/test_dir")
