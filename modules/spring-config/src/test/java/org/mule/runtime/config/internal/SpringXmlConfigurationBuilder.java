/*
 * Copyright 2023 Salesforce, Inc. All rights reserved.
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.config.internal;

import static org.mule.runtime.api.config.MuleRuntimeFeature.ENTITY_RESOLVER_FAIL_ON_FIRST_ERROR;
import static org.mule.runtime.api.i18n.I18nMessageFactory.createStaticMessage;
import static org.mule.runtime.ast.api.util.MuleAstUtils.emptyArtifact;
import static org.mule.runtime.config.api.dsl.ArtifactDeclarationUtils.toArtifactast;
import static org.mule.runtime.config.internal.ApplicationFilteredFromPolicyArtifactAst.applicationFilteredFromPolicyArtifactAst;
import static org.mule.runtime.config.internal.registry.AbstractSpringRegistry.SPRING_APPLICATION_CONTEXT;
import static org.mule.runtime.core.api.config.bootstrap.ArtifactType.APP;
import static org.mule.runtime.core.api.config.bootstrap.ArtifactType.DOMAIN;
import static org.mule.runtime.core.api.config.bootstrap.ArtifactType.POLICY;
import static org.mule.runtime.core.api.lifecycle.LifecycleUtils.initialiseIfNeeded;

import static java.util.Collections.emptyMap;
import static java.util.Collections.emptySet;
import static java.util.Optional.empty;
import static java.util.Optional.of;

import static org.apache.commons.lang3.ArrayUtils.isEmpty;

import org.mule.runtime.api.component.ConfigurationProperties;
import org.mule.runtime.api.config.FeatureFlaggingService;
import org.mule.runtime.api.exception.MuleRuntimeException;
import org.mule.runtime.api.lifecycle.InitialisationException;
import org.mule.runtime.api.lifecycle.Startable;
import org.mule.runtime.api.memory.management.MemoryManagementService;
import org.mule.runtime.api.meta.model.ExtensionModel;
import org.mule.runtime.api.metadata.ExpressionLanguageMetadataService;
import org.mule.runtime.app.declaration.api.ArtifactDeclaration;
import org.mule.runtime.ast.api.ArtifactAst;
import org.mule.runtime.ast.api.xml.AstXmlParser;
import org.mule.runtime.ast.api.xml.AstXmlParser.Builder;
import org.mule.runtime.config.api.ArtifactContextFactory;
import org.mule.runtime.config.api.dsl.model.ComponentBuildingDefinitionRegistry;
import org.mule.runtime.config.api.properties.ConfigurationPropertiesHierarchyBuilder;
import org.mule.runtime.config.api.properties.ConfigurationPropertiesResolver;
import org.mule.runtime.config.internal.artifact.SpringArtifactContext;
import org.mule.runtime.config.internal.context.BaseConfigurationComponentLocator;
import org.mule.runtime.config.internal.context.BaseMuleArtifactContext;
import org.mule.runtime.config.internal.context.MuleArtifactContext;
import org.mule.runtime.config.internal.context.lazy.LazyMuleArtifactContext;
import org.mule.runtime.config.internal.model.ComponentBuildingDefinitionRegistryFactory;
import org.mule.runtime.config.internal.model.ComponentModelInitializer;
import org.mule.runtime.config.internal.model.DefaultComponentBuildingDefinitionRegistryFactory;
import org.mule.runtime.config.internal.registry.BaseSpringRegistry;
import org.mule.runtime.config.internal.registry.SpringRegistry;
import org.mule.runtime.config.internal.resolvers.ConfigurationDependencyResolver;
import org.mule.runtime.core.api.MuleContext;
import org.mule.runtime.core.api.config.ConfigurationException;
import org.mule.runtime.core.api.config.bootstrap.ArtifactType;
import org.mule.runtime.core.api.config.builders.AbstractResourceConfigurationBuilder;
import org.mule.runtime.core.api.extension.ExtensionManager;
import org.mule.runtime.core.api.lifecycle.LifecycleManager;
import org.mule.runtime.core.internal.config.ParentMuleContextAwareConfigurationBuilder;
import org.mule.runtime.core.internal.context.DefaultMuleContext;
import org.mule.runtime.core.internal.context.MuleContextWithRegistry;
import org.mule.runtime.core.internal.context.NullDomainMuleContextLifecycleStrategy;
import org.mule.runtime.core.internal.exception.ContributedErrorTypeLocator;
import org.mule.runtime.core.internal.exception.ContributedErrorTypeRepository;
import org.mule.runtime.deployment.model.api.artifact.ArtifactContext;
import org.mule.runtime.dsl.api.ConfigResource;
import org.mule.runtime.internal.memory.management.ArtifactMemoryManagementService;
import org.mule.runtime.internal.memory.management.DefaultMemoryManagementService;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

import org.springframework.context.ApplicationContext;
import org.springframework.context.ConfigurableApplicationContext;

/**
 * <code>SpringXmlConfigurationBuilder</code> enables Mule to be configured from a Spring XML Configuration file used with Mule
 * name-spaces. Multiple configuration files can be loaded from this builder (specified as a comma-separated list).
 *
 * @deprecated Use {@link ArtifactAstConfigurationBuilder} instead.
 */
@Deprecated
public class SpringXmlConfigurationBuilder extends AbstractResourceConfigurationBuilder
    implements ParentMuleContextAwareConfigurationBuilder, ArtifactContextFactory {

  private MemoryManagementService memoryManagementService;
  private ArtifactDeclaration artifactDeclaration;
  private boolean enableLazyInit = false;
  private boolean disableXmlValidations = false;

  private ArtifactAst parentArtifactAst;
  private ApplicationContext parentContext;
  private MuleArtifactContext muleArtifactContext;
  private final ArtifactType artifactType;
  private final ComponentBuildingDefinitionRegistryFactory componentBuildingDefinitionRegistryFactory =
      new DefaultComponentBuildingDefinitionRegistryFactory();

  private SpringXmlConfigurationBuilder(String[] configResources, Map<String, String> artifactProperties,
                                        ArtifactType artifactType, boolean enableLazyInit, boolean disableXmlValidations,
                                        MemoryManagementService memoryManagementService)
      throws ConfigurationException {
    super(configResources, artifactProperties);
    this.artifactType = artifactType;
    this.enableLazyInit = enableLazyInit;
    this.disableXmlValidations = disableXmlValidations;
    this.memoryManagementService = memoryManagementService;
  }

  public SpringXmlConfigurationBuilder(String[] configResources, Map<String, String> artifactProperties,
                                       ArtifactType artifactType, boolean enableLazyInit, boolean disableXmlValidations)
      throws ConfigurationException {
    this(configResources, artifactProperties, artifactType, enableLazyInit, disableXmlValidations,
         DefaultMemoryManagementService.getInstance());
  }

  // TODO: MULE-19422 Remove specific tests usages
  @Deprecated
  public SpringXmlConfigurationBuilder(String configResources, Map<String, String> artifactProperties, ArtifactType artifactType)
      throws ConfigurationException {
    this(new String[] {configResources}, artifactProperties, artifactType, false, false);
  }

  // TODO: MULE-19422 Remove specific tests usages
  @Deprecated
  public SpringXmlConfigurationBuilder(String configResource) throws ConfigurationException {
    this(configResource, emptyMap(), APP);
  }

  // TODO: MULE-19422 Remove specific tests usages
  @Deprecated
  public SpringXmlConfigurationBuilder(String[] configFiles, Map<String, String> artifactProperties)
      throws ConfigurationException {
    this(configFiles, artifactProperties, APP, false, false);
  }

  // TODO: MULE-19422 Remove specific tests usages
  @Deprecated
  public SpringXmlConfigurationBuilder(String[] configFiles, boolean enableLazyInit, boolean disableXmlValidations)
      throws ConfigurationException {
    this(configFiles, emptyMap(), APP, enableLazyInit, disableXmlValidations,
         DefaultMemoryManagementService.getInstance());
  }

  public SpringXmlConfigurationBuilder(String[] configurationFiles, ArtifactDeclaration artifactDeclaration,
                                       Map<String, String> artifactProperties, ArtifactType artifactType,
                                       boolean enableLazyInitialisation, boolean disableXmlValidations)
      throws ConfigurationException {
    this(configurationFiles, artifactProperties, artifactType, enableLazyInitialisation, disableXmlValidations,
         DefaultMemoryManagementService.getInstance());
    this.artifactDeclaration = artifactDeclaration;
  }

  public SpringXmlConfigurationBuilder(String[] configurationFiles, ArtifactDeclaration artifactDeclaration,
                                       Map<String, String> artifactProperties, ArtifactType artifactType,
                                       boolean enableLazyInitialisation, boolean disableXmlValidations,
                                       MemoryManagementService memoryManagementService)
      throws ConfigurationException {
    this(configurationFiles, artifactDeclaration, artifactProperties, artifactType, enableLazyInitialisation,
         disableXmlValidations);
    this.memoryManagementService = memoryManagementService;
  }

  @Override
  protected void doConfigure(MuleContext muleContext) throws Exception {
    if (isEmpty(artifactConfigResources) && artifactType == DOMAIN) {
      ((DefaultMuleContext) muleContext).setLifecycleStrategy(new NullDomainMuleContextLifecycleStrategy());
      return;
    }

    initialiseIfNeeded(this, muleContext);
    Optional<ConfigurationProperties> parentConfigurationProperties = resolveParentConfigurationProperties();

    final BaseMuleArtifactContext baseMuleArtifactContext =
        createBaseContext(muleContext, parentConfigurationProperties);
    serviceConfigurators.forEach(serviceConfigurator -> serviceConfigurator.configure(muleContext.getCustomizationService()));
    createBaseRegistry(muleContext, baseMuleArtifactContext);

    muleArtifactContext = createApplicationContext(muleContext,
                                                   parentConfigurationProperties,
                                                   baseMuleArtifactContext.getBean(BaseConfigurationComponentLocator.class),
                                                   baseMuleArtifactContext.getBean(ContributedErrorTypeRepository.class),
                                                   baseMuleArtifactContext.getBean(ContributedErrorTypeLocator.class),
                                                   baseMuleArtifactContext.getBean(FeatureFlaggingService.class),
                                                   baseMuleArtifactContext.getBean(ExpressionLanguageMetadataService.class));
    muleArtifactContext.setParent(baseMuleArtifactContext);
    createSpringRegistry((DefaultMuleContext) muleContext, baseMuleArtifactContext, muleArtifactContext);
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

  /**
   * Template method for modifying the list of resources to be loaded. This operation is a no-op by default.
   *
   * @param allResources the list of {@link ConfigResource} to be loaded
   */
  protected void addResources(List<ConfigResource> allResources) {}

  private MuleArtifactContext createApplicationContext(MuleContext muleContext,
                                                       Optional<ConfigurationProperties> parentConfigurationProperties,
                                                       BaseConfigurationComponentLocator baseConfigurationComponentLocator,
                                                       ContributedErrorTypeRepository errorTypeRepository,
                                                       ContributedErrorTypeLocator errorTypeLocator,
                                                       FeatureFlaggingService featureFlaggingService,
                                                       ExpressionLanguageMetadataService expressionLanguageMetadataService)
      throws Exception {
    // TODO MULE-10084 : Refactor to only accept artifactConfiguration and not artifactConfigResources
    return doCreateApplicationContext(muleContext, artifactDeclaration, parentConfigurationProperties,
                                      baseConfigurationComponentLocator, errorTypeRepository, errorTypeLocator,
                                      featureFlaggingService, expressionLanguageMetadataService);
  }

  private MuleArtifactContext doCreateApplicationContext(MuleContext muleContext,
                                                         ArtifactDeclaration artifactDeclaration,
                                                         Optional<ConfigurationProperties> parentConfigurationProperties,
                                                         BaseConfigurationComponentLocator baseConfigurationComponentLocator,
                                                         ContributedErrorTypeRepository errorTypeRepository,
                                                         ContributedErrorTypeLocator errorTypeLocator,
                                                         FeatureFlaggingService featureFlaggingService,
                                                         ExpressionLanguageMetadataService expressionLanguageMetadataService) {
    final ArtifactAst artifactAst =
        createApplicationModel(getExtensions(muleContext.getExtensionManager()),
                               artifactDeclaration, resolveArtifactConfigResources(), getArtifactProperties(),
                               disableXmlValidations, featureFlaggingService);
    final ComponentBuildingDefinitionRegistry componentBuildingDefinitionRegistry =
        componentBuildingDefinitionRegistryFactory.create(artifactAst.dependencies(),
                                                          artifactAst::dependenciesDsl);

    MuleArtifactContext muleArtifactContext;
    if (enableLazyInit) {
      muleArtifactContext = new LazyMuleArtifactContext(muleContext, artifactAst,
                                                        parentConfigurationProperties,
                                                        baseConfigurationComponentLocator,
                                                        errorTypeRepository, errorTypeLocator,
                                                        getArtifactProperties(), artifactType,
                                                        resolveComponentModelInitializer(),
                                                        componentBuildingDefinitionRegistry,
                                                        new ArtifactMemoryManagementService(memoryManagementService),
                                                        featureFlaggingService, expressionLanguageMetadataService);
    } else {
      muleArtifactContext = new MuleArtifactContext(muleContext, artifactAst,
                                                    parentConfigurationProperties,
                                                    baseConfigurationComponentLocator,
                                                    errorTypeRepository, errorTypeLocator,
                                                    getArtifactProperties(), artifactType,
                                                    componentBuildingDefinitionRegistry,
                                                    new ArtifactMemoryManagementService(memoryManagementService),
                                                    featureFlaggingService, expressionLanguageMetadataService);
      muleArtifactContext.initialize();
    }

    return muleArtifactContext;
  }

  private Set<ExtensionModel> getExtensions(ExtensionManager extensionManager) {
    return extensionManager == null ? emptySet() : extensionManager.getExtensions();
  }

  private ArtifactAst createApplicationModel(Set<ExtensionModel> extensions,
                                             ArtifactDeclaration artifactDeclaration,
                                             ConfigResource[] artifactConfigResources,
                                             Map<String, String> artifactProperties,
                                             boolean disableXmlValidations,
                                             FeatureFlaggingService featureFlaggingService) {
    try {
      final ArtifactAst artifactAst;

      if (artifactDeclaration == null) {
        if (artifactConfigResources.length == 0) {
          artifactAst = emptyArtifact();
        } else {
          final AstXmlParser parser =
              createMuleXmlParser(extensions, artifactProperties, disableXmlValidations, featureFlaggingService);
          artifactAst = parser.parse(artifactConfigResources);
        }
      } else {
        artifactAst = toArtifactast(artifactDeclaration, extensions);
      }

      return artifactAst;
    } catch (MuleRuntimeException e) {
      throw e;
    } catch (Exception e) {
      throw new MuleRuntimeException(e);
    }
  }

  private AstXmlParser createMuleXmlParser(Set<ExtensionModel> extensions,
                                           Map<String, String> artifactProperties, boolean disableXmlValidations,
                                           FeatureFlaggingService featureFlaggingService) {
    ConfigurationPropertiesResolver propertyResolver = new ConfigurationPropertiesHierarchyBuilder()
        .withApplicationProperties(artifactProperties)
        .build();

    Builder builder = AstXmlParser.builder()
        .withPropertyResolver(propertyKey -> (String) propertyResolver.resolveValue(propertyKey))
        .withExtensionModels(extensions)
        .withParentArtifact(resolveParentArtifact(featureFlaggingService));
    if (!featureFlaggingService.isEnabled(ENTITY_RESOLVER_FAIL_ON_FIRST_ERROR)) {
      builder.withLegacyFailStrategy();
    }
    if (disableXmlValidations) {
      builder.withSchemaValidationsDisabled();
    }

    switch (artifactType) {
      case APP:
        builder.withArtifactType(org.mule.runtime.ast.api.ArtifactType.APPLICATION);
        break;
      case DOMAIN:
        builder.withArtifactType(org.mule.runtime.ast.api.ArtifactType.DOMAIN);
        break;
      case POLICY:
        builder.withArtifactType(org.mule.runtime.ast.api.ArtifactType.POLICY);
        break;
      default:
        break;
    }

    return builder.build();
  }

  protected ArtifactAst resolveParentArtifact(FeatureFlaggingService featureFlaggingService) {
    if (POLICY.equals(artifactType)) {
      return applicationFilteredFromPolicyArtifactAst(parentArtifactAst,
                                                      featureFlaggingService);
    }
    return parentArtifactAst;
  }

  private ConfigResource[] resolveArtifactConfigResources() {
    return artifactConfigResources;
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
    if (parentContext != null && parentContext instanceof ComponentModelInitializer) {
      parentLazyComponentInitializer = of((ComponentModelInitializer) parentContext);
    }

    return parentLazyComponentInitializer;
  }

  private void createSpringRegistry(DefaultMuleContext muleContext, ApplicationContext baseApplicationContext,
                                    MuleArtifactContext applicationContext)
      throws Exception {
    SpringRegistry registry;

    if (parentContext != null) {
      registry = createRegistryWithParentContext(muleContext, baseApplicationContext, applicationContext, parentContext);
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
                                                         MuleArtifactContext applicationContext,
                                                         ApplicationContext parentContext)
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
    this.parentArtifactAst = parentAst;
  }

}
