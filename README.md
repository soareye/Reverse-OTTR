# Reverse OTTR

Reverse OTTR is a query language for RDF, defined as the reverse of the OTTR expansion process. See https://ottr.xyz.
This is an implementation of Reverse OTTR, computing the reverse of the OTTR implementation, Lutra. See https://gitlab.com/ottr/lutra/lutra.

Reasonable Ontology Templates (OTTR) encapsulates Resource Description Framework (RDF) graph patterns in templates. The graph patterns in OTTR templates have variables that are replaced with RDF terms during the expansion process, building a graph. The reversal of this process describes a query language and is implemented in this project.

## Dependencies

- Java 11
- Lutra (version 0.6.10)

This project is compiled and executed using Lutra release 0.6.10 as a .jar-file. Lutra .jar-files for different versions can be found at https://ottr.xyz/downloads/lutra/.

## Execution

This implementation offers no standard method of execution, but functions simply as an API to be imported into other Java projects. The main entry-point is the evaluateQuery-method in the Evaluator-class. Given a file Test.java that imports the packages of this repository, and Lutra version 0.6.10 in lutra.jar, then Test.java can be compiled and executed from the commandline as follows, assuming Java is properly installed.

```
javac -jar lutra.jar Test.java
java -jar lutra.jar Test
```

## Structure
The main package reverseottr contains three sub-packages evaluator, model and reader.
- evaluator contains the main evaluation functions of Reverse OTTR, as well as an implementation of reverse list expansion,
- model encodes placeholders as terms, as well as the partial order on ground terms and provides methods for computing the greatest lower bound of two ground terms and to check whether two terms are less than or equal to each other.
- reader contains helper-classes for reading graphs and template libraries. The reader.RDFToOTTR-class provides a method for translating a graph into mappings that represent instances of the Triple and NullableTriple base templates.

The following UML Package diagram gives an overview of the package system of this repository and shows its dependence on the overarching package of Lutra.

![UML Package diagram for the reverseottr-package](https://github.com/soareye/Reverse-OTTR/blob/main/package.jpg)

The following UML Class diagram gives an overview of the different classes in this repository and how they depend on each other.

![UML Class diagram for the reverseottr-package](https://github.com/soareye/Reverse-OTTR/blob/main/class.jpg)