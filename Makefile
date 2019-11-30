
update:
# this will only update the cite by converting emacs files to html
deploy:
	echo "deploying site!"
# this will build and deploy the entire site
	lein build-site;git add .;git commit -m "content update";git push;cd target/nickgeorge.net/; git add .;git commit -m "automated commit."; git push
view:
	echo "Starting server to view website"
	lein ring server
