/*
 * Copyright 2023 Salesforce, Inc. All rights reserved.
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.module.deployment.test.internal;

import static org.mule.runtime.container.api.ContainerClassLoaderProvider.createContainerClassLoader;
import static org.mule.runtime.container.api.MuleFoldersUtil.getAppDataFolder;
import static org.mule.runtime.deployment.model.api.artifact.ArtifactDescriptorFactoryProvider.artifactDescriptorFactoryProvider;
import static org.mule.runtime.module.deployment.internal.processor.SerializedAstArtifactConfigurationProcessor.serializedAstWithFallbackArtifactConfigurationProcessor;

import static org.mockito.Mockito.mock;

import org.mule.runtime.api.deployment.meta.MulePluginModel;
import org.mule.runtime.api.memory.management.MemoryManagementService;
import org.mule.runtime.api.service.ServiceRepository;
import org.mule.runtime.container.api.ModuleRepository;
import org.mule.runtime.deployment.model.api.artifact.DescriptorLoaderRepositoryFactory;
import org.mule.runtime.deployment.model.api.builder.DomainClassLoaderBuilderFactory;
import org.mule.runtime.deployment.model.api.domain.Domain;
import org.mule.runtime.deployment.model.api.plugin.resolver.PluginDependenciesResolver;
import org.mule.runtime.module.artifact.activation.api.classloader.ArtifactClassLoaderResolver;
import org.mule.runtime.module.artifact.activation.api.descriptor.DeployableArtifactDescriptorFactory;
import org.mule.runtime.module.artifact.activation.api.extension.discovery.ExtensionModelLoaderRepository;
import org.mule.runtime.module.artifact.activation.internal.classloader.DefaultArtifactClassLoaderResolver;
import org.mule.runtime.module.artifact.activation.internal.nativelib.DefaultNativeLibraryFinderFactory;
import org.mule.runtime.module.artifact.api.classloader.ArtifactClassLoader;
import org.mule.runtime.module.artifact.api.classloader.ClassLoaderRepository;
import org.mule.runtime.module.artifact.api.classloader.DeployableArtifactClassLoaderFactory;
import org.mule.runtime.module.artifact.api.descriptor.AbstractArtifactDescriptorFactory;
import org.mule.runtime.module.artifact.api.descriptor.ArtifactDescriptorValidatorBuilder;
import org.mule.runtime.module.artifact.api.descriptor.ArtifactPluginDescriptor;
import org.mule.runtime.module.artifact.api.descriptor.DescriptorLoaderRepository;
import org.mule.runtime.module.artifact.api.descriptor.DomainDescriptor;
import org.mule.runtime.module.deployment.impl.internal.artifact.DefaultArtifactDescriptorFactoryProvider;
import org.mule.runtime.module.deployment.impl.internal.artifact.DefaultClassLoaderManager;
import org.mule.runtime.module.deployment.impl.internal.classloader.TrackingArtifactClassLoaderResolverDecorator;
import org.mule.runtime.module.deployment.impl.internal.domain.DefaultDomainFactory;
import org.mule.runtime.module.deployment.impl.internal.domain.DefaultDomainManager;
import org.mule.runtime.module.deployment.impl.internal.domain.DomainDescriptorFactory;
import org.mule.runtime.module.deployment.impl.internal.domain.wrapper.TestDomainWrapper;
import org.mule.runtime.module.deployment.impl.internal.plugin.ArtifactPluginDescriptorLoader;
import org.mule.runtime.module.license.api.LicenseValidator;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.Properties;

public class TestDomainFactory extends DefaultDomainFactory {

  private static List<Runnable> afterTasks = new ArrayList<>();

  private boolean failOnStop;
  private boolean failOnDispose;

  public static TestDomainFactory createDomainFactory(DeployableArtifactClassLoaderFactory<DomainDescriptor> deployableArtifactClassLoaderFactory,
                                                      ArtifactClassLoader containerClassLoader,
                                                      ServiceRepository serviceRepository,
                                                      ModuleRepository moduleRepository,
                                                      DescriptorLoaderRepository descriptorLoaderRepository) {
    AbstractArtifactDescriptorFactory<MulePluginModel, ArtifactPluginDescriptor> artifactPluginDescriptorFactory =
        artifactDescriptorFactoryProvider()
            .createArtifactPluginDescriptorFactory(new DescriptorLoaderRepositoryFactory().createDescriptorLoaderRepository(),
                                                   ArtifactDescriptorValidatorBuilder.builder());
    ArtifactPluginDescriptorLoader artifactPluginDescriptorLoader =
        new ArtifactPluginDescriptorLoader(artifactPluginDescriptorFactory);
    DomainDescriptorFactory domainDescriptorFactory =
        new DomainDescriptorFactory(artifactPluginDescriptorLoader, descriptorLoaderRepository,
                                    ArtifactDescriptorValidatorBuilder.builder());
    final DefaultClassLoaderManager artifactClassLoaderManager = new DefaultClassLoaderManager();
    PluginDependenciesResolver pluginDependenciesResolver =
        new DefaultArtifactDescriptorFactoryProvider().createBundlePluginDependenciesResolver(artifactPluginDescriptorFactory);

    ArtifactClassLoaderResolver artifactClassLoaderResolver =
        new TrackingArtifactClassLoaderResolverDecorator(artifactClassLoaderManager,
                                                         new DefaultArtifactClassLoaderResolver(createContainerClassLoader(moduleRepository),
                                                                                                moduleRepository,
                                                                                                new DefaultNativeLibraryFinderFactory(name -> getAppDataFolder(name))));

    DomainClassLoaderBuilderFactory domainClassLoaderBuilderFactory =
        new DomainClassLoaderBuilderFactory(artifactClassLoaderResolver);

    return new TestDomainFactory(artifactClassLoaderManager, serviceRepository, domainDescriptorFactory,
                                 DeployableArtifactDescriptorFactory.defaultArtifactDescriptorFactory(),
                                 domainClassLoaderBuilderFactory,
                                 ExtensionModelLoaderRepository.getExtensionModelLoaderManager());
  }

  private TestDomainFactory(ClassLoaderRepository classLoaderRepository,
                            ServiceRepository serviceRepository,
                            DomainDescriptorFactory domainDescriptorFactory,
                            DeployableArtifactDescriptorFactory deployableArtifactDescriptorFactory,
                            DomainClassLoaderBuilderFactory domainClassLoaderBuilderFactory,
                            ExtensionModelLoaderRepository extensionModelLoaderRepository) {
    super(domainDescriptorFactory, deployableArtifactDescriptorFactory, new DefaultDomainManager(),
          classLoaderRepository, serviceRepository, domainClassLoaderBuilderFactory,
          extensionModelLoaderRepository, mock(LicenseValidator.class),
          mock(MemoryManagementService.class),
          serializedAstWithFallbackArtifactConfigurationProcessor());
  }

  @Override
  public Domain createArtifact(File artifactLocation, Optional<Properties> properties) throws IOException {
    final Domain domain = super.createArtifact(artifactLocation, properties);
    TestDomainWrapper testDomainWrapper = new TestDomainWrapper(domain);

    if (this.failOnStop) {
      testDomainWrapper.setFailOnStop();
      afterTasks.add(() -> domain.stop());
    }
    if (this.failOnDispose) {
      testDomainWrapper.setFailOnDispose();
      afterTasks.add(() -> domain.dispose());
    }
    return testDomainWrapper;
  }

  public void setFailOnStopApplication() {
    failOnStop = true;
  }

  public void setFailOnDisposeApplication() {
    failOnDispose = true;
  }

  /**
   * Finish the dispose/stop process that was aborted due to an exception.
   */
  public static void after() {
    afterTasks.forEach(Runnable::run);
    afterTasks.clear();
  }
}
