package io.fares.bind.kafka;

import io.fares.bind.maven.*;
import org.apache.maven.settings.Settings;
import org.eclipse.aether.RepositorySystem;
import org.eclipse.aether.RepositorySystemSession;
import org.eclipse.aether.artifact.Artifact;
import org.eclipse.aether.artifact.DefaultArtifact;
import org.eclipse.aether.resolution.ArtifactResolutionException;
import org.eclipse.aether.resolution.ArtifactResult;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.AssertionsForClassTypes.assertThatThrownBy;
import static org.assertj.core.util.Files.currentFolder;

@ExtendWith(MavenExtension.class)
class ArtifactResolutionTest {

  private static final Logger log = LoggerFactory.getLogger(ArtifactResolutionTest.class);

  // set up the remote repository pointing to the filesystem based test support repository
  private final Settings settings = SettingsBuilder.builder()
    .fileRepository(currentFolder().toPath(), "..", "test-support", "repository")
    .build();

  @MavenRepositorySystem
  private RepositorySystem system;

  @MavenRepositorySystemSession
  private RepositorySystemSession session;

  /**
   * the artifact resolver used to retrieve the remote resources
   */
  private Aether aether;

  @BeforeEach
  void setupAether() {
    aether = new Aether(system, session, settings);
  }

  @Test
  void itShouldResolveArtifactFromRepository() throws Exception {

    Artifact artifact = new DefaultArtifact("io.fares.examples:alpha-model-project::1.0.0");

    ArtifactResult artifactResult = aether.resolveArtifact(artifact.toString());

    assertThat(artifactResult)
      .isNotNull()
      .extracting(ArtifactResult::getLocalArtifactResult)
      .isNotNull();

    File artifactJar = artifactResult.getLocalArtifactResult().getFile();

    log.atInfo()
      .setMessage("resolved artifact {} to jar {}")
      .addArgument(artifact::toString)
      .addArgument(artifactJar::getAbsolutePath)
      .log();

    assertThat(artifactJar)
      .isNotNull()
      .exists();

    aether.loadArtifact(artifact.toString());

    assertThat(aether.getClassLoader()).isNotNull();


  }

  @Test
  void itShouldFailResolutionForNonExistentArtifact() {
    Artifact artifact = new DefaultArtifact("io.fares.examples:non-existent:1.0.0");
    assertThatThrownBy(() -> aether.resolveArtifact(artifact))
      .isInstanceOf(ArtifactResolutionException.class)
      .hasMessageContaining("artifacts could not be resolved");
  }

}
