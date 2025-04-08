/*
 * Copyright 2023 Salesforce, Inc. All rights reserved.
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.container.internal;

import static org.mule.test.allure.AllureConstants.ArtifactDeploymentFeature.DeploymentSuccessfulStory.POLICY_ISOLATION;
import static org.mule.test.allure.AllureConstants.ArtifactDeploymentFeature.POLICY_DEPLOYMENT;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import org.mule.runtime.module.artifact.api.classloader.ClassLoaderLookupPolicy;
import org.mule.runtime.module.artifact.api.classloader.RegionClassLoader;
import org.mule.runtime.module.artifact.api.descriptor.ArtifactDescriptor;
import org.mule.tck.junit4.AbstractMuleTestCase;

import io.qameta.allure.Feature;
import io.qameta.allure.Issue;
import io.qameta.allure.Story;
import org.junit.Test;

@Feature(POLICY_DEPLOYMENT)
@Story(POLICY_ISOLATION)
@Issue("W-17340911")
public class IsolatedPolicyClassLoaderTestCase extends AbstractMuleTestCase {

  protected static final String ARTIFACT_ID = "testAppId";
  public static final String APP_NAME = "testApp";

  protected final ArtifactDescriptor artifactDescriptor;
  protected final ClassLoaderLookupPolicy lookupPolicy = mock(ClassLoaderLookupPolicy.class);

  public IsolatedPolicyClassLoaderTestCase() {
    artifactDescriptor = new ArtifactDescriptor(APP_NAME);
  }

  @Test
  public void getIsolatedPolicyClassLoaderInstance() {
    FilteringContainerClassLoader mockContainerClassLoader = mock(FilteringContainerClassLoader.class);
    when(mockContainerClassLoader.getArtifactId()).thenReturn(ARTIFACT_ID);
    when(mockContainerClassLoader.getArtifactDescriptor()).thenReturn(artifactDescriptor);
    when(mockContainerClassLoader.getClassLoaderLookupPolicy()).thenReturn(lookupPolicy);

    IsolatedPolicyClassLoader instance1 = IsolatedPolicyClassLoader.getInstance(mockContainerClassLoader);
    IsolatedPolicyClassLoader instance2 = IsolatedPolicyClassLoader.getInstance(mockContainerClassLoader);
    assertThat(instance1, is(instance2));
  }

  @Test(expected = IllegalArgumentException.class)
  public void getIsolatedPolicyClassLoaderInstanceWithNullRegionClassLoader() {
    IsolatedPolicyClassLoader.getInstance(null);
  }
}
