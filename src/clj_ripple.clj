;Version: 0.1.0
;Copyright: Eduardo Emilio Julián Pereyra, 2011
;Email: eduardoejp@gmail.com
;License: EPL 1.0 -> http://www.eclipse.org/legal/epl-v10.html

(ns clj-ripple
  #^{:doc "This is a library for translating Clojure source code to Ripple source code to make it easier to embed Ripple in a Clojure program."
     :author "Eduardo Emilio Julián Pereyra"}
  (:import (org.openrdf.sail.memory MemoryStore)
    (net.fortytwo.linkeddata.sail LinkedDataSail)
    (net.fortytwo.ripple.model.impl.sesame  SesameModel)
    (net.fortytwo.ripple.query QueryEngine QueryPipe)
    (net.fortytwo.flow Collector)
    (net.fortytwo.ripple Ripple)
    (java.util Properties)
    (net.fortytwo.ripple.model RippleValue RDFValue NumericValue$Type)
    (net.fortytwo.ripple.model.impl.sesame SesameNumericValue SesameList)))

(def +default-conf+
  #^{:doc "This constant defines the default configuration for a Ripple system."}
  {"net.fortytwo.ripple.demo.sailType" "net.fortytwo.linkeddata.sail.LinkedDataSail",
   "net.fortytwo.ripple.demo.linkedDataSailBaseSail" "org.openrdf.sail.memory.MemoryStore"})
(def #^{:doc "Dynamic var holding the current QueryPipe."} *ripple*)
(def #^{:doc "Dynamic var holding the current sink. It normally is a Collector sink."} *sink*)

(defn- map->props
  "Given a hash-map of the properties for Ripple, transforms it into a java.util.Properties object."
  [m]
  (let [p (Properties.)]
    (doall (for [pair m] (.setProperty p (first pair) (second pair))))
    p))

(defn init-ripple
  "Initializes Ripple given a set of configurations as a Clojure hash-map. If no properties are given, initializes Ripple with the default configuration."
  ([props] (-> props map->props list into-array Ripple/initialize) props)
  ([] (init-ripple +default-conf+)))

(defmacro with-ripple
  "Executes the given forms in an environment with *ripple* bound to a fresh QueryPipe and *sink* bound to a Collector sink."
  [& forms]
  `(let [mstore# (doto (MemoryStore.) .initialize)
         ldsail# (doto (LinkedDataSail. mstore#) .initialize)
         model# (SesameModel. ldsail#)]
     (binding [*sink* (Collector.)]
       (binding [*ripple* (-> model# QueryEngine. (QueryPipe. *sink*))]
         (let [res# (do ~@forms)]
           (.shutDown model#) (.shutDown ldsail#) (.shutDown mstore#)
           res#)))))

(defn- to-clj
  "Given a value or a list, transforms it into its Clojure equivalent."
  [value]
  (cond
    (instance? RDFValue value) (let [vstr (.toString value)]
                                 (if (and (.startsWith vstr "\"") (.endsWith vstr "\""))
                                   (.substring vstr 1 (-> value .toString .length (- 1)))
                                   vstr))
    (instance? SesameNumericValue value) (cond
                                           (= NumericValue$Type/INTEGER (.getDatatype value)) (.intValue value)
                                           (= NumericValue$Type/LONG (.getDatatype value)) (.longValue value)
                                           (= NumericValue$Type/DECIMAL (.getDatatype value)) (.decimalValue value)
                                           (= NumericValue$Type/FLOAT (.getDatatype value)) (.floatValue value)
                                           (= NumericValue$Type/DOUBLE (.getDatatype value)) (.doubleValue value))
    (instance? SesameList value) (map to-clj (.toJavaList value))
    (instance? RippleValue value) value
    ))
(defn- to-cljs
  "Same as to-clj, but en masse."
  [results] (map to-clj (.toJavaList results)))

(defn run-ripple
  "Given lines of Ripple code as strings, executes the code and returns the resulting stack as a lazy-seq."
  [& code]
  (doall (for [c code] (.put *ripple* c)))
  (map to-cljs *sink*))

(defn- add-ns
  "Namespace qualifies symbols."
  [sym]
  (if (.contains sym "/")
    (if (.startsWith sym "!/")
      (.substring (.replace sym "/" ":") 1)
      (.replace sym "/" ":"))
    sym))

(defn- trans-sym [sym]
  (if (or (.endsWith sym "?") (.endsWith sym "*") (.endsWith sym "+"))
    sym
    (str sym ".")))

(defn- trans-token
  "Given a token, returns the apropiate translation for it."
  [tok]
  (cond
    (symbol? tok) (cond
                    (= '. tok) ".", (= 'op tok) "."
                    (= '>> tok) ">>"
                    (= '<< tok) "<<"
                    :else (-> tok str trans-sym add-ns symbol))
    
    (number? tok) (str tok)
    
    (or (seq? tok) (list? tok) (instance? clojure.lang.Cons tok))
    (cond
      (= `deref (first tok)) (str "<" (second tok) ">")
      (= `quote (first tok)) (if (symbol? (second tok))
                               (-> tok second str add-ns symbol)
                               (-> tok second str))
      :else (str "(" (apply str (butlast (interleave (map trans-token tok) (repeat " ")))) ")"))
    
    (vector? tok) (->> (cond
                         (= 2 (count tok)) (str (first tok) "{" (second tok) "}")
                         (= 3 (count tok)) (str (first tok) "{" (second tok) "," (nth tok 2) "}"))
                    symbol trans-token str butlast (apply str) symbol)
    
    (string? tok) (str "\"" tok "\"")))

(let [member? (fn [coll x] (if (some #(= % x) coll) true false))]
  (defn- param-list
    "Define parametered lists/words."
    [l]
    (let [[_ word args & words] l]
      (apply str (if (= 'def _) "@list " "@relist ")
        (apply str (interleave args (repeat " ")))
        word ": "
        (-> (map #(if (member? args %) (str %) (trans-token %)) words)
          (interleave (repeat " "))
          butlast seq)
        ))))

(defn translate
  "Given an S-Expression, returns the equivalent Ripple code as a String."
  [sexp]
  (let [fexp (first sexp)]
    (cond
      (= 'prefix fexp) (if (= 2 (count sexp))
                         (str "@prefix : <" (second sexp) ">")
                         (str "@prefix " (second sexp)": <" (nth sexp 2) ">"))
      
      (= 'unprefix fexp) (str "@unprefix " (second sexp))
      
      (or (= 'def fexp) (= 'redef fexp))
      (if (vector? (nth sexp 2))
        (param-list sexp)
        (str (if (= 'def fexp) "@list " "@relist ") (second sexp) ": "
          (apply str (butlast (interleave (map trans-token (nnext sexp)) (repeat " "))))))
      
      (= 'undef fexp) (str "@unlist " (second sexp))
      
      (= 'show fexp) (str "@show " (case (second sexp) :prefixes "prefixes", :contexts "contexts"))
      
      :else (apply str (vec (butlast (interleave (map trans-token sexp) (repeat " ")))))
      )))

(defmacro ripple
  "Given a set of sexps in Clojure code, translates them to Ripple code, executes them and returns the results in the sink as a lazy-seq."
  [& sexp]
  `(with-ripple (run-ripple ~@(map translate sexp))))
