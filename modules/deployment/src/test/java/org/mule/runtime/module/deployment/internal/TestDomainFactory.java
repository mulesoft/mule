/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.module.deployment.internal;

import static org.mockito.Mockito.mock;
import static org.mule.runtime.deployment.model.api.artifact.ArtifactDescriptorFactoryProvider.artifactDescriptorFactoryProvider;

import org.mule.runtime.api.deployment.meta.MulePluginModel;
import org.mule.runtime.api.memory.management.MemoryManagementService;
import org.mule.runtime.api.service.ServiceRepository;
import org.mule.runtime.container.api.ModuleRepository;
import org.mule.runtime.core.internal.config.RuntimeLockFactoryUtil;
import org.mule.runtime.deployment.model.api.artifact.DescriptorLoaderRepositoryFactory;
import org.mule.runtime.deployment.model.api.builder.DomainClassLoaderBuilderFactory;
import org.mule.runtime.deployment.model.api.domain.Domain;
import org.mule.runtime.deployment.model.api.domain.DomainDescriptor;
import org.mule.runtime.deployment.model.api.plugin.ArtifactPluginClassLoaderFactory;
import org.mule.runtime.deployment.model.api.plugin.ArtifactPluginDescriptor;
import org.mule.runtime.deployment.model.api.plugin.resolver.PluginDependenciesResolver;
import org.mule.runtime.deployment.model.internal.DefaultRegionPluginClassLoadersFactory;
import org.mule.runtime.deployment.model.internal.artifact.extension.ExtensionModelLoaderManager;
import org.mule.runtime.deployment.model.internal.artifact.extension.MuleExtensionModelLoaderManager;
import org.mule.runtime.module.artifact.api.classloader.ArtifactClassLoader;
import org.mule.runtime.module.artifact.api.classloader.ClassLoaderRepository;
import org.mule.runtime.module.artifact.api.classloader.DeployableArtifactClassLoaderFactory;
import org.mule.runtime.module.artifact.api.classloader.TrackingArtifactClassLoaderFactory;
import org.mule.runtime.module.artifact.api.descriptor.AbstractArtifactDescriptorFactory;
import org.mule.runtime.module.artifact.api.descriptor.ArtifactDescriptorValidatorBuilder;
import org.mule.runtime.module.artifact.api.descriptor.DescriptorLoaderRepository;
import org.mule.runtime.module.deployment.impl.internal.artifact.DefaultArtifactDescriptorFactoryProvider;
import org.mule.runtime.module.deployment.impl.internal.artifact.DefaultClassLoaderManager;
import org.mule.runtime.module.deployment.impl.internal.domain.DefaultDomainFactory;
import org.mule.runtime.module.deployment.impl.internal.domain.DefaultDomainManager;
import org.mule.runtime.module.deployment.impl.internal.domain.DomainDescriptorFactory;
import org.mule.runtime.module.deployment.impl.internal.domain.TestDomainWrapper;
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

    DomainClassLoaderBuilderFactory domainClassLoaderBuilderFactory =
        new DomainClassLoaderBuilderFactory(containerClassLoader, deployableArtifactClassLoaderFactory,
                                            new DefaultRegionPluginClassLoadersFactory(new TrackingArtifactClassLoaderFactory<>(artifactClassLoaderManager,
                                                                                                                                new ArtifactPluginClassLoaderFactory()),
                                                                                       moduleRepository));

    return new TestDomainFactory(artifactClassLoaderManager, serviceRepository, domainDescriptorFactory,
                                 pluginDependenciesResolver,
                                 domainClassLoaderBuilderFactory, new MuleExtensionModelLoaderManager(containerClassLoader));
  }

  private TestDomainFactory(ClassLoaderRepository classLoaderRepository,
                            ServiceRepository serviceRepository,
                            DomainDescriptorFactory domainDescriptorFactory,
                            PluginDependenciesResolver pluginDependenciesResolver,
                            DomainClassLoaderBuilderFactory domainClassLoaderBuilderFactory,
                            ExtensionModelLoaderManager extensionModelLoaderManager) {
    super(domainDescriptorFactory, new DefaultDomainManager(),
          classLoaderRepository, serviceRepository,
          pluginDependenciesResolver, domainClassLoaderBuilderFactory,
          extensionModelLoaderManager, mock(LicenseValidator.class),
          RuntimeLockFactoryUtil.getRuntimeLockFactory(),
          mock(MemoryManagementService.class));
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
