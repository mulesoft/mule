/*
 * Copyright 2023 Salesforce, Inc. All rights reserved.
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

import static java.util.Optional.empty;
import static java.util.Optional.of;

import org.mule.runtime.api.component.ConfigurationProperties;
import org.mule.runtime.api.config.FeatureFlaggingService;
import org.mule.runtime.api.lifecycle.InitialisationException;
import org.mule.runtime.api.lifecycle.Startable;
import org.mule.runtime.api.memory.management.MemoryManagementService;
import org.mule.runtime.api.metadata.ExpressionLanguageMetadataService;
import org.mule.runtime.ast.api.ArtifactAst;
import org.mule.runtime.config.api.ArtifactContextFactory;
import org.mule.runtime.config.api.dsl.model.ComponentBuildingDefinitionRegistry;
import org.mule.runtime.config.internal.artifact.SpringArtifactContext;
import org.mule.runtime.config.internal.context.BaseConfigurationComponentLocator;
import org.mule.runtime.config.internal.context.BaseMuleArtifactContext;
import org.mule.runtime.config.internal.context.MuleArtifactContext;
import org.mule.runtime.config.internal.context.lazy.LazyMuleArtifactContext;
import org.mule.runtime.config.internal.model.ComponentModelInitializer;
import org.mule.runtime.config.internal.model.DefaultComponentBuildingDefinitionRegistryFactory;
import org.mule.runtime.config.internal.registry.BaseSpringRegistry;
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
    implements ParentMuleContextAwareConfigurationBuilder, ArtifactContextFactory {

  private final boolean enableLazyInit;

  private final ArtifactAst artifactAst;
  private final Map<String, String> artifactProperties;
  private ApplicationContext parentContext;
  private MuleArtifactContext muleArtifactContext;
  private final ArtifactType artifactType;
  private final MemoryManagementService memoryManagementService;
  private final ComponentBuildingDefinitionRegistry componentBuildingDefinitionRegistry;

  private ArtifactAstConfigurationBuilder(ArtifactAst artifactAst, Map<String, String> artifactProperties,
                                          ArtifactType artifactType, boolean enableLazyInit,
                                          MemoryManagementService memoryManagementService,
                                          ComponentBuildingDefinitionRegistry componentBuildingDefinitionRegistry)
      throws ConfigurationException {
    this.artifactAst = artifactAst;
    this.artifactProperties = artifactProperties;
    this.artifactType = artifactType;
    this.enableLazyInit = enableLazyInit;
    this.memoryManagementService = memoryManagementService;
    this.componentBuildingDefinitionRegistry = componentBuildingDefinitionRegistry;
  }

  public ArtifactAstConfigurationBuilder(ArtifactAst artifactAst, Map<String, String> artifactProperties,
                                         ArtifactType artifactType, boolean enableLazyInit,
                                         ComponentBuildingDefinitionRegistry componentBuildingDefinitionRegistry)
      throws ConfigurationException {
    this(artifactAst, artifactProperties, artifactType, enableLazyInit,
         DefaultMemoryManagementService.getInstance(),
         componentBuildingDefinitionRegistry);
  }

  public ArtifactAstConfigurationBuilder(ArtifactAst artifactAst, Map<String, String> artifactProperties)
      throws ConfigurationException {
    this(artifactAst, artifactProperties, ArtifactType.APP, false,
         new DefaultComponentBuildingDefinitionRegistryFactory()
             .create(artifactAst.dependencies(),
                     artifactAst::dependenciesDsl));
  }

  @Override
  protected void doConfigure(MuleContext muleContext) throws Exception {
    if (emptyArtifact().equals(artifactAst) && artifactType == DOMAIN) {
      ((DefaultMuleContext) muleContext).setLifecycleStrategy(new NullDomainMuleContextLifecycleStrategy());
      return;
    }

    initialiseIfNeeded(this, muleContext);
    Optional<ConfigurationProperties> parentConfigurationProperties = resolveParentConfigurationProperties();

    final BaseMuleArtifactContext baseMuleArtifactContext = createBaseContext(muleContext, parentConfigurationProperties);
    serviceConfigurators.forEach(serviceConfigurator -> serviceConfigurator.configure(muleContext.getCustomizationService()));
    BaseSpringRegistry baseRegistry = createBaseRegistry(muleContext, baseMuleArtifactContext);

    try {
      muleArtifactContext = createApplicationContext(muleContext,
                                                     baseMuleArtifactContext.getBean(BaseConfigurationComponentLocator.class),
                                                     baseMuleArtifactContext.getBean(ContributedErrorTypeRepository.class),
                                                     baseMuleArtifactContext.getBean(ContributedErrorTypeLocator.class),
                                                     baseMuleArtifactContext.getBean(FeatureFlaggingService.class),
                                                     baseMuleArtifactContext.getBean(ExpressionLanguageMetadataService.class));
      muleArtifactContext.setParent(baseMuleArtifactContext);
      createSpringRegistry((DefaultMuleContext) muleContext, baseMuleArtifactContext, muleArtifactContext);
    } catch (Exception e) {
      baseRegistry.dispose();
      throw e;
    }
  }

  private BaseMuleArtifactContext createBaseContext(MuleContext muleContext,
                                                    Optional<ConfigurationProperties> parentConfigurationProperties) {
    final BaseMuleArtifactContext baseMuleArtifactContext = new BaseMuleArtifactContext(muleContext,
                                                                                        parentConfigurationProperties,
                                                                                        getArtifactProperties(),
                                                                                        artifactType,
                                                                                        enableLazyInit);
    if (baseMuleArtifactContext instanceof ConfigurableApplicationContext) {
      baseMuleArtifactContext.setParent(parentContext);
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
                                                       BaseConfigurationComponentLocator baseConfigurationComponentLocator,
                                                       ContributedErrorTypeRepository errorTypeRepository,
                                                       ContributedErrorTypeLocator errorTypeLocator,
                                                       FeatureFlaggingService featureFlaggingService,
                                                       ExpressionLanguageMetadataService expressionLanguageMetadataService)
      throws Exception {
    // TODO MULE-10084 : Refactor to only accept artifactConfiguration and not artifactConfigResources
    return doCreateApplicationContext(muleContext,
                                      baseConfigurationComponentLocator, errorTypeRepository, errorTypeLocator,
                                      featureFlaggingService, expressionLanguageMetadataService);
  }

  private MuleArtifactContext doCreateApplicationContext(MuleContext muleContext,
                                                         BaseConfigurationComponentLocator baseConfigurationComponentLocator,
                                                         ContributedErrorTypeRepository errorTypeRepository,
                                                         ContributedErrorTypeLocator errorTypeLocator,
                                                         FeatureFlaggingService featureFlaggingService,
                                                         ExpressionLanguageMetadataService expressionLanguageMetadataService) {
    if (enableLazyInit) {
      return new LazyMuleArtifactContext(muleContext, artifactAst,
                                         resolveParentConfigurationProperties(),
                                         baseConfigurationComponentLocator,
                                         errorTypeRepository, errorTypeLocator,
                                         getArtifactProperties(), artifactType,
                                         resolveComponentModelInitializer(),
                                         componentBuildingDefinitionRegistry,
                                         new ArtifactMemoryManagementService(memoryManagementService),
                                         featureFlaggingService, expressionLanguageMetadataService);
    } else {
      MuleArtifactContext context;
      context = new MuleArtifactContext(muleContext, artifactAst,
                                        resolveParentConfigurationProperties(),
                                        baseConfigurationComponentLocator,
                                        errorTypeRepository, errorTypeLocator,
                                        getArtifactProperties(), artifactType,
                                        componentBuildingDefinitionRegistry,
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

  public Map<String, String> getArtifactProperties() {
    return artifactProperties;
  }
}
