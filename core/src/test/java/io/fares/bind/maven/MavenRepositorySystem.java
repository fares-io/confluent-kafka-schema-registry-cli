package io.fares.bind.maven;

import java.lang.annotation.Retention;
import java.lang.annotation.Target;

import static java.lang.annotation.ElementType.FIELD;
import static java.lang.annotation.RetentionPolicy.RUNTIME;

/**
 * Marker annotation for the {@link MavenExtension} to inject the maven {@link org.eclipse.aether.RepositorySystem} instance.
 */
@Target({ FIELD })
@Retention(RUNTIME)
public @interface MavenRepositorySystem {

}
