(ns website-clj.export-helpers
  "helper functions for saving the git directory, cname, and gitignore from `stasis/empty-directory!`
  This exists to help with rendering static sites on github." 
  (:require [clojure.string :as str]
            [clojure.java.shell :as shell]))


(defn cp-cname
  "copy the CNAME file to the export directory.
  `export-dir` is a var that contains the parth to the base of the website. 
  CNAME must be in the directory for github pages domain mapping."
  [export-dir]
  (shell/sh "cp" "resources/CNAME" (str export-dir "/CNAME")))

(defn cp-gitignore
  "copy the gitignore file from a safe location to the base of the github pages repo for rendering."
  [export-dir]
  (shell/sh "cp" "target/.gitignore" (str export-dir "/.gitignore")))

(defn save-git
  "copy .git repo to a safe directory to save it from deletion. 
  `safe-dir` is a path to a directory that will not be emptied by `stasis/empty-directory!`
  `export-dir` is the export directory where your site will be made."
  [safe-dir export-dir] 
  (shell/sh "mv" (str export-dir "/.git") (str safe-dir "/.git")))

(defn replace-git
  "Puts the gir directory back into the export directory.
  `safe-dir` is a path to a directory that will not be emptied by `stasis/empty-directory!`
  `export-dir` is the export directory where your site will be made."
  [safe-dir export-dir]
  (shell/sh "mv" (str safe-dir "/.git") (str export-dir "/.git")))
