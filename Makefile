
update:
# this will only update the cite by converting emacs files to html
deploy:
	echo "testing building..."
# this will build and deploy the entire site
	pwd
view:
	echo "Starting server to view website"
	lein ring server
