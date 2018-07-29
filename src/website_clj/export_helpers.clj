(ns website-clj.export-helpers
  (:require [clojure.string :as str]
            [clojure.java.shell :as shell]))


(defn cp-cname [export-dir]
  (shell/sh "cp" "resources/CNAME" (str export-dir "/CNAME")))

(defn cp-gitignore [export-dir]
  (shell/sh "cp" "target/.gitignore" (str export-dir "/.gitignore")))

(defn save-git [safe-dir export-dir] 
  (shell/sh "mv" (str export-dir "/.git") (str safe-dir "/.git")))

(defn replace-git [safe-dir export-dir]
  (shell/sh "mv" (str safe-dir "/.git") (str export-dir "/.git")))
