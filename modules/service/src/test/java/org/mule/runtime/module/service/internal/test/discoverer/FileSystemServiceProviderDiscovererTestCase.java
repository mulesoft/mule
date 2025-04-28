/*
 * Copyright 2023 Salesforce, Inc. All rights reserved.
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.module.service.internal.test.discoverer;

import static org.mule.runtime.container.api.MuleFoldersUtil.getServicesFolder;
import static org.mule.runtime.core.api.config.MuleProperties.MULE_HOME_DIRECTORY_PROPERTY;
import static org.mule.runtime.module.artifact.api.descriptor.ArtifactDescriptorValidatorBuilder.builder;
import static org.mule.tck.junit4.matcher.Eventually.eventually;
import static org.mule.tck.util.CollectableReference.collectedByGc;
import static org.mule.test.allure.AllureConstants.ServicesFeature.SERVICES;
import static org.mule.test.allure.AllureConstants.ServicesFeature.ServicesStory.SERVICE_PROVIDER_DISCOVERER;

import static java.security.AccessController.doPrivileged;
import static java.security.AccessController.getContext;

import static org.apache.commons.io.FileUtils.moveDirectory;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.empty;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.is;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static org.mockito.hamcrest.MockitoHamcrest.argThat;

import org.mule.runtime.api.artifact.ArtifactType;
import org.mule.runtime.api.service.ServiceDefinition;
import org.mule.runtime.api.service.ServiceProvider;
import org.mule.runtime.module.artifact.api.classloader.ArtifactClassLoader;
import org.mule.runtime.module.artifact.api.classloader.ClassLoaderLookupPolicy;
import org.mule.runtime.module.artifact.api.classloader.MuleArtifactClassLoader;
import org.mule.runtime.module.artifact.api.descriptor.ArtifactDescriptorValidator;
import org.mule.runtime.module.artifact.api.descriptor.BundleDescriptor;
import org.mule.runtime.module.artifact.api.descriptor.BundleDescriptorLoader;
import org.mule.runtime.module.artifact.api.descriptor.ClassLoaderConfigurationLoader;
import org.mule.runtime.module.artifact.api.descriptor.DescriptorLoaderRepository;
import org.mule.runtime.module.service.api.artifact.IServiceClassLoaderFactory;
import org.mule.runtime.module.service.api.artifact.ServiceDescriptor;
import org.mule.runtime.module.service.api.discoverer.ServiceAssembly;
import org.mule.runtime.module.service.builder.ServiceFileBuilder;
import org.mule.runtime.module.service.internal.discoverer.FileSystemServiceProviderDiscoverer;
import org.mule.runtime.module.service.internal.test.discoverer.ServiceRegistryTestCase.FooService;
import org.mule.tck.junit4.AbstractMuleTestCase;
import org.mule.tck.junit4.rule.SystemPropertyTemporaryFolder;
import org.mule.tck.util.CollectableReference;

import java.io.File;
import java.net.URL;
import java.net.URLClassLoader;
import java.security.AccessControlContext;
import java.security.CodeSource;
import java.security.PermissionCollection;
import java.security.Principal;
import java.security.PrivilegedAction;
import java.security.ProtectionDomain;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicReference;

import io.qameta.allure.Feature;
import io.qameta.allure.Issue;
import io.qameta.allure.Story;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;

@Feature(SERVICES)
@Story(SERVICE_PROVIDER_DISCOVERER)
public class FileSystemServiceProviderDiscovererTestCase extends AbstractMuleTestCase {

  @Rule
  public SystemPropertyTemporaryFolder temporaryFolder = new SystemPropertyTemporaryFolder(MULE_HOME_DIRECTORY_PROPERTY);

  private final IServiceClassLoaderFactory serviceClassLoaderFactory = mock(IServiceClassLoaderFactory.class);
  private final ArtifactClassLoader containerClassLoader = mock(ArtifactClassLoader.class);
  private final DescriptorLoaderRepository descriptorLoaderRepository = mock(DescriptorLoaderRepository.class);
  private final ArtifactDescriptorValidator artifactDescriptorValidator = mock(ArtifactDescriptorValidator.class);

  @Before
  public void setUp() throws Exception {
    FooServiceProvider.INVOKED = false;
    BarServiceProvider.INVOKED = false;

    final File servicesFolder = getServicesFolder();
    assertThat(servicesFolder.mkdir(), is(true));
    when(containerClassLoader.getClassLoader()).thenReturn(getClass().getClassLoader());
    when(containerClassLoader.getClassLoaderLookupPolicy()).thenReturn(mock(ClassLoaderLookupPolicy.class));

    BundleDescriptorLoader bundleDescriptorLoaderMock = mock(BundleDescriptorLoader.class);
    when(bundleDescriptorLoaderMock.supportsArtifactType(ArtifactType.SERVICE)).thenReturn(true);
    when(bundleDescriptorLoaderMock.load(any(File.class), any(Map.class), eq(ArtifactType.SERVICE)))
        .thenReturn(new BundleDescriptor.Builder()
            .setGroupId("mockGroupId")
            .setArtifactId("mockArtifactId")
            .setVersion("1.0.0")
            .setClassifier("mule-service")
            .setType("jar")
            .build());


    when(descriptorLoaderRepository.get(anyString(), any(), argThat(equalTo(BundleDescriptorLoader.class))))
        .thenReturn(bundleDescriptorLoaderMock);
    when(descriptorLoaderRepository.get(anyString(), any(),
                                        argThat(equalTo(ClassLoaderConfigurationLoader.class))))
                                            .thenReturn(mock(ClassLoaderConfigurationLoader.class));

    doNothing().when(artifactDescriptorValidator).validate(any());
  }

  @Test
  public void discoversNoServices() throws Exception {
    final FileSystemServiceProviderDiscoverer serviceProviderDiscoverer =
        new FileSystemServiceProviderDiscoverer(containerClassLoader, serviceClassLoaderFactory, descriptorLoaderRepository,
                                                builder());

    final List<ServiceAssembly> discover = serviceProviderDiscoverer.discover();
    assertThat(discover, is(empty()));
  }

  @Test
  public void discoversServices() throws Exception {
    installService("fooService", FooServiceProvider.class);
    installService("barService", BarServiceProvider.class);

    ArtifactClassLoader serviceClassLoader = mock(ArtifactClassLoader.class);
    when(serviceClassLoaderFactory.create(anyString(),
                                          any(ServiceDescriptor.class), any(ClassLoader.class), any(
                                                                                                    ClassLoaderLookupPolicy.class)))
                                                                                                        .thenReturn(serviceClassLoader);
    final FileSystemServiceProviderDiscoverer serviceProviderDiscoverer =
        new FileSystemServiceProviderDiscoverer(containerClassLoader, serviceClassLoaderFactory, descriptorLoaderRepository,
                                                builder());

    final List<ServiceAssembly> assemblies = serviceProviderDiscoverer.discover();

    assertThat(assemblies.size(), equalTo(2));

    assertThat(FooServiceProvider.INVOKED, is(false));
    assertThat(BarServiceProvider.INVOKED, is(false));

    assemblies.forEach(l -> l.getServiceProvider().getServiceDefinition());

    assertThat(FooServiceProvider.INVOKED, is(true));
    assertThat(BarServiceProvider.INVOKED, is(true));
  }

  @Test
  @Issue("W-17750750")
  public void serviceClassLoaderCreationDoesNotLeakCallerClassLoader() throws Exception {
    CollectableReference<ClassLoader> callerClassLoaderReference = new CollectableReference<>(new URLClassLoader(new URL[0]));
    installService("fooService", FooServiceProvider.class);

    // Some resources created with the service classloader (like the Threads) will retain a reference to the AccessControlContext,
    // so we're mimicking that behavior by holding this reference, which will be set when the IServiceClassLoaderFactory#create
    // method is called.
    AtomicReference<AccessControlContext> accOnServiceClassLoaderCreation = new AtomicReference<>();
    when(serviceClassLoaderFactory.create(anyString(),
                                          any(ServiceDescriptor.class), any(ClassLoader.class),
                                          any(ClassLoaderLookupPolicy.class)))
                                              .thenAnswer(invocation -> {
                                                // Just intercepting this call to save a strong reference to the ACC.
                                                accOnServiceClassLoaderCreation.set(getContext());
                                                return mock(MuleArtifactClassLoader.class);
                                              });

    // Now we discover the service lazily
    final FileSystemServiceProviderDiscoverer serviceProviderDiscoverer =
        new FileSystemServiceProviderDiscoverer(containerClassLoader, serviceClassLoaderFactory, descriptorLoaderRepository,
                                                builder());
    final List<ServiceAssembly> assemblies = serviceProviderDiscoverer.discover();
    assertThat("Should only have the foo service", assemblies.size(), equalTo(1));
    ServiceAssembly fooServiceAssembly = assemblies.get(0);

    // And we trigger the actual service classloader creation with the caller ACC in the stack.
    doPrivileged((PrivilegedAction<Void>) () -> {
      fooServiceAssembly.getClassLoader();
      return null;
    }, mockAccessControlContext(callerClassLoaderReference.get()));

    // We do have a strong reference to the AccessControlContext at accOnServiceClassLoaderCreation, but we
    // should not be retaining the caller classloader reference anywhere.
    assertThat(callerClassLoaderReference, is(eventually(collectedByGc())));
  }

  private static AccessControlContext mockAccessControlContext(ClassLoader classLoader) {
    return new AccessControlContext(new ProtectionDomain[] {
        new ProtectionDomain(mock(CodeSource.class),
                             mock(PermissionCollection.class),
                             classLoader,
                             new Principal[0])
    });
  }

  private void installService(String serviceName, Class<? extends ServiceProvider> providerClass) throws Exception {
    installService(serviceName, providerClass, false);
  }

  private void installService(String serviceName, Class<? extends ServiceProvider> providerClass, boolean corrupted)
      throws Exception {
    final ServiceFileBuilder fooService =
        new ServiceFileBuilder(serviceName)
            .forContract(FooService.class.getName())
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
