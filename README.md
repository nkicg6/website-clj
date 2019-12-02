# website-clj

The source code for my website, rewritten in Clojure. 

## Usage
Check out the site at: https://nickgeorge.net/
Viewing via ring server, updating, and deploying are all automated via make. 
`make update` to call emacs/org-mode to compile org files to html
`make view` will start a ring server to view the site. 
`make deploy` will run update, build the site, and automatically comit for deployment to github pages. 

## License

Copyright © 2019 Nick George

Distributed under the Eclipse Public License either version 1.0 or (at
your option) any later version.
