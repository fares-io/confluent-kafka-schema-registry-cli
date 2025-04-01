# Confluent Kafka Schema Registry Tool

A selection of tools to build and deploy json schema models to
the [confluent kafka schema registry](https://www.confluent.io/product/confluent-platform/data-compatibility).

* maven plugin to support building schema model bindings
* `kscharc` or **k**afka **sch**em**a** **r**egistry **c**ommandline tool
* (future) terraform plugin

## Features

### Loading Model Libraries

The `Aether` module loads model libraries from a maven repository into an isolated classloader. The aether module contains state in that an artifact is registered in the separating classloader. The content of it's classloader is consumed by the `ModelScanner`.

* list maven dependencies in context
* load a maven dependency into context
* unload one/all maven dependency from context

Configuration:

* local repository
* remote repository
* authentication token to access remote repository

### Scanning Model Libraries

The `ModelScanner` will look into a classpath and find all file resources with extension `.jschema` and classes annotated with `@Schema` annotation. It will extract the information and make it available for inspection or for registration with a schema registry.

Configuration:

## Usage

Enter the interactive shell `kscharc`

```shell
# list settings used to resolve maven resources
mvn settings
# load a library into the shell context
mvn load -d "io.fares.examples:alpha-model-project::1.0.0"
mvn load -d "io.fares.examples:beta-model-project::1.0.0"
# list all libraries loaded into the shell context
mvn list
# clear all libraries loaded into the shell context
mvn clear

# display schema information in all loaded libraries
schema list
# register schema information in loaded libraries in the schema registry
schema register -d io.fares.examples:alpha-model-project::1.0.0
```


## Testing

### Example Schema Models

To facilitate deep integration testing, the project includes a set of example model projects under `test-support`. These can be opened separately in an IDE and
extended.

The projects will deploy to a local maven repository in the base of the `test-support` folder.

```shell
cd test-support/model-projects
mvn clean deploy
```

Also ensure to check in the new model jars added to the testing repository.

