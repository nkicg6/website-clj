(defproject website-clj "0.1.0-SNAPSHOT"
  :description "Personal website built with Clojure, Stasis, and Hiccup"
  :url "http://nickgeorge.net"
  :license {:name "Eclipse Public License"
            :url "http://www.eclipse.org/legal/epl-v10.html"}
  :dependencies [[org.clojure/clojure "1.8.0"]
                 [stasis "1.0.0"]
                 [ring "1.2.1"]
                 [hiccup "1.0.5"]
                 [me.raynes/cegdown "0.1.1"]]
  :ring {:handler website-clj.web/app}
  :profiles {:dev {:plugins [[lein-ring "0.8.10"]]}})
