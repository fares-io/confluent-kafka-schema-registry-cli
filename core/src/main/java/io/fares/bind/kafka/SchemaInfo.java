package io.fares.bind.kafka;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.net.URI;
import java.util.List;
import java.util.Optional;

/**
 * Information gathered about the schema definition from scanning the libraries.
 *
 * @param resourceName the resourceName of the resource which provided the schema (e.g. the class resourceName for annotations or file resourceName for resources)
 * @param schema       the raw value of the {@code $schema} field
 * @param subject      the raw value of the {@code title} field which is mapped to the subject in Kafka schema registry
 * @param ref          the raw value of the {@code $ref} field indicating a relationship to another schema resource
 * @param text         the raw string of the schema either read from the annotation or the file resource
 * @param refs         any references attached to the schema usually using the @{@link io.confluent.kafka.schemaregistry.annotations.SchemaReference} annotation
 */
public record SchemaInfo(
  @NotNull String resourceName,
  @NotNull URI resourceUri,
  @NotNull String schema,
  @NotNull String subject,
  @Nullable String ref,
  @NotNull String text,
  @NotNull List<SchemaReference> refs) implements Comparable<SchemaInfo> {

  public @NotNull Optional<String> refPath() {
    return Optional.ofNullable(ref).map(r -> {
      int anchorIdx = r.indexOf("#");
      if (anchorIdx > 0) {
        return r.substring(0, anchorIdx);
      } else {
        return r;
      }
    });
  }

  /**
   * @return {@code true} when there are no references discovered in annotations and there is no {@code $ref} field in the schema that might point to an external schema
   */
  public boolean hasNoReferences() {
    return ref == null && refs.isEmpty();
  }

  /**
   * @return the negated version of {@link #hasNoReferences()}
   */
  public boolean hasReferences() {
    return !hasNoReferences();
  }

  @Override
  public String toString() {

    StringBuilder sb = new StringBuilder("schema\n").append(text);

    if (!refs.isEmpty()) {
      sb.append('\n').append("refs [");
      refs.forEach(ref -> sb.append("\n  ").append(ref));
      sb.append("\n]");
    }

    return sb.toString();
  }

  @Override
  public int compareTo(@NotNull SchemaInfo o) {
    if (this.hasNoReferences() == o.hasNoReferences()) {
      return 0;
    } else if (this.hasNoReferences() && o.hasReferences()) {
      return -1;
    } else {
      return 1;
    }
  }

}
