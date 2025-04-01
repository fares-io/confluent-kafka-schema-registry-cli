package io.fares.bind.kafka;

import io.confluent.kafka.schemaregistry.avro.AvroSchemaProvider;
import io.confluent.kafka.schemaregistry.client.SchemaRegistryClient;
import io.confluent.kafka.schemaregistry.json.JsonSchemaProvider;
import io.fares.bind.maven.*;
import org.apache.maven.settings.Settings;
import org.eclipse.aether.RepositorySystem;
import org.eclipse.aether.RepositorySystemSession;
import org.jetbrains.annotations.NotNull;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.slf4j.bridge.SLF4JBridgeHandler;

import java.util.List;

import static io.confluent.kafka.schemaregistry.client.SchemaRegistryClientFactory.newClient;
import static java.util.Collections.emptyMap;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.util.Files.currentFolder;

@ExtendWith(MavenExtension.class)
class ModelScannerTest {

  private static final org.slf4j.Logger log = org.slf4j.LoggerFactory.getLogger(ModelScannerTest.class);

  /// need to reset the java util logging root handlers and install SLF4J
  static {
    SLF4JBridgeHandler.removeHandlersForRootLogger();
    SLF4JBridgeHandler.install();
  }

  // set up the remote repository pointing to the filesystem based test support repository
  private final Settings settings = SettingsBuilder.builder()
    .fileRepository(currentFolder().toPath(), "..", "test-support", "repository")
    .build();

  private final SchemaRegistryClient schemaRegistryClient = newClient(
    List.of("mock://localhost:8081"),
    1000,
    List.of(new AvroSchemaProvider(), new JsonSchemaProvider()),
    emptyMap(),
    emptyMap()
  );

  @MavenRepositorySystem
  private RepositorySystem system;

  @MavenRepositorySystemSession
  private RepositorySystemSession session;

  /**
   * the artifact resolver used to retrieve the remote resources
   */
  private Aether aether;

  /**
   * the scanner used to extract jsonschema information from a model library
   */
  private KScharc kscharc;


  @BeforeEach
  void setupTest() {
    aether = new Aether(system, session, settings);
    kscharc = new KScharc(() -> aether.getClassLoader(), schemaRegistryClient);
  }

  @Test
  void itShouldLoadAlpha() throws Exception {
    loadSchema("io.fares.examples:alpha-model-project::1.0.0");
  }

  @Test
  void itShouldLoadBeta() throws Exception {
    loadSchema("io.fares.examples:beta-model-project::1.0.0");
  }

  @Test
  void itShouldLoadGamma() throws Exception {
    loadSchema("io.fares.examples:gamma-model-project::1.0.0");
  }

  void loadSchema(@NotNull String coordinates) throws Exception {

    // load a model into the aether classloader for scanning
    aether.loadArtifact(coordinates);

    kscharc.loadSchemaInfo().forEach(schemaInfo -> {
        log.atInfo()
          .setMessage("registered resource={} subject={} refs={}")
          .addArgument(schemaInfo::resourceName)
          .addArgument(schemaInfo::subject)
          .addArgument(schemaInfo::hasReferences)
          .log();
      }
    );

    assertThat(schemaRegistryClient.getSchemaById(1)).isNotNull();

  }


}
