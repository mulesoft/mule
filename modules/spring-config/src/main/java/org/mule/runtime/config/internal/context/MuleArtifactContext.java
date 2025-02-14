/*
 * Copyright 2023 Salesforce, Inc. All rights reserved.
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.config.internal.context;

import static org.mule.runtime.api.config.MuleRuntimeFeature.DISABLE_ATTRIBUTE_PARAMETER_WHITESPACE_TRIMMING;
import static org.mule.runtime.api.config.MuleRuntimeFeature.DISABLE_POJO_TEXT_CDATA_WHITESPACE_TRIMMING;
import static org.mule.runtime.api.config.MuleRuntimeFeature.DISABLE_REGISTRY_BOOTSTRAP_OPTIONAL_ENTRIES;
import static org.mule.runtime.api.config.MuleRuntimeFeature.VALIDATE_APPLICATION_MODEL_WITH_REGION_CLASSLOADER;
import static org.mule.runtime.api.util.Preconditions.checkArgument;
import static org.mule.runtime.ast.api.util.AstTraversalDirection.BOTTOM_UP;
import static org.mule.runtime.ast.api.util.MuleAstUtils.recursiveStreamWithHierarchy;
import static org.mule.runtime.ast.api.util.MuleAstUtils.validatorBuilder;
import static org.mule.runtime.config.api.dsl.CoreDslConstants.CONFIGURATION_IDENTIFIER;
import static org.mule.runtime.config.api.dsl.CoreDslConstants.NOTIFICATIONS_IDENTIFIER;
import static org.mule.runtime.config.internal.context.AbstractSpringMuleContextServiceConfigurator.getBeanDefinitionBuilder;
import static org.mule.runtime.config.internal.dsl.spring.BeanDefinitionFactory.SPRING_SINGLETON_OBJECT;
import static org.mule.runtime.config.internal.model.ApplicationModel.findComponentDefinitionModel;
import static org.mule.runtime.config.internal.model.ApplicationModel.prepareAstForRuntime;
import static org.mule.runtime.config.internal.model.ApplicationModelAstPostProcessor.AST_POST_PROCESSORS;
import static org.mule.runtime.config.internal.model.properties.PropertiesHierarchyCreationUtils.createConfigurationAttributeResolver;
import static org.mule.runtime.config.internal.parsers.generic.AutoIdUtils.uniqueValue;
import static org.mule.runtime.core.api.config.MuleProperties.OBJECT_MULE_CONFIGURATION;
import static org.mule.runtime.core.api.config.MuleProperties.OBJECT_MULE_CONTEXT;
import static org.mule.runtime.core.api.config.MuleProperties.OBJECT_NOTIFICATION_MANAGER;
import static org.mule.runtime.core.api.config.MuleProperties.OBJECT_REGISTRY;
import static org.mule.runtime.core.api.config.bootstrap.ArtifactType.APP;
import static org.mule.runtime.core.api.config.bootstrap.ArtifactType.DOMAIN;
import static org.mule.runtime.core.api.config.bootstrap.ArtifactType.POLICY;
import static org.mule.runtime.core.api.lifecycle.LifecycleUtils.disposeIfNeeded;
import static org.mule.runtime.core.api.lifecycle.LifecycleUtils.initialiseIfNeeded;
import static org.mule.runtime.core.internal.el.function.MuleFunctionsBindingContextProvider.CORE_FUNCTIONS_PROVIDER_REGISTRY_KEY;
import static org.mule.runtime.core.internal.exception.ErrorTypeLocatorFactory.createDefaultErrorTypeLocator;
import static org.mule.runtime.extension.api.stereotype.MuleStereotypes.APP_CONFIG;
import static org.mule.runtime.module.artifact.activation.api.ast.ArtifactAstUtils.parseArtifactExtensionModel;
import static org.mule.runtime.module.artifact.activation.internal.ast.validation.AstValidationUtils.logWarningsAndThrowIfContainsErrors;
import static org.mule.runtime.module.extension.internal.manager.ExtensionErrorsRegistrant.registerErrorMappings;

import static java.lang.String.format;
import static java.util.Comparator.comparing;
import static java.util.Objects.requireNonNull;
import static java.util.Optional.of;
import static java.util.Optional.ofNullable;
import static java.util.stream.Collectors.toList;
import static java.util.stream.Collectors.toSet;

import static org.slf4j.LoggerFactory.getLogger;
import static org.springframework.beans.CachedIntrospectionResults.clearClassLoader;

import org.mule.runtime.api.artifact.Registry;
import org.mule.runtime.api.component.Component;
import org.mule.runtime.api.component.ComponentIdentifier;
import org.mule.runtime.api.component.ConfigurationProperties;
import org.mule.runtime.api.config.FeatureFlaggingService;
import org.mule.runtime.api.exception.ErrorTypeRepository;
import org.mule.runtime.api.exception.MuleRuntimeException;
import org.mule.runtime.api.ioc.ConfigurableObjectProvider;
import org.mule.runtime.api.ioc.ObjectProvider;
import org.mule.runtime.api.lifecycle.InitialisationException;
import org.mule.runtime.api.memory.management.MemoryManagementService;
import org.mule.runtime.api.meta.model.ExtensionModel;
import org.mule.runtime.api.meta.model.stereotype.HasStereotypeModel;
import org.mule.runtime.api.metadata.ExpressionLanguageMetadataService;
import org.mule.runtime.api.notification.ConnectionNotification;
import org.mule.runtime.api.notification.ConnectionNotificationListener;
import org.mule.runtime.api.notification.CustomNotification;
import org.mule.runtime.api.notification.CustomNotificationListener;
import org.mule.runtime.api.notification.ExceptionNotification;
import org.mule.runtime.api.notification.ExceptionNotificationListener;
import org.mule.runtime.api.notification.ExtensionNotification;
import org.mule.runtime.api.notification.ExtensionNotificationListener;
import org.mule.runtime.api.notification.ManagementNotification;
import org.mule.runtime.api.notification.ManagementNotificationListener;
import org.mule.runtime.api.notification.Notification;
import org.mule.runtime.api.notification.NotificationListener;
import org.mule.runtime.api.notification.SecurityNotification;
import org.mule.runtime.api.notification.SecurityNotificationListener;
import org.mule.runtime.api.notification.TransactionNotification;
import org.mule.runtime.api.notification.TransactionNotificationListener;
import org.mule.runtime.api.util.Pair;
import org.mule.runtime.ast.api.ArtifactAst;
import org.mule.runtime.ast.api.ComponentAst;
import org.mule.runtime.ast.api.validation.Validation;
import org.mule.runtime.ast.api.validation.ValidationResult;
import org.mule.runtime.config.internal.bean.NotificationConfig;
import org.mule.runtime.config.internal.bean.NotificationConfig.EnabledNotificationConfig;
import org.mule.runtime.config.internal.bean.ServerNotificationManagerConfigurator;
import org.mule.runtime.config.internal.dsl.model.SpringComponentModel;
import org.mule.runtime.config.internal.dsl.spring.BeanDefinitionFactory;
import org.mule.runtime.config.internal.editors.MulePropertyEditorRegistrar;
import org.mule.runtime.config.internal.model.ApplicationModel;
import org.mule.runtime.config.internal.model.ApplicationModelAstPostProcessor;
import org.mule.runtime.config.internal.model.ComponentBuildingDefinitionRegistryFactory;
import org.mule.runtime.config.internal.model.dsl.ClassLoaderResourceProvider;
import org.mule.runtime.config.internal.model.dsl.config.PropertiesResolverConfigurationProperties;
import org.mule.runtime.config.internal.processor.ComponentLocatorCreatePostProcessor;
import org.mule.runtime.config.internal.processor.DiscardedOptionalBeanPostProcessor;
import org.mule.runtime.config.internal.processor.LifecycleStatePostProcessor;
import org.mule.runtime.config.internal.processor.MuleInjectorProcessor;
import org.mule.runtime.config.privileged.spring.ByteBuddySpringCachesManager;
import org.mule.runtime.config.internal.registry.OptionalObjectsController;
import org.mule.runtime.config.internal.util.LaxInstantiationStrategyWrapper;
import org.mule.runtime.config.internal.validation.ast.ReusableArtifactAstDependencyGraphProvider;
import org.mule.runtime.core.api.MuleContext;
import org.mule.runtime.core.api.config.ConfigurationException;
import org.mule.runtime.core.api.config.bootstrap.ArtifactType;
import org.mule.runtime.core.api.context.notification.MuleContextNotification;
import org.mule.runtime.core.api.context.notification.MuleContextNotificationListener;
import org.mule.runtime.core.api.extension.ExtensionManager;
import org.mule.runtime.core.api.transaction.TransactionManagerFactory;
import org.mule.runtime.core.api.transformer.Converter;
import org.mule.runtime.core.internal.context.MuleContextWithRegistry;
import org.mule.runtime.core.internal.el.function.MuleFunctionsBindingContextProvider;
import org.mule.runtime.core.internal.exception.ContributedErrorTypeLocator;
import org.mule.runtime.core.internal.exception.ContributedErrorTypeRepository;
import org.mule.runtime.core.internal.registry.DefaultRegistry;
import org.mule.runtime.core.internal.registry.MuleRegistry;
import org.mule.runtime.core.internal.registry.TransformerResolver;
import org.mule.runtime.core.internal.util.DefaultResourceLocator;
import org.mule.runtime.core.privileged.component.AnnotatedObjectInvocationHandler;
import org.mule.runtime.core.privileged.exception.ErrorTypeLocator;
import org.mule.runtime.module.artifact.api.classloader.ArtifactClassLoader;
import org.mule.runtime.module.artifact.api.classloader.RegionClassLoader;
import org.mule.runtime.module.artifact.internal.classloader.WithAttachedClassLoaders;
import org.mule.runtime.module.extension.internal.manager.CompositeArtifactExtensionManager;
import org.mule.runtime.module.extension.internal.util.IntrospectionUtils;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.function.Predicate;

import com.google.common.collect.ImmutableList;
import org.slf4j.Logger;
import org.springframework.beans.CachedIntrospectionResults;
import org.springframework.beans.factory.NoSuchBeanDefinitionException;
import org.springframework.beans.factory.config.BeanDefinition;
import org.springframework.beans.factory.config.BeanPostProcessor;
import org.springframework.beans.factory.config.ConfigurableListableBeanFactory;
import org.springframework.beans.factory.support.BeanDefinitionRegistry;
import org.springframework.beans.factory.support.CglibSubclassingInstantiationStrategy;
import org.springframework.beans.factory.support.DefaultListableBeanFactory;
import org.springframework.beans.factory.support.GenericBeanDefinition;
import org.springframework.context.annotation.ContextAnnotationAutowireCandidateResolver;
import org.springframework.context.support.AbstractRefreshableConfigApplicationContext;

/**
 * <code>MuleArtifactContext</code> is a simple extension application context that allows resources to be loaded from the
 * Classpath of file system using the MuleBeanDefinitionReader.
 */
public class MuleArtifactContext extends AbstractRefreshableConfigApplicationContext {

  private static final Logger LOGGER = getLogger(MuleArtifactContext.class);

  public static final String INNER_BEAN_PREFIX = "(inner bean)";

  private final OptionalObjectsController optionalObjectsController;
  private final DefaultRegistry serviceDiscoverer;
  private final DefaultResourceLocator resourceLocator;
  private final PropertiesResolverConfigurationProperties configurationProperties;
  protected final MemoryManagementService memoryManagementService;
  private final ExpressionLanguageMetadataService expressionLanguageMetadataService;
  private ArtifactAst applicationModel;
  private final MuleContextWithRegistry muleContext;
  private final FeatureFlaggingService featureFlaggingService;
  private final MuleFunctionsBindingContextProvider coreFunctionsProvider;
  private final BeanDefinitionFactory beanDefinitionFactory;
  private final ArtifactType artifactType;
  private final BaseConfigurationComponentLocator baseComponentLocator;
  protected SpringConfigurationComponentLocator componentLocator;
  private final ContributedErrorTypeRepository errorTypeRepository;
  private final ContributedErrorTypeLocator errorTypeLocator;
  private final Map<String, String> artifactProperties;
  private final boolean addToolingObjectsToRegistry;
  protected List<ConfigurableObjectProvider> objectProviders = new ArrayList<>();
  private final ExtensionManager extensionManager;
  // TODO W-10855416: remove this
  private final boolean validateAppModelWithRegionClassloader;

  /**
   * Parses configuration files creating a spring ApplicationContext which is used as a parent registry using the SpringRegistry
   * registry implementation to wraps the spring ApplicationContext
   *
   * @param muleContext                                the {@link MuleContext} that own this context
   * @param artifactAst                                the definition of the artifact to create a context for
   * @param optionalObjectsController                  the {@link OptionalObjectsController} to use. Cannot be {@code null} @see
   *                                                   org.mule.runtime.config.internal.SpringRegistry
   * @param parentConfigurationProperties              the resolver for properties from the parent artifact to be used as fallback
   *                                                   in this artifact.
   * @param baseConfigurationComponentLocator          indirection to the actual ConfigurationComponentLocator in the full
   *                                                   registry
   * @param errorTypeRepository                        repository where the errors of the artifact will be registered.
   * @param errorTypeLocator                           locator where the errors of the artifact will be registered.
   * @param artifactProperties                         map of properties that can be referenced from the
   *                                                   {@code artifactConfigResources} as external configuration values
   * @param artifactType                               the type of artifact to determine the base objects of the created context.
   * @param componentBuildingDefinitionRegistryFactory
   * @since 3.7.0
   */
  public MuleArtifactContext(MuleContext muleContext, ArtifactAst artifactAst,
                             OptionalObjectsController optionalObjectsController,
                             Optional<ConfigurationProperties> parentConfigurationProperties,
                             BaseConfigurationComponentLocator baseConfigurationComponentLocator,
                             ContributedErrorTypeRepository errorTypeRepository,
                             ContributedErrorTypeLocator errorTypeLocator,
                             Map<String, String> artifactProperties,
                             boolean addToolingObjectsToRegistry,
                             ArtifactType artifactType,
                             ComponentBuildingDefinitionRegistryFactory componentBuildingDefinitionRegistryFactory,
                             MemoryManagementService memoryManagementService,
                             FeatureFlaggingService featureFlaggingService,
                             ExpressionLanguageMetadataService expressionLanguageMetadataService) {
    checkArgument(optionalObjectsController != null, "optionalObjectsController cannot be null");
    this.muleContext = (MuleContextWithRegistry) muleContext;
    this.featureFlaggingService = featureFlaggingService;
    this.expressionLanguageMetadataService = expressionLanguageMetadataService;
    this.coreFunctionsProvider = this.muleContext.getRegistry().get(CORE_FUNCTIONS_PROVIDER_REGISTRY_KEY);
    this.optionalObjectsController = optionalObjectsController;
    this.artifactType = artifactType;
    this.serviceDiscoverer = new DefaultRegistry(muleContext);
    this.resourceLocator = new DefaultResourceLocator();
    this.baseComponentLocator = baseConfigurationComponentLocator;
    this.errorTypeRepository = errorTypeRepository;
    this.errorTypeLocator = errorTypeLocator;
    this.artifactProperties = artifactProperties;
    this.addToolingObjectsToRegistry = addToolingObjectsToRegistry;
    this.memoryManagementService = memoryManagementService;

    extensionManager = muleContext.getExtensionManager();

    componentLocator = new SpringConfigurationComponentLocator(componentName -> {
      try {
        BeanDefinition beanDefinition = getBeanFactory().getBeanDefinition(componentName);
        return beanDefinition.isPrototype();
      } catch (NoSuchBeanDefinitionException e) {
        return false;
      }
    });

    // TODO (MULE-19608) remove this and make it into a component building definition
    this.beanDefinitionFactory =
        new BeanDefinitionFactory(muleContext.getConfiguration().getId(),
                                  componentBuildingDefinitionRegistryFactory.create(artifactAst.dependencies()),
                                  featureFlaggingService.isEnabled(DISABLE_ATTRIBUTE_PARAMETER_WHITESPACE_TRIMMING),
                                  featureFlaggingService.isEnabled(DISABLE_POJO_TEXT_CDATA_WHITESPACE_TRIMMING));

    // TODO W-10855416: remove this
    this.validateAppModelWithRegionClassloader =
        featureFlaggingService.isEnabled(VALIDATE_APPLICATION_MODEL_WITH_REGION_CLASSLOADER);

    this.applicationModel = artifactAst;

    // TODO MULE-18786 create the providers that depend on the AST only, and for the rest delegate on the resolver from the base
    // context
    this.configurationProperties = createConfigurationAttributeResolver(applicationModel, parentConfigurationProperties,
                                                                        artifactProperties,
                                                                        new ClassLoaderResourceProvider(muleContext
                                                                            .getExecutionClassLoader()),
                                                                        of(featureFlaggingService));

    try {
      initialiseIfNeeded(configurationProperties.getConfigurationPropertiesResolver());
      applicationModel.updatePropertiesResolver(configurationProperties.getConfigurationPropertiesResolver());

      validateArtifact(applicationModel);
    } catch (ConfigurationException | InitialisationException e) {
      throw new MuleRuntimeException(e);
    }
    registerErrors(applicationModel);
    registerApplicationExtensionModel();
  }

  protected MuleRegistry getMuleRegistry() {
    return this.muleContext.getRegistry();
  }

  protected void validateArtifact(final ArtifactAst artifactAst) throws ConfigurationException {
    doValidateModel(artifactAst, v -> true);
  }

  protected final void doValidateModel(ArtifactAst appModel, Predicate<Validation> validationsFilter)
      throws ConfigurationException {

    final ValidationResult validation = validatorBuilder()
        .withValidationEnricher(new RuntimeValidationEnricher(new ReusableArtifactAstDependencyGraphProvider(appModel),
                                                              muleContext))
        .withValidationsFilter(validationsFilter)
        // get the region classloader from the artifact one
        .withArtifactRegionClassLoader(getValidationClassloader())
        .build()
        .validate(appModel);

    logWarningsAndThrowIfContainsErrors(validation, LOGGER);
  }

  // TODO W-10855416: remove this and only validate it with the region classloader
  private ClassLoader getValidationClassloader() {
    if (validateAppModelWithRegionClassloader) {
      return muleContext.getExecutionClassLoader().getParent();
    }
    return muleContext.getExecutionClassLoader();
  }


  private ClassLoader getRegionClassLoader() {
    return muleContext.getExecutionClassLoader().getParent();
  }

  protected void registerApplicationExtensionModel() {
    if (!artifactType.equals(APP)) {
      return;
    }

    final String appName = muleContext.getConfiguration().getId();

    if (extensionManager.getExtension(appName).isPresent()) {
      logModelNotGenerated("ExtensionModel already registered");
      return;
    }

    fetchOrGenerateApplicationExtensionModel(appName).ifPresent(appExtensionModel -> {
      ExtensionManager appManager = extensionManager instanceof CompositeArtifactExtensionManager
          ? ((CompositeArtifactExtensionManager) extensionManager).getChildExtensionManager()
          : extensionManager;

      appManager.registerExtension(appExtensionModel);
    });
  }

  private Optional<ExtensionModel> fetchOrGenerateApplicationExtensionModel(String appName) {
    Optional<ExtensionModel> appExtensionModel = applicationModel.dependencies().stream()
        .filter(em -> em.getName().equals(appName))
        .findFirst();

    if (appExtensionModel.isPresent()) {
      return appExtensionModel;
    }

    try {
      return parseArtifactExtensionModel(applicationModel, getRegionClassLoader(), muleContext,
                                         expressionLanguageMetadataService);
    } catch (ConfigurationException e) {
      throw new MuleRuntimeException(e);
    }
  }

  private void logModelNotGenerated(String reason) {
    if (LOGGER.isWarnEnabled()) {
      LOGGER.warn("{}. ExtensionModel for app {} not generated", reason, muleContext.getConfiguration().getId());
    }
  }

  protected void registerErrors(final ArtifactAst artifactAst) {
    doRegisterErrors(artifactAst);
  }

  protected void doRegisterErrors(final ArtifactAst artifactAst) {
    final ErrorTypeRepository errorTypeRepository = artifactAst.enrichedErrorTypeRepository();
    final ErrorTypeLocator errorTypeLocator =
        createDefaultErrorTypeLocator(errorTypeRepository, ofNullable(featureFlaggingService));

    final Set<ExtensionModel> dependencies = artifactAst.dependencies();
    registerErrorMappings(errorTypeRepository, errorTypeLocator, dependencies);

    // Because instances of the repository and locator may be already created and injected into another objects, those instances
    // cannot just be set into the registry, and this contributing layer is needed to ensure the correct functioning of the DI
    // mechanism.
    this.errorTypeRepository.setDelegate(errorTypeRepository);
    this.errorTypeLocator.setDelegate(errorTypeLocator);
  }

  public void initialize() {
    applicationModel = prepareAstForRuntime(applicationModel, applicationModel.dependencies());
  }

  @Override
  protected void prepareBeanFactory(ConfigurableListableBeanFactory beanFactory) {
    super.prepareBeanFactory(beanFactory);
    beanFactory.setBeanExpressionResolver(null);

    registerEditors(beanFactory);

    registerAnnotationConfigProcessors((BeanDefinitionRegistry) beanFactory, beanFactory);

    addBeanPostProcessors(beanFactory,
                          new MuleContextPostProcessor(muleContext),
                          // TODO W-10736276 Remove this postProcessor
                          new DiscardedOptionalBeanPostProcessor(getOptionalObjectsController(),
                                                                 (DefaultListableBeanFactory) beanFactory),
                          new LifecycleStatePostProcessor(muleContext.getLifecycleManager().getState()),
                          new ComponentLocatorCreatePostProcessor(componentLocator));

    beanFactory.registerSingleton(OBJECT_MULE_CONTEXT, muleContext);

    prepareObjectProviders(objectProviders);
  }

  /**
   * Configures the given {@link ConfigurableObjectProvider}s.
   *
   * @param configurableObjectProviders The {@link ConfigurableObjectProvider}s to configure.
   */
  protected void prepareObjectProviders(List<ConfigurableObjectProvider> configurableObjectProviders) {
    // If the object providers list is empty we don't even bother creating the ObjectProviderConfiguration
    // (this may happen during lazy initialization for example)
    if (configurableObjectProviders.isEmpty()) {
      return;
    }

    MuleArtifactObjectProvider muleArtifactObjectProvider = new MuleArtifactObjectProvider(this);
    ImmutableObjectProviderConfiguration providerConfiguration =
        new ImmutableObjectProviderConfiguration(configurationProperties, muleArtifactObjectProvider);
    for (ConfigurableObjectProvider objectProvider : configurableObjectProviders) {
      objectProvider.configure(providerConfiguration);
    }
  }

  /**
   * Process all the {@link ObjectProvider}s from the {@link ApplicationModel} to get their beans and register them inside the
   * spring bean factory so they can be used for dependency injection.
   *
   * @param beanFactory the spring bean factory where the objects will be registered.
   */
  protected void registerObjectFromObjectProviders(ConfigurableListableBeanFactory beanFactory) {
    ((ObjectProviderAwareBeanFactory) beanFactory).setObjectProviders(objectProviders);
  }

  private List<Pair<ComponentAst, Optional<String>>> lookObjectProvidersComponentModels(ArtifactAst applicationModel,
                                                                                        Map<ComponentAst, SpringComponentModel> springComponentModels) {
    return applicationModel.topLevelComponentsStream()
        .filter(componentModel -> {
          final SpringComponentModel springModel = springComponentModels.get(componentModel);
          return springModel != null && springModel.getType() != null
              && ConfigurableObjectProvider.class.isAssignableFrom(springModel.getType());
        })
        .map(componentModel -> new Pair<>(componentModel, componentModel.getComponentId()))
        .collect(toList());
  }

  private void registerEditors(ConfigurableListableBeanFactory beanFactory) {
    MulePropertyEditorRegistrar registrar = new MulePropertyEditorRegistrar();
    registrar.setMuleContext(muleContext);
    beanFactory.addPropertyEditorRegistrar(registrar);
  }

  protected void addBeanPostProcessors(ConfigurableListableBeanFactory beanFactory, BeanPostProcessor... processors) {
    for (BeanPostProcessor processor : processors) {
      beanFactory.addBeanPostProcessor(processor);
    }
  }

  @Override
  public void close() {
    if (isRunning()) {
      try {
        super.close();
      } catch (Exception e) {
        for (ObjectProvider objectProvider : objectProviders) {
          disposeIfNeeded(objectProvider, LOGGER);
        }
        throw new MuleRuntimeException(e);
      }
      disposeIfNeeded(configurationProperties.getConfigurationPropertiesResolver(), LOGGER);
      // Additional Spring cache cleanup
      clearSpringSoftReferencesCachesForDynamicClassLoaders();
      try {
        ByteBuddySpringCachesManager.clearCaches();
        // Needed due to a shade of spring-core in the extensions-support module.
        IntrospectionUtils.resetCommonCaches();
      } catch (Exception e) {
        LOGGER.warn("Spring caches cleanup failed", e);
      }
    }
  }

  /**
   * We use ByteBuddy to enhance classes defined with the Java SDK, in order to make them implement the {@link Component}
   * interface. The classloader used to load such dynamic classes is being hold by a cache in Spring, and that cache can be
   * cleared by calling {@link CachedIntrospectionResults#clearClassLoader}. Notice that this method can be called with the
   * classloader of the class itself, or any of the parents in its hierarchy.
   * 
   * @see AnnotatedObjectInvocationHandler#addAnnotationsToClass
   * @see CachedIntrospectionResults#clearClassLoader
   */
  private void clearSpringSoftReferencesCachesForDynamicClassLoaders() {
    ClassLoader regionClassLoader = getRegionClassLoader();
    if (!(regionClassLoader instanceof RegionClassLoader)) {
      // The method #getRegionClassLoader() should always return the corresponding RegionClassLoader. However, in the
      // integration tests (which is an ArtifactFunctionalTestCase), the classloader here is an instance of
      // TestRegionClassLoader. That class extends RegionClassLoader, but it's loaded with a different classloader, and
      // then we would be getting a ClassCastException here. That's the only reason for this early-return.
      LOGGER.debug("Got an instance of '{}' as region classloader. We can't clean the spring soft-references caches.",
                   regionClassLoader.getClass().getCanonicalName());
      return;
    }
    RegionClassLoader region = (RegionClassLoader) regionClassLoader;
    clearClassLoader(region.getClassLoader());
    for (ArtifactClassLoader pluginClassLoader : region.getArtifactPluginClassLoaders()) {
      if (pluginClassLoader instanceof WithAttachedClassLoaders) {
        WithAttachedClassLoaders withAttachedClassLoaders = (WithAttachedClassLoaders) pluginClassLoader;
        for (ClassLoader dynamicClassLoader : withAttachedClassLoaders.getAttachedClassLoaders()) {
          clearClassLoader(dynamicClassLoader);
        }
      }
    }
  }

  @Override
  protected void loadBeanDefinitions(DefaultListableBeanFactory beanFactory) throws IOException {
    createApplicationComponents(beanFactory, applicationModel, true);
  }

  /**
   * @param componentAst The {@link ComponentAst} to test.
   * @return if the {@code componentAst} needs to be always enabled.
   */
  protected static boolean isAlwaysEnabledComponent(ComponentAst componentAst) {
    return componentAst.getModel(HasStereotypeModel.class)
        .map(stm -> stm.getStereotype() != null && stm.getStereotype().isAssignableTo(APP_CONFIG))
        .orElse(false);
  }

  /**
   * Callback to perform actions when a new configurable object provider is discovered as part of the creation of components.
   * <p>
   * Derived classes must call the super class's implementation of this method.
   *
   * @param objectProvider The {@link ConfigurableObjectProvider} just discovered.
   */
  protected void onObjectProviderDiscovered(ConfigurableObjectProvider objectProvider) {
    objectProviders.add(objectProvider);
  }

  /**
   * Creates the definition for all the objects to be created form the enabled components in the {@code applicationModel}.
   *
   * @param beanFactory      the bean factory in which definition must be created.
   * @param applicationModel the artifact application model.
   * @param mustBeRoot       if the component must be root to be created.
   * @return an order list of the created bean names. The order must be respected for the creation of the objects.
   */
  protected List<Pair<String, ComponentAst>> createApplicationComponents(DefaultListableBeanFactory beanFactory,
                                                                         ArtifactAst applicationModel,
                                                                         boolean mustBeRoot) {
    Map<ComponentAst, SpringComponentModel> springComponentModels = new LinkedHashMap<>();

    return doCreateApplicationComponents(beanFactory, applicationModel, mustBeRoot, springComponentModels);
  }

  protected List<Pair<String, ComponentAst>> doCreateApplicationComponents(DefaultListableBeanFactory beanFactory,
                                                                           ArtifactAst applicationModel, boolean mustBeRoot,
                                                                           Map<ComponentAst, SpringComponentModel> springComponentModels) {
    Set<String> alwaysEnabledTopLevelComponents = new HashSet<>();
    Set<ComponentIdentifier> alwaysEnabledUnnamedTopLevelComponents = applicationModel.topLevelComponentsStream()
        .filter(MuleArtifactContext::isAlwaysEnabledComponent)
        .peek(cm -> cm.getComponentId().ifPresent(alwaysEnabledTopLevelComponents::add))
        .filter(cm -> !cm.getComponentId().isPresent())
        .map(ComponentAst::getIdentifier)
        .collect(toSet());
    Set<String> alwaysEnabledGeneratedTopLevelComponentsName = new HashSet<>();

    List<Pair<String, ComponentAst>> createdComponentModels = new ArrayList<>();

    Set<ComponentAst> rootComponents = resolveRootComponents(applicationModel);

    recursiveStreamWithHierarchy(applicationModel, BOTTOM_UP, true)
        // Create component if must not be root is mandatory or component is a root component or component is child of a root
        // component
        .filter(cm -> !mustBeRoot || rootComponents.contains(cm.getFirst())
            || cm.getSecond().stream().anyMatch(rootComponents::contains))
        .filter(cm -> !isIgnored(cm.getFirst()))
        .forEach(cm -> {
          if (rootComponents.contains(cm.getFirst())) {
            cm.getFirst().getComponentId()
                .ifPresent(componentName -> createdComponentModels.add(new Pair<>(componentName, cm.getFirst())));
          }

          beanDefinitionFactory.resolveComponent(springComponentModels,
                                                 cm.getSecond(),
                                                 cm.getFirst(), beanFactory, componentLocator);

          if (rootComponents.contains(cm.getFirst())) {
            componentLocator.addComponentLocation(cm.getFirst().getLocation());
          }
        });


    springComponentModels.values().stream()
        .filter(resolvedComponentModel -> rootComponents.contains(resolvedComponentModel.getComponent()))
        .forEach(resolvedComponentModel -> registerRootSpringBean(beanFactory, alwaysEnabledUnnamedTopLevelComponents,
                                                                  alwaysEnabledGeneratedTopLevelComponentsName,
                                                                  createdComponentModels, resolvedComponentModel));

    // This should only be done once at the initial application model creation, called from Spring
    List<Pair<ComponentAst, Optional<String>>> objectProvidersByName =
        lookObjectProvidersComponentModels(applicationModel, springComponentModels);

    objectProvidersByName.stream()
        .map(pair -> springComponentModels.get(pair.getFirst()).getObjectInstance())
        .forEach(this::onObjectProviderDiscovered);
    registerObjectFromObjectProviders(beanFactory);

    Set<String> objectProviderNames = objectProvidersByName.stream()
        .map(Pair::getSecond)
        .filter(Optional::isPresent)
        .map(Optional::get)
        .collect(toSet());

    // Put object providers first, then always enabled components, then the rest
    createdComponentModels.sort(comparing(beanNameAndComponent -> {
      final String beanName = beanNameAndComponent.getFirst();
      if (objectProviderNames.contains(beanName)) {
        return 1;
      } else if (alwaysEnabledGeneratedTopLevelComponentsName.contains(beanName)) {
        return 2;
      } else if (alwaysEnabledTopLevelComponents.contains(beanName)) {
        return 3;
      } else {
        return 4;
      }
    }));

    return createdComponentModels;
  }

  protected Set<ComponentAst> resolveRootComponents(ArtifactAst applicationModel) {
    Set<ComponentAst> rootComponents = new HashSet<>(applicationModel.topLevelComponents());
    for (ApplicationModelAstPostProcessor astPostProcessor : AST_POST_PROCESSORS.get()) {
      rootComponents = astPostProcessor.resolveRootComponents(rootComponents, extensionManager.getExtensions());
    }
    return rootComponents;
  }

  protected boolean isIgnored(ComponentAst componentAst) {
    return beanDefinitionFactory.isComponentIgnored(componentAst.getIdentifier());
  }

  private void registerRootSpringBean(DefaultListableBeanFactory beanFactory,
                                      Set<ComponentIdentifier> alwaysEnabledUnnamedTopLevelComponents,
                                      Set<String> alwaysEnabledGeneratedTopLevelComponentsName,
                                      List<Pair<String, ComponentAst>> createdComponentModels,
                                      SpringComponentModel resolvedComponentModel) {
    String nameAttribute = resolvedComponentModel.getComponent().getComponentId()
        .orElse(resolvedComponentModel.getComponentName());
    if (nameAttribute == null) {
      // This may be a configuration that does not requires a name.
      nameAttribute = uniqueValue(resolvedComponentModel.getBeanDefinition().getBeanClassName());

      if (alwaysEnabledUnnamedTopLevelComponents.contains(resolvedComponentModel.getComponent().getIdentifier())) {
        alwaysEnabledGeneratedTopLevelComponentsName.add(nameAttribute);
        createdComponentModels.add(new Pair<>(nameAttribute, resolvedComponentModel.getComponent()));
      } else if (resolvedComponentModel.getType() != null
          && TransactionManagerFactory.class.isAssignableFrom(resolvedComponentModel.getType())) {
        createdComponentModels.add(new Pair<>(nameAttribute, resolvedComponentModel.getComponent()));
      }
    }

    beanFactory.registerBeanDefinition(nameAttribute,
                                       requireNonNull(resolvedComponentModel.getBeanDefinition(),
                                                      "BeanDefinition null for "
                                                          + resolvedComponentModel.getComponent().toString()));
    postProcessBeanDefinition(resolvedComponentModel, beanFactory, nameAttribute);
  }

  @Override
  protected void customizeBeanFactory(DefaultListableBeanFactory beanFactory) {
    super.customizeBeanFactory(beanFactory);
    baseComponentLocator.setDelegate(componentLocator);
    createServiceConfigurator(beanFactory).createArtifactServices();
  }

  protected SpringMuleContextServiceConfigurator createServiceConfigurator(DefaultListableBeanFactory beanFactory) {
    return new SpringMuleContextServiceConfigurator(muleContext,
                                                    getCoreFunctionsProvider(),
                                                    getConfigurationProperties(),
                                                    artifactProperties,
                                                    addToolingObjectsToRegistry,
                                                    getArtifactType(),
                                                    getApplicationModel(),
                                                    getOptionalObjectsController(),
                                                    beanFactory,
                                                    getServiceDiscoverer(),
                                                    getResourceLocator(),
                                                    memoryManagementService);
  }

  @Override
  protected void postProcessBeanFactory(ConfigurableListableBeanFactory beanFactory) {
    BeanDefinitionRegistry beanDefinitionRegistry = (BeanDefinitionRegistry) beanFactory;

    if (!findComponentDefinitionModel(applicationModel, NOTIFICATIONS_IDENTIFIER).isPresent()) {
      registerNotificationManagerBean(beanDefinitionRegistry);
    }
  }

  private void registerNotificationManagerBean(BeanDefinitionRegistry beanDefinitionRegistry) {
    beanDefinitionRegistry
        .registerBeanDefinition(OBJECT_NOTIFICATION_MANAGER,
                                getBeanDefinitionBuilder(ServerNotificationManagerConfigurator.class)
                                    .addPropertyValue("enabledNotifications", ImmutableList
                                        .<NotificationConfig<? extends Notification, ? extends NotificationListener>>builder()
                                        .add(new EnabledNotificationConfig<>(MuleContextNotificationListener.class,
                                                                             MuleContextNotification.class))
                                        .add(new EnabledNotificationConfig<>(SecurityNotificationListener.class,
                                                                             SecurityNotification.class))
                                        .add(new EnabledNotificationConfig<>(ManagementNotificationListener.class,
                                                                             ManagementNotification.class))
                                        .add(new EnabledNotificationConfig<>(ConnectionNotificationListener.class,
                                                                             ConnectionNotification.class))
                                        .add(new EnabledNotificationConfig<>(CustomNotificationListener.class,
                                                                             CustomNotification.class))
                                        .add(new EnabledNotificationConfig<>(ExceptionNotificationListener.class,
                                                                             ExceptionNotification.class))
                                        .add(new EnabledNotificationConfig<>(TransactionNotificationListener.class,
                                                                             TransactionNotification.class))
                                        .add(new EnabledNotificationConfig<>(ExtensionNotificationListener.class,
                                                                             ExtensionNotification.class))
                                        .build())
                                    .getBeanDefinition());
  }

  private void registerAnnotationConfigProcessors(BeanDefinitionRegistry registry, ConfigurableListableBeanFactory beanFactory) {
    registerInjectorProcessor(beanFactory);
  }

  protected void registerInjectorProcessor(ConfigurableListableBeanFactory beanFactory) {
    MuleInjectorProcessor muleInjectorProcessor = null;
    if (artifactType.equals(APP) || artifactType.equals(POLICY) || artifactType.equals(DOMAIN)) {
      muleInjectorProcessor = new MuleInjectorProcessor();
    }
    if (muleInjectorProcessor != null) {
      muleInjectorProcessor.setBeanFactory(beanFactory);
      beanFactory.addBeanPostProcessor(muleInjectorProcessor);
    }
  }

  @Override
  protected DefaultListableBeanFactory createBeanFactory() {
    // Copy all postProcessors defined in the defaultMuleConfig so that they get applied to the child container
    DefaultListableBeanFactory beanFactory = new ObjectProviderAwareBeanFactory(getInternalParentBeanFactory());
    beanFactory.setAutowireCandidateResolver(new ContextAnnotationAutowireCandidateResolver());

    // TODO W-10736276 Remove this
    if (!featureFlaggingService.isEnabled(DISABLE_REGISTRY_BOOTSTRAP_OPTIONAL_ENTRIES)) {
      beanFactory.setInstantiationStrategy(new LaxInstantiationStrategyWrapper(new CglibSubclassingInstantiationStrategy(),
                                                                               getOptionalObjectsController()));
    }

    return beanFactory;
  }

  /**
   * {@inheritDoc} This implementation returns {@code false} if the context hasn't been initialised yet, in opposition to the
   * default implementation which throws an exception
   */
  @Override
  public boolean isRunning() {
    try {
      return super.isRunning();
    } catch (IllegalStateException e) {
      return false;
    }
  }

  /**
   * Forces the registration of instances of {@link TransformerResolver} and {@link Converter} to be created, so that they are
   * added to the transformation graph.
   */
  protected static void postProcessBeanDefinition(SpringComponentModel resolvedComponent, BeanDefinitionRegistry registry,
                                                  String beanName) {
    if (Converter.class.isAssignableFrom(resolvedComponent.getType())) {
      GenericBeanDefinition converterBeanDefinitionCopy = new GenericBeanDefinition(resolvedComponent.getBeanDefinition());
      converterBeanDefinitionCopy.setScope(SPRING_SINGLETON_OBJECT);
      registry.registerBeanDefinition(beanName + "-" + "converter", converterBeanDefinitionCopy);
    }
  }

  public MuleContextWithRegistry getMuleContext() {
    return muleContext;
  }

  protected MuleFunctionsBindingContextProvider getCoreFunctionsProvider() {
    return coreFunctionsProvider;
  }

  protected PropertiesResolverConfigurationProperties getConfigurationProperties() {
    return configurationProperties;
  }

  protected ArtifactType getArtifactType() {
    return artifactType;
  }

  public OptionalObjectsController getOptionalObjectsController() {
    return optionalObjectsController;
  }

  public Registry getRegistry() {
    return getMuleContext().getRegistry().get(OBJECT_REGISTRY);
  }

  @Override
  public String toString() {
    return format("%s: %s (%s)", this.getClass().getName(), muleContext.getConfiguration().getId(), artifactType.name());
  }

  public ArtifactAst getApplicationModel() {
    return applicationModel;
  }

  protected DefaultRegistry getServiceDiscoverer() {
    return serviceDiscoverer;
  }

  protected DefaultResourceLocator getResourceLocator() {
    return resourceLocator;
  }

  public boolean isAddToolingObjectsToRegistry() {
    return addToolingObjectsToRegistry;
  }
}
