# qnx-buildfile-lang

This is an Xtext-based grammar for parsing QNX buildfiles. 

The rest of this README is for developers who want to contribute to the grammar. If you are a developer interested in using it, please go to https://gvergine.github.io/qnx-buildfile-lang

## Prerequisites

- Recent Linux with jdk 21
- Eclipse IDE for DSL Developers
- Docker (for compiling command line)

## Compile from Eclipse

- Clone this repo via Eclipse
- It should compile in Eclipse out of the box

## Compile from command line

Using docker:
```
./docker_mvn.sh clean install
```

