package io.fares.bind.maven;

import org.apache.maven.settings.Profile;
import org.apache.maven.settings.Repository;
import org.apache.maven.settings.Settings;
import org.jetbrains.annotations.NotNull;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.file.Path;
import java.nio.file.Paths;

import static java.util.Arrays.stream;
import static java.util.Collections.singletonList;

public class SettingsBuilder {

  private static final Logger log = LoggerFactory.getLogger(SettingsBuilder.class);

  private Path repositoryPath;

  /**
   * Returns the current directory as a {@link File}.
   *
   * @return the current directory
   * @throws UncheckedIOException if the current directory cannot be obtained
   */
  public static @NotNull File currentDirectory() {
    try {
      return new File(".").getCanonicalFile();
    } catch (IOException e) {
      throw new UncheckedIOException("unable to get current directory", e);
    }
  }

  public static SettingsBuilder builder() {
    return new SettingsBuilder();
  }

  public SettingsBuilder fileRepository(@NotNull Path path) {
    this.repositoryPath = path;
    return this;
  }

  /**
   * Constructs a {@link Path} for the {@code pwd} (current path) joining the folders to point to a file based repository.
   *
   * @param folders the folders used to construct the path
   * @return the builder instance
   */
  public SettingsBuilder fileRepository(@NotNull String... folders) {
    return fileRepository(currentDirectory().toPath(), folders);
  }

  /**
   * Constructs a {@link Path} for the given folders to point to a file based repository.
   *
   * @param baseDirectoryPath the base directory from where to start the path construction using the folders
   * @param folders           the folders used to construct the path
   * @return the builder instance
   */
  public SettingsBuilder fileRepository(@NotNull Path baseDirectoryPath, @NotNull String... folders) {
    return fileRepository(stream(folders)
      .map(Paths::get)
      .reduce(baseDirectoryPath, Path::resolve)
      .normalize()
      .toAbsolutePath());
  }

  /**
   * Builds a maven setting with a single repository pointing to a file system based repository.
   *
   * @return the maven settings
   */
  public Settings build() {

    if (repositoryPath == null) {
      throw new IllegalStateException("repository path is not configured");
    }

    Repository repository = new Repository();
    repository.setId("local");
    repository.setUrl(repositoryPath.toUri().toString());

    Profile profile = new Profile();
    profile.setId("default");
    profile.setRepositories(singletonList(repository));

    Settings settings = new Settings();
    settings.addProfile(profile);
    settings.addActiveProfile(profile.getId());

    return settings;

  }


}
