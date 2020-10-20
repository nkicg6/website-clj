(defproject website-clj "0.1.0-SNAPSHOT"
  :description "Personal website built with Clojure, Stasis, and Hiccup"
  :url "http://nickgeorge.net"
  :license {:name "Eclipse Public License"
            :url "http://www.eclipse.org/legal/epl-v10.html"}
  :dependencies [[org.clojure/clojure "1.8.0"]
                 [stasis "1.0.0"]
                 [ring "1.7.1"]
                 [digest "1.4.9"]
                 [hiccup "1.0.5"]
                 [enlive "1.1.6"]]
  :ring {:handler website-clj.website/app}
  :profiles {:dev {:plugins [[lein-ring "0.8.10"]]}}
  :aliases {"build-site" ["run" "-m" "website-clj.website/export"]})
