(defproject clj-ripple "0.1.0"
  :dependencies [[org.clojure/clojure "1.2.0"]
                 [org.clojure/clojure-contrib "1.2.0"]
                 [org.openrdf.sesame/sesame-sail-memory "2.3.3"]
                 [net.fortytwo.ripple/ripple-blueprints "0.7-SNAPSHOT"]]
	:repositories {"aduna" "http://repo.aduna-software.org/maven2/releases/"
	               "ripple" "http://fortytwo.net/maven2"}
  :namespaces :all)
