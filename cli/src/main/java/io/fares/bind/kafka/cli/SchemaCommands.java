package io.fares.bind.kafka.cli;

import io.fares.bind.kafka.KScharc;
import io.fares.bind.kafka.SchemaInfo;
import io.fares.bind.maven.Aether;
import org.eclipse.aether.resolution.ArtifactResolutionException;
import org.jetbrains.annotations.NotNull;
import org.springframework.shell.command.CommandContext;
import org.springframework.shell.command.annotation.Command;
import org.springframework.shell.command.annotation.Option;

import java.io.PrintWriter;

@Command(command = "schema", alias = { "s" }, group = SchemaCommands.COMMAND_GROUP)
public class SchemaCommands {

  static final String COMMAND_GROUP = "Schema commands";

  private final KScharc kscharc;

  private final Aether aether;

  public SchemaCommands(@NotNull KScharc kscharc, @NotNull Aether aether) {
    this.kscharc = kscharc;
    this.aether = aether;
  }

  @Command(command = "list", alias = { "l", "ls" }, group = COMMAND_GROUP, description = "display all schema information inside the maven dependencies")
  public void listSchema(@NotNull CommandContext context) {
    kscharc.loadSchemaInfo().forEach(schemaInfo -> printSchemaInfo(context.getTerminal().writer(), schemaInfo));
  }

  @Command(command = "register", alias = { "r" }, group = COMMAND_GROUP, description = "registers the schema information found in loaded maven dependencies")
  public void registerSchema(@NotNull CommandContext context, @Option(longNames = "dependency", shortNames = 'd') String coordinates) throws ArtifactResolutionException {
    aether.loadArtifact(coordinates);
    kscharc.loadSchemaInfo().forEach(schemaInfo -> printSchemaInfo(context.getTerminal().writer(), schemaInfo));

  }

  private void printSchemaInfo(@NotNull PrintWriter writer, @NotNull SchemaInfo schemaInfo) {
    writer.println("---");

    writer.print("resource: ");
    writer.println(schemaInfo.resourceName());

    writer.print("subject: ");
    writer.println(schemaInfo.subject());

    if (!schemaInfo.refs().isEmpty()) {
      writer.println("refs: ");
      schemaInfo.refs().forEach(ref ->
        writer.println("  ref name=" + ref.getName() + " subject=" + ref.getSubject() + " version=" + ref.getVersion()));
    }
  }


}
