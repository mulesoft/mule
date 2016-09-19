/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.module.deployment.internal;

import org.mule.runtime.container.internal.ContainerClassLoaderFactory;
import org.mule.runtime.deployment.model.api.application.Application;
import org.mule.runtime.deployment.model.api.plugin.ArtifactPluginClassLoaderFactory;
import org.mule.runtime.deployment.model.api.plugin.ArtifactPluginDescriptor;
import org.mule.runtime.deployment.model.internal.application.MuleApplicationClassLoaderFactory;
import org.mule.runtime.deployment.model.internal.domain.DomainClassLoaderFactory;
import org.mule.runtime.deployment.model.internal.nativelib.DefaultNativeLibraryFinderFactory;
import org.mule.runtime.module.artifact.classloader.ArtifactClassLoader;
import org.mule.runtime.module.artifact.classloader.ArtifactClassLoaderFilterFactory;
import org.mule.runtime.module.deployment.internal.application.ApplicationClassLoaderBuilderFactory;
import org.mule.runtime.module.deployment.internal.application.ApplicationDescriptorFactory;
import org.mule.runtime.module.deployment.internal.application.DefaultApplicationFactory;
import org.mule.runtime.module.deployment.internal.domain.DefaultDomainFactory;
import org.mule.runtime.module.deployment.internal.domain.DefaultDomainManager;
import org.mule.runtime.module.deployment.internal.plugin.ArtifactPluginDescriptorFactory;
import org.mule.runtime.module.deployment.internal.plugin.ArtifactPluginDescriptorLoader;
import org.mule.runtime.module.deployment.internal.plugin.DefaultArtifactPluginRepository;
import org.mule.runtime.module.service.DefaultServiceDiscoverer;
import org.mule.runtime.module.service.FileSystemServiceProviderDiscoverer;
import org.mule.runtime.module.service.MuleServiceManager;
import org.mule.runtime.module.service.ReflectionServiceProviderResolutionHelper;
import org.mule.runtime.module.service.ReflectionServiceResolver;
import org.mule.runtime.module.service.ServiceClassLoaderFactory;

/**
 * Registry of mule artifact resources required to construct new artifacts.
 *
 * @since 4.0
 */
public class MuleArtifactResourcesRegistry {

  private final ArtifactPluginDescriptorLoader artifactPluginDescriptorLoader;
  private final DefaultDomainManager domainManager;
  private final DefaultDomainFactory domainFactory;
  private final DefaultApplicationFactory applicationFactory;
  private final DefaultArtifactPluginRepository artifactPluginRepository;
  private final DomainClassLoaderFactory domainClassLoaderFactory;
  private final TemporaryArtifactClassLoaderBuilderFactory temporaryArtifactClassLoaderBuilderFactory;
  private final ArtifactClassLoader containerClassLoader;
  private final MuleServiceManager serviceManager;
  private final ArtifactPluginClassLoaderFactory artifactPluginClassLoaderFactory;

  /**
   * Creates a repository for resources required for mule artifacts.
   */
  public MuleArtifactResourcesRegistry() {
    containerClassLoader = new ContainerClassLoaderFactory().createContainerClassLoader(getClass().getClassLoader());
    domainManager = new DefaultDomainManager();
    domainClassLoaderFactory = new DomainClassLoaderFactory(containerClassLoader.getClassLoader());
    domainFactory = new DefaultDomainFactory(domainClassLoaderFactory, domainManager, containerClassLoader);
    artifactPluginClassLoaderFactory = new ArtifactPluginClassLoaderFactory();
    final ArtifactPluginDescriptorFactory artifactPluginDescriptorFactory =
        new ArtifactPluginDescriptorFactory(new ArtifactClassLoaderFilterFactory());
    artifactPluginRepository = new DefaultArtifactPluginRepository(artifactPluginDescriptorFactory);
    artifactPluginDescriptorLoader = new ArtifactPluginDescriptorLoader(artifactPluginDescriptorFactory);
    final ApplicationDescriptorFactory applicationDescriptorFactory =
        new ApplicationDescriptorFactory(artifactPluginDescriptorLoader, artifactPluginRepository);
    MuleApplicationClassLoaderFactory applicationClassLoaderFactory =
        new MuleApplicationClassLoaderFactory(new DefaultNativeLibraryFinderFactory());
    ApplicationClassLoaderBuilderFactory applicationClassLoaderBuilderFactory =
        new ApplicationClassLoaderBuilderFactory(applicationClassLoaderFactory, artifactPluginRepository,
                                                 artifactPluginClassLoaderFactory);
    serviceManager =
        new MuleServiceManager(new DefaultServiceDiscoverer(new FileSystemServiceProviderDiscoverer(containerClassLoader,
                                                                                                    new ServiceClassLoaderFactory()),
                                                            new ReflectionServiceResolver(new ReflectionServiceProviderResolutionHelper())));
    applicationFactory = new DefaultApplicationFactory(applicationClassLoaderBuilderFactory, applicationDescriptorFactory,
                                                       artifactPluginRepository, domainManager, serviceManager);
    temporaryArtifactClassLoaderBuilderFactory =
        new TemporaryArtifactClassLoaderBuilderFactory(artifactPluginRepository, null);
  }

  /**
   * @return a loader for creating the {@link ArtifactPluginDescriptor} from a zipped extension
   */
  public ArtifactPluginDescriptorLoader getArtifactPluginDescriptorLoader() {
    return artifactPluginDescriptorLoader;
  }

  /**
   * @return the domain factory used to create and register domains
   */
  public DefaultDomainFactory getDomainFactory() {
    return domainFactory;
  }

  /**
   * @return factory for creating {@link Application} artifacts
   */
  public DefaultApplicationFactory getApplicationFactory() {
    return applicationFactory;
  }

  /**
   * @return repository of plugins provided by the container
   */
  public DefaultArtifactPluginRepository getArtifactPluginRepository() {
    return artifactPluginRepository;
  }

  /**
   * @return factory for creating domain class loaders
   */
  public DomainClassLoaderFactory getDomainClassLoaderFactory() {
    return domainClassLoaderFactory;
  }


  /**
   * @return factory for creating artifact plugin class loaders
   */
  public ArtifactPluginClassLoaderFactory getArtifactPluginClassLoaderFactory() {
    return artifactPluginClassLoaderFactory;
  }

  /**
   * @return factory for creating a builder instance for configuring and instantiate a temporary artifact
   */
  public TemporaryArtifactClassLoaderBuilderFactory getTemporaryArtifactClassLoaderBuilderFactory() {
    return temporaryArtifactClassLoaderBuilderFactory;
  }

  /**
   * @return the container class loader
   */
  public ArtifactClassLoader getContainerClassLoader() {
    return containerClassLoader;
  }

  /**
   * @return the manager of container services that must be included in each artifact
   */
  public MuleServiceManager getServiceManager() {
    return serviceManager;
  }
}
