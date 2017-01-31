/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */

package org.mule.runtime.module.deployment.impl.internal.artifact;

import static java.util.Collections.singleton;
import static java.util.Optional.empty;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.core.Is.is;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import org.mule.runtime.core.api.registry.ServiceRegistry;
import org.mule.runtime.module.artifact.descriptor.BundleDescriptorLoader;
import org.mule.runtime.module.artifact.descriptor.ClassLoaderModelLoader;
import org.mule.tck.junit4.AbstractMuleTestCase;
import org.mule.tck.size.SmallTest;

import java.util.Collection;
import java.util.Optional;

import org.junit.Test;

@SmallTest
public class ServiceRegistryDescriptorLoaderRepositoryTestCase extends AbstractMuleTestCase {

  public static final String LOADER_ID = "loader";
  private final ServiceRegistry serviceRegistry = mock(ServiceRegistry.class);
  private final ServiceRegistryDescriptorLoaderRepository repository =
      new ServiceRegistryDescriptorLoaderRepository(serviceRegistry);

  @Test
  public void initializesClassLoaderModelLoadersOnce() throws Exception {
    repository.get("any", ClassLoaderModelLoader.class);
    repository.get("any", ClassLoaderModelLoader.class);

    verify(serviceRegistry).lookupProviders(ClassLoaderModelLoader.class, getClass().getClassLoader());
    verify(serviceRegistry).lookupProviders(BundleDescriptorLoader.class, getClass().getClassLoader());
    verify(serviceRegistry, never()).lookupProvider(ClassLoaderModelLoader.class, getClass().getClassLoader());
    verify(serviceRegistry, never()).lookupProvider(BundleDescriptorLoader.class, getClass().getClassLoader());
  }

  @Test
  public void doesNotFindInvalidLoaderId() throws Exception {
    Optional<ClassLoaderModelLoader> invalid = repository.get("invalid", ClassLoaderModelLoader.class);
    assertThat(invalid, is(empty()));
  }

  @Test
  public void findsLoader() throws Exception {
    ClassLoaderModelLoader expectedClassLoaderModelLoader = mock(ClassLoaderModelLoader.class);
    when(expectedClassLoaderModelLoader.getId()).thenReturn(LOADER_ID);
    Collection<ClassLoaderModelLoader> classLoaderModelLoaders = singleton(expectedClassLoaderModelLoader);
    when(serviceRegistry.lookupProviders(ClassLoaderModelLoader.class, getClass().getClassLoader()))
        .thenReturn(classLoaderModelLoaders);
    Optional<ClassLoaderModelLoader> classLoaderModelLoader = repository.get(LOADER_ID, ClassLoaderModelLoader.class);

    assertThat(classLoaderModelLoader.get(), is(expectedClassLoaderModelLoader));
  }

  @Test
  public void findsLoaderIdWithType() throws Exception {
    ClassLoaderModelLoader classLoaderModelLoader = mock(ClassLoaderModelLoader.class);
    when(classLoaderModelLoader.getId()).thenReturn(LOADER_ID);
    Collection<ClassLoaderModelLoader> classLoaderModelLoaders = singleton(classLoaderModelLoader);
    when(serviceRegistry.lookupProviders(ClassLoaderModelLoader.class, getClass().getClassLoader()))
        .thenReturn(classLoaderModelLoaders);
    Optional<BundleDescriptorLoader> bundleDescriptorLoader = repository.get(LOADER_ID, BundleDescriptorLoader.class);

    assertThat(bundleDescriptorLoader, is(empty()));
  }
}
