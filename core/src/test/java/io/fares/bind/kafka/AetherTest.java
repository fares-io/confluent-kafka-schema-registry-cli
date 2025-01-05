package io.fares.bind.kafka;

import io.fares.bind.maven.Aether;
import org.apache.maven.settings.Settings;
import org.codehaus.plexus.classworlds.ClassWorld;
import org.codehaus.plexus.classworlds.realm.DuplicateRealmException;
import org.eclipse.aether.RepositorySystem;
import org.eclipse.aether.RepositorySystemSession;
import org.eclipse.aether.repository.LocalArtifactResult;
import org.eclipse.aether.resolution.ArtifactRequest;
import org.eclipse.aether.resolution.ArtifactResolutionException;
import org.eclipse.aether.resolution.ArtifactResult;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class AetherTest {

  @Mock
  RepositorySystem system;

  @Mock
  RepositorySystemSession session;

  @Mock
  Settings settings;

  @Mock
  ClassWorld classWorld;

  @Mock
  ArtifactResult artifactResult;

  @Mock
  LocalArtifactResult localArtifactResult;

  @Test
  void itShouldFailToCreateAetherWithDuplicateRealm() throws DuplicateRealmException {

    doThrow(new DuplicateRealmException(classWorld, Aether.MAVEN_DEPENDENCY_REALM_ID))
      .when(classWorld).newRealm(Mockito.anyString(), any());

    assertThatThrownBy(() -> new Aether(system, session, settings, classWorld))
      .isInstanceOf(IllegalArgumentException.class)
      .hasMessageContaining("failed to create artifact realm");

  }

  @Test
  void itShouldResolveArtifact() throws Exception {

    // the artifact was resolved and present
    doReturn(true).when(artifactResult).isResolved();
    doReturn(localArtifactResult).when(artifactResult).getLocalArtifactResult();
    doReturn(true).when(localArtifactResult).isAvailable();
    doReturn(artifactResult).when(system).resolveArtifact(eq(session), any(ArtifactRequest.class));

    Aether aether = new Aether(system, session, settings, classWorld);

    aether.resolveArtifact("io.fares.examples:alpha-model-project::1.0.0");

    verify(system, times(1)).newResolutionRepositories(eq(session), anyList());
    verify(system, times(1)).resolveArtifact(eq(session), any(ArtifactRequest.class));

    verify(artifactResult, times(1)).getExceptions();

    verifyNoMoreInteractions(system);
    verifyNoInteractions(session);
    verifyNoMoreInteractions(artifactResult);

  }

  @Test
  void itShouldResolveTwiceWhenArtifactNotLocal() throws Exception {

    // the artifact was resolved but not present
    doReturn(true).when(artifactResult).isResolved();
    doReturn(localArtifactResult).when(artifactResult).getLocalArtifactResult();
    doReturn(false).when(localArtifactResult).isAvailable();
    doReturn(artifactResult).when(system).resolveArtifact(eq(session), any(ArtifactRequest.class));

    Aether aether = new Aether(system, session, settings, classWorld);

    aether.resolveArtifact("io.fares.examples:alpha-model-project::1.0.0");

    verify(system, times(2)).newResolutionRepositories(eq(session), anyList());
    verify(system, times(2)).resolveArtifact(eq(session), any(ArtifactRequest.class));

    verify(artifactResult, times(1)).getExceptions();

    verifyNoMoreInteractions(system);
    verifyNoInteractions(session);
    verifyNoMoreInteractions(artifactResult);

  }


  @Test
  void itShouldThrowWhenArtifactDoesNotResolve() throws Exception {

    doReturn(artifactResult)
      .when(system).resolveArtifact(eq(session), any(ArtifactRequest.class));

    // the artifact was resolved and present
    when(artifactResult.isResolved()).thenReturn(true);
    when(artifactResult.getLocalArtifactResult()).thenReturn(localArtifactResult);
    when(artifactResult.getExceptions()).thenReturn(List.of(new RuntimeException("expected failure")));

    when(localArtifactResult.isAvailable()).thenReturn(true);

    Aether aether = new Aether(system, session, settings, classWorld);

    assertThatThrownBy(() -> aether.resolveArtifact("io.fares.examples:alpha-model-project::1.0.0"))
      .isInstanceOf(ArtifactResolutionException.class)
      .hasMessageContaining("failed to resolve artifact");

    verify(system, times(1)).newResolutionRepositories(eq(session), anyList());
    verify(system, times(1)).resolveArtifact(eq(session), any(ArtifactRequest.class));

    verify(artifactResult, times(1)).getExceptions();

    verifyNoMoreInteractions(system);
    verifyNoInteractions(session);
    verifyNoMoreInteractions(artifactResult);

  }

}
