package io.fares.bind.kafka.cli;


import org.eclipse.aether.RepositorySystem;
import org.eclipse.aether.RepositorySystemSession;
import org.jetbrains.annotations.NotNull;

public class ShellContext {

  private RepositorySystem system;

  private RepositorySystemSession session;

  public RepositorySystem getSystem() {
    if (system == null) {
      throw new IllegalStateException("Maven repository system is not initialized");
    }
    return system;
  }

  public void setSystem(@NotNull RepositorySystem system) {
    this.system = system;
  }

  public @NotNull RepositorySystemSession getSession() {
    if (session == null) {
      throw new IllegalStateException("Maven repository system session is not initialized");
    }
    return session;
  }

  public void setSession(@NotNull RepositorySystemSession session) {
    this.session = session;
  }

}
