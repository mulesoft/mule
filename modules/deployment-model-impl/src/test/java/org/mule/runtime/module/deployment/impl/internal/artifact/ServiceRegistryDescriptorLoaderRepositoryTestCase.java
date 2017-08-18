/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */

package org.mule.runtime.module.deployment.impl.internal.artifact;

import static java.util.Collections.singleton;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.core.Is.is;
import static org.junit.rules.ExpectedException.none;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.mule.runtime.core.api.config.bootstrap.ArtifactType.PLUGIN;
import static org.mule.runtime.module.deployment.impl.internal.artifact.ServiceRegistryDescriptorLoaderRepository.noRegisteredLoaderError;
import org.mule.runtime.core.api.registry.ServiceRegistry;
import org.mule.runtime.module.artifact.api.descriptor.BundleDescriptorLoader;
import org.mule.runtime.module.artifact.api.descriptor.ClassLoaderModelLoader;
import org.mule.runtime.module.artifact.api.descriptor.LoaderNotFoundException;
import org.mule.tck.junit4.AbstractMuleTestCase;
import org.mule.tck.size.SmallTest;

import java.util.Collection;

import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;

@SmallTest
public class ServiceRegistryDescriptorLoaderRepositoryTestCase extends AbstractMuleTestCase {

  private static final String LOADER_ID = "loader";

  private final ServiceRegistry serviceRegistry = mock(ServiceRegistry.class);
  private final ServiceRegistryDescriptorLoaderRepository repository =
      new ServiceRegistryDescriptorLoaderRepository(serviceRegistry);
  @Rule
  public ExpectedException expectedException = none();

  @Test
  public void initializesClassLoaderModelLoadersOnce() throws Exception {
    ClassLoaderModelLoader expectedClassLoaderModelLoader = mock(ClassLoaderModelLoader.class);
    when(expectedClassLoaderModelLoader.getId()).thenReturn(LOADER_ID);
    Collection<ClassLoaderModelLoader> classLoaderModelLoaders = singleton(expectedClassLoaderModelLoader);
    when(serviceRegistry.lookupProviders(ClassLoaderModelLoader.class, getClass().getClassLoader()))
        .thenReturn(classLoaderModelLoaders);
    when(expectedClassLoaderModelLoader.supportsArtifactType(PLUGIN)).thenReturn(true);

    repository.get(LOADER_ID, PLUGIN, ClassLoaderModelLoader.class);
    repository.get(LOADER_ID, PLUGIN, ClassLoaderModelLoader.class);

    verify(serviceRegistry).lookupProviders(ClassLoaderModelLoader.class, getClass().getClassLoader());
    verify(serviceRegistry).lookupProviders(BundleDescriptorLoader.class, getClass().getClassLoader());
    verify(serviceRegistry, never()).lookupProvider(ClassLoaderModelLoader.class, getClass().getClassLoader());
    verify(serviceRegistry, never()).lookupProvider(BundleDescriptorLoader.class, getClass().getClassLoader());
  }

  @Test
  public void doesNotFindInvalidLoaderId() throws Exception {
    expectedException.expect(LoaderNotFoundException.class);
    expectedException.expectMessage(noRegisteredLoaderError("invalid", ClassLoaderModelLoader.class));

    repository.get("invalid", PLUGIN, ClassLoaderModelLoader.class);
  }

  @Test
  public void findsLoader() throws Exception {
    ClassLoaderModelLoader expectedClassLoaderModelLoader = mock(ClassLoaderModelLoader.class);
    when(expectedClassLoaderModelLoader.getId()).thenReturn(LOADER_ID);
    when(expectedClassLoaderModelLoader.supportsArtifactType(PLUGIN)).thenReturn(true);
    Collection<ClassLoaderModelLoader> classLoaderModelLoaders = singleton(expectedClassLoaderModelLoader);
    when(serviceRegistry.lookupProviders(ClassLoaderModelLoader.class, getClass().getClassLoader()))
        .thenReturn(classLoaderModelLoaders);
    ClassLoaderModelLoader classLoaderModelLoader = repository.get(LOADER_ID, PLUGIN, ClassLoaderModelLoader.class);

    assertThat(classLoaderModelLoader, is(expectedClassLoaderModelLoader));
  }

  @Test
  public void findsLoaderIdWithType() throws Exception {
    ClassLoaderModelLoader classLoaderModelLoader = mock(ClassLoaderModelLoader.class);
    when(classLoaderModelLoader.getId()).thenReturn(LOADER_ID);
    Collection<ClassLoaderModelLoader> classLoaderModelLoaders = singleton(classLoaderModelLoader);
    when(serviceRegistry.lookupProviders(ClassLoaderModelLoader.class, getClass().getClassLoader()))
        .thenReturn(classLoaderModelLoaders);

    expectedException.expect(LoaderNotFoundException.class);
    expectedException.expectMessage(noRegisteredLoaderError(LOADER_ID, BundleDescriptorLoader.class));

    repository.get(LOADER_ID, PLUGIN, BundleDescriptorLoader.class);
  }
}
