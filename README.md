# Confluent Kafka Schema Registry Tool

A selection of tools to build and deploy json schema models to
the [confluent kafka schema registry](https://www.confluent.io/product/confluent-platform/data-compatibility).

* maven plugin to support building schema model bindings
* `kscharc` or **k**afka **sch**em**a** **r**egistry **c**ommandline tool
* (future) terraform plugin

## Features

### Loading Model Libraries

The `Aether` system will allow the kscharc tool to load model libraries from a maven repository into a separating classpath loader.

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

