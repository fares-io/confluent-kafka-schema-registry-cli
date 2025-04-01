package io.fares.bind.maven;

import org.apache.maven.settings.Settings;
import org.codehaus.plexus.classworlds.ClassWorld;
import org.codehaus.plexus.classworlds.realm.ClassRealm;
import org.codehaus.plexus.classworlds.realm.DuplicateRealmException;
import org.codehaus.plexus.classworlds.realm.NoSuchRealmException;
import org.eclipse.aether.DefaultRepositorySystemSession;
import org.eclipse.aether.RepositorySystem;
import org.eclipse.aether.RepositorySystemSession;
import org.eclipse.aether.artifact.Artifact;
import org.eclipse.aether.artifact.DefaultArtifact;
import org.eclipse.aether.repository.LocalRepository;
import org.eclipse.aether.repository.LocalRepositoryManager;
import org.eclipse.aether.repository.RemoteRepository;
import org.eclipse.aether.resolution.ArtifactRequest;
import org.eclipse.aether.resolution.ArtifactResolutionException;
import org.eclipse.aether.resolution.ArtifactResult;
import org.jetbrains.annotations.NotNull;

import java.io.Closeable;
import java.io.File;
import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URLClassLoader;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

public class Aether implements Closeable, ClassLoaderProvider {

  public static final String MAVEN_DEPENDENCY_REALM_ID = "dependencies";

  /**
   * the repository system used to resolve artifacts
   */
  private final RepositorySystem system;

  /**
   * the repository system session used to resolve artifacts
   */
  private final RepositorySystemSession session;

  /**
   * the maven settings used to configure the aether component
   */
  private final Settings settings;

  /**
   * the remote repository templates used when searching for artifacts
   */
  private final List<RemoteRepository> remoteRepositoryTemplates = new ArrayList<>();

  /**
   * the separated classloader used to provide access to the resolved artifacts
   */
  private final ClassWorld classWorld;

  public Aether(@NotNull RepositorySystem system,
                @NotNull RepositorySystemSession session,
                @NotNull Settings settings) {
    this(system, session, settings, new ClassWorld("application", Thread.currentThread().getContextClassLoader()));
  }

  public Aether(@NotNull RepositorySystem system,
                @NotNull RepositorySystemSession session,
                @NotNull Settings settings,
                @NotNull ClassWorld classWorld) {
    this.system = system;
    this.session = session;
    this.settings = settings;
    this.classWorld = classWorld;

    // TODO check if we can offload the remote repository configuration to either system or session
    // TODO need to also consider mirror settings

    List<String> activeProfiles = settings.getActiveProfiles();
    // generate a remote repository template for each repository in the provided settings
    settings.getProfiles().stream()
      .filter(profile -> activeProfiles.contains(profile.getId()))
      .flatMap(profile -> profile.getRepositories().stream())
      .map(repository -> new RemoteRepository.Builder(repository.getId(), repository.getLayout(), repository.getUrl()).build())
      .forEach(remoteRepositoryTemplates::add);

    try {
      classWorld.newRealm(MAVEN_DEPENDENCY_REALM_ID, null);
    } catch (DuplicateRealmException e) {
      throw new IllegalArgumentException("failed to create artifact realm", e);
    }

  }

  public @NotNull RepositorySystem getSystem() {
    return system;
  }

  public @NotNull RepositorySystemSession getSession() {
    return session;
  }

  public @NotNull Settings getSettings() {
    return settings;
  }

  public @NotNull ClassRealm getMavenRealm() {
    return classWorld.getClassRealm(MAVEN_DEPENDENCY_REALM_ID);
  }

  /**
   * Replace the local repository on the maven repository session.
   *
   * @param localRepositoryFolder the folder where the local repository will be created
   */
  public void changeLocalRepository(@NotNull File localRepositoryFolder) {
    if (session instanceof DefaultRepositorySystemSession defaultSession) {
      LocalRepository localRepository = new LocalRepository(localRepositoryFolder);
      LocalRepositoryManager localRepositoryManager = system.newLocalRepositoryManager(session, localRepository);
      defaultSession.setLocalRepositoryManager(localRepositoryManager);
      // TODO clear the separated classloader as the resources are no longer loaded
    } else {
      throw new IllegalStateException("unable to update the local repository on the repository system session");
    }
  }

  /**
   * Get the classloader containing the resolved artifacts.
   *
   * @return the classloader
   */
  @Override
  public URLClassLoader getClassLoader() {
    return classWorld.getClassRealm(MAVEN_DEPENDENCY_REALM_ID);
  }

  /**
   * Resolves and downloads the artifact GAV provided.
   *
   * @param coordinates the maven coordinates in GAV format
   * @return the artifact resolution result
   * @throws ArtifactResolutionException when the artifact cannot be resolved
   */
  public @NotNull ArtifactResult resolveArtifact(@NotNull String coordinates) throws ArtifactResolutionException {
    return resolveArtifact(new DefaultArtifact(coordinates));
  }

  /**
   * Resolves and downloads the artifact provided.
   *
   * @param artifact the artifact to resolve
   * @return the artifact resolution result
   * @throws ArtifactResolutionException when the artifact cannot be resolved
   */
  public @NotNull ArtifactResult resolveArtifact(@NotNull Artifact artifact) throws ArtifactResolutionException {

    final String requestId = UUID.randomUUID().toString();

    ArtifactResult artifactResult = system.resolveArtifact(session, new ArtifactRequest(artifact, getRemoteRepositories(), requestId));

    if (artifactResult.isResolved() &&
      artifactResult.getLocalArtifactResult() != null &&
      !artifactResult.getLocalArtifactResult().isAvailable()) {
      // FIXME not sure why we need to double dip here
      artifactResult = system.resolveArtifact(session, new ArtifactRequest(artifact, getRemoteRepositories(), requestId));
    }

    // barf when errors in the response
    if (!artifactResult.getExceptions().isEmpty()) {
      throw new ArtifactResolutionException(List.of(artifactResult), "failed to resolve artifact: " + artifact);
    }

    return artifactResult;

  }

  /**
   * Resolves and then loads the artifact into a special classloader that is partitioned so that only the loaded artifacts are made available for searching.
   *
   * @param coordinates the maven coordinates in GAV format
   * @throws ArtifactResolutionException when the artifact cannot be resolved
   */
  public void loadArtifact(@NotNull String coordinates) throws ArtifactResolutionException {
    loadArtifact(new DefaultArtifact(coordinates));
  }

  /**
   * Resolves and then loads the artifact into a special classloader that is partitioned so that only the loaded artifacts are made available for searching.
   *
   * @param artifact the artifact to load
   * @throws ArtifactResolutionException when the artifact cannot be resolved
   */
  public void loadArtifact(@NotNull Artifact artifact) throws ArtifactResolutionException {

    ArtifactResult artifactResult = resolveArtifact(artifact);
    File artifactJar = artifactResult.getLocalArtifactResult().getFile();

    try {
      classWorld.getClassRealm(MAVEN_DEPENDENCY_REALM_ID).addURL(artifactJar.toURI().toURL());
    } catch (MalformedURLException e) {
      throw new IllegalArgumentException("failed to load artifact " + artifactJar.getAbsolutePath(), e);
    }

  }

  public void unloadArtifacts() {
    try {
      classWorld.disposeRealm(MAVEN_DEPENDENCY_REALM_ID);
      classWorld.newRealm(MAVEN_DEPENDENCY_REALM_ID, null);
    } catch (NoSuchRealmException | DuplicateRealmException e) {
      throw new IllegalStateException("failed to clear maven dependencies realm", e);
    }
  }

  /**
   * Closes the underlying classloader used to manage scanning of the model artifacts.
   *
   * @throws IOException when the classloader cannot be closed
   */
  @Override
  public void close() throws IOException {
    classWorld.close();
  }

  /**
   * Get the remote repositories as configured in the maven settings.
   *
   * @return the remote repositories
   */
  private @NotNull List<RemoteRepository> getRemoteRepositories() {
    return system.newResolutionRepositories(session, remoteRepositoryTemplates);
  }

}
