(ns website-clj.export-helpers
  "helper functions for saving the git directory, cname, and gitignore from `stasis/empty-directory!`
  This exists to help with rendering static sites on github." 
  (:require [clojure.string :as str]
            [clojure.java.io :as io]
            [me.raynes.fs :as fs]))

(defn- delete! [path]
  (cond
    (fs/directory? path) (fs/delete-dir path)
    (fs/file? path) (fs/delete path)
    :else "None"))

(defn clear-directory! [target]
  "Deletes all files and directories from target except .git."
  (let [as-files (fs/list-dir target)
        no-git (filter #(not (str/includes? % ".git")) as-files)]
    (map delete! no-git)))

