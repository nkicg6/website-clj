# Makefile to update, view with a server, and publish my static website. 
.PHONY: update deploy view

gitlabpath="/Users/nick/personal_projects/nkicg6.gitlab.io/"
resumepath="/Users/nick/Dropbox/CV_biosketch/industry-resume/"

update:
# this will only update the site by converting emacs files to html
	@echo "updating site..."
	emacs -batch --load publish.el --eval '(org-publish "clj-site")'

view:
	@echo "updating site..."
	emacs -batch --load publish.el --eval '(org-publish "clj-site")'
	@echo "Starting server to view website"
	lein ring server

deploy:
	@echo "building and moving to gitlab..."
	@echo "Removing dir rm -r $(gitlabpath)public/* ..."; \
	rm -rf $(gitlabpath)public/*; \
	echo "Adding font dir $(gitlabpath)public/fonts..."; \
	mkdir $(gitlabpath)public/fonts;\
	mkdir $(gitlabpath)public/img;\
	mkdir $(gitlabpath)public/font-awesome-4.7.0;\
	echo "Copying fonts $(gitlabpath)font-backup/ to $(gitlabpath)public/fonts/ ..."; \

	cp -R "$(gitlabpath)font-backup/" "$(gitlabpath)public/fonts/";\
	echo "Copying images..."; \
	cp -R "resources/public/img/" "$(gitlabpath)public/img/";\
	echo "converting site with emacs...";\
	emacs -batch --load publish.el --eval '(org-publish "clj-site")';\
	echo "Building site with clojure...";\
	lein build-site;\
	echo "Adding resume...";\
	echo "Copying font-awesome-4.7.0 logos";\
	cp -R "$(gitlabpath)font-awesome-4.7.0/" "$(gitlabpath)public/font-awesome-4.7.0";\
	echo "Pandoc convert resume...";\
	pandoc -s "$(resumepath)org/nicholasmgeorge-resume-html.org" -o "$(gitlabpath)resumetmp.html";\
	echo "Add css to resume...";\
	python3 resume.py -body "$(gitlabpath)resumetmp.html" -headerfooter "$(gitlabpath)websiteresume_header.html" -out "$(gitlabpath)public/nicholasmgeorge-resume.html";\
	echo "removing temp resume...";\
	rm "$(gitlabpath)resumetmp.html"
	echo "Pushing to gitlab...";\
	cd $(gitlabpath);\
	git add .;git commit -m "Automated post push";git push;\
	echo "Done!"

resume:
	@echo "Adding resume...";\
	echo "Copying font-awesome-4.7.0 logos";\
	cp -R "$(gitlabpath)font-awesome-4.7.0/" "$(gitlabpath)public/font-awesome-4.7.0";\
	echo "Pandoc convert resume...";\
	pandoc -s "$(resumepath)org/nicholasmgeorge-resume-html.org" -o "$(gitlabpath)resumetmp.html";\
	echo "Add css to resume...";\
	python3 resume.py -body "$(gitlabpath)resumetmp.html" -headerfooter "$(gitlabpath)websiteresume_header.html" -out "$(gitlabpath)public/nicholasmgeorge-resume.html";\
	echo "removing temp resume...";\
	rm "$(gitlabpath)resumetmp.html";\
	echo "Done."
