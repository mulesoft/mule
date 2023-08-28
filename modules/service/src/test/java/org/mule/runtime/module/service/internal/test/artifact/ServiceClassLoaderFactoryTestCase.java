/*
 * Copyright 2023 Salesforce, Inc. All rights reserved.
 */
package org.mule.runtime.module.service.internal.test.artifact;

import static org.mule.runtime.module.artifact.api.classloader.ParentFirstLookupStrategy.PARENT_FIRST;
import static org.mule.runtime.module.service.api.artifact.ServiceClassLoaderFactoryProvider.serviceClassLoaderFactory;

import static junit.framework.TestCase.fail;
import static org.hamcrest.core.IsEqual.equalTo;
import static org.junit.Assert.assertThat;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import org.mule.runtime.module.artifact.api.classloader.ArtifactClassLoader;
import org.mule.runtime.module.artifact.api.classloader.ClassLoaderLookupPolicy;
import org.mule.runtime.module.artifact.api.classloader.MuleArtifactClassLoader;
import org.mule.runtime.module.service.api.artifact.ServiceClassLoaderFactory;
import org.mule.runtime.module.service.api.artifact.ServiceDescriptor;
import org.mule.tck.junit4.AbstractMuleTestCase;

import java.net.URL;

import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;

public class ServiceClassLoaderFactoryTestCase extends AbstractMuleTestCase {

  private static final String SERVICE_ID = "service/serviceId";

  private final ClassLoaderLookupPolicy lookupPolicy = mock(ClassLoaderLookupPolicy.class);
  @Rule
  public TemporaryFolder serviceFolder = new TemporaryFolder();
  private final ServiceClassLoaderFactory factory = serviceClassLoaderFactory();
  private ServiceDescriptor descriptor;

  @Before
  public void setUp() throws Exception {
    descriptor = new ServiceDescriptor("testService");
    descriptor.setRootFolder(serviceFolder.getRoot());

    when(lookupPolicy.getClassLookupStrategy(anyString())).thenReturn(PARENT_FIRST);
  }

  @Test
  public void createsEmptyClassLoader() throws Exception {
    final ArtifactClassLoader artifactClassLoader =
        factory.create(SERVICE_ID, descriptor, getClass().getClassLoader(), lookupPolicy);
    final MuleArtifactClassLoader classLoader = (MuleArtifactClassLoader) artifactClassLoader.getClassLoader();
    assertThat(classLoader.getURLs(), equalTo(new URL[0]));
  }

  @Test
  public void usesClassLoaderLookupPolicy() throws Exception {
    final ArtifactClassLoader artifactClassLoader =
        factory.create(SERVICE_ID, descriptor, getClass().getClassLoader(), lookupPolicy);
    final MuleArtifactClassLoader classLoader = (MuleArtifactClassLoader) artifactClassLoader.getClassLoader();

    final String className = "com.dummy.Foo";
    try {
      classLoader.loadClass(className);
      fail("Able to load an un-existent class");
    } catch (ClassNotFoundException e) {
      // Expected
    }

    verify(lookupPolicy).getClassLookupStrategy(className);
  }
}
