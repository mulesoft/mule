/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.config.internal.context;

import static org.mule.runtime.api.config.MuleRuntimeFeature.DISABLE_ATTRIBUTE_PARAMETER_WHITESPACE_TRIMMING;
import static org.mule.runtime.api.config.MuleRuntimeFeature.DISABLE_POJO_TEXT_CDATA_WHITESPACE_TRIMMING;
import static org.mule.runtime.api.i18n.I18nMessageFactory.createStaticMessage;
import static org.mule.runtime.api.util.Preconditions.checkArgument;
import static org.mule.runtime.ast.api.util.AstTraversalDirection.BOTTOM_UP;
import static org.mule.runtime.ast.api.util.MuleAstUtils.recursiveStreamWithHierarchy;
import static org.mule.runtime.ast.api.util.MuleAstUtils.validatorBuilder;
import static org.mule.runtime.ast.api.validation.Validation.Level.ERROR;
import static org.mule.runtime.ast.api.validation.Validation.Level.WARN;
import static org.mule.runtime.config.api.dsl.CoreDslConstants.CONFIGURATION_IDENTIFIER;
import static org.mule.runtime.config.internal.dsl.spring.BeanDefinitionFactory.SPRING_SINGLETON_OBJECT;
import static org.mule.runtime.config.internal.model.ApplicationModel.findComponentDefinitionModel;
import static org.mule.runtime.config.internal.model.ApplicationModel.prepareAstForRuntime;
import static org.mule.runtime.config.internal.model.ApplicationModelAstPostProcessor.AST_POST_PROCESSORS;
import static org.mule.runtime.config.internal.model.properties.PropertiesResolverUtils.createConfigurationAttributeResolver;
import static org.mule.runtime.config.internal.parsers.generic.AutoIdUtils.uniqueValue;
import static org.mule.runtime.core.api.config.MuleProperties.OBJECT_MULE_CONFIGURATION;
import static org.mule.runtime.core.api.config.MuleProperties.OBJECT_MULE_CONTEXT;
import static org.mule.runtime.core.api.config.MuleProperties.OBJECT_REGISTRY;
import static org.mule.runtime.core.api.config.bootstrap.ArtifactType.APP;
import static org.mule.runtime.core.api.config.bootstrap.ArtifactType.DOMAIN;
import static org.mule.runtime.core.api.config.bootstrap.ArtifactType.POLICY;
import static org.mule.runtime.core.api.lifecycle.LifecycleUtils.disposeIfNeeded;
import static org.mule.runtime.core.api.lifecycle.LifecycleUtils.initialiseIfNeeded;
import static org.mule.runtime.core.internal.el.function.MuleFunctionsBindingContextProvider.CORE_FUNCTIONS_PROVIDER_REGISTRY_KEY;
import static org.mule.runtime.core.internal.exception.ErrorTypeLocatorFactory.createDefaultErrorTypeLocator;
import static org.mule.runtime.extension.api.stereotype.MuleStereotypes.APP_CONFIG;
import static org.mule.runtime.module.extension.internal.manager.ExtensionErrorsRegistrant.registerErrorMappings;

import static java.lang.String.format;
import static java.lang.System.lineSeparator;
import static java.util.Collections.emptySet;
import static java.util.Comparator.comparing;
import static java.util.Objects.requireNonNull;
import static java.util.Optional.of;
import static java.util.stream.Collectors.joining;
import static java.util.stream.Collectors.toList;
import static java.util.stream.Collectors.toSet;

import static org.slf4j.LoggerFactory.getLogger;
import static org.springframework.beans.factory.support.BeanDefinitionBuilder.genericBeanDefinition;
import static org.springframework.context.annotation.AnnotationConfigUtils.CONFIGURATION_ANNOTATION_PROCESSOR_BEAN_NAME;
import static org.springframework.context.annotation.AnnotationConfigUtils.REQUIRED_ANNOTATION_PROCESSOR_BEAN_NAME;

import org.mule.runtime.api.artifact.Registry;
import org.mule.runtime.api.component.ComponentIdentifier;
import org.mule.runtime.api.component.ConfigurationProperties;
import org.mule.runtime.api.config.FeatureFlaggingService;
import org.mule.runtime.api.exception.ErrorTypeRepository;
import org.mule.runtime.api.exception.MuleException;
import org.mule.runtime.api.exception.MuleRuntimeException;
import org.mule.runtime.api.ioc.ConfigurableObjectProvider;
import org.mule.runtime.api.ioc.ObjectProvider;
import org.mule.runtime.api.lifecycle.InitialisationException;
import org.mule.runtime.api.memory.management.MemoryManagementService;
import org.mule.runtime.api.meta.model.ExtensionModel;
import org.mule.runtime.api.meta.model.stereotype.HasStereotypeModel;
import org.mule.runtime.api.util.Pair;
import org.mule.runtime.ast.api.ArtifactAst;
import org.mule.runtime.ast.api.ComponentAst;
import org.mule.runtime.ast.api.validation.Validation;
import org.mule.runtime.ast.api.validation.ValidationResult;
import org.mule.runtime.ast.api.validation.ValidationResultItem;
import org.mule.runtime.config.internal.dsl.model.ClassLoaderResourceProvider;
import org.mule.runtime.config.internal.dsl.model.SpringComponentModel;
import org.mule.runtime.config.internal.dsl.model.config.PropertiesResolverConfigurationProperties;
import org.mule.runtime.config.internal.dsl.spring.BeanDefinitionFactory;
import org.mule.runtime.config.internal.editors.MulePropertyEditorRegistrar;
import org.mule.runtime.config.internal.factories.MuleConfigurationConfigurator;
import org.mule.runtime.config.internal.model.ApplicationModel;
import org.mule.runtime.config.internal.model.ApplicationModelAstPostProcessor;
import org.mule.runtime.config.internal.model.ComponentBuildingDefinitionRegistryFactory;
import org.mule.runtime.config.internal.processor.ComponentLocatorCreatePostProcessor;
import org.mule.runtime.config.internal.processor.DiscardedOptionalBeanPostProcessor;
import org.mule.runtime.config.internal.processor.LifecycleStatePostProcessor;
import org.mule.runtime.config.internal.processor.MuleInjectorProcessor;
import org.mule.runtime.config.internal.processor.PostRegistrationActionsPostProcessor;
import org.mule.runtime.config.internal.registry.OptionalObjectsController;
import org.mule.runtime.config.internal.util.LaxInstantiationStrategyWrapper;
import org.mule.runtime.core.api.MuleContext;
import org.mule.runtime.core.api.config.ConfigurationException;
import org.mule.runtime.core.api.config.bootstrap.ArtifactType;
import org.mule.runtime.core.api.extension.ExtensionManager;
import org.mule.runtime.core.api.transaction.TransactionManagerFactory;
import org.mule.runtime.core.api.transformer.Converter;
import org.mule.runtime.core.internal.context.MuleContextWithRegistry;
import org.mule.runtime.core.internal.el.function.MuleFunctionsBindingContextProvider;
import org.mule.runtime.core.internal.exception.ContributedErrorTypeLocator;
import org.mule.runtime.core.internal.exception.ContributedErrorTypeRepository;
import org.mule.runtime.core.internal.registry.DefaultRegistry;
import org.mule.runtime.core.internal.registry.MuleRegistry;
import org.mule.runtime.core.internal.registry.MuleRegistryHelper;
import org.mule.runtime.core.internal.registry.TransformerResolver;
import org.mule.runtime.core.internal.util.DefaultResourceLocator;
import org.mule.runtime.core.privileged.exception.ErrorTypeLocator;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.function.Predicate;

import org.slf4j.Logger;
import org.springframework.beans.factory.NoSuchBeanDefinitionException;
import org.springframework.beans.factory.annotation.RequiredAnnotationBeanPostProcessor;
import org.springframework.beans.factory.config.BeanDefinition;
import org.springframework.beans.factory.config.BeanPostProcessor;
import org.springframework.beans.factory.config.ConfigurableListableBeanFactory;
import org.springframework.beans.factory.support.BeanDefinitionRegistry;
import org.springframework.beans.factory.support.CglibSubclassingInstantiationStrategy;
import org.springframework.beans.factory.support.DefaultListableBeanFactory;
import org.springframework.beans.factory.support.GenericBeanDefinition;
import org.springframework.beans.factory.support.RootBeanDefinition;
import org.springframework.context.annotation.ConfigurationClassPostProcessor;
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
  private ArtifactAst applicationModel;
  private final MuleContextWithRegistry muleContext;
  private final MuleFunctionsBindingContextProvider coreFunctionsProvider;
  private final BeanDefinitionFactory beanDefinitionFactory;
  private final ArtifactType artifactType;
  private final BaseConfigurationComponentLocator baseComponentLocator;
  protected SpringConfigurationComponentLocator componentLocator;
  private final ContributedErrorTypeRepository errorTypeRepository;
  private final ContributedErrorTypeLocator errorTypeLocator;
  private final Map<String, String> artifactProperties;
  protected List<ConfigurableObjectProvider> objectProviders = new ArrayList<>();
  private final ExtensionManager extensionManager;


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
                             Map<String, String> artifactProperties, ArtifactType artifactType,
                             ComponentBuildingDefinitionRegistryFactory componentBuildingDefinitionRegistryFactory,
                             MemoryManagementService memoryManagementService,
                             FeatureFlaggingService featureFlaggingService) {
    checkArgument(optionalObjectsController != null, "optionalObjectsController cannot be null");
    this.muleContext = (MuleContextWithRegistry) muleContext;
    this.coreFunctionsProvider = this.muleContext.getRegistry().get(CORE_FUNCTIONS_PROVIDER_REGISTRY_KEY);
    this.optionalObjectsController = optionalObjectsController;
    this.artifactType = artifactType;
    this.serviceDiscoverer = new DefaultRegistry(muleContext);
    this.resourceLocator = new DefaultResourceLocator();
    this.baseComponentLocator = baseConfigurationComponentLocator;
    this.errorTypeRepository = errorTypeRepository;
    this.errorTypeLocator = errorTypeLocator;
    this.artifactProperties = artifactProperties;
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
        .withValidationEnricher(v -> {
          try {
            muleContext.getInjector().inject(v);
          } catch (MuleException e) {
            throw new MuleRuntimeException(e);
          }
        })
        .withValidationsFilter(validationsFilter)
        // get the region classloader from the artifact one
        .withArtifactRegionClassLoader(this.muleContext.getExecutionClassLoader().getParent())
        .build()
        .validate(appModel);

    final Collection<ValidationResultItem> items = validation.getItems();

    items.stream()
        .filter(v -> v.getValidation().getLevel().equals(WARN))
        .forEach(v -> LOGGER.warn(componentsLocation(v)));

    final List<ValidationResultItem> errors = items.stream()
        .filter(v -> v.getValidation().getLevel().equals(ERROR))
        .collect(toList());

    if (!errors.isEmpty()) {
      throw new ConfigurationException(createStaticMessage(validation.getItems()
          .stream()
          .map(this::componentsLocation)
          .collect(joining(lineSeparator()))));
    }
  }

  private String componentsLocation(ValidationResultItem v) {
    return v.getComponents().stream()
        .map(component -> component.getMetadata().getFileName().orElse("unknown") + ":"
            + component.getMetadata().getStartLine().orElse(-1))
        .collect(joining("; ", "[", "]")) + ": " + v.getMessage();
  }

  protected void registerApplicationExtensionModel() {
    if (!artifactType.equals(APP)) {
      return;
    }

    
  }

  protected void registerErrors(final ArtifactAst artifactAst) {
    doRegisterErrors(artifactAst);
  }

  protected void doRegisterErrors(final ArtifactAst artifactAst) {
    final ErrorTypeRepository errorTypeRepository = artifactAst.getErrorTypeRepository();
    final ErrorTypeLocator errorTypeLocator = createDefaultErrorTypeLocator(errorTypeRepository);

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
                          new PostRegistrationActionsPostProcessor((MuleRegistryHelper) muleContext
                              .getRegistry(), beanFactory),
                          new DiscardedOptionalBeanPostProcessor(optionalObjectsController,
                                                                 (DefaultListableBeanFactory) beanFactory),
                          new LifecycleStatePostProcessor(muleContext.getLifecycleManager().getState()),
                          new ComponentLocatorCreatePostProcessor(componentLocator));

    beanFactory.registerSingleton(OBJECT_MULE_CONTEXT, muleContext);

    prepareObjectProviders();
  }

  protected void prepareObjectProviders() {
    MuleArtifactObjectProvider muleArtifactObjectProvider = new MuleArtifactObjectProvider(this);
    ImmutableObjectProviderConfiguration providerConfiguration =
        new ImmutableObjectProviderConfiguration(configurationProperties, muleArtifactObjectProvider);
    for (ConfigurableObjectProvider objectProvider : objectProviders) {
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
      super.close();
      disposeIfNeeded(configurationProperties.getConfigurationPropertiesResolver(), LOGGER);
    }
  }

  @Override
  protected void loadBeanDefinitions(DefaultListableBeanFactory beanFactory) throws IOException {
    createApplicationComponents(beanFactory, applicationModel, true);
  }

  @Override
  public void destroy() {
    try {
      super.destroy();
    } catch (Exception e) {
      for (ObjectProvider objectProvider : objectProviders) {
        disposeIfNeeded(objectProvider, LOGGER);
      }
      throw new MuleRuntimeException(e);
    }
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
        .filter(cm -> cm.getModel(HasStereotypeModel.class)
            .map(stm -> stm.getStereotype().isAssignableTo(APP_CONFIG))
            .orElse(false))
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
        .forEach(this.objectProviders::add);
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
    String nameAttribute = resolvedComponentModel.getComponent().getComponentId().orElse(null);
    if (resolvedComponentModel.getComponent().getIdentifier().equals(CONFIGURATION_IDENTIFIER)) {
      nameAttribute = OBJECT_MULE_CONFIGURATION;
    } else if (nameAttribute == null) {
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
                                                    getArtifactType(),
                                                    getOptionalObjectsController(),
                                                    beanFactory,
                                                    getServiceDiscoverer(),
                                                    getResourceLocator(),
                                                    memoryManagementService);
  }

  @Override
  protected void postProcessBeanFactory(ConfigurableListableBeanFactory beanFactory) {
    Optional<ComponentAst> configurationOptional = findComponentDefinitionModel(applicationModel, CONFIGURATION_IDENTIFIER);
    if (configurationOptional.isPresent()) {
      return;
    }
    BeanDefinitionRegistry beanDefinitionRegistry = (BeanDefinitionRegistry) beanFactory;
    beanDefinitionRegistry.registerBeanDefinition(OBJECT_MULE_CONFIGURATION,
                                                  genericBeanDefinition(MuleConfigurationConfigurator.class).getBeanDefinition());
  }

  private void registerAnnotationConfigProcessors(BeanDefinitionRegistry registry, ConfigurableListableBeanFactory beanFactory) {
    registerAnnotationConfigProcessor(registry, CONFIGURATION_ANNOTATION_PROCESSOR_BEAN_NAME,
                                      ConfigurationClassPostProcessor.class, null);
    registerAnnotationConfigProcessor(registry, REQUIRED_ANNOTATION_PROCESSOR_BEAN_NAME,
                                      RequiredAnnotationBeanPostProcessor.class, null);
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

  private void registerAnnotationConfigProcessor(BeanDefinitionRegistry registry, String key, Class<?> type, Object source) {
    RootBeanDefinition beanDefinition = new RootBeanDefinition(type);
    beanDefinition.setSource(source);
    registerPostProcessor(registry, beanDefinition, key);
  }

  protected void registerPostProcessor(BeanDefinitionRegistry registry, RootBeanDefinition definition, String beanName) {
    definition.setRole(BeanDefinition.ROLE_INFRASTRUCTURE);
    registry.registerBeanDefinition(beanName, definition);
  }

  @Override
  protected DefaultListableBeanFactory createBeanFactory() {
    // Copy all postProcessors defined in the defaultMuleConfig so that they get applied to the child container
    DefaultListableBeanFactory beanFactory = new ObjectProviderAwareBeanFactory(getInternalParentBeanFactory());
    beanFactory.setAutowireCandidateResolver(new ContextAnnotationAutowireCandidateResolver());
    beanFactory.setInstantiationStrategy(new LaxInstantiationStrategyWrapper(new CglibSubclassingInstantiationStrategy(),
                                                                             optionalObjectsController));

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
   * Forces the registration of instances of {@link TransformerResolver} and {@link Converter} to be created, so that
   * {@link PostRegistrationActionsPostProcessor} can work its magic and add them to the transformation graph
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

  private Set<ExtensionModel> getExtensions() {
    return extensionManager == null ? emptySet() : extensionManager.getExtensions();
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
}
