# Reverse OTTR

Reverse OTTR is a query language for RDF, defined as the reverse of the OTTR expansion process. See https://ottr.xyz.
This is an implementation of Reverse OTTR, computing the reverse of the OTTR implementation, Lutra. See https://gitlab.com/ottr/lutra/lutra.

Reasonable Ontology Templates (OTTR) encapsulates Resource Description Framework (RDF) graph patterns in templates. The graph patterns in OTTR templates have variables that are replaced with RDF terms during the expansion process, building a graph. The reversal of this process describes a query language.

## Dependencies

- Java 11
- Lutra (version 0.6.10)

This project is compiled and executed using Lutra release 0.6.10 as a .jar-file. Lutra .jar-files for different versions can be found at https://ottr.xyz/downloads/lutra/.

## Execution

This implementation offers no standard method of execution, but functions simply as API to be imported into other Java projects. The main entry-point is the evaluateQuery-method in the Evaluator-class. Given a file Test.java that imports the packages of this repository, and Lutra version 0.6.10 in lutra.jar, then Test.java can be compiled and executed from the commandline as follows, assuming Java is properly installed.

```
javac -jar lutra.jar Test.java
java -jar lutra.jar Test
```
