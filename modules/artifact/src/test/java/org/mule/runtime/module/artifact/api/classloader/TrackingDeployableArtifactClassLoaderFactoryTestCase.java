/*
 * Copyright 2023 Salesforce, Inc. All rights reserved.
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.module.artifact.api.classloader;

import static org.mule.runtime.module.artifact.api.classloader.ParentFirstLookupStrategy.PARENT_FIRST;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import org.mule.runtime.module.artifact.api.descriptor.ArtifactDescriptor;
import org.mule.tck.junit4.AbstractMuleTestCase;
import org.mule.tck.size.SmallTest;

import java.net.URL;

import org.junit.Before;
import org.junit.Test;

@SmallTest
public class TrackingDeployableArtifactClassLoaderFactoryTestCase extends AbstractMuleTestCase {

  public static final String ARTIFACT_ID = "testId";
  public static final String ARTIFACT_NAME = "test";
  private ArtifactClassLoaderManager artifactClassLoaderManager;
  private DeployableArtifactClassLoaderFactory<ArtifactDescriptor> delegateFactory;
  private TrackingDeployableArtifactClassLoaderFactory<ArtifactDescriptor> factory;
  private ArtifactClassLoader parent;
  private ArtifactDescriptor descriptor;
  private ArtifactClassLoader classLoader;

  @Before
  public void setUp() throws Exception {
    artifactClassLoaderManager = mock(ArtifactClassLoaderManager.class);
    delegateFactory = mock(DeployableArtifactClassLoaderFactory.class);
    factory = new TrackingDeployableArtifactClassLoaderFactory<>(artifactClassLoaderManager, delegateFactory);
    parent = mock(ArtifactClassLoader.class);
    descriptor = new ArtifactDescriptor(ARTIFACT_NAME);
    ClassLoaderLookupPolicy lookupPolicy = mock(ClassLoaderLookupPolicy.class);
    classLoader = new MuleArtifactClassLoader(ARTIFACT_NAME, descriptor, new URL[0], getClass().getClassLoader(), lookupPolicy);

    when(lookupPolicy.getClassLookupStrategy(any())).thenReturn(PARENT_FIRST);
    when(delegateFactory.create(ARTIFACT_ID, parent, descriptor)).thenReturn(classLoader);
  }

  @Test
  public void registersClassLoader() throws Exception {
    ArtifactClassLoader artifactClassLoader = factory.create(ARTIFACT_ID, parent, descriptor);

    verify(artifactClassLoaderManager).register(artifactClassLoader);
  }

  @Test
  public void disposesClassLoader() throws Exception {
    ArtifactClassLoader artifactClassLoader = factory.create(ARTIFACT_ID, parent, descriptor);

    artifactClassLoader.dispose();

    verify(artifactClassLoaderManager).unregister(artifactClassLoader.getArtifactId());
  }
}
