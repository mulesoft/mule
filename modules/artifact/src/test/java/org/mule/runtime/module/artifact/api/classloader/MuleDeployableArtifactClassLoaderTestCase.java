/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.module.artifact.api.classloader;

import static org.mule.runtime.module.artifact.api.classloader.DefaultArtifactClassLoaderFilter.NULL_CLASSLOADER_FILTER;
import static org.mule.runtime.module.artifact.api.classloader.ParentFirstLookupStrategy.PARENT_FIRST;

import static org.mockito.Answers.RETURNS_DEEP_STUBS;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

import org.mule.runtime.module.artifact.api.descriptor.ArtifactDescriptor;
import org.mule.tck.junit4.AbstractMuleTestCase;

import java.net.URL;

import org.junit.Test;

public class MuleDeployableArtifactClassLoaderTestCase extends AbstractMuleTestCase {

  private static final String ARTIFACT_ID = "testDeployable";
  public static final String DEPLOYABLE_NAME = "testApp";
  private final ArtifactDescriptor artifactDescriptor = new ArtifactDescriptor(DEPLOYABLE_NAME);

  @Test
  public void disposesClassLoaderAndRegionCorrectly() {
    RegionClassLoader regionClassLoader =
        spy(new RegionClassLoader(ARTIFACT_ID, artifactDescriptor, getClass().getClassLoader(), mock(ClassLoaderLookupPolicy.class)));

    final MuleDeployableArtifactClassLoader ownerClassLoader = spy(new MuleDeployableArtifactClassLoader(ARTIFACT_ID, artifactDescriptor, new URL[0], regionClassLoader, mock(ClassLoaderLookupPolicy.class)));
    final ArtifactClassLoader regionMember2 = mock(ArtifactClassLoader.class, RETURNS_DEEP_STUBS);
    regionClassLoader.addClassLoader(ownerClassLoader, NULL_CLASSLOADER_FILTER);
    regionClassLoader.addClassLoader(regionMember2, NULL_CLASSLOADER_FILTER);

    ownerClassLoader.disposeWithRegion();

    verify(regionClassLoader, times(1)).dispose();
    verify(ownerClassLoader, times(1)).dispose();
    verify(regionMember2, times(1)).dispose();
  }
}
