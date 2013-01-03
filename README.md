# What is Jayes?

Jayes is a Bayesian Network Library for Java. It has initially been created as my bachelor's thesis
and it's goal is to provide highly efficient Bayesian Networks algorithms to the open souce community.
In fact, there are already good Bayesian Networks libraries available, but they are either closed-source,
GPL-licensed or rather inefficient. For certain projects, these aspects hinder the usage of such libraries.
This is why Jayes was created. 

# Who uses Jayes?

Jayes was first created to serve as a component of the [Code Recommenders Project](http://www.eclipse.org/recommenders/) at Eclipse.
There it is used to predict method calls invoked by the developer.
I would love to know when other people use it and how :-) 

# Features:
- exact inference of marginals in Bayesian Networks
- support of [XMLBIF 0.3](http://www.cs.cmu.edu/~fgcozman/Research/InterchangeFormat/)
- partial support of XDSL format used by [GeNIe](http://genie.sis.pitt.edu/)

# Dependencies
Since 1.0.2, Jayes depends on the Code Recommenders Project at Eclipse. The main project only uses the common build infrastructure,
and could also be built without this dependency. To this end, just delete the reference to the parent POM. 
If you don't want to specify a dependency on the tycho maven plugin, just change the packaging to "jar" (no dependencies yet for the main project).
Some subprojects may also depend on bundles from Code Recommenders.

Do the following to get it to build:

Clone http://git.eclipse.org/gitroot/recommenders/org.eclipse.recommenders.git
Perform a mvn install.
Go ahead and build Jayes!

# License

Jayes is licensed under the [Eclipse Public License 1.0](http://www.eclipse.org/legal/epl-v10.html)