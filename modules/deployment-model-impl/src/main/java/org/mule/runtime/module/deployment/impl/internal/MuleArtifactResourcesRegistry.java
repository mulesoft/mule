/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.module.deployment.impl.internal;

import static org.mule.runtime.api.util.Preconditions.checkArgument;

import org.mule.runtime.container.api.ModuleRepository;
import org.mule.runtime.container.internal.ContainerClassLoaderFactory;
import org.mule.runtime.container.internal.ContainerModuleDiscoverer;
import org.mule.runtime.container.internal.DefaultModuleRepository;
import org.mule.runtime.core.registry.SpiServiceRegistry;
import org.mule.runtime.deployment.model.api.application.Application;
import org.mule.runtime.deployment.model.api.application.ApplicationDescriptor;
import org.mule.runtime.deployment.model.api.domain.DomainDescriptor;
import org.mule.runtime.deployment.model.api.plugin.ArtifactPluginClassLoaderFactory;
import org.mule.runtime.deployment.model.api.plugin.ArtifactPluginDescriptor;
import org.mule.runtime.deployment.model.api.policy.PolicyTemplateDescriptor;
import org.mule.runtime.deployment.model.internal.application.MuleApplicationClassLoaderFactory;
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
import org.mule.runtime.module.deployment.impl.internal.application.ToolingApplicationFactory;
import org.mule.runtime.module.deployment.impl.internal.artifact.DefaultClassLoaderManager;
import org.mule.runtime.module.deployment.impl.internal.artifact.ServiceRegistryDescriptorLoaderRepository;
import org.mule.runtime.module.deployment.impl.internal.domain.DefaultDomainFactory;
import org.mule.runtime.module.deployment.impl.internal.domain.DefaultDomainManager;
import org.mule.runtime.module.deployment.impl.internal.plugin.ArtifactPluginDescriptorFactory;
import org.mule.runtime.module.deployment.impl.internal.plugin.ArtifactPluginDescriptorLoader;
import org.mule.runtime.module.deployment.impl.internal.plugin.DefaultArtifactPluginRepository;
import org.mule.runtime.module.deployment.impl.internal.plugin.MuleExtensionModelLoaderManager;
import org.mule.runtime.module.deployment.impl.internal.policy.ApplicationPolicyTemplateClassLoaderBuilderFactory;
import org.mule.runtime.module.deployment.impl.internal.policy.PolicyTemplateClassLoaderBuilderFactory;
import org.mule.runtime.module.extension.internal.loader.ExtensionModelLoaderManager;
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
  private final ToolingApplicationFactory toolingApplicationFactory;
  private final TemporaryApplicationFactory temporaryApplicationFactory;
  private final DefaultArtifactPluginRepository artifactPluginRepository;
  private final DeployableArtifactClassLoaderFactory<DomainDescriptor> domainClassLoaderFactory;
  private final ArtifactClassLoader containerClassLoader;
  private final MuleServiceManager serviceManager;
  private final ExtensionModelLoaderManager extensionModelLoaderManager;
  private final ArtifactClassLoaderFactory<ArtifactPluginDescriptor> artifactPluginClassLoaderFactory;
  private final DefaultClassLoaderManager artifactClassLoaderManager;
  private final ModuleRepository moduleRepository;
  private final ApplicationClassLoaderBuilderFactory applicationClassLoaderBuilderFactory;
  private final ApplicationDescriptorFactory applicationDescriptorFactory;
  private final PluginDependenciesResolver pluginDependenciesResolver;
  private final ServiceRegistryDescriptorLoaderRepository descriptorLoaderRepository;

  /**
   * Builds a {@link MuleArtifactResourcesRegistry} instance
   */
  public static class Builder {

    private ModuleRepository moduleRepository;

    /**
     * Configures the {@link ModuleRepository} to use
     *
     * @param moduleRepository provides access to the modules available on the container. Non null.
     * @return same builder instance
     */
    public Builder moduleRepository(ModuleRepository moduleRepository) {
      checkArgument(moduleRepository != null, "moduleRepository cannot be null");

      this.moduleRepository = moduleRepository;
      return this;
    }

    /**
     * Builds the desired instance
     *
     * @return a new {@link MuleArtifactResourcesRegistry} with the provided configuration.
     */
    public MuleArtifactResourcesRegistry build() {
      if (moduleRepository == null) {
        moduleRepository = new DefaultModuleRepository(new ContainerModuleDiscoverer(this.getClass().getClassLoader()));
      }

      return new MuleArtifactResourcesRegistry(moduleRepository);
    }
  }

  /**
   * Creates a repository for resources required for mule artifacts.
   */
  private MuleArtifactResourcesRegistry(ModuleRepository moduleRepository) {
    this.moduleRepository = moduleRepository;

    containerClassLoader =
        new ContainerClassLoaderFactory(moduleRepository).createContainerClassLoader(getClass().getClassLoader());
    artifactClassLoaderManager = new DefaultClassLoaderManager();

    domainManager = new DefaultDomainManager();
    this.domainClassLoaderFactory = trackDeployableArtifactClassLoaderFactory(
                                                                              new DomainClassLoaderFactory(containerClassLoader
                                                                                  .getClassLoader()));

    this.artifactPluginClassLoaderFactory =
        trackArtifactClassLoaderFactory(new ArtifactPluginClassLoaderFactory(moduleRepository));
    final ArtifactPluginDescriptorFactory artifactPluginDescriptorFactory =
        new ArtifactPluginDescriptorFactory();
    artifactPluginRepository = new DefaultArtifactPluginRepository(artifactPluginDescriptorFactory);
    artifactPluginDescriptorLoader = new ArtifactPluginDescriptorLoader(artifactPluginDescriptorFactory);
    descriptorLoaderRepository = new ServiceRegistryDescriptorLoaderRepository(new SpiServiceRegistry());
    applicationDescriptorFactory =
        new ApplicationDescriptorFactory(artifactPluginDescriptorLoader, artifactPluginRepository, descriptorLoaderRepository);
    DeployableArtifactClassLoaderFactory<ApplicationDescriptor> applicationClassLoaderFactory =
        trackDeployableArtifactClassLoaderFactory(new MuleApplicationClassLoaderFactory(new DefaultNativeLibraryFinderFactory()));
    pluginDependenciesResolver = new BundlePluginDependenciesResolver(artifactPluginDescriptorFactory);
    applicationClassLoaderBuilderFactory =
        new ApplicationClassLoaderBuilderFactory(applicationClassLoaderFactory, this.artifactPluginClassLoaderFactory);
    ArtifactClassLoaderFactory<ServiceDescriptor> serviceClassLoaderFactory = new ServiceClassLoaderFactory();
    serviceManager =
        new MuleServiceManager(new DefaultServiceDiscoverer(
                                                            new FileSystemServiceProviderDiscoverer(containerClassLoader,
                                                                                                    trackArtifactClassLoaderFactory(serviceClassLoaderFactory)),
                                                            new ReflectionServiceResolver(new ReflectionServiceProviderResolutionHelper())));
    extensionModelLoaderManager = new MuleExtensionModelLoaderManager(containerClassLoader);
    domainFactory = new DefaultDomainFactory(this.domainClassLoaderFactory, domainManager, containerClassLoader,
                                             artifactClassLoaderManager, serviceManager);

    DeployableArtifactClassLoaderFactory<PolicyTemplateDescriptor> policyClassLoaderFactory =
        trackDeployableArtifactClassLoaderFactory(new PolicyTemplateClassLoaderFactory());
    PolicyTemplateClassLoaderBuilderFactory policyTemplateClassLoaderBuilderFactory =
        new ApplicationPolicyTemplateClassLoaderBuilderFactory(policyClassLoaderFactory, artifactPluginClassLoaderFactory);

    applicationFactory = new DefaultApplicationFactory(applicationClassLoaderBuilderFactory, applicationDescriptorFactory,
                                                       artifactPluginRepository, domainManager, serviceManager,
                                                       extensionModelLoaderManager,
                                                       artifactClassLoaderManager, policyTemplateClassLoaderBuilderFactory,
                                                       pluginDependenciesResolver, artifactPluginDescriptorLoader);
    toolingApplicationFactory = new ToolingApplicationFactory(applicationClassLoaderBuilderFactory, applicationDescriptorFactory,
                                                              artifactPluginRepository, domainManager, serviceManager,
                                                              extensionModelLoaderManager,
                                                              artifactClassLoaderManager, policyTemplateClassLoaderBuilderFactory,
                                                              pluginDependenciesResolver, artifactPluginDescriptorLoader);
    temporaryApplicationFactory = new TemporaryApplicationFactory(applicationClassLoaderBuilderFactory,
                                                                  new TemporaryApplicationDescriptorFactory(artifactPluginDescriptorLoader,
                                                                                                            artifactPluginRepository,
                                                                                                            descriptorLoaderRepository),
                                                                  artifactPluginRepository, domainManager, serviceManager,
                                                                  extensionModelLoaderManager,
                                                                  artifactClassLoaderManager,
                                                                  policyTemplateClassLoaderBuilderFactory,
                                                                  pluginDependenciesResolver,
                                                                  artifactPluginDescriptorLoader);

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
   * @return factory for creating {@link Application} artifacts for Tooling
   */
  public ToolingApplicationFactory getToolingApplicationFactory() {
    return toolingApplicationFactory;
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
   * @return the manager of available extension loaders.
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

  public ApplicationDescriptorFactory getApplicationDescriptorFactory() {
    return applicationDescriptorFactory;
  }

  /**
   * @return resolver for dependencies for plugins
   */
  public PluginDependenciesResolver getPluginDependenciesResolver() {
    return pluginDependenciesResolver;
  }
}
