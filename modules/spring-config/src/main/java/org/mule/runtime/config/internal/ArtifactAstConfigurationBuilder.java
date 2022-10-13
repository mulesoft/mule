/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.config.internal;

import static org.mule.runtime.api.i18n.I18nMessageFactory.createStaticMessage;
import static org.mule.runtime.ast.api.util.MuleAstUtils.emptyArtifact;
import static org.mule.runtime.config.internal.registry.AbstractSpringRegistry.SPRING_APPLICATION_CONTEXT;
import static org.mule.runtime.core.api.config.bootstrap.ArtifactType.DOMAIN;
import static org.mule.runtime.core.api.lifecycle.LifecycleUtils.initialiseIfNeeded;
import static org.mule.runtime.core.internal.config.RuntimeLockFactoryUtil.getRuntimeLockFactory;

import static java.util.Optional.empty;
import static java.util.Optional.of;
import static java.util.Optional.ofNullable;

import org.mule.runtime.api.component.ConfigurationProperties;
import org.mule.runtime.api.config.FeatureFlaggingService;
import org.mule.runtime.api.lifecycle.InitialisationException;
import org.mule.runtime.api.lifecycle.Startable;
import org.mule.runtime.api.lock.LockFactory;
import org.mule.runtime.api.memory.management.MemoryManagementService;
import org.mule.runtime.api.metadata.ExpressionLanguageMetadataService;
import org.mule.runtime.ast.api.ArtifactAst;
import org.mule.runtime.config.api.ArtifactContextFactory;
import org.mule.runtime.config.internal.artifact.SpringArtifactContext;
import org.mule.runtime.config.internal.context.BaseConfigurationComponentLocator;
import org.mule.runtime.config.internal.context.BaseMuleArtifactContext;
import org.mule.runtime.config.internal.context.MuleArtifactContext;
import org.mule.runtime.config.internal.context.lazy.LazyMuleArtifactContext;
import org.mule.runtime.config.internal.model.ComponentBuildingDefinitionRegistryFactory;
import org.mule.runtime.config.internal.model.ComponentModelInitializer;
import org.mule.runtime.config.internal.registry.BaseSpringRegistry;
import org.mule.runtime.config.internal.registry.CompositeOptionalObjectsController;
import org.mule.runtime.config.internal.registry.DefaultOptionalObjectsController;
import org.mule.runtime.config.internal.registry.OptionalObjectsController;
import org.mule.runtime.config.internal.registry.SpringRegistry;
import org.mule.runtime.config.internal.resolvers.ConfigurationDependencyResolver;
import org.mule.runtime.core.api.MuleContext;
import org.mule.runtime.core.api.config.ConfigurationException;
import org.mule.runtime.core.api.config.bootstrap.ArtifactType;
import org.mule.runtime.core.api.config.builders.AbstractConfigurationBuilder;
import org.mule.runtime.core.api.lifecycle.LifecycleManager;
import org.mule.runtime.core.internal.config.ParentMuleContextAwareConfigurationBuilder;
import org.mule.runtime.core.internal.context.DefaultMuleContext;
import org.mule.runtime.core.internal.context.MuleContextWithRegistry;
import org.mule.runtime.core.internal.context.NullDomainMuleContextLifecycleStrategy;
import org.mule.runtime.core.internal.exception.ContributedErrorTypeLocator;
import org.mule.runtime.core.internal.exception.ContributedErrorTypeRepository;
import org.mule.runtime.deployment.model.api.artifact.ArtifactContext;
import org.mule.runtime.internal.memory.management.ArtifactMemoryManagementService;
import org.mule.runtime.internal.memory.management.DefaultMemoryManagementService;

import java.util.Map;
import java.util.Optional;

import org.springframework.context.ApplicationContext;
import org.springframework.context.ConfigurableApplicationContext;

/**
 * Generates a Spring context for the artifact of the provided {@link ArtifactAst}, and sets it as the registry in the
 * {@link MuleContext}.
 * 
 * @since 4.5
 */
public class ArtifactAstConfigurationBuilder extends AbstractConfigurationBuilder
    implements ParentMuleContextAwareConfigurationBuilder, ArtifactContextFactory,
    ComponentBuildingDefinitionRegistryFactoryAware {

  private boolean enableLazyInit = false;

  private final ArtifactAst artifactAst;
  private final Map<String, String> artifactProperties;
  private ApplicationContext parentContext;
  private MuleArtifactContext muleArtifactContext;
  private final ArtifactType artifactType;
  private final LockFactory runtimeLockFactory;
  private final MemoryManagementService memoryManagementService;
  private Optional<ComponentBuildingDefinitionRegistryFactory> componentBuildingDefinitionRegistryFactory = empty();

  private ArtifactAstConfigurationBuilder(ArtifactAst artifactAst, Map<String, String> artifactProperties,
                                          ArtifactType artifactType, boolean enableLazyInit,
                                          LockFactory runtimeLockFactory, MemoryManagementService memoryManagementService)
      throws ConfigurationException {
    this.artifactAst = artifactAst;
    this.artifactProperties = artifactProperties;
    this.artifactType = artifactType;
    this.enableLazyInit = enableLazyInit;
    this.runtimeLockFactory = runtimeLockFactory;
    this.memoryManagementService = memoryManagementService;
  }

  public ArtifactAstConfigurationBuilder(ArtifactAst artifactAst, Map<String, String> artifactProperties,
                                         ArtifactType artifactType, boolean enableLazyInit)
      throws ConfigurationException {
    this(artifactAst, artifactProperties, artifactType, enableLazyInit,
         getRuntimeLockFactory(),
         DefaultMemoryManagementService.getInstance());
  }

  @Override
  protected void doConfigure(MuleContext muleContext) throws Exception {
    if (emptyArtifact().equals(artifactAst) && artifactType == DOMAIN) {
      ((DefaultMuleContext) muleContext).setLifecycleStrategy(new NullDomainMuleContextLifecycleStrategy());
      return;
    }

    initialiseIfNeeded(this, muleContext);
    OptionalObjectsController applicationObjectController = createApplicationObjectController();
    Optional<ConfigurationProperties> parentConfigurationProperties = resolveParentConfigurationProperties();

    final BaseMuleArtifactContext baseMuleArtifactContext =
        createBaseContext(muleContext, applicationObjectController, parentConfigurationProperties);
    serviceConfigurators.forEach(serviceConfigurator -> serviceConfigurator.configure(muleContext.getCustomizationService()));
    createBaseRegistry(muleContext, baseMuleArtifactContext);

    muleArtifactContext = createApplicationContext(muleContext,
                                                   applicationObjectController,
                                                   baseMuleArtifactContext.getBean(BaseConfigurationComponentLocator.class),
                                                   baseMuleArtifactContext.getBean(ContributedErrorTypeRepository.class),
                                                   baseMuleArtifactContext.getBean(ContributedErrorTypeLocator.class),
                                                   baseMuleArtifactContext.getBean(FeatureFlaggingService.class),
                                                   baseMuleArtifactContext.getBean(ExpressionLanguageMetadataService.class));
    muleArtifactContext.setParent(baseMuleArtifactContext);
    createSpringRegistry((DefaultMuleContext) muleContext, baseMuleArtifactContext, muleArtifactContext);
  }

  private BaseMuleArtifactContext createBaseContext(MuleContext muleContext,
                                                    OptionalObjectsController applicationObjectController,
                                                    Optional<ConfigurationProperties> parentConfigurationProperties) {
    final BaseMuleArtifactContext baseMuleArtifactContext = new BaseMuleArtifactContext(muleContext,
                                                                                        applicationObjectController,
                                                                                        parentConfigurationProperties,
                                                                                        getArtifactProperties(),
                                                                                        artifactType,
                                                                                        enableLazyInit);
    if (baseMuleArtifactContext instanceof ConfigurableApplicationContext) {
      ((ConfigurableApplicationContext) baseMuleArtifactContext).setParent(parentContext);
    }
    return baseMuleArtifactContext;
  }

  private BaseSpringRegistry createBaseRegistry(MuleContext muleContext, final BaseMuleArtifactContext baseMuleArtifactContext)
      throws InitialisationException {
    BaseSpringRegistry baseRegistry = new BaseSpringRegistry(baseMuleArtifactContext, muleContext,
                                                             ((DefaultMuleContext) muleContext).getLifecycleInterceptor());

    ((DefaultMuleContext) muleContext).setRegistry(baseRegistry);
    ((DefaultMuleContext) muleContext).setInjector(baseRegistry);
    baseRegistry.initialise();

    return baseRegistry;
  }

  private MuleArtifactContext createApplicationContext(MuleContext muleContext,
                                                       OptionalObjectsController optionalObjectsController,
                                                       BaseConfigurationComponentLocator baseConfigurationComponentLocator,
                                                       ContributedErrorTypeRepository errorTypeRepository,
                                                       ContributedErrorTypeLocator errorTypeLocator,
                                                       FeatureFlaggingService featureFlaggingService,
                                                       ExpressionLanguageMetadataService expressionLanguageMetadataService)
      throws Exception {
    // TODO MULE-10084 : Refactor to only accept artifactConfiguration and not artifactConfigResources
    return doCreateApplicationContext(muleContext, optionalObjectsController,
                                      baseConfigurationComponentLocator, errorTypeRepository, errorTypeLocator,
                                      featureFlaggingService, expressionLanguageMetadataService);
  }

  protected OptionalObjectsController createApplicationObjectController() {
    OptionalObjectsController applicationObjectcontroller = new DefaultOptionalObjectsController();

    // >> TODO W-10736276 Remove this
    OptionalObjectsController parentObjectController = null;

    if (parentContext instanceof MuleArtifactContext) {
      parentObjectController = ((MuleArtifactContext) parentContext).getOptionalObjectsController();
    }

    if (parentObjectController != null) {
      applicationObjectcontroller = new CompositeOptionalObjectsController(applicationObjectcontroller, parentObjectController);
    }
    // << TODO W-10736276 Remove this

    return applicationObjectcontroller;
  }

  private MuleArtifactContext doCreateApplicationContext(MuleContext muleContext,
                                                         OptionalObjectsController optionalObjectsController,
                                                         BaseConfigurationComponentLocator baseConfigurationComponentLocator,
                                                         ContributedErrorTypeRepository errorTypeRepository,
                                                         ContributedErrorTypeLocator errorTypeLocator,
                                                         FeatureFlaggingService featureFlaggingService,
                                                         ExpressionLanguageMetadataService expressionLanguageMetadataService) {
    ComponentBuildingDefinitionRegistryFactory resolvedComponentBuildingDefinitionRegistryFactory =
        this.componentBuildingDefinitionRegistryFactory
            .orElse(new DefaultComponentBuildingDefinitionRegistryFactory());

    if (enableLazyInit) {
      return new LazyMuleArtifactContext(muleContext, artifactAst,
                                         optionalObjectsController,
                                         resolveParentConfigurationProperties(),
                                         baseConfigurationComponentLocator,
                                         errorTypeRepository, errorTypeLocator,
                                         getArtifactProperties(), artifactType,
                                         resolveComponentModelInitializer(),
                                         runtimeLockFactory,
                                         resolvedComponentBuildingDefinitionRegistryFactory,
                                         new ArtifactMemoryManagementService(memoryManagementService),
                                         featureFlaggingService, expressionLanguageMetadataService);
    } else {
      MuleArtifactContext context;
      context = new MuleArtifactContext(muleContext, artifactAst,
                                        optionalObjectsController,
                                        resolveParentConfigurationProperties(),
                                        baseConfigurationComponentLocator,
                                        errorTypeRepository, errorTypeLocator,
                                        getArtifactProperties(), artifactType,
                                        resolvedComponentBuildingDefinitionRegistryFactory,
                                        new ArtifactMemoryManagementService(memoryManagementService),
                                        featureFlaggingService, expressionLanguageMetadataService);
      context.initialize();
      return context;
    }
  }

  private Optional<ConfigurationProperties> resolveParentConfigurationProperties() {
    Optional<ConfigurationProperties> parentConfigurationProperties = empty();
    if (parentContext != null) {
      parentConfigurationProperties = of(parentContext.getBean(ConfigurationProperties.class));
    }

    return parentConfigurationProperties;
  }

  private Optional<ComponentModelInitializer> resolveComponentModelInitializer() {
    Optional<ComponentModelInitializer> parentLazyComponentInitializer = empty();
    if (parentContext instanceof ComponentModelInitializer) {
      parentLazyComponentInitializer = of((ComponentModelInitializer) parentContext);
    }

    return parentLazyComponentInitializer;
  }

  private void createSpringRegistry(DefaultMuleContext muleContext, ApplicationContext baseApplicationContext,
                                    MuleArtifactContext applicationContext)
      throws Exception {
    SpringRegistry registry;

    if (parentContext != null) {
      registry = createRegistryWithParentContext(muleContext, baseApplicationContext, applicationContext);
    } else {
      registry = new SpringRegistry(baseApplicationContext, applicationContext, muleContext,
                                    new ConfigurationDependencyResolver(applicationContext.getApplicationModel()),
                                    muleContext.getLifecycleInterceptor());
    }

    muleContext.setRegistry(registry);
    muleContext.setInjector(registry);
  }

  private SpringRegistry createRegistryWithParentContext(MuleContext muleContext,
                                                         ApplicationContext baseApplicationContext,
                                                         MuleArtifactContext applicationContext)
      throws ConfigurationException {
    if (baseApplicationContext instanceof ConfigurableApplicationContext) {
      return new SpringRegistry(baseApplicationContext, applicationContext, muleContext,
                                new ConfigurationDependencyResolver(applicationContext.getApplicationModel()),
                                ((DefaultMuleContext) muleContext).getLifecycleInterceptor());
    } else {
      throw new ConfigurationException(createStaticMessage("Cannot set a parent context if the ApplicationContext does not implement ConfigurableApplicationContext"));
    }
  }

  @Override
  protected synchronized void applyLifecycle(LifecycleManager lifecycleManager) throws Exception {
    // If the MuleContext is started, start all objects in the new Registry.
    if (lifecycleManager.isPhaseComplete(Startable.PHASE_NAME)) {
      lifecycleManager.fireLifecycle(Startable.PHASE_NAME);
    }
  }

  @Override
  public ArtifactContext createArtifactContext() {
    return new SpringArtifactContext(muleArtifactContext);
  }

  @Override
  public void setParentContext(MuleContext domainContext, ArtifactAst parentAst) {
    this.parentContext = ((MuleContextWithRegistry) domainContext).getRegistry().get(SPRING_APPLICATION_CONTEXT);
  }

  @Override
  public void setComponentBuildingDefinitionRegistryFactory(ComponentBuildingDefinitionRegistryFactory componentBuildingDefinitionRegistryFactory) {
    this.componentBuildingDefinitionRegistryFactory = ofNullable(componentBuildingDefinitionRegistryFactory);
  }

  public Map<String, String> getArtifactProperties() {
    return artifactProperties;
  }
}
