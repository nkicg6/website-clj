.PHONY: update deploy view

gitlabpath="/Users/nick/personal_projects/nkicg6.gitlab.io/"


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
	echo "Copying fonts $(gitlabpath)font-backup/ to $(gitlabpath)public/fonts/ ..."; \
	cp -R "$(gitlabpath)font-backup/" "$(gitlabpath)public/fonts/";\
	echo "Copying images..."; \
	cp -R "resources/public/img/" "$(gitlabpath)public/img/";\
	echo "converting site with emacs...";\
	emacs -batch --load publish.el --eval '(org-publish "clj-site")';\
	echo "Building site with clojure...";\
	lein build-site;\
	echo "Pushing to gitlab...";\
	cd $(gitlabpath);\
	git add .;git commit -m "Automated post push";git push;\
	echo "Done!"
