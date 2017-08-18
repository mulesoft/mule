/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */

package org.mule.runtime.module.deployment.impl.internal.artifact;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import org.mule.runtime.module.artifact.api.classloader.ArtifactClassLoader;
import org.mule.tck.junit4.AbstractMuleTestCase;
import org.mule.tck.size.SmallTest;

import java.util.Optional;

import org.junit.Test;

@SmallTest
public class DefaultArtifactClassLoaderManagerTestCase extends AbstractMuleTestCase {

  public static final String ARTIFACT_ID = "ID";

  private final DefaultClassLoaderManager manager = new DefaultClassLoaderManager();

  @Test
  public void registersArtifactClassLoader() throws Exception {
    ArtifactClassLoader artifactClassLoader = getArtifactClassLoader();

    manager.register(artifactClassLoader);

    assertThat(manager.find(ARTIFACT_ID).get(), is(artifactClassLoader.getClassLoader()));
  }

  @Test
  public void unregistersArtifactClassLoader() throws Exception {
    ArtifactClassLoader artifactClassLoader = getArtifactClassLoader();

    manager.register(artifactClassLoader);

    assertThat(manager.unregister(ARTIFACT_ID), is(artifactClassLoader));
  }

  @Test
  public void getIdForRegisteredArtifactClassLoader() {
    ArtifactClassLoader artifactClassLoader = getArtifactClassLoader();

    manager.register(artifactClassLoader);

    assertThat(manager.getId(artifactClassLoader.getClassLoader()), is(Optional.of(ARTIFACT_ID)));
  }

  @Test
  public void getIdForNonRegisteredArtifactClassLoader() {
    ArtifactClassLoader artifactClassLoader = getArtifactClassLoader();

    assertThat(manager.getId(artifactClassLoader.getClassLoader()), is(Optional.empty()));
  }

  @Test(expected = IllegalArgumentException.class)
  public void failsToRegisterNullArtifactClassLoader() throws Exception {
    manager.register(null);
  }

  @Test(expected = IllegalArgumentException.class)
  public void failsToUnregisterEmptyArtifactId() throws Exception {
    manager.unregister(null);
  }

  private ArtifactClassLoader getArtifactClassLoader() {
    ArtifactClassLoader artifactClassLoader = mock(ArtifactClassLoader.class);
    when(artifactClassLoader.getArtifactId()).thenReturn(ARTIFACT_ID);
    ClassLoader expectedClassLoader = getClass().getClassLoader();
    when(artifactClassLoader.getClassLoader()).thenReturn(expectedClassLoader);
    return artifactClassLoader;
  }
}
