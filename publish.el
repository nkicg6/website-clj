;; publish org-mode project.from makefile 
;; based on: https://stackoverflow.com/questions/46295511/how-to-run-org-mode-commands-from-shell
(add-to-list 'load-path "~/.emacs.d/elpy")
(require 'org)

(load-theme leuveun)

(setq org-publish-project-alist
      '(
        ("programming"
         :base-directory "~/personal_projects/website-clj/resources/org-programming"
         :base-extension "org"
         :publishing-directory "~/personal_projects/website-clj/resources/programming"
         :publishing-function org-html-publish-to-html
         :headline-levels 4
         :html-extension "html"
         :body-only t)
        ("science"
         :base-directory "~/personal_projects/website-clj/resources/org-science"
         :base-extension "org"
         :publishing-directory "~/personal_projects/website-clj/resources/science"
         :publishing-function org-html-publish-to-html
         :headline-levels 4
         :html-extension "html"
         :body-only t)
        ("clj-site" :components ("programming" "science"))))
