.PHONY: update deploy view

gitlabpath="/Users/nick/personal_projects/nkicg6.gitlab.io/"

update:
# this will only update the cite by converting emacs files to html
	@echo "updating site..."
	emacs -batch --load publish.el --eval '(org-publish "clj-site")'
deploy:
	@echo "deploying site."
	@echo "Updating now from emacs..."
	emacs -batch --load publish.el --eval '(org-publish "clj-site")'
# this will build and deploy the entire site
	@echo "building and pushing via git..."
	lein build-site;git add .;git commit -m "content update";git push;cd target/nickgeorge.net/; git add .;git commit -m "automated commit."; git push
	@echo "Done!"
view:
	@echo "updating site..."
	emacs -batch --load publish.el --eval '(org-publish "clj-site")'
	@echo "Starting server to view website"
	lein ring server


gitlabdeploy:
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
	echo "Making CNAME...";\
	"echo nickgeorge.net > $(gitlabpath)public/CNAME";\
	echo "converting site with emacs...";\
	emacs -batch --load publish.el --eval '(org-publish "clj-site")';\
	echo "Building site with clojure...";\
	lein build-site;
