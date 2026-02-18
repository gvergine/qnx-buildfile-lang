# qnx-buildfile-lang

![](documentation/images/vscode_plugin.gif)

qnx-buildfile-lang is an open-source toolkit for QNX buildfiles (.build). It provides:

- **VS Code extension**: real-time validation, content assist, quickfixes, outline, syntax highlighting
- **Eclipse plugin**: same features for Eclipse-based workflows
- **CLI validator**: validate buildfiles from the command line or CI pipelines
- **Java library**: parse, validate, and manipulate buildfiles programmatically  

The rest of this README is for developers who want to contribute to the grammar. If you are a developer interested in using it, please go to https://gvergine.github.io/qnx-buildfile-lang

## Prerequisites

- Recent Linux with jdk 17
- Eclipse IDE for DSL Developers
  Maven (minimum version 3.9)

## Compile from Eclipse

- Clone this repo via Eclipse
- It should compile in Eclipse out of the box

## Compile from command line

```
mvn clean package
```

## Release (only for mantainers)

```
mvn -P release clean deploy -DskipPublishing=true
```

Test and publish manually on Sonatype.
