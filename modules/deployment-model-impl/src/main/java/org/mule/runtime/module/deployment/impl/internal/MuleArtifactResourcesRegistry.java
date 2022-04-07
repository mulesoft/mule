/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.module.deployment.impl.internal;

import static org.mule.runtime.api.util.Preconditions.checkArgument;
import static org.mule.runtime.container.api.ContainerClassLoaderProvider.createContainerClassLoader;
import static org.mule.runtime.container.api.MuleFoldersUtil.getAppDataFolder;
import static org.mule.runtime.core.api.config.FeatureFlaggingRegistry.getInstance;
import static org.mule.runtime.core.api.config.MuleProperties.MULE_CONTAINER_FEATURE_MANAGEMENT_SERVICE;
import static org.mule.runtime.core.api.config.MuleProperties.MULE_MEMORY_MANAGEMENT_SERVICE;
import static org.mule.runtime.core.api.config.MuleProperties.SERVER_NOTIFICATION_MANAGER;
import static org.mule.runtime.core.internal.profiling.AbstractProfilingService.configureEnableProfilingService;
import static org.mule.runtime.deployment.model.api.artifact.ArtifactDescriptorFactoryProvider.artifactDescriptorFactoryProvider;
import static org.mule.runtime.deployment.model.api.builder.DeployableArtifactClassLoaderFactoryProvider.domainClassLoaderFactory;
import static org.mule.runtime.module.license.api.LicenseValidatorProvider.discoverLicenseValidator;

import static java.lang.Thread.currentThread;

import org.mule.runtime.api.config.FeatureFlaggingService;
import org.mule.runtime.api.deployment.meta.MuleApplicationModel;
import org.mule.runtime.api.deployment.meta.MulePluginModel;
import org.mule.runtime.api.exception.MuleRuntimeException;
import org.mule.runtime.api.memory.management.MemoryManagementService;
import org.mule.runtime.api.meta.MuleVersion;
import org.mule.runtime.api.profiling.ProfilingService;
import org.mule.runtime.api.scheduler.SchedulerService;
import org.mule.runtime.api.util.LazyValue;
import org.mule.runtime.artifact.activation.internal.TrackingArtifactClassLoaderResolverDecorator;
import org.mule.runtime.container.api.ModuleRepository;
import org.mule.runtime.container.internal.ContainerModuleDiscoverer;
import org.mule.runtime.container.internal.DefaultModuleRepository;
import org.mule.runtime.core.api.config.FeatureContext;
import org.mule.runtime.core.api.config.FeatureFlaggingRegistry;
import org.mule.runtime.core.api.context.notification.ServerNotificationManager;
import org.mule.runtime.core.api.registry.SpiServiceRegistry;
import org.mule.runtime.core.internal.config.FeatureFlaggingServiceBuilder;
import org.mule.runtime.core.internal.lock.ServerLockFactory;
import org.mule.runtime.core.internal.profiling.DefaultProfilingService;
import org.mule.runtime.core.internal.registry.SimpleRegistry;
import org.mule.runtime.core.privileged.registry.RegistrationException;
import org.mule.runtime.deployment.model.api.application.Application;
import org.mule.runtime.deployment.model.api.artifact.ArtifactConfigurationProcessor;
import org.mule.runtime.deployment.model.api.artifact.DescriptorLoaderRepositoryFactory;
import org.mule.runtime.deployment.model.api.builder.ApplicationClassLoaderBuilderFactory;
import org.mule.runtime.deployment.model.api.builder.DomainClassLoaderBuilderFactory;
import org.mule.runtime.deployment.model.api.builder.RegionPluginClassLoadersFactory;
import org.mule.runtime.deployment.model.api.plugin.resolver.PluginDependenciesResolver;
import org.mule.runtime.deployment.model.api.policy.PolicyTemplateDescriptor;
import org.mule.runtime.deployment.model.internal.DefaultRegionPluginClassLoadersFactory;
import org.mule.runtime.deployment.model.internal.artifact.ServiceRegistryDescriptorLoaderRepository;
import org.mule.runtime.deployment.model.internal.artifact.extension.ExtensionModelLoaderManager;
import org.mule.runtime.deployment.model.internal.artifact.extension.MuleExtensionModelLoaderManager;
import org.mule.runtime.deployment.model.internal.policy.PolicyTemplateClassLoaderFactory;
import org.mule.runtime.internal.memory.management.ArtifactMemoryManagementService;
import org.mule.runtime.internal.memory.management.DefaultMemoryManagementService;
import org.mule.runtime.internal.memory.management.ProfiledMemoryManagementService;
import org.mule.runtime.module.artifact.activation.api.classloader.ArtifactClassLoaderResolver;
import org.mule.runtime.module.artifact.activation.internal.classloader.DefaultArtifactClassLoaderResolver;
import org.mule.runtime.module.artifact.activation.internal.nativelib.DefaultNativeLibraryFinderFactory;
import org.mule.runtime.module.artifact.api.classloader.ArtifactClassLoader;
import org.mule.runtime.module.artifact.api.classloader.ArtifactClassLoaderFactory;
import org.mule.runtime.module.artifact.api.classloader.DeployableArtifactClassLoaderFactory;
import org.mule.runtime.module.artifact.api.classloader.TrackingArtifactClassLoaderFactory;
import org.mule.runtime.module.artifact.api.classloader.TrackingDeployableArtifactClassLoaderFactory;
import org.mule.runtime.module.artifact.api.descriptor.AbstractArtifactDescriptorFactory;
import org.mule.runtime.module.artifact.api.descriptor.ApplicationDescriptor;
import org.mule.runtime.module.artifact.api.descriptor.ArtifactDescriptor;
import org.mule.runtime.module.artifact.api.descriptor.ArtifactDescriptorValidatorBuilder;
import org.mule.runtime.module.artifact.api.descriptor.ArtifactPluginDescriptor;
import org.mule.runtime.module.artifact.api.descriptor.DomainDescriptor;
import org.mule.runtime.module.deployment.impl.internal.application.ApplicationDescriptorFactory;
import org.mule.runtime.module.deployment.impl.internal.application.DefaultApplicationFactory;
import org.mule.runtime.module.deployment.impl.internal.artifact.AbstractDeployableDescriptorFactory;
import org.mule.runtime.module.deployment.impl.internal.artifact.DefaultArtifactDescriptorFactoryProvider;
import org.mule.runtime.module.deployment.impl.internal.artifact.DefaultClassLoaderManager;
import org.mule.runtime.module.deployment.impl.internal.domain.DefaultDomainFactory;
import org.mule.runtime.module.deployment.impl.internal.domain.DefaultDomainManager;
import org.mule.runtime.module.deployment.impl.internal.domain.DomainDescriptorFactory;
import org.mule.runtime.module.deployment.impl.internal.domain.DomainRepository;
import org.mule.runtime.module.deployment.impl.internal.plugin.ArtifactPluginDescriptorLoader;
import org.mule.runtime.module.deployment.impl.internal.policy.ApplicationPolicyTemplateClassLoaderBuilderFactory;
import org.mule.runtime.module.deployment.impl.internal.policy.PolicyTemplateClassLoaderBuilderFactory;
import org.mule.runtime.module.license.api.LicenseValidator;
import org.mule.runtime.module.service.api.manager.ServiceManager;
import org.mule.runtime.module.service.internal.artifact.ServiceClassLoaderFactory;
import org.mule.runtime.module.service.internal.artifact.ServiceDescriptor;
import org.mule.runtime.module.service.internal.discoverer.DefaultServiceDiscoverer;
import org.mule.runtime.module.service.internal.discoverer.FileSystemServiceProviderDiscoverer;
import org.mule.runtime.module.service.internal.discoverer.ReflectionServiceResolver;
import org.mule.runtime.module.service.internal.manager.ServiceRegistry;

/**
 * Registry of mule artifact resources required to construct new artifacts.
 *
 * @since 4.0
 */
public class MuleArtifactResourcesRegistry extends SimpleRegistry {

  private static final String CONTAINER_FEATURE_CONTEXT_NAME = "Container";
  private final ArtifactPluginDescriptorLoader artifactPluginDescriptorLoader;
  private final DefaultDomainManager domainManager;
  private final DefaultDomainFactory domainFactory;
  private final DefaultApplicationFactory applicationFactory;
  private final DeployableArtifactClassLoaderFactory<DomainDescriptor> domainClassLoaderFactory;
  private final ArtifactClassLoader containerClassLoader;
  private final ServiceManager serviceManager;
  private final ExtensionModelLoaderManager extensionModelLoaderManager;
  private final DefaultClassLoaderManager artifactClassLoaderManager;
  private final ApplicationClassLoaderBuilderFactory applicationClassLoaderBuilderFactory;
  private final DomainClassLoaderBuilderFactory domainClassLoaderBuilderFactory;
  private final DomainDescriptorFactory domainDescriptorFactory;
  private final ApplicationDescriptorFactory applicationDescriptorFactory;
  private final PluginDependenciesResolver pluginDependenciesResolver;
  private final ServiceRegistryDescriptorLoaderRepository descriptorLoaderRepository;
  private final RegionPluginClassLoadersFactory pluginClassLoadersFactory;
  private final ArtifactConfigurationProcessor artifactConfigurationProcessor;
  private final AbstractDeployableDescriptorFactory<MuleApplicationModel, ApplicationDescriptor> toolingApplicationDescriptorFactory;
  private final ServerLockFactory runtimeLockFactory;
  private final ProfiledMemoryManagementService memoryManagementService;
  private final ProfilingService containerProfilingService;
  private final ServerNotificationManager serverNotificationManager;


  /**
   * Builds a {@link MuleArtifactResourcesRegistry} instance
   */
  public static class Builder {

    private ModuleRepository moduleRepository;
    private ArtifactConfigurationProcessor artifactConfigurationProcessor;

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
     * Configures the {@link ArtifactConfigurationProcessor} to use.
     *
     * @param artifactConfigurationProcessor the processor to use for building the application model.
     * @return the same builder instance
     *
     * @since 4.5
     */
    public Builder artifactConfigurationProcessor(ArtifactConfigurationProcessor artifactConfigurationProcessor) {
      checkArgument(artifactConfigurationProcessor != null, "artifactConfigurationProcessor cannot be null");

      this.artifactConfigurationProcessor = artifactConfigurationProcessor;
      return this;
    }

    /**
     * Builds the desired instance
     *
     * @return a new {@link MuleArtifactResourcesRegistry} with the provided configuration.
     */
    public MuleArtifactResourcesRegistry build() {
      if (moduleRepository == null) {
        moduleRepository = new DefaultModuleRepository(new ContainerModuleDiscoverer(getClass().getClassLoader()));
      }

      try {
        return new MuleArtifactResourcesRegistry(moduleRepository, artifactConfigurationProcessor);
      } catch (RegistrationException e) {
        throw new MuleRuntimeException(e);
      }
    }
  }

  /**
   * Creates a repository for resources required for mule artifacts.
   */
  private MuleArtifactResourcesRegistry(ModuleRepository moduleRepository,
                                        ArtifactConfigurationProcessor artifactConfigurationProcessor)
      throws RegistrationException {
    // Creates a registry to be used as an injector.
    super(null, null);

    // A "container level" notification server is created and registered in order to be injectable
    serverNotificationManager = ServerNotificationManager
        .createDefaultNotificationManager(new LazyValue<>(this::getSchedulerService),
                                          new LazyValue<>(() -> "containerServerNotificationManager"));
    registerObject(SERVER_NOTIFICATION_MANAGER, serverNotificationManager);

    this.memoryManagementService = DefaultMemoryManagementService.getInstance();
    this.containerProfilingService = new DefaultProfilingService();

    memoryManagementService.setProfilingService(containerProfilingService);

    MemoryManagementService artifactMemoryManagementService = new ArtifactMemoryManagementService(memoryManagementService);

    // Registers the memory management so that this can be injected.
    registerObject(MULE_MEMORY_MANAGEMENT_SERVICE, artifactMemoryManagementService);
    registerObject(MULE_CONTAINER_FEATURE_MANAGEMENT_SERVICE, getContainerFeatureFlaggingService());

    runtimeLockFactory = new ServerLockFactory();

    containerClassLoader = createContainerClassLoader(moduleRepository, getClass().getClassLoader());
    artifactClassLoaderManager = new DefaultClassLoaderManager();
    LicenseValidator licenseValidator = discoverLicenseValidator(currentThread().getContextClassLoader());

    domainManager = new DefaultDomainManager();
    this.domainClassLoaderFactory =
        trackDeployableArtifactClassLoaderFactory(domainClassLoaderFactory(name -> getAppDataFolder(name)));

    final AbstractArtifactDescriptorFactory<MulePluginModel, ArtifactPluginDescriptor> artifactPluginDescriptorFactory =
        artifactDescriptorFactoryProvider()
            .createArtifactPluginDescriptorFactory(new DescriptorLoaderRepositoryFactory().createDescriptorLoaderRepository(),
                                                   ArtifactDescriptorValidatorBuilder.builder());
    artifactPluginDescriptorLoader = new ArtifactPluginDescriptorLoader(artifactPluginDescriptorFactory);
    descriptorLoaderRepository = new ServiceRegistryDescriptorLoaderRepository(new SpiServiceRegistry());

    ArtifactDescriptorValidatorBuilder artifactDescriptorValidatorBuilder = ArtifactDescriptorValidatorBuilder.builder();

    domainDescriptorFactory = new DomainDescriptorFactory(artifactPluginDescriptorLoader, descriptorLoaderRepository,
                                                          artifactDescriptorValidatorBuilder);
    applicationDescriptorFactory = new ApplicationDescriptorFactory(artifactPluginDescriptorLoader, descriptorLoaderRepository,
                                                                    artifactDescriptorValidatorBuilder);

    ArtifactClassLoaderResolver artifactClassLoaderResolver =
        new TrackingArtifactClassLoaderResolverDecorator(artifactClassLoaderManager,
                                                         new DefaultArtifactClassLoaderResolver(moduleRepository,
                                                                                                new DefaultNativeLibraryFinderFactory(name -> getAppDataFolder(name))));

    pluginClassLoadersFactory = new DefaultRegionPluginClassLoadersFactory(artifactClassLoaderResolver);
    applicationClassLoaderBuilderFactory = new ApplicationClassLoaderBuilderFactory(artifactClassLoaderResolver);
    domainClassLoaderBuilderFactory = new DomainClassLoaderBuilderFactory(artifactClassLoaderResolver);

    this.artifactConfigurationProcessor = artifactConfigurationProcessor;

    ArtifactClassLoaderFactory<ServiceDescriptor> serviceClassLoaderFactory = new ServiceClassLoaderFactory();
    serviceManager =
        ServiceManager.create(new DefaultServiceDiscoverer(new FileSystemServiceProviderDiscoverer(containerClassLoader,
                                                                                                   trackArtifactClassLoaderFactory(serviceClassLoaderFactory),
                                                                                                   descriptorLoaderRepository,
                                                                                                   artifactDescriptorValidatorBuilder),
                                                           new ReflectionServiceResolver(new ServiceRegistry(), this)));
    extensionModelLoaderManager = new MuleExtensionModelLoaderManager(containerClassLoader);

    pluginDependenciesResolver =
        new DefaultArtifactDescriptorFactoryProvider().createBundlePluginDependenciesResolver(artifactPluginDescriptorFactory);
    domainFactory = new DefaultDomainFactory(domainDescriptorFactory, domainManager,
                                             artifactClassLoaderManager, serviceManager,
                                             pluginDependenciesResolver, domainClassLoaderBuilderFactory,
                                             extensionModelLoaderManager, licenseValidator,
                                             runtimeLockFactory,
                                             memoryManagementService,
                                             artifactConfigurationProcessor);

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
                                                       licenseValidator,
                                                       runtimeLockFactory,
                                                       memoryManagementService,
                                                       artifactConfigurationProcessor);
    toolingApplicationDescriptorFactory =
        new ApplicationDescriptorFactory(artifactPluginDescriptorLoader, descriptorLoaderRepository,
                                         artifactDescriptorValidatorBuilder);
  }

  private FeatureFlaggingService getContainerFeatureFlaggingService() {
    FeatureFlaggingRegistry ffRegistry = getInstance();

    configureEnableProfilingService();

    return new FeatureFlaggingServiceBuilder()
        .withContext(new FeatureContext(new MuleVersion("4.5.0-SNAPSHOT"), CONTAINER_FEATURE_CONTEXT_NAME))
        .withMuleContextFlags(ffRegistry.getFeatureConfigurations())
        .withFeatureContextFlags(ffRegistry.getFeatureFlagConfigurations())
        .build();
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
   * @return the domain repository where domains are registered once deployed.
   */
  public DomainRepository getDomainRepository() {
    return domainManager;
  }

  /**
   * @return the domain descriptor factory used to created domains.
   */
  public DomainDescriptorFactory getDomainDescriptorFactory() {
    return domainDescriptorFactory;
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

  /**
   * @return the {@link AbstractDeployableDescriptorFactory}.
   */
  public AbstractDeployableDescriptorFactory<MuleApplicationModel, ApplicationDescriptor> getToolingApplicationDescriptorFactory() {
    return toolingApplicationDescriptorFactory;
  }

  /**
   * @return resolver for dependencies for plugins
   */
  public PluginDependenciesResolver getPluginDependenciesResolver() {
    return pluginDependenciesResolver;
  }

  /**
   * @return the processor to use for building the application model
   */
  public ArtifactConfigurationProcessor getArtifactConfigurationProcessor() {
    return artifactConfigurationProcessor;
  }

  /**
   * @return a {@link ServerLockFactory} that can be shared between deployable artifacts.
   */
  public ServerLockFactory getRuntimeLockFactory() {
    return runtimeLockFactory;
  }

  /**
   * @return a {@link MemoryManagementService} that can be shared between deployable artifacts.
   */
  public MemoryManagementService getMemoryManagementService() {
    return memoryManagementService;
  }

  /**
   * @return a {@link ProfilingService} that can be shared between deployable artifacts.
   */
  public ProfilingService getContainerProfilingService() {
    return containerProfilingService;
  }

  private SchedulerService getSchedulerService() {
    return (SchedulerService) serviceManager.getServices().stream().filter(s -> s instanceof SchedulerService).findFirst().get();
  }

}
