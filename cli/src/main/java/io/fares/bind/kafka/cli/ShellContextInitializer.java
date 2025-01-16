package io.fares.bind.kafka.cli;

import org.eclipse.aether.RepositorySystem;
import org.eclipse.aether.RepositorySystemSession;
import org.jetbrains.annotations.NotNull;
import org.springframework.beans.BeansException;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationContextAware;

/**
 * Initialises the {@link ShellContext} with components needed during execution of tasks.
 */
public class ShellContextInitializer implements ApplicationContextAware {

  @Override
  public void setApplicationContext(@NotNull ApplicationContext applicationContext) throws BeansException {

    ShellContext shellContext = ShellContextHolder.getContext();

    RepositorySystem system = applicationContext.getBean(RepositorySystem.class);
    shellContext.setSystem(system);

    RepositorySystemSession session = applicationContext.getBean(RepositorySystemSession.class);
    shellContext.setSession(session);

  }

}
