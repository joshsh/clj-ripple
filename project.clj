(defproject clj-ripple "0.1.0-SNAPSHOT"
  :dependencies [[org.clojure/clojure "1.2.0"]
                 [org.clojure/clojure-contrib "1.2.0"]
                 [org.openrdf.sesame/sesame-sail-memory "2.3.3"]
                 [net.fortytwo.ripple/ripple-blueprints "0.7-SNAPSHOT"]]
	:dev-dependencies [[lein-clojars "0.6.0"]]
	:repositories {"aduna" "http://repo.aduna-software.org/maven2/releases/"
	               "ripple" "http://fortytwo.net/maven2"}
  :namespaces :all)
