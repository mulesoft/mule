/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.module.deployment.impl.internal;

import static java.lang.Thread.currentThread;
import static org.mule.runtime.api.util.Preconditions.checkArgument;
import static org.mule.runtime.module.license.api.LicenseValidatorProvider.discoverLicenseValidator;
import org.mule.runtime.container.api.ModuleRepository;
import org.mule.runtime.container.internal.ContainerClassLoaderFactory;
import org.mule.runtime.container.internal.ContainerModuleDiscoverer;
import org.mule.runtime.container.internal.DefaultModuleRepository;
import org.mule.runtime.core.api.registry.SpiServiceRegistry;
import org.mule.runtime.deployment.model.api.application.Application;
import org.mule.runtime.deployment.model.api.application.ApplicationDescriptor;
import org.mule.runtime.deployment.model.api.domain.DomainDescriptor;
import org.mule.runtime.deployment.model.api.plugin.ArtifactPluginClassLoaderFactory;
import org.mule.runtime.deployment.model.api.plugin.ArtifactPluginDescriptor;
import org.mule.runtime.deployment.model.api.policy.PolicyTemplateDescriptor;
import org.mule.runtime.deployment.model.internal.DefaultRegionPluginClassLoadersFactory;
import org.mule.runtime.deployment.model.internal.RegionPluginClassLoadersFactory;
import org.mule.runtime.deployment.model.internal.application.MuleApplicationClassLoaderFactory;
import org.mule.runtime.deployment.model.internal.domain.DomainClassLoaderFactory;
import org.mule.runtime.deployment.model.internal.nativelib.DefaultNativeLibraryFinderFactory;
import org.mule.runtime.deployment.model.internal.plugin.BundlePluginDependenciesResolver;
import org.mule.runtime.deployment.model.internal.plugin.PluginDependenciesResolver;
import org.mule.runtime.deployment.model.internal.policy.PolicyTemplateClassLoaderFactory;
import org.mule.runtime.module.artifact.api.classloader.ArtifactClassLoader;
import org.mule.runtime.module.artifact.api.classloader.ArtifactClassLoaderFactory;
import org.mule.runtime.module.artifact.api.classloader.DeployableArtifactClassLoaderFactory;
import org.mule.runtime.module.artifact.api.classloader.TrackingArtifactClassLoaderFactory;
import org.mule.runtime.module.artifact.api.classloader.TrackingDeployableArtifactClassLoaderFactory;
import org.mule.runtime.module.artifact.api.descriptor.ArtifactDescriptor;
import org.mule.runtime.module.deployment.impl.internal.application.ApplicationClassLoaderBuilderFactory;
import org.mule.runtime.module.deployment.impl.internal.application.ApplicationDescriptorFactory;
import org.mule.runtime.module.deployment.impl.internal.application.DefaultApplicationFactory;
import org.mule.runtime.module.deployment.impl.internal.artifact.DefaultClassLoaderManager;
import org.mule.runtime.module.deployment.impl.internal.artifact.ServiceRegistryDescriptorLoaderRepository;
import org.mule.runtime.module.deployment.impl.internal.domain.DefaultDomainFactory;
import org.mule.runtime.module.deployment.impl.internal.domain.DefaultDomainManager;
import org.mule.runtime.module.deployment.impl.internal.domain.DomainClassLoaderBuilderFactory;
import org.mule.runtime.module.deployment.impl.internal.domain.DomainDescriptorFactory;
import org.mule.runtime.module.deployment.impl.internal.plugin.ArtifactPluginDescriptorFactory;
import org.mule.runtime.module.deployment.impl.internal.plugin.ArtifactPluginDescriptorLoader;
import org.mule.runtime.module.deployment.impl.internal.plugin.MuleExtensionModelLoaderManager;
import org.mule.runtime.module.deployment.impl.internal.policy.ApplicationPolicyTemplateClassLoaderBuilderFactory;
import org.mule.runtime.module.deployment.impl.internal.policy.PolicyTemplateClassLoaderBuilderFactory;
import org.mule.runtime.module.extension.internal.loader.ExtensionModelLoaderManager;
import org.mule.runtime.module.license.api.LicenseValidator;
import org.mule.runtime.module.service.api.manager.ServiceManager;
import org.mule.runtime.module.service.internal.artifact.ServiceClassLoaderFactory;
import org.mule.runtime.module.service.internal.artifact.ServiceDescriptor;
import org.mule.runtime.module.service.internal.discoverer.DefaultServiceDiscoverer;
import org.mule.runtime.module.service.internal.discoverer.FileSystemServiceProviderDiscoverer;
import org.mule.runtime.module.service.internal.discoverer.ReflectionServiceProviderResolutionHelper;
import org.mule.runtime.module.service.internal.discoverer.ReflectionServiceResolver;

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
  private final DeployableArtifactClassLoaderFactory<DomainDescriptor> domainClassLoaderFactory;
  private final ArtifactClassLoader containerClassLoader;
  private final ServiceManager serviceManager;
  private final ExtensionModelLoaderManager extensionModelLoaderManager;
  private final ArtifactClassLoaderFactory<ArtifactPluginDescriptor> artifactPluginClassLoaderFactory;
  private final DefaultClassLoaderManager artifactClassLoaderManager;
  private final ApplicationClassLoaderBuilderFactory applicationClassLoaderBuilderFactory;
  private final DomainClassLoaderBuilderFactory domainClassLoaderBuilderFactory;
  private final DomainDescriptorFactory domainDescriptorFactory;
  private final ApplicationDescriptorFactory applicationDescriptorFactory;
  private final PluginDependenciesResolver pluginDependenciesResolver;
  private final ServiceRegistryDescriptorLoaderRepository descriptorLoaderRepository;
  private final RegionPluginClassLoadersFactory pluginClassLoadersFactory;


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
    containerClassLoader =
        new ContainerClassLoaderFactory(moduleRepository).createContainerClassLoader(getClass().getClassLoader());
    artifactClassLoaderManager = new DefaultClassLoaderManager();
    LicenseValidator licenseValidator = discoverLicenseValidator(currentThread().getContextClassLoader());

    domainManager = new DefaultDomainManager();
    this.domainClassLoaderFactory = trackDeployableArtifactClassLoaderFactory(
                                                                              new DomainClassLoaderFactory(containerClassLoader
                                                                                  .getClassLoader()));

    this.artifactPluginClassLoaderFactory =
        trackArtifactClassLoaderFactory(new ArtifactPluginClassLoaderFactory());
    final ArtifactPluginDescriptorFactory artifactPluginDescriptorFactory =
        new ArtifactPluginDescriptorFactory();
    artifactPluginDescriptorLoader = new ArtifactPluginDescriptorLoader(artifactPluginDescriptorFactory);
    descriptorLoaderRepository = new ServiceRegistryDescriptorLoaderRepository(new SpiServiceRegistry());
    domainDescriptorFactory = new DomainDescriptorFactory(artifactPluginDescriptorLoader, descriptorLoaderRepository);
    applicationDescriptorFactory = new ApplicationDescriptorFactory(artifactPluginDescriptorLoader, descriptorLoaderRepository);
    DeployableArtifactClassLoaderFactory<ApplicationDescriptor> applicationClassLoaderFactory =
        trackDeployableArtifactClassLoaderFactory(new MuleApplicationClassLoaderFactory(new DefaultNativeLibraryFinderFactory()));
    pluginDependenciesResolver = new BundlePluginDependenciesResolver(artifactPluginDescriptorFactory);
    pluginClassLoadersFactory = new DefaultRegionPluginClassLoadersFactory(artifactPluginClassLoaderFactory, moduleRepository);
    applicationClassLoaderBuilderFactory =
        new ApplicationClassLoaderBuilderFactory(applicationClassLoaderFactory, this.artifactPluginClassLoaderFactory,
                                                 pluginClassLoadersFactory);
    domainClassLoaderBuilderFactory =
        new DomainClassLoaderBuilderFactory(containerClassLoader, domainClassLoaderFactory,
                                            pluginClassLoadersFactory);
    ArtifactClassLoaderFactory<ServiceDescriptor> serviceClassLoaderFactory = new ServiceClassLoaderFactory();
    serviceManager =
        ServiceManager.create(new DefaultServiceDiscoverer(
                                                           new FileSystemServiceProviderDiscoverer(containerClassLoader,
                                                                                                   trackArtifactClassLoaderFactory(serviceClassLoaderFactory),
                                                                                                   descriptorLoaderRepository),
                                                           new ReflectionServiceResolver(new ReflectionServiceProviderResolutionHelper())));
    extensionModelLoaderManager = new MuleExtensionModelLoaderManager(containerClassLoader);
    domainFactory =
        new DefaultDomainFactory(domainDescriptorFactory, domainManager,
                                 artifactClassLoaderManager, serviceManager,
                                 pluginDependenciesResolver, domainClassLoaderBuilderFactory, extensionModelLoaderManager);

    DeployableArtifactClassLoaderFactory<PolicyTemplateDescriptor> policyClassLoaderFactory =
        trackDeployableArtifactClassLoaderFactory(new PolicyTemplateClassLoaderFactory());
    PolicyTemplateClassLoaderBuilderFactory policyTemplateClassLoaderBuilderFactory =
        new ApplicationPolicyTemplateClassLoaderBuilderFactory(policyClassLoaderFactory, pluginClassLoadersFactory);

    applicationFactory = new DefaultApplicationFactory(applicationClassLoaderBuilderFactory, applicationDescriptorFactory,
                                                       domainManager, serviceManager,
                                                       extensionModelLoaderManager,
                                                       artifactClassLoaderManager, policyTemplateClassLoaderBuilderFactory,
                                                       pluginDependenciesResolver,
                                                       artifactPluginDescriptorLoader,
                                                       licenseValidator);
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
  public ServiceManager getServiceManager() {
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
