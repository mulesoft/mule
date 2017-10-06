/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.module.deployment.internal;

import org.mule.runtime.container.api.ModuleRepository;
import org.mule.runtime.deployment.model.api.domain.Domain;
import org.mule.runtime.deployment.model.api.plugin.ArtifactPluginClassLoaderFactory;
import org.mule.runtime.deployment.model.internal.DefaultRegionPluginClassLoadersFactory;
import org.mule.runtime.deployment.model.internal.domain.DomainClassLoaderFactory;
import org.mule.runtime.deployment.model.internal.plugin.BundlePluginDependenciesResolver;
import org.mule.runtime.deployment.model.internal.plugin.PluginDependenciesResolver;
import org.mule.runtime.module.artifact.api.classloader.ArtifactClassLoader;
import org.mule.runtime.module.artifact.api.classloader.ClassLoaderRepository;
import org.mule.runtime.module.artifact.api.classloader.TrackingArtifactClassLoaderFactory;
import org.mule.runtime.module.deployment.impl.internal.artifact.DefaultClassLoaderManager;
import org.mule.runtime.module.artifact.api.descriptor.DescriptorLoaderRepository;
import org.mule.runtime.module.deployment.impl.internal.domain.DefaultDomainFactory;
import org.mule.runtime.module.deployment.impl.internal.domain.DefaultDomainManager;
import org.mule.runtime.module.deployment.impl.internal.domain.DomainClassLoaderBuilderFactory;
import org.mule.runtime.module.deployment.impl.internal.domain.DomainDescriptorFactory;
import org.mule.runtime.module.deployment.impl.internal.domain.TestDomainWrapper;
import org.mule.runtime.module.deployment.impl.internal.plugin.ArtifactPluginDescriptorFactory;
import org.mule.runtime.module.deployment.impl.internal.plugin.ArtifactPluginDescriptorLoader;
import org.mule.runtime.module.deployment.impl.internal.plugin.MuleExtensionModelLoaderManager;
import org.mule.runtime.module.extension.internal.loader.ExtensionModelLoaderManager;
import org.mule.runtime.module.service.api.manager.ServiceRepository;

import java.io.File;
import java.io.IOException;
import java.util.Optional;
import java.util.Properties;

public class TestDomainFactory extends DefaultDomainFactory {

  private boolean failOnStop;
  private boolean failOnDispose;

  public static TestDomainFactory createDomainFactory(
                                                      DomainClassLoaderFactory domainClassLoaderFactory,
                                                      ArtifactClassLoader containerClassLoader,
                                                      ServiceRepository serviceRepository,
                                                      ModuleRepository moduleRepository,
                                                      DescriptorLoaderRepository descriptorLoaderRepository) {
    ArtifactPluginDescriptorFactory artifactPluginDescriptorFactory =
        new ArtifactPluginDescriptorFactory();
    ArtifactPluginDescriptorLoader artifactPluginDescriptorLoader =
        new ArtifactPluginDescriptorLoader(artifactPluginDescriptorFactory);
    DomainDescriptorFactory domainDescriptorFactory =
        new DomainDescriptorFactory(artifactPluginDescriptorLoader, descriptorLoaderRepository);
    final DefaultClassLoaderManager artifactClassLoaderManager = new DefaultClassLoaderManager();
    PluginDependenciesResolver pluginDependenciesResolver = new BundlePluginDependenciesResolver(artifactPluginDescriptorFactory);

    DomainClassLoaderBuilderFactory domainClassLoaderBuilderFactory =
        new DomainClassLoaderBuilderFactory(containerClassLoader, domainClassLoaderFactory,
                                            new DefaultRegionPluginClassLoadersFactory(new TrackingArtifactClassLoaderFactory<>(artifactClassLoaderManager,
                                                                                                                                new ArtifactPluginClassLoaderFactory()),
                                                                                       moduleRepository));

    return new TestDomainFactory(artifactClassLoaderManager, serviceRepository, domainDescriptorFactory,
                                 pluginDependenciesResolver,
                                 domainClassLoaderBuilderFactory, new MuleExtensionModelLoaderManager(containerClassLoader));
  }

  private TestDomainFactory(ClassLoaderRepository classLoaderRepository,
                            ServiceRepository serviceRepository, DomainDescriptorFactory domainDescriptorFactory,
                            PluginDependenciesResolver pluginDependenciesResolver,
                            DomainClassLoaderBuilderFactory domainClassLoaderBuilderFactory,
                            ExtensionModelLoaderManager extensionModelLoaderManager) {
    super(domainDescriptorFactory, new DefaultDomainManager(),
          classLoaderRepository, serviceRepository,
          pluginDependenciesResolver, domainClassLoaderBuilderFactory, extensionModelLoaderManager);
  }

  @Override
  public Domain createArtifact(File artifactLocation, Optional<Properties> properties) throws IOException {
    TestDomainWrapper testDomainWrapper = new TestDomainWrapper(super.createArtifact(artifactLocation, properties));
    if (this.failOnStop) {
      testDomainWrapper.setFailOnStop();
    }
    if (this.failOnDispose) {
      testDomainWrapper.setFailOnDispose();
    }
    return testDomainWrapper;
  }

  public void setFailOnStopApplication() {
    failOnStop = true;
  }

  public void setFailOnDisposeApplication() {
    failOnDispose = true;
  }

}
