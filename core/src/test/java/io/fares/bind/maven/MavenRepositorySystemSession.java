package io.fares.bind.maven;

import java.lang.annotation.Retention;
import java.lang.annotation.Target;

import static java.lang.annotation.ElementType.FIELD;
import static java.lang.annotation.RetentionPolicy.RUNTIME;

/**
 * Marker annotation for the {@link MavenExtension} to inject the maven {@link org.eclipse.aether.RepositorySystemSession} instance.
 */
@Target({ FIELD })
@Retention(RUNTIME)
public @interface MavenRepositorySystemSession {

  /**
   * The resourceName of the folder in the {@code target} directory where the local maven repository will be managed.
   * <p>
   * <br>
   * <b>Note:</b> the path fragments will be appended to the {@code target} directory in the current working directory of the project.
   *
   * @return the folders that are appended to the {@code ./target} directory
   */

  String[] localRepositoryPath() default { "maven-local-test-repository" };

}
