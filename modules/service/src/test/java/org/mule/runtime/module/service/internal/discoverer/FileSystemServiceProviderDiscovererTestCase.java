/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */

package org.mule.runtime.module.service.internal.discoverer;

import static java.util.Collections.emptyList;
import static org.apache.commons.io.FileUtils.moveFile;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.any;
import static org.hamcrest.Matchers.empty;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.hasItem;
import static org.hamcrest.Matchers.instanceOf;
import static org.hamcrest.Matchers.is;
import static org.mockito.Matchers.anyObject;
import static org.mockito.Matchers.anyString;
import static org.mockito.Matchers.argThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static org.mule.runtime.container.api.MuleFoldersUtil.getServicesFolder;
import static org.mule.runtime.core.api.config.MuleProperties.MULE_HOME_DIRECTORY_PROPERTY;
import org.mule.runtime.api.service.ServiceDefinition;
import org.mule.runtime.api.service.ServiceProvider;
import org.mule.runtime.api.util.Pair;
import org.mule.runtime.module.artifact.api.classloader.ArtifactClassLoader;
import org.mule.runtime.module.artifact.api.classloader.ClassLoaderLookupPolicy;
import org.mule.runtime.module.artifact.api.descriptor.BundleDescriptorLoader;
import org.mule.runtime.module.artifact.api.descriptor.ClassLoaderModelLoader;
import org.mule.runtime.module.artifact.api.descriptor.DescriptorLoaderRepository;
import org.mule.runtime.module.service.internal.artifact.ServiceDescriptor;
import org.mule.runtime.module.service.api.discoverer.ServiceResolutionError;
import org.mule.runtime.module.service.builder.ServiceFileBuilder;
import org.mule.runtime.module.service.internal.artifact.ServiceClassLoaderFactory;
import org.mule.tck.junit4.AbstractMuleTestCase;
import org.mule.tck.junit4.rule.SystemPropertyTemporaryFolder;

import java.io.File;
import java.util.List;
import java.util.stream.Collectors;

import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;

public class FileSystemServiceProviderDiscovererTestCase extends AbstractMuleTestCase {

  private final ServiceClassLoaderFactory serviceClassLoaderFactory = mock(ServiceClassLoaderFactory.class);
  @Rule
  public SystemPropertyTemporaryFolder temporaryFolder = new SystemPropertyTemporaryFolder(MULE_HOME_DIRECTORY_PROPERTY);
  private ArtifactClassLoader containerClassLoader = mock(ArtifactClassLoader.class);
  private DescriptorLoaderRepository descriptorLoaderRepository = mock(DescriptorLoaderRepository.class);

  @Before
  public void setUp() throws Exception {
    final File servicesFolder = getServicesFolder();
    assertThat(servicesFolder.mkdir(), is(true));
    when(containerClassLoader.getClassLoader()).thenReturn(getClass().getClassLoader());
    when(containerClassLoader.getClassLoaderLookupPolicy()).thenReturn(mock(ClassLoaderLookupPolicy.class));

    when(descriptorLoaderRepository.get(anyString(), anyObject(), argThat(equalTo(BundleDescriptorLoader.class))))
        .thenReturn(mock(BundleDescriptorLoader.class));
    when(descriptorLoaderRepository.get(anyString(), anyObject(), argThat(equalTo(ClassLoaderModelLoader.class))))
        .thenReturn(mock(ClassLoaderModelLoader.class));
  }

  @Test
  public void discoversNoServices() throws Exception {
    final FileSystemServiceProviderDiscoverer serviceProviderDiscoverer =
        new FileSystemServiceProviderDiscoverer(containerClassLoader, serviceClassLoaderFactory, descriptorLoaderRepository);

    final List<Pair<ArtifactClassLoader, ServiceProvider>> discover = serviceProviderDiscoverer.discover();

    assertThat(discover, is(empty()));
  }

  @Test
  public void discoversServices() throws Exception {
    installService("fooService", FooServiceProvider.class);
    installService("barService", BarServiceProvider.class);

    ArtifactClassLoader serviceClassLoader = mock(ArtifactClassLoader.class);
    when(serviceClassLoaderFactory.create(argThat(any(String.class)),
                                          argThat(any(ServiceDescriptor.class)), argThat(any(ClassLoader.class)), argThat(any(
                                                                                                                              ClassLoaderLookupPolicy.class))))
                                                                                                                                  .thenReturn(serviceClassLoader);
    final FileSystemServiceProviderDiscoverer serviceProviderDiscoverer =
        new FileSystemServiceProviderDiscoverer(containerClassLoader, serviceClassLoaderFactory, descriptorLoaderRepository);

    final List<Pair<ArtifactClassLoader, ServiceProvider>> serviceProvidersPairs = serviceProviderDiscoverer.discover();

    List<ServiceProvider> serviceProviders = serviceProvidersPairs.stream().map(Pair::getSecond).collect(Collectors.toList());

    assertThat(serviceProviders.size(), equalTo(2));
    assertThat(serviceProviders, hasItem(instanceOf(FooServiceProvider.class)));
    assertThat(serviceProviders, hasItem(instanceOf(BarServiceProvider.class)));
  }

  @Test(expected = ServiceResolutionError.class)
  public void detectsCorruptServiceFile() throws Exception {
    installCorruptedService("fooService", FooServiceProvider.class);

    ArtifactClassLoader serviceClassLoader = mock(ArtifactClassLoader.class);
    when(serviceClassLoaderFactory.create(argThat(any(String.class)),
                                          argThat(any(ServiceDescriptor.class)), argThat(any(ClassLoader.class)), argThat(any(
                                                                                                                              ClassLoaderLookupPolicy.class))))
                                                                                                                                  .thenReturn(serviceClassLoader);
    final FileSystemServiceProviderDiscoverer serviceProviderDiscoverer =
        new FileSystemServiceProviderDiscoverer(containerClassLoader, serviceClassLoaderFactory, descriptorLoaderRepository);

    serviceProviderDiscoverer.discover();
  }

  private void installService(String serviceName, Class<? extends ServiceProvider> providerClass) throws Exception {
    installService(serviceName, providerClass, false);
  }

  private void installCorruptedService(String serviceName, Class<? extends ServiceProvider> providerClass) throws Exception {
    installService(serviceName, providerClass, true);
  }

  private void installService(String serviceName, Class<? extends ServiceProvider> providerClass, boolean corrupted)
      throws Exception {
    final ServiceFileBuilder fooService =
        new ServiceFileBuilder(serviceName).withServiceProviderClass(providerClass.getName());
    if (corrupted) {
      fooService.corrupted();
    }

    File installedService = new File(getServicesFolder(), fooService.getArtifactFile().getName());
    moveFile(fooService.getArtifactFile(), installedService);
  }

  public static class FooServiceProvider implements ServiceProvider {

    @Override
    public List<ServiceDefinition> providedServices() {
      return emptyList();
    }
  }

  public static class BarServiceProvider implements ServiceProvider {

    @Override
    public List<ServiceDefinition> providedServices() {
      return emptyList();
    }
  }
}
