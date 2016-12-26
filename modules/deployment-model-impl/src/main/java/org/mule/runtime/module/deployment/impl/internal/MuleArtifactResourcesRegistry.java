/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.module.deployment.impl.internal;

import org.mule.runtime.container.internal.ContainerClassLoaderFactory;
import org.mule.runtime.deployment.model.api.application.Application;
import org.mule.runtime.deployment.model.api.application.ApplicationDescriptor;
import org.mule.runtime.deployment.model.api.domain.DomainDescriptor;
import org.mule.runtime.deployment.model.api.plugin.ArtifactPluginClassLoaderFactory;
import org.mule.runtime.deployment.model.api.plugin.ArtifactPluginDescriptor;
import org.mule.runtime.deployment.model.api.policy.PolicyTemplateDescriptor;
import org.mule.runtime.deployment.model.internal.application.MuleApplicationClassLoaderFactory;
import org.mule.runtime.deployment.model.internal.artifact.DefaultDependenciesProvider;
import org.mule.runtime.deployment.model.internal.domain.DomainClassLoaderFactory;
import org.mule.runtime.deployment.model.internal.nativelib.DefaultNativeLibraryFinderFactory;
import org.mule.runtime.deployment.model.internal.plugin.BundlePluginDependenciesResolver;
import org.mule.runtime.deployment.model.internal.plugin.PluginDependenciesResolver;
import org.mule.runtime.deployment.model.internal.policy.PolicyTemplateClassLoaderFactory;
import org.mule.runtime.module.artifact.classloader.ArtifactClassLoader;
import org.mule.runtime.module.artifact.classloader.ArtifactClassLoaderFactory;
import org.mule.runtime.module.artifact.classloader.DeployableArtifactClassLoaderFactory;
import org.mule.runtime.module.artifact.classloader.TrackingArtifactClassLoaderFactory;
import org.mule.runtime.module.artifact.classloader.TrackingDeployableArtifactClassLoaderFactory;
import org.mule.runtime.module.artifact.descriptor.ArtifactDescriptor;
import org.mule.runtime.module.deployment.impl.internal.application.ApplicationClassLoaderBuilderFactory;
import org.mule.runtime.module.deployment.impl.internal.application.ApplicationDescriptorFactory;
import org.mule.runtime.module.deployment.impl.internal.application.DefaultApplicationFactory;
import org.mule.runtime.module.deployment.impl.internal.application.TemporaryApplicationDescriptorFactory;
import org.mule.runtime.module.deployment.impl.internal.application.TemporaryApplicationFactory;
import org.mule.runtime.module.deployment.impl.internal.artifact.DefaultClassLoaderManager;
import org.mule.runtime.module.deployment.impl.internal.domain.DefaultDomainFactory;
import org.mule.runtime.module.deployment.impl.internal.domain.DefaultDomainManager;
import org.mule.runtime.module.deployment.impl.internal.plugin.ArtifactPluginDescriptorFactory;
import org.mule.runtime.module.deployment.impl.internal.plugin.ArtifactPluginDescriptorLoader;
import org.mule.runtime.module.deployment.impl.internal.plugin.DefaultArtifactPluginRepository;
import org.mule.runtime.module.extension.internal.loader.ExtensionModelLoaderManager;
import org.mule.runtime.module.deployment.impl.internal.plugin.MuleExtensionModelLoaderManager;
import org.mule.runtime.module.deployment.impl.internal.policy.ApplicationPolicyTemplateClassLoaderBuilderFactory;
import org.mule.runtime.module.deployment.impl.internal.policy.PolicyTemplateClassLoaderBuilderFactory;
import org.mule.runtime.module.deployment.impl.internal.temporary.TemporaryArtifactClassLoaderBuilderFactory;
import org.mule.runtime.module.service.DefaultServiceDiscoverer;
import org.mule.runtime.module.service.FileSystemServiceProviderDiscoverer;
import org.mule.runtime.module.service.MuleServiceManager;
import org.mule.runtime.module.service.ReflectionServiceProviderResolutionHelper;
import org.mule.runtime.module.service.ReflectionServiceResolver;
import org.mule.runtime.module.service.ServiceClassLoaderFactory;
import org.mule.runtime.module.service.ServiceDescriptor;

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
  private final TemporaryApplicationFactory temporaryApplicationFactory;
  private final DefaultArtifactPluginRepository artifactPluginRepository;
  private final DeployableArtifactClassLoaderFactory<DomainDescriptor> domainClassLoaderFactory;
  private final TemporaryArtifactClassLoaderBuilderFactory temporaryArtifactClassLoaderBuilderFactory;
  private final ArtifactClassLoader containerClassLoader;
  private final MuleServiceManager serviceManager;
  private final ExtensionModelLoaderManager extensionModelLoaderManager;
  private final ArtifactClassLoaderFactory<ArtifactPluginDescriptor> artifactPluginClassLoaderFactory;
  private final DefaultClassLoaderManager artifactClassLoaderManager;

  /**
   * Creates a repository for resources required for mule artifacts.
   */
  public MuleArtifactResourcesRegistry() {
    containerClassLoader = new ContainerClassLoaderFactory().createContainerClassLoader(getClass().getClassLoader());
    artifactClassLoaderManager = new DefaultClassLoaderManager();

    domainManager = new DefaultDomainManager();
    this.domainClassLoaderFactory = trackDeployableArtifactClassLoaderFactory(
                                                                              new DomainClassLoaderFactory(containerClassLoader
                                                                                  .getClassLoader()));
    this.artifactPluginClassLoaderFactory = trackArtifactClassLoaderFactory(new ArtifactPluginClassLoaderFactory());
    final ArtifactPluginDescriptorFactory artifactPluginDescriptorFactory =
        new ArtifactPluginDescriptorFactory();
    artifactPluginRepository = new DefaultArtifactPluginRepository(artifactPluginDescriptorFactory);
    artifactPluginDescriptorLoader = new ArtifactPluginDescriptorLoader(artifactPluginDescriptorFactory);
    final ApplicationDescriptorFactory applicationDescriptorFactory =
        new ApplicationDescriptorFactory(artifactPluginDescriptorLoader, artifactPluginRepository);
    DeployableArtifactClassLoaderFactory<ApplicationDescriptor> applicationClassLoaderFactory =
        trackDeployableArtifactClassLoaderFactory(new MuleApplicationClassLoaderFactory(new DefaultNativeLibraryFinderFactory()));
    DefaultDependenciesProvider dependenciesProvider = new DefaultDependenciesProvider();
    PluginDependenciesResolver pluginDependenciesResolver =
        new BundlePluginDependenciesResolver(artifactPluginDescriptorFactory, dependenciesProvider);
    ApplicationClassLoaderBuilderFactory applicationClassLoaderBuilderFactory =
        new ApplicationClassLoaderBuilderFactory(applicationClassLoaderFactory, artifactPluginRepository,
                                                 this.artifactPluginClassLoaderFactory,
                                                 pluginDependenciesResolver);
    ArtifactClassLoaderFactory<ServiceDescriptor> serviceClassLoaderFactory = new ServiceClassLoaderFactory();
    serviceManager =
        new MuleServiceManager(new DefaultServiceDiscoverer(
                                                            new FileSystemServiceProviderDiscoverer(containerClassLoader,
                                                                                                    trackArtifactClassLoaderFactory(serviceClassLoaderFactory)),
                                                            new ReflectionServiceResolver(new ReflectionServiceProviderResolutionHelper())));
    extensionModelLoaderManager = new MuleExtensionModelLoaderManager(containerClassLoader);
    domainFactory = new DefaultDomainFactory(this.domainClassLoaderFactory, domainManager, containerClassLoader,
                                             artifactClassLoaderManager, serviceManager);

    ArtifactClassLoaderFactory<PolicyTemplateDescriptor> policyClassLoaderFactory =
        trackArtifactClassLoaderFactory(new PolicyTemplateClassLoaderFactory());
    PolicyTemplateClassLoaderBuilderFactory policyTemplateClassLoaderBuilderFactory =
        new ApplicationPolicyTemplateClassLoaderBuilderFactory(policyClassLoaderFactory, artifactPluginRepository,
                                                               artifactPluginClassLoaderFactory,
                                                               pluginDependenciesResolver);

    applicationFactory = new DefaultApplicationFactory(applicationClassLoaderBuilderFactory, applicationDescriptorFactory,
                                                       artifactPluginRepository, domainManager, serviceManager,
                                                       extensionModelLoaderManager,
                                                       artifactClassLoaderManager, policyTemplateClassLoaderBuilderFactory);
    temporaryApplicationFactory = new TemporaryApplicationFactory(applicationClassLoaderBuilderFactory,
                                                                  new TemporaryApplicationDescriptorFactory(artifactPluginDescriptorLoader,
                                                                                                            artifactPluginRepository),
                                                                  artifactPluginRepository, domainManager, serviceManager,
                                                                  extensionModelLoaderManager,
                                                                  artifactClassLoaderManager,
                                                                  policyTemplateClassLoaderBuilderFactory);

    temporaryArtifactClassLoaderBuilderFactory =
        new TemporaryArtifactClassLoaderBuilderFactory(artifactPluginClassLoaderFactory,
                                                       applicationClassLoaderFactory,
                                                       pluginDependenciesResolver);
  }

  private <T extends ArtifactDescriptor> ArtifactClassLoaderFactory<T> trackArtifactClassLoaderFactory(ArtifactClassLoaderFactory<T> artifactClassLoaderFactory) {
    return new TrackingArtifactClassLoaderFactory<>(artifactClassLoaderManager, artifactClassLoaderFactory);
  }

  private <T extends ArtifactDescriptor> DeployableArtifactClassLoaderFactory<T> trackDeployableArtifactClassLoaderFactory(DeployableArtifactClassLoaderFactory<T> deployableArtifactClassLoaderFactory) {
    return new TrackingDeployableArtifactClassLoaderFactory<>(artifactClassLoaderManager,
                                                              deployableArtifactClassLoaderFactory);
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
   * @return factory for creating temporary {@link Application} artifacts
   */
  public TemporaryApplicationFactory getTemporaryApplicationFactory() {
    return temporaryApplicationFactory;
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
  public DeployableArtifactClassLoaderFactory<DomainDescriptor> getDomainClassLoaderFactory() {
    return domainClassLoaderFactory;
  }


  /**
   * @return factory for creating artifact plugin class loaders
   */
  public ArtifactClassLoaderFactory<ArtifactPluginDescriptor> getArtifactPluginClassLoaderFactory() {
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

  /**
   * @return the manager of container extension loaders.
   */
  public ExtensionModelLoaderManager getExtensionModelLoaderManager() {
    return extensionModelLoaderManager;
  }

  /**
   * @return the artifact classLoader manager
   */
  public DefaultClassLoaderManager getArtifactClassLoaderManager() {
    return artifactClassLoaderManager;
  }
}
