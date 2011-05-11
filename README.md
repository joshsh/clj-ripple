
clj-ripple
==========

clj-ripple is a library for translating Clojure source code into Ripple source code, which can then be executed.
This allows for easy embedding of Ripple into Clojure programs.

Usage
-----

Simply add this to your leiningen deps: `[clj-ripple "0.1.0-SNAPSHOT"] `

Syntax
------

The `ripple` macro takes specially formatted Clojure source code and translates it (using the `translate` function) into the equivalent Ripple source code.
It then executes it using the `run-ripple` function, inside an evironment created with the `with-ripple` macro, which dynamically binds the `*ripple*` var
to a fresh `QueryPipe` and the `*sink*` var to a fresh `Collector<T,E>` sink.

Although all of those components (`with-ripple`, `run-ripple` & `translate`) can be used separately, it is best to simple use the `ripple` macro.

The result is return as a Clojure lazy sequence containing all the stacks as sequences.
The topmost elements in the stacks appear first and are followed by the elements at the bottom.
Example: ((top, top-1, top-2), (top, top-1), (top, top-1, top-2, ..., top-n))

Examples
--------

clj-ripple syntax and its equivalent Ripple source code can be understood by reading the following examples:

	; Before code is executed, Ripple must be initialized through the init-ripple function. When passed no parameters, it initializes Ripple
	; with a default configuration (see Ripple wiki's "Running Ripple")
	; For a custom configuration, pass the properties as a Clojure hash-map.
	(init-ripple)

	@prefix foaf: <http://xmlns.com/foaf/0.1/>
	@list dan: <http://danbri.org/foaf.rdf#danbri>
	:dan. foaf:knows. foaf:name.
	(ripple
	  (prefix foaf "http://xmlns.com/foaf/0.1/") ; An string passed to prefix is inmediately put inside <>
	  (def dan @"http://danbri.org/foaf.rdf#danbri") ; Since regular strings can be used as data, in non-prefix situations, strings must be dereferenced in order to be evaluated as URLs.
	  (!/dan foaf/knows foaf/name) ; !/ is the default namespace.
	  )

	@list days: "Monday" "Tuesday" "Wednesday" "Thursday" "Friday" "Saturday" "Sunday"
	1 2 3 :days.
	(ripple
	  (def days "Monday" "Tuesday" "Wednesday" "Thursday" "Friday" "Saturday" "Sunday")
	  (1 2 3 !/days) ; By default, lists and other primitives are applied the . (op) operator.
	  )

	@list days: "Monday" "Tuesday" "Wednesday" "Thursday" "Friday" "Saturday" "Sunday"
	1 2 3 :days
	(ripple
	  (def days "Monday" "Tuesday" "Wednesday" "Thursday" "Friday" "Saturday" "Sunday")
	  (1 2 3 '!/days) ; But quoting them removes the . (op) operator.
	  )

	16 dup. mul.
	(ripple
	  (16 dup mul)
	  )

	@list n triangle:
	  n 1 add. n mul. 2 div.
	4 :triangle.
	(ripple
	  (def triangle [n] ; To create a parametered list, give it an args vector.
	    n 1 add n mul 2 div)
	  (4 !/triangle)
	  )

	@list fact:
	  dup. 0 equal.
	  (1 popd.)
	  (dup. 1 sub. :fact. mul.)
	  branch.
	5 :fact.
	(ripple
	  (def fact
	    dup 0 equal
	    (1 popd)
	    (dup 1 sub !/fact mul)
	    branch)
	  (5 !/fact)
	  )

	; Top level lists are evaluated as single lines of Ripple code. Different lines must be specified as different lists to avoid confusion
	; Lists inside lists, however, are interpreted as Ripple lists.

	@show prefixes
	@show contexts
	(ripple
	  (show :prefixes)
	  (show :contexts)
	  )

	(10 20 30) rdf:rest{2} rdf:first.
	(ripple
	  ((10 20 30) [rdf/rest 2] rdf/first)
	  )

	(10 20 30) rdf:rest{0,1} rdf:first.
	(ripple
	  ((10 20 30) [rdf/rest 0 1] rdf/first)
	  )

Further Information
-------------------

For more information about Ripple, please visit the following websites:

1.	http://ripple.fortytwo.net/

2.	https://github.com/joshsh/ripple/wiki

