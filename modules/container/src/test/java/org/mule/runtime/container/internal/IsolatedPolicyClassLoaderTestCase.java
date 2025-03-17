/*
 * Copyright 2023 Salesforce, Inc. All rights reserved.
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.container.internal;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import org.mule.runtime.module.artifact.api.classloader.ClassLoaderLookupPolicy;
import org.mule.runtime.module.artifact.api.classloader.RegionClassLoader;
import org.mule.runtime.module.artifact.api.descriptor.ArtifactDescriptor;

import org.junit.Test;

public class IsolatedPolicyClassLoaderTestCase {

  protected static final String PACKAGE_NAME = "java.lang";
  protected static final String CLASS_NAME = PACKAGE_NAME + ".Object";
  private static final Class PARENT_LOADED_CLASS = Object.class;
  protected static final String ARTIFACT_ID = "testAppId";
  public static final String APP_NAME = "testApp";

  protected final ArtifactDescriptor artifactDescriptor;
  protected final ClassLoaderLookupPolicy lookupPolicy = mock(ClassLoaderLookupPolicy.class);

  public IsolatedPolicyClassLoaderTestCase() {
    artifactDescriptor = new ArtifactDescriptor(APP_NAME);
  }

  @Test
  public void getIsolatedPolicyClassLoaderInstance() throws ClassNotFoundException {
    final ClassLoader parentClassLoader = mock(ClassLoader.class);
    when(parentClassLoader.loadClass(CLASS_NAME)).thenReturn(PARENT_LOADED_CLASS);

    RegionClassLoader regionClassLoader = new RegionClassLoader(ARTIFACT_ID, artifactDescriptor, parentClassLoader, lookupPolicy);

    IsolatedPolicyClassLoader instance1 = IsolatedPolicyClassLoader.getInstance(regionClassLoader);
    IsolatedPolicyClassLoader instance2 = IsolatedPolicyClassLoader.getInstance(regionClassLoader);
    assertThat(instance1, is(instance2));
  }

  @Test(expected = IllegalArgumentException.class)
  public void getIsolatedPolicyClassLoaderInstanceWithNullRegionClassLoader() {
    IsolatedPolicyClassLoader.getInstance(null);
  }
}
