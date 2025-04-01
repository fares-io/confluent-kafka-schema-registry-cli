package io.fares.bind.kafka.cli;

import io.fares.bind.maven.Aether;
import org.apache.maven.settings.Profile;
import org.apache.maven.settings.Server;
import org.apache.maven.settings.Settings;
import org.eclipse.aether.resolution.ArtifactResolutionException;
import org.jetbrains.annotations.NotNull;
import org.jline.terminal.Terminal;
import org.springframework.shell.command.CommandContext;
import org.springframework.shell.command.annotation.Command;
import org.springframework.shell.command.annotation.Option;

import java.io.PrintWriter;
import java.net.URL;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

@Command(command = "mvn", alias = { "m" }, group = MavenCommands.COMMAND_GROUP)
public class MavenCommands {

  static final String COMMAND_GROUP = "Maven commands";

  private final Aether aether;

  public MavenCommands(@NotNull Aether aether) {
    this.aether = aether;
  }

  @Command(command = "settings", alias = { "s" }, group = COMMAND_GROUP, description = "display the maven settings used to load model libraries")
  public void displaySettings(@NotNull CommandContext context) {
    Terminal terminal = context.getTerminal();
    PrintWriter writer = terminal.writer();
    Settings settings = aether.getSettings();
    writer.println("servers: " + settings.getServers().stream().map(Server::getId).collect(Collectors.joining(" ")));
    writer.println("active profiles: " + String.join(" ", settings.getActiveProfiles()));
    writer.println("remote repositories: ");
    settings.getProfiles().stream()
      .filter(profile -> settings.getActiveProfiles().contains(profile.getId()))
      .map(Profile::getRepositories)
      .flatMap(List::stream)
      .forEach(repository -> {
        writer.println("  " + repository.getId() + " > " + repository.getUrl());
      });
    writer.println("local repository: " + aether.getSession().getLocalRepository().getBasedir().getAbsolutePath());
  }

  @Command(command = "list", alias = { "l", "ls" }, group = COMMAND_GROUP, description = "display all loaded maven dependencies")
  public String listDependencies(@NotNull CommandContext context) {
    return Arrays.stream(aether.getClassLoader().getURLs()).map(URL::toString)
      .collect(Collectors.joining("\n"));
  }

  @Command(command = "load", group = COMMAND_GROUP, description = "load a maven dependency into context")
  public String loadDependencies(@Option(longNames = "dependency", shortNames = 'd') String coordinates) throws ArtifactResolutionException {
    aether.loadArtifact(coordinates);
    return "loaded artifact " + coordinates;
  }

  @Command(command = "clear", group = COMMAND_GROUP, description = "clear a maven dependency from context")
  public String clear() {
    aether.unloadArtifacts();
    return "unloaded all artifacts";
  }

}
