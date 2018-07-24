/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */

package org.mule.runtime.module.service.internal.discoverer;

import static org.apache.commons.io.FileUtils.moveDirectory;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.any;
import static org.hamcrest.Matchers.empty;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.is;
import static org.mockito.Matchers.anyObject;
import static org.mockito.Matchers.anyString;
import static org.mockito.Matchers.argThat;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static org.mule.runtime.container.api.MuleFoldersUtil.getServicesFolder;
import static org.mule.runtime.core.api.config.MuleProperties.MULE_HOME_DIRECTORY_PROPERTY;
import static org.mule.runtime.module.artifact.api.descriptor.ArtifactDescriptorValidatorBuilder.builder;
import org.mule.runtime.api.service.ServiceDefinition;
import org.mule.runtime.api.service.ServiceProvider;
import org.mule.runtime.module.artifact.api.classloader.ArtifactClassLoader;
import org.mule.runtime.module.artifact.api.classloader.ClassLoaderLookupPolicy;
import org.mule.runtime.module.artifact.api.descriptor.ArtifactDescriptorValidator;
import org.mule.runtime.module.artifact.api.descriptor.BundleDescriptorLoader;
import org.mule.runtime.module.artifact.api.descriptor.ClassLoaderModelLoader;
import org.mule.runtime.module.artifact.api.descriptor.DescriptorLoaderRepository;
import org.mule.runtime.module.service.api.discoverer.ServiceLocator;
import org.mule.runtime.module.service.builder.ServiceFileBuilder;
import org.mule.runtime.module.service.internal.artifact.ServiceClassLoaderFactory;
import org.mule.runtime.module.service.internal.artifact.ServiceDescriptor;
import org.mule.runtime.module.service.internal.discoverer.ServiceRegistryTestCase.FooService;
import org.mule.tck.junit4.AbstractMuleTestCase;
import org.mule.tck.junit4.rule.SystemPropertyTemporaryFolder;

import java.io.File;
import java.util.List;

import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;

public class FileSystemServiceProviderDiscovererTestCase extends AbstractMuleTestCase {

  @Rule
  public SystemPropertyTemporaryFolder temporaryFolder = new SystemPropertyTemporaryFolder(MULE_HOME_DIRECTORY_PROPERTY);

  private final ServiceClassLoaderFactory serviceClassLoaderFactory = mock(ServiceClassLoaderFactory.class);
  private ArtifactClassLoader containerClassLoader = mock(ArtifactClassLoader.class);
  private DescriptorLoaderRepository descriptorLoaderRepository = mock(DescriptorLoaderRepository.class);
  private ArtifactDescriptorValidator artifactDescriptorValidator = mock(ArtifactDescriptorValidator.class);

  @Before
  public void setUp() throws Exception {
    FooServiceProvider.INVOKED = false;
    BarServiceProvider.INVOKED = false;

    final File servicesFolder = getServicesFolder();
    assertThat(servicesFolder.mkdir(), is(true));
    when(containerClassLoader.getClassLoader()).thenReturn(getClass().getClassLoader());
    when(containerClassLoader.getClassLoaderLookupPolicy()).thenReturn(mock(ClassLoaderLookupPolicy.class));

    when(descriptorLoaderRepository.get(anyString(), anyObject(), argThat(equalTo(BundleDescriptorLoader.class))))
        .thenReturn(mock(BundleDescriptorLoader.class));
    when(descriptorLoaderRepository.get(anyString(), anyObject(), argThat(equalTo(ClassLoaderModelLoader.class))))
        .thenReturn(mock(ClassLoaderModelLoader.class));

    doNothing().when(artifactDescriptorValidator).validate(anyObject());
  }

  @Test
  public void discoversNoServices() throws Exception {
    final FileSystemServiceProviderDiscoverer serviceProviderDiscoverer =
        new FileSystemServiceProviderDiscoverer(containerClassLoader, serviceClassLoaderFactory, descriptorLoaderRepository,
                                                builder());

    final List<ServiceLocator> discover = serviceProviderDiscoverer.discover();
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
        new FileSystemServiceProviderDiscoverer(containerClassLoader, serviceClassLoaderFactory, descriptorLoaderRepository,
                                                builder());

    final List<ServiceLocator> locators = serviceProviderDiscoverer.discover();

    assertThat(locators.size(), equalTo(2));

    assertThat(FooServiceProvider.INVOKED, is(false));
    assertThat(BarServiceProvider.INVOKED, is(false));

    locators.forEach(l -> l.getServiceProvider().getServiceDefinition());

    assertThat(FooServiceProvider.INVOKED, is(true));
    assertThat(BarServiceProvider.INVOKED, is(true));
  }

  private void installService(String serviceName, Class<? extends ServiceProvider> providerClass) throws Exception {
    installService(serviceName, providerClass, false);
  }

  private void installService(String serviceName, Class<? extends ServiceProvider> providerClass, boolean corrupted)
      throws Exception {
    final ServiceFileBuilder fooService =
        new ServiceFileBuilder(serviceName)
            .satisfyingServiceClassName(FooService.class.getName())
            .withServiceProviderClass(providerClass.getName()).unpack(true);
    if (corrupted) {
      fooService.corrupted();
    }

    final File artifactFile = fooService.getArtifactFile();
    File installedService = new File(getServicesFolder(), artifactFile.getName());
    moveDirectory(artifactFile, installedService);
  }

  public static class FooServiceProvider implements ServiceProvider {

    private static boolean INVOKED = false;

    @Override
    public ServiceDefinition getServiceDefinition() {
      INVOKED = true;
      return null;
    }
  }

  public static class BarServiceProvider implements ServiceProvider {

    private static boolean INVOKED = false;

    @Override
    public ServiceDefinition getServiceDefinition() {
      INVOKED = true;
      return null;
    }
  }
}
