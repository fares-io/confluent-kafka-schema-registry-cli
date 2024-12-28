package io.fares.bind.maven;

import org.apache.maven.cli.transfer.Slf4jMavenTransferListener;
import org.apache.maven.repository.internal.MavenRepositorySystemUtils;
import org.eclipse.aether.DefaultRepositorySystemSession;
import org.eclipse.aether.RepositorySystem;
import org.eclipse.aether.repository.LocalRepository;
import org.eclipse.aether.repository.LocalRepositoryManager;
import org.eclipse.aether.supplier.RepositorySystemSupplier;
import org.jetbrains.annotations.NotNull;
import org.junit.jupiter.api.extension.AfterAllCallback;
import org.junit.jupiter.api.extension.BeforeAllCallback;
import org.junit.jupiter.api.extension.BeforeEachCallback;
import org.junit.jupiter.api.extension.ExtensionContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.nio.file.Path;
import java.nio.file.Paths;

import static io.fares.bind.maven.SettingsBuilder.currentDirectory;
import static java.util.Arrays.stream;
import static org.junit.jupiter.api.extension.ExtensionContext.Namespace.create;
import static org.junit.platform.commons.support.AnnotationSupport.findAnnotatedFields;
import static org.junit.platform.commons.util.AnnotationUtils.findAnnotation;

/**
 * A supporting class to initialise a maven system used for testing.
 */
public class MavenExtension implements BeforeAllCallback, AfterAllCallback, BeforeEachCallback {

  private static final Logger log = LoggerFactory.getLogger(MavenExtension.class);

  private static final ExtensionContext.Namespace MAVEN = create("io.fares.maven");

  private static final String SYSTEM = "system", SESSION = "session";


  /**
   * Create the maven embedder repository system.
   *
   * @param context the current extension context
   */
  @Override
  public void beforeAll(@NotNull ExtensionContext context) {
    RepositorySystemSupplier repositorySystemSupplier = new RepositorySystemSupplier();
    RepositorySystem system = repositorySystemSupplier.get();
    context.getStore(MAVEN).put(SYSTEM, system);
  }

  /**
   * Shutdown the maven embedder repository system.
   *
   * @param context the current extension context
   */
  @Override
  public void afterAll(@NotNull ExtensionContext context) {
    final RepositorySystem system = context.getStore(MAVEN).get(SYSTEM, RepositorySystem.class);
    system.shutdown();
  }

  /**
   * Inject the maven repository system session into the test instance for each test.
   *
   * @param context the current extension context
   */
  @Override
  public void beforeEach(@NotNull ExtensionContext context) {

    // get the repository system from context store so we can inject it and create a session
    final RepositorySystem system = context.getStore(MAVEN).get(SYSTEM, RepositorySystem.class);

    // inject the system into the test instance if annotated fields are present
    context.getTestInstance()
      .ifPresent(testInstance -> findAnnotatedFields(testInstance.getClass(), MavenRepositorySystem.class)
        .forEach(field -> {
          try {
            field.setAccessible(true);
            field.set(testInstance, system);
          } catch (IllegalAccessException e) {
            throw new IllegalStateException("unable to inject maven repository system into field " + field.getName(), e);
          }
        }));

    // create a session for each annotated session field and inject it
    context.getTestInstance()
      .ifPresent(testInstance -> findAnnotatedFields(testInstance.getClass(), MavenRepositorySystemSession.class)
        .forEach(field -> {
          try {

            DefaultRepositorySystemSession session = MavenRepositorySystemUtils.newSession();

            // configure the listener used to report artifact downloads
            session.setTransferListener(new Slf4jMavenTransferListener(log));

            Path repositoryRootPath = currentDirectory().toPath().resolve("target").toAbsolutePath();

            findAnnotation(field, MavenRepositorySystemSession.class)
              .ifPresent(annotation -> {
                String[] folders = annotation.localRepositoryPath();
                // TODO validate that the repository folders are not empty
                Path localRepositoryPath = stream(folders)
                  .map(Paths::get)
                  .reduce(repositoryRootPath, Path::resolve)
                  .normalize()
                  .toAbsolutePath();

                log.atDebug().setMessage("local repository: {}").addArgument(localRepositoryPath::toString).log();

                // set up the local repository manager for the maven session
                LocalRepository localRepository = new LocalRepository(localRepositoryPath.toFile());
                LocalRepositoryManager localRepositoryManager = system.newLocalRepositoryManager(session, localRepository);
                session.setLocalRepositoryManager(localRepositoryManager);

              });

            field.setAccessible(true);
            field.set(testInstance, session);

            // finally we store a reference to the session per field
            context.getStore(MAVEN).put(SESSION + '-' + field.getName(), session);

          } catch (IllegalAccessException e) {
            throw new IllegalStateException("unable to inject maven repository system session into field " + field.getName(), e);
          }
        }));

  }

}
