.PHONY: update deploy view

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
	@echo "Starting server to view website"
	lein ring server
