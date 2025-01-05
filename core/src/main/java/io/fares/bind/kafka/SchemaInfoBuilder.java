package io.fares.bind.kafka;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.confluent.kafka.schemaregistry.annotations.Schema;
import io.github.classgraph.AnnotationInfo;
import io.github.classgraph.ClassInfo;
import io.github.classgraph.Resource;
import org.jetbrains.annotations.NotNull;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.net.URI;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;

class SchemaInfoBuilder {

  /**
   * the jackson mapper used to poke around in the schema text
   */
  private final ObjectMapper mapper;

  /**
   * any references associated with the schema that need to be linked
   */
  private final List<SchemaReference> refs = new ArrayList<>();

  /**
   * the resourceName of the resource which provided the schema (e.g. the class resourceName for annotations or file resourceName for resources)
   */
  private String resourceName;

  /**
   * the uri of the resource if it was loaded from a filesystem resource such as file, zip or jar
   */
  private URI resourceUri;

  /**
   * the raw string of the schema either read from the annotation or the file resource
   */
  private String schemaText;


  private SchemaInfoBuilder(@NotNull ObjectMapper mapper) {
    this.mapper = mapper;
  }

  public static @NotNull SchemaInfoBuilder schemaInfoBuilder() {
    return schemaInfoBuilder(new ObjectMapper());
  }

  public static @NotNull SchemaInfoBuilder schemaInfoBuilder(@NotNull ObjectMapper mapper) {
    return new SchemaInfoBuilder(mapper);
  }

  public static @NotNull SchemaInfoBuilder schemaInfoBuilder(@NotNull Consumer<ObjectMapper> configurer) {
    ObjectMapper mapper = new ObjectMapper();
    configurer.accept(mapper);
    return new SchemaInfoBuilder(mapper);
  }

  public SchemaInfoBuilder value(@NotNull String value) {
    this.schemaText = value;
    return this;
  }

  private void addRefs(Object... refs) {
    for (Object ref : refs) {
      if (ref instanceof AnnotationInfo annotationInfo) {
        final SchemaReference schemaReference = new SchemaReference();
        annotationInfo.getParameterValues(true).forEach(value -> {
          if ("name".equals(value.getName()) && value.getValue() instanceof String str) {
            schemaReference.setName(str);
          } else if ("subject".equals(value.getName()) && value.getValue() instanceof String str) {
            schemaReference.setSubject(str);
          } else if ("version".equals(value.getName()) && value.getValue() instanceof Integer intVal) {
            schemaReference.setVersion(intVal);
          }
        });
        // TODO validate the ref is complete
        this.refs.add(schemaReference);
      }
    }
  }

  public SchemaInfoBuilder load(@NotNull Resource resource) {
    try {
      schemaText = resource.getContentAsString();

      // extract the full name of the resource
      resourceUri = resource.getURI().normalize();

      // extract the simple name without path from the resource
      String path = resource.getPath();
      int idx = path.lastIndexOf("/");
      if (idx > -1) {
        resourceName = path.substring(idx + 1);
      } else {
        resourceName = path;
      }
      return this;
    } catch (IOException e) {
      throw new UncheckedIOException("failed to load schema from resource " + resource.getURI(), e);
    }
  }

  public SchemaInfoBuilder load(@NotNull ClassInfo classInfo) {
    final AnnotationInfo annotationInfo = classInfo.getAnnotationInfo(Schema.class);
    annotationInfo.getParameterValues(true)
      .forEach(value -> {
        if ("value".equals(value.getName()) && value.getValue() instanceof String str) {
          // the value of the schema annotation is the entity definition schema
          this.schemaText = str;
        } else if ("refs".equals(value.getName()) && value.getValue() instanceof Object[] arr) {
          // any ref annotations that need to be attached to the schema
          addRefs(arr);
        }
      });
    resourceName = classInfo.getName();
    resourceUri = classInfo.getResource().getURI();
    return this;
  }

  public SchemaInfo build() throws SchemaParseException {
    // TODO validate schemaInfo
    try {
      JsonNode node = mapper.readTree(schemaText);

      return new SchemaInfo(
        resourceName,
        resourceUri,
        node.path("$schema").asText(),
        node.path("title").asText(),
        node.path("$ref").asText(null),
        mapper.writerWithDefaultPrettyPrinter().writeValueAsString(node),
        refs
      );

    } catch (JsonProcessingException e) {
      throw new SchemaParseException("failed to parse schema from " + resourceName, e);
    }
  }


}
