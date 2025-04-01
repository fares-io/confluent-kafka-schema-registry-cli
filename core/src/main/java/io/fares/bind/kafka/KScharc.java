package io.fares.bind.kafka;

import io.confluent.kafka.schemaregistry.ParsedSchema;
import io.confluent.kafka.schemaregistry.client.SchemaRegistryClient;
import io.confluent.kafka.schemaregistry.client.rest.exceptions.RestClientException;
import io.confluent.kafka.schemaregistry.json.JsonSchema;
import org.jetbrains.annotations.NotNull;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.util.*;
import java.util.function.Supplier;

public class KScharc {

  private static final Logger log = LoggerFactory.getLogger(KScharc.class);

  private final SchemaRegistryClient registryClient;

  private final ModelScanner scanner;

  private final Map<String, String> refPointers = new HashMap<>();

  public KScharc(@NotNull Supplier<ClassLoader> classLoader,
                 @NotNull SchemaRegistryClient registryClient) {
    this.scanner = new ModelScanner(classLoader);
    this.registryClient = registryClient;
  }

  public List<SchemaInfo> getSchemaInfo() {
    return scanner.scan();
  }

  public List<SchemaInfo> loadSchemaInfo() {

    // now we need to trail through all schema that have a ref but no associated refs

    return scanner.scan()
      .stream()
      .map(this::registerResource)
      .peek(schemaInfo ->
        log.atDebug()
          .setMessage(() -> "load {} subject={}\n{}\n")
          .addArgument(schemaInfo::resourceName)
          .addArgument(schemaInfo::subject)
          .addArgument(schemaInfo::text)
          .log()
      )
      .sorted()
      .map(this::resolveRef)
      .map(this::register)
      .toList();

  }

  private @NotNull SchemaInfo register(@NotNull SchemaInfo schemaInfo) {

    // map collected information from the annotation to the kafka entity
    List<io.confluent.kafka.schemaregistry.client.rest.entities.SchemaReference> refs = new ArrayList<>();
    schemaInfo.refs()
      .stream().map(this::toRef)
      .forEach(refs::add);

    Optional<ParsedSchema> schema = registryClient.parseSchema(JsonSchema.TYPE, schemaInfo.text(), refs);

    if (schema.isPresent()) {
      try {
        registryClient.register(schemaInfo.subject(), schema.get());
      } catch (IOException | RestClientException e) {
        throw new SchemaParseException("failed to parse schema " + schemaInfo.resourceName(), e);
      }
    } else {
      throw new SchemaParseException("unable to parse schema " + schemaInfo.resourceName());
    }
    return schemaInfo;
  }

  private @NotNull SchemaInfo resolveRef(@NotNull SchemaInfo schemaInfo) {

    if (schemaInfo.refPath().isPresent()) {

      String refPath = schemaInfo.refPath().get();

      // if we have a ref path that does not match any ref in the refs declaration,
      // we construct a schema reference from what we loaded so far
      if (schemaInfo.refs().stream().noneMatch(ref -> refPath.equals(ref.getName()))) {
        if (refPointers.containsKey(refPath)) {
          SchemaReference reference = new SchemaReference();
          reference.setName(refPath);
          reference.setSubject(refPointers.get(refPath));

          List<SchemaReference> refs = new ArrayList<>(schemaInfo.refs());
          refs.add(reference);

          return new SchemaInfo(
            schemaInfo.resourceName(),
            schemaInfo.resourceUri(),
            schemaInfo.schema(),
            schemaInfo.subject(),
            schemaInfo.ref(),
            schemaInfo.text(),
            refs);

        } else {
          log.atWarn().setMessage(() -> "schema resource {} does not have a valid schema reference for $ref {} and will most likely fail registration")
            .addArgument(schemaInfo::resourceName)
            .addArgument(schemaInfo::ref)
            .log();
        }
      }
    }

    return schemaInfo;

  }

  private @NotNull SchemaInfo registerResource(@NotNull SchemaInfo schemaInfo) {
    // TODO maybe only register if it's a file??
    refPointers.put(schemaInfo.resourceName(), schemaInfo.subject());
    return schemaInfo;
  }

  private @NotNull io.confluent.kafka.schemaregistry.client.rest.entities.SchemaReference toRef(@NotNull SchemaReference ref) {
    return new io.confluent.kafka.schemaregistry.client.rest.entities.SchemaReference(ref.getName(), ref.getSubject(), ref.getVersion());
  }

}
