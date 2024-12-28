package io.fares.bind.kafka;

import io.confluent.kafka.schemaregistry.annotations.Schema;
import io.github.classgraph.ClassGraph;
import io.github.classgraph.ScanResult;
import org.jetbrains.annotations.NotNull;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.List;

import static io.fares.bind.kafka.SchemaInfoBuilder.schemaInfoBuilder;

/**
 * Scans a given classloader for {@code .jschema} files and annotated classes
 * with Kafka schema registry annotations and builds a list of schema information.
 */
class ModelScanner {

  /**
   * the scanner just wraps the {@link ClassGraph} scanner, we use a handle to classgraph to enable debug
   */
  private static final Logger classGraphLogger = LoggerFactory.getLogger("io.github.classgraph.ClassGraph");

  /**
   * the classgraph scanner used under the covers to scan for resources in the artifact classloader
   */
  private final ClassGraph classGraph;

  /**
   * Create an instance of the model scanner.
   *
   * @param artifactClassLoader the classloader used to scan for resources
   */
  public ModelScanner(@NotNull ClassLoader artifactClassLoader) {
    classGraph = new ClassGraph()
      .verbose(classGraphLogger.isDebugEnabled())
      .enableAnnotationInfo()
      .ignoreParentClassLoaders()
      .ignoreParentModuleLayers()
      .overrideClassLoaders(artifactClassLoader);
  }

  /**
   * Scans through the provided classloader and returns all present resources or annotations.
   *
   * @return the list of schema found
   */
  public List<SchemaInfo> scan() {

    List<SchemaInfo> schemaInfo = new ArrayList<>();

    try (ScanResult scan = classGraph.scan()) {

      // find all files ending in *.jschema
      scan.getResourcesWithExtension("jschema")
        .forEach(resourceInfo -> schemaInfo.add(schemaInfoBuilder().load(resourceInfo).build()));

      // find classes annotated with @io.confluent.kafka.schemaregistry.annotations.Schema
      scan.getClassesWithAnnotation(Schema.class)
        .forEach(classInfo -> schemaInfo.add(schemaInfoBuilder().load(classInfo).build()));

    }

    return schemaInfo;

  }

}