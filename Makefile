# Makefile to update, view with a server, and publish my static website. 
.PHONY: update deploy view

gitlabpath="/Users/nick/personal_projects/nkicg6.gitlab.io/"
resumepath="/Users/nick/Dropbox/CV_biosketch/industry-resume/"

update:
# this will only update the site by converting emacs files to html
	@echo "updating site..."
	emacs -batch --load publish.el --eval '(org-publish "clj-site")'

view:
	@echo "[INFO] updating site..."
	emacs -batch --load publish.el --eval '(org-publish "clj-site")'
	@echo "[INFO] Starting server to view website"
	lein ring server

deploy:
	@echo "[INFO] building and moving to gitlab...";\
	echo "[INFO] Clearing public directory..."; \
	rm -rf $(gitlabpath)public/*; \
	echo "[INFO] Adding fonts and image directory..."; \
	mkdir $(gitlabpath)public/fonts;\
	mkdir $(gitlabpath)public/img;\
	mkdir $(gitlabpath)public/font-awesome-4.7.0;\
	echo "[INFO] Copying fonts..."; \
	cp -R "$(gitlabpath)font-backup/" "$(gitlabpath)public/fonts/";\
	echo "[INFO] Copying images...";\
	cp -R "resources/public/img/" "$(gitlabpath)public/img/";\
	echo "[INFO] Org->html with org-mode...";\
	emacs -batch --load publish.el --eval '(org-publish "clj-site")';\
	echo "[INFO] Building site with Clojure...";\
	lein build-site;\
	echo "[INFO] Adding resume...";\
	echo "[INFO] Copying font-awesome-4.7.0 logos";\
	cp -R "$(gitlabpath)font-awesome-4.7.0/" "$(gitlabpath)public/font-awesome-4.7.0";\
	echo "[INFO] Convert org-resume -> html with pandoc...";\
	pandoc -s "$(resumepath)org/nicholasmgeorge-resume-html.org" -o "$(gitlabpath)resumetmp.html";\
	echo "[INFO] Add css to resume...";\
	python3 resume.py -body "$(gitlabpath)resumetmp.html" -headerfooter "$(gitlabpath)websiteresume_header.html" -out "$(gitlabpath)public/nicholasmgeorge-resume.html";\
	echo "[INFO] Removing temp resume...";\
	rm "$(gitlabpath)resumetmp.html";\
	echo "[INFO] Pushing to gitlab...";\
	cd $(gitlabpath);\
	git add .;git commit -m "Automated post push";git push;\
	echo "[INFO] Done!"

resume:
	@echo "[INFO] Adding resume...";\
	echo "[INFO] Copying font-awesome-4.7.0 logos";\
	cp -R "$(gitlabpath)font-awesome-4.7.0/" "$(gitlabpath)public/font-awesome-4.7.0";\
	echo "[INFO] Pandoc convert resume...";\
	pandoc -s "$(resumepath)org/nicholasmgeorge-resume-html.org" -o "$(gitlabpath)resumetmp.html";\
	echo "[INFO] Add css to resume...";\
	python3 resume.py -body "$(gitlabpath)resumetmp.html" -headerfooter "$(gitlabpath)websiteresume_header.html" -out "$(gitlabpath)public/nicholasmgeorge-resume.html";\
	echo "[INFO] removing temp resume...";\
	rm "$(gitlabpath)resumetmp.html";\
	echo "[INFO] Done."

clean:
	@echo "[INFO] Cleaning...";\
	rm -f $(wildcard resources/programming/*~);\
	rm -f $(wildcard resources/science/*~);
