/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */

package org.mule.runtime.deployment.model.internal.artifact;

import static java.util.Collections.singleton;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.core.Is.is;
import static org.junit.rules.ExpectedException.none;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.mule.runtime.core.api.config.bootstrap.ArtifactType.PLUGIN;
import static org.mule.runtime.deployment.model.internal.artifact.ServiceRegistryDescriptorLoaderRepository.noRegisteredLoaderError;

import org.mule.runtime.core.api.registry.ServiceRegistry;
import org.mule.runtime.module.artifact.api.descriptor.BundleDescriptorLoader;
import org.mule.runtime.module.artifact.api.descriptor.ClassLoaderConfigurationLoader;
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
  public void initializesClassLoaderConfigurationLoadersOnce() throws Exception {
    ClassLoaderConfigurationLoader expectedClassLoaderConfigurationLoader = mock(ClassLoaderConfigurationLoader.class);
    when(expectedClassLoaderConfigurationLoader.getId()).thenReturn(LOADER_ID);
    Collection<ClassLoaderConfigurationLoader> classLoaderConfigurationLoaders = singleton(expectedClassLoaderConfigurationLoader);
    when(serviceRegistry.lookupProviders(ClassLoaderConfigurationLoader.class, getClass().getClassLoader()))
        .thenReturn(classLoaderConfigurationLoaders);
    when(expectedClassLoaderConfigurationLoader.supportsArtifactType(PLUGIN)).thenReturn(true);

    repository.get(LOADER_ID, PLUGIN, ClassLoaderConfigurationLoader.class);
    repository.get(LOADER_ID, PLUGIN, ClassLoaderConfigurationLoader.class);

    verify(serviceRegistry).lookupProviders(ClassLoaderConfigurationLoader.class, getClass().getClassLoader());
    verify(serviceRegistry).lookupProviders(BundleDescriptorLoader.class, getClass().getClassLoader());
    verify(serviceRegistry, never()).lookupProvider(ClassLoaderConfigurationLoader.class, getClass().getClassLoader());
    verify(serviceRegistry, never()).lookupProvider(BundleDescriptorLoader.class, getClass().getClassLoader());
  }

  @Test
  public void doesNotFindInvalidLoaderId() throws Exception {
    expectedException.expect(LoaderNotFoundException.class);
    expectedException.expectMessage(noRegisteredLoaderError("invalid", ClassLoaderConfigurationLoader.class));

    repository.get("invalid", PLUGIN, ClassLoaderConfigurationLoader.class);
  }

  @Test
  public void findsLoader() throws Exception {
    ClassLoaderConfigurationLoader expectedClassLoaderConfigurationLoader = mock(ClassLoaderConfigurationLoader.class);
    when(expectedClassLoaderConfigurationLoader.getId()).thenReturn(LOADER_ID);
    when(expectedClassLoaderConfigurationLoader.supportsArtifactType(PLUGIN)).thenReturn(true);
    Collection<ClassLoaderConfigurationLoader> classLoaderConfigurationLoaders = singleton(expectedClassLoaderConfigurationLoader);
    when(serviceRegistry.lookupProviders(ClassLoaderConfigurationLoader.class, getClass().getClassLoader()))
        .thenReturn(classLoaderConfigurationLoaders);
    ClassLoaderConfigurationLoader classLoaderConfigurationLoader = repository.get(LOADER_ID, PLUGIN, ClassLoaderConfigurationLoader.class);

    assertThat(classLoaderConfigurationLoader, is(expectedClassLoaderConfigurationLoader));
  }

  @Test
  public void findsLoaderIdWithType() throws Exception {
    ClassLoaderConfigurationLoader classLoaderConfigurationLoader = mock(ClassLoaderConfigurationLoader.class);
    when(classLoaderConfigurationLoader.getId()).thenReturn(LOADER_ID);
    Collection<ClassLoaderConfigurationLoader> classLoaderConfigurationLoaders = singleton(classLoaderConfigurationLoader);
    when(serviceRegistry.lookupProviders(ClassLoaderConfigurationLoader.class, getClass().getClassLoader()))
        .thenReturn(classLoaderConfigurationLoaders);

    expectedException.expect(LoaderNotFoundException.class);
    expectedException.expectMessage(noRegisteredLoaderError(LOADER_ID, BundleDescriptorLoader.class));

    repository.get(LOADER_ID, PLUGIN, BundleDescriptorLoader.class);
  }
}
