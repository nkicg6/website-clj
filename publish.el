;; publish org-mode project.from makefile 
;; based on: https://stackoverflow.com/questions/46295511/how-to-run-org-mode-commands-from-shell
;; command:
;; 
(add-to-list 'load-path "~/.emacs.d/elpa/org-20191125")

(require 'org)
(require 'ox-publish)
(require 'ox-html)

(setq org-publish-project-alist
      '(("programming"
         :base-directory "~/personal_projects/website_clj/resources/org_programming"
         :base-extension "org"
         :publishing-directory "~/personal_projects/website_clj/resources/programming"
         :publishing-function org-html-publish-to-html
         :headline-levels 4
         :html-extension "html"
         :body-only t)
        ("science"
         :base-directory "~/personal_projects/website_clj/resources/org_science"
         :base-extension "org"
         :publishing-directory "~/personal_projects/website_clj/resources/science"
         :publishing-function org-html-publish-to-html
         :headline-levels 4
         :html-extension "html"
         :body-only t)
        ("clj-site" :components ("programming" "science"))))
