/*
 * Copyright 2023 Salesforce, Inc. All rights reserved.
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.config.internal.context.lazy;

import static org.mule.runtime.api.component.TypedComponentIdentifier.ComponentType.OPERATION;
import static org.mule.runtime.api.component.TypedComponentIdentifier.ComponentType.SCOPE;
import static org.mule.runtime.api.component.TypedComponentIdentifier.ComponentType.SOURCE;
import static org.mule.runtime.api.component.TypedComponentIdentifier.ComponentType.SUB_FLOW;
import static org.mule.runtime.api.i18n.I18nMessageFactory.createStaticMessage;
import static org.mule.runtime.api.util.Preconditions.checkState;
import static org.mule.runtime.ast.api.util.MuleAstUtils.filteredArtifactAst;
import static org.mule.runtime.ast.api.util.MuleAstUtils.resolveOrphanComponents;
import static org.mule.runtime.ast.graph.api.ArtifactAstDependencyGraphFactory.generateFor;
import static org.mule.runtime.config.internal.parsers.generic.AutoIdUtils.uniqueValue;
import static org.mule.runtime.core.api.config.MuleDeploymentProperties.MULE_LAZY_INIT_ENABLE_DSL_DECLARATION_VALIDATIONS_DEPLOYMENT_PROPERTY;
import static org.mule.runtime.core.api.config.MuleProperties.OBJECT_SECURITY_MANAGER;
import static org.mule.runtime.core.api.config.MuleProperties.OBJECT_TRANSACTION_MANAGER;
import static org.mule.runtime.core.api.lifecycle.LifecycleUtils.initialiseIfNeeded;
import static org.mule.runtime.core.api.util.ClassUtils.withContextClassLoader;
import static org.mule.runtime.core.privileged.registry.LegacyRegistryUtils.registerObject;
import static org.mule.runtime.core.privileged.registry.LegacyRegistryUtils.unregisterObject;

import static java.lang.String.format;
import static java.lang.ThreadLocal.withInitial;
import static java.util.Arrays.asList;
import static java.util.Collections.emptyList;
import static java.util.Collections.sort;

import org.mule.runtime.api.component.ConfigurationProperties;
import org.mule.runtime.api.component.location.Location;
import org.mule.runtime.api.config.FeatureFlaggingService;
import org.mule.runtime.api.exception.MuleException;
import org.mule.runtime.api.exception.MuleRuntimeException;
import org.mule.runtime.api.functional.Either;
import org.mule.runtime.api.ioc.ConfigurableObjectProvider;
import org.mule.runtime.api.lifecycle.Initialisable;
import org.mule.runtime.api.lifecycle.InitialisationException;
import org.mule.runtime.api.lifecycle.LifecycleException;
import org.mule.runtime.api.lifecycle.Startable;
import org.mule.runtime.api.memory.management.MemoryManagementService;
import org.mule.runtime.api.metadata.ExpressionLanguageMetadataService;
import org.mule.runtime.api.util.Pair;
import org.mule.runtime.ast.api.ArtifactAst;
import org.mule.runtime.ast.api.ComponentAst;
import org.mule.runtime.ast.graph.api.ArtifactAstDependencyGraph;
import org.mule.runtime.config.api.dsl.model.ComponentBuildingDefinitionRegistry;
import org.mule.runtime.config.internal.context.BaseConfigurationComponentLocator;
import org.mule.runtime.config.internal.context.MuleArtifactContext;
import org.mule.runtime.config.internal.context.SpringConfigurationComponentLocator;
import org.mule.runtime.config.internal.context.SpringMuleContextServiceConfigurator;
import org.mule.runtime.config.internal.dsl.model.SpringComponentModel;
import org.mule.runtime.config.internal.model.ComponentModelInitializer;
import org.mule.runtime.config.internal.validation.IgnoreOnLazyInit;
import org.mule.runtime.core.api.MuleContext;
import org.mule.runtime.core.api.config.ConfigurationException;
import org.mule.runtime.core.api.config.bootstrap.ArtifactType;
import org.mule.runtime.core.api.security.SecurityManager;
import org.mule.runtime.core.internal.exception.ContributedErrorTypeLocator;
import org.mule.runtime.core.internal.exception.ContributedErrorTypeRepository;
import org.mule.runtime.core.internal.transaction.TransactionManagerFactory;
import org.mule.runtime.core.privileged.processor.chain.MessageProcessorChain;
import org.mule.runtime.core.privileged.processor.chain.MessageProcessorChainBuilder;
import org.mule.runtime.core.privileged.registry.RegistrationException;
import org.mule.runtime.extension.api.runtime.config.ConfigurationProvider;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.function.Predicate;
import java.util.function.Supplier;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.BeansException;
import org.springframework.beans.factory.NoSuchBeanDefinitionException;
import org.springframework.beans.factory.config.BeanDefinition;
import org.springframework.beans.factory.config.ConfigurableListableBeanFactory;
import org.springframework.beans.factory.support.DefaultListableBeanFactory;
import org.springframework.core.ResolvableType;
import org.springframework.lang.Nullable;

/**
 * Implementation of {@link MuleArtifactContext} that allows to create configuration components lazily.
 * <p/>
 * Components will be created upon request to use the from the exposed services.
 *
 * @since 4.0
 */
public class LazyMuleArtifactContext extends MuleArtifactContext
    implements LazyComponentInitializerAdapter, ComponentModelInitializer {

  private static final Logger LOGGER = LoggerFactory.getLogger(LazyMuleArtifactContext.class);

  private final boolean dslDeclarationValidationEnabled;

  private final Optional<ComponentModelInitializer> parentComponentModelInitializer;

  // postProcessedGraph is used when we get locations and initialize some components
  // while baseGraph is used when we validate Ast during lazy-init.
  private final ArtifactAstDependencyGraph postProcessedGraph;
  private final ArtifactAstDependencyGraph baseGraph;
  private final ComponentInitializationState currentComponentInitializationState;

  // Used for detecting cycles when initializing beans that are dynamically referenced
  private final ThreadLocal<Set<String>> beanNamesBeingInitialized = withInitial(HashSet::new);

  private final Map<String, String> artifactProperties;

  /**
   * Parses configuration files creating a spring ApplicationContext which is used as a parent registry using the SpringRegistry
   * registry implementation to wraps the spring ApplicationContext
   *
   * @param muleContext                                the {@link MuleContext} that own this context
   * @param artifactAst                                the definition of the artifact to create a context for
   * @param parentConfigurationProperties              the resolver for properties from the parent artifact to be used as fallback
   *                                                   in this artifact.
   * @param baseConfigurationComponentLocator          indirection to the actual ConfigurationComponentLocator in the full
   *                                                   registry
   * @param errorTypeRepository                        repository where the errors of the artifact will be registered.
   * @param errorTypeLocator                           locator where the errors of the artifact will be registered.
   * @param artifactProperties                         map of properties that can be referenced from the
   *                                                   {@code artifactConfigResources} as external configuration values
   * @param artifactType                               the type of artifact to determine the base objects of the created context.
   * @param parentComponentModelInitializer
   * @param componentBuildingDefinitionRegistryFactory
   * @param featureFlaggingService
   * @since 4.0
   */
  public LazyMuleArtifactContext(MuleContext muleContext, ArtifactAst artifactAst,
                                 Optional<ConfigurationProperties> parentConfigurationProperties,
                                 BaseConfigurationComponentLocator baseConfigurationComponentLocator,
                                 ContributedErrorTypeRepository errorTypeRepository,
                                 ContributedErrorTypeLocator errorTypeLocator,
                                 Map<String, String> artifactProperties,
                                 ArtifactType artifactType,
                                 Optional<ComponentModelInitializer> parentComponentModelInitializer,
                                 ComponentBuildingDefinitionRegistry componentBuildingDefinitionRegistry,
                                 MemoryManagementService memoryManagementService,
                                 FeatureFlaggingService featureFlaggingService,
                                 ExpressionLanguageMetadataService expressionLanguageMetadataService)
      throws BeansException {
    super(muleContext, artifactAst, parentConfigurationProperties,
          baseConfigurationComponentLocator, errorTypeRepository, errorTypeLocator,
          artifactProperties,
          artifactType, componentBuildingDefinitionRegistry, memoryManagementService,
          featureFlaggingService, expressionLanguageMetadataService);

    // Changes the component locator in order to allow accessing any component by location even when they are prototype
    this.componentLocator = new SpringConfigurationComponentLocator();
    this.currentComponentInitializationState = new ComponentInitializationState(componentLocator);

    this.parentComponentModelInitializer = parentComponentModelInitializer;

    this.dslDeclarationValidationEnabled = Boolean.valueOf(artifactProperties
        .getOrDefault(MULE_LAZY_INIT_ENABLE_DSL_DECLARATION_VALIDATIONS_DEPLOYMENT_PROPERTY, Boolean.FALSE.toString()));

    this.artifactProperties = artifactProperties;

    this.baseGraph = generateFor(getApplicationModel());

    initialize();
    // Graph should be generated after the initialize() method since the applicationModel will change by macro expanding XmlSdk
    // components.
    this.postProcessedGraph = generateFor(getApplicationModel());
  }

  @Override
  protected SpringMuleContextServiceConfigurator createServiceConfigurator(DefaultListableBeanFactory beanFactory) {
    return new LazySpringMuleContextServiceConfigurator(this,
                                                        artifactProperties,
                                                        getMuleContext(),
                                                        getCoreFunctionsProvider(),
                                                        getConfigurationProperties(),
                                                        getArtifactType(),
                                                        getApplicationModel(),
                                                        beanFactory,
                                                        getServiceDiscoverer(),
                                                        getResourceLocator(),
                                                        memoryManagementService);
  }

  @Override
  protected void validateArtifact(ArtifactAst artifactAst) {
    // Nothing to do, validation is done after calculating the minimal artifact in #createComponents
  }

  @Override
  protected void registerErrors(ArtifactAst artifactAst) {
    // Nothing to do, errorType repository is done after calculating the minimal artifact in #createComponents
  }

  @Override
  protected void prepareBeanFactory(ConfigurableListableBeanFactory beanFactory) {
    super.prepareBeanFactory(beanFactory);
    currentComponentInitializationState.registerTrackingPostProcessor(beanFactory);
  }

  @Override
  public Object getBean(String name) throws BeansException {
    return tryWithLazyInitializationFallback(name, () -> super.getBean(name));
  }

  @Override
  public <T> T getBean(String name, Class<T> requiredType) throws BeansException {
    return tryWithLazyInitializationFallback(name, () -> super.getBean(name, requiredType));
  }

  @Override
  public boolean containsBean(String name) {
    return tryWithLazyInitializationFallback(name, () -> super.containsBean(name));
  }

  @Override
  public boolean isSingleton(String name) throws NoSuchBeanDefinitionException {
    return tryWithLazyInitializationFallback(name, () -> super.isSingleton(name));
  }

  @Override
  public boolean isPrototype(String name) throws NoSuchBeanDefinitionException {
    return tryWithLazyInitializationFallback(name, () -> super.isPrototype(name));
  }

  @Override
  public boolean isTypeMatch(String name, ResolvableType typeToMatch) throws NoSuchBeanDefinitionException {
    return tryWithLazyInitializationFallback(name, () -> super.isTypeMatch(name, typeToMatch));
  }

  @Override
  public boolean isTypeMatch(String name, Class<?> typeToMatch) throws NoSuchBeanDefinitionException {
    return tryWithLazyInitializationFallback(name, () -> super.isTypeMatch(name, typeToMatch));
  }

  @Override
  @Nullable
  public Class<?> getType(String name) throws NoSuchBeanDefinitionException {
    return tryWithLazyInitializationFallback(name, () -> super.getType(name));
  }

  @Override
  @Nullable
  public Class<?> getType(String name, boolean allowFactoryBeanInit) throws NoSuchBeanDefinitionException {
    return tryWithLazyInitializationFallback(name, () -> super.getType(name, allowFactoryBeanInit));
  }

  @Override
  public String[] getAliases(String name) {
    return tryWithLazyInitializationFallback(name, () -> super.getAliases(name));
  }

  private <T> T tryWithLazyInitializationFallback(String name, Supplier<T> supplier) {
    // It is possible that the operation on the bean factory failed because the bean definition was not yet registered.
    // We will try to do that now.
    // If it fails, we will re-throw the original exception as if the lazy initialization attempt never occurred.
    try {
      T returnValue = supplier.get();
      // Some BeanFactory implementations return null instead of NoSuchBeanDefinitionException.
      if (returnValue == null) {
        // This Either may be a right, but we still want to return null in that case as did the original supplier.
        return initializeAndRetry(name, supplier).getLeft();
      }
      return returnValue;
    } catch (NoSuchBeanDefinitionException e) {
      Either<T, Throwable> result = initializeAndRetry(name, supplier);
      if (result.isRight()) {
        // Rethrows the original exception.
        throw e;
      }
      return result.getLeft();
    }
  }

  private synchronized <T> Either<T, Throwable> initializeAndRetry(String name, Supplier<T> supplier) {
    // Re-checks for bean existence in the registry again before attempting initialization.
    // This is because some other thread may have initialized the bean already.
    try {
      T returnValue = supplier.get();
      if (returnValue == null) {
        return doInitializeAndRetry(name, supplier);
      }
      return Either.left(returnValue);
    } catch (NoSuchBeanDefinitionException e) {
      return doInitializeAndRetry(name, supplier);
    }
  }

  private <T> Either<T, Throwable> doInitializeAndRetry(String name, Supplier<T> supplier) {
    // Checks for cycles
    if (beanNamesBeingInitialized.get().contains(name)) {
      throw new IllegalArgumentException(format("A cyclic dependency was found when trying to initialize bean '%s'.", name));
    }

    // Tries to initialize a component with a componentId matching the bean name
    try {
      beanNamesBeingInitialized.get().add(name);
      initializeAdditionalComponent(componentAst -> componentAst.getComponentId().map(id -> id.equals(name)).orElse(false));
    } catch (Exception initializationException) {
      return Either.right(initializationException);
    } finally {
      beanNamesBeingInitialized.get().remove(name);
    }
    return Either.left(supplier.get());
  }

  private void applyLifecycle(List<Object> components, boolean applyStartPhase) {
    getMuleContext().withLifecycleLock(() -> {
      if (getMuleContext().isInitialised()) {
        initializeComponents(components);
      }
      if (getMuleContext().isStarted()) {
        if (applyStartPhase) {
          startComponent(components);
        } else {
          startConfigurationProviders(components);
        }
      }
    });
  }

  /**
   * Starts {@link ConfigurationProvider} components as they should be started no matter if the request has set to not apply start
   * phase in the rest of the components.
   *
   * @param components list of components created
   */
  private void startConfigurationProviders(List<Object> components) {
    components.stream()
        .filter(ConfigurationProvider.class::isInstance)
        .forEach(configurationProviders -> {
          try {
            getMuleRegistry().applyLifecycle(configurationProviders, Initialisable.PHASE_NAME, Startable.PHASE_NAME);
          } catch (MuleException e) {
            throw new MuleRuntimeException(e);
          }
        });
  }

  private void initializeComponents(List<Object> components) {
    for (Object object : components) {
      LOGGER.debug("Initializing component '{}'...", object);
      try {
        if (object instanceof MessageProcessorChain) {
          // When created it will be initialized
        } else {
          doInitializeComponent(object);
        }
      } catch (MuleException e) {
        throw new MuleRuntimeException(e);
      }
    }
  }

  private void doInitializeComponent(Object component) throws LifecycleException, MuleException {
    try {
      component = getMuleContext().getInjector().inject(component);
    } catch (MuleException e) {
      throw new LifecycleException(e, component);
    }
    getMuleRegistry().applyLifecycle(component, Initialisable.PHASE_NAME);
  }

  private void startComponent(List<Object> components) {
    for (Object object : components) {
      LOGGER.debug("Starting component '{}'...", object);
      try {
        if (object instanceof MessageProcessorChain) {
          // Has to be ignored as when it is registered it will be started too
        } else {
          getMuleRegistry().applyLifecycle(object, Initialisable.PHASE_NAME, Startable.PHASE_NAME);
        }
      } catch (MuleException e) {
        throw new MuleRuntimeException(e);
      }
    }
  }

  @Override
  public void initializeComponent(Location location) {
    initializeComponent(location, true);
  }

  @Override
  public void initializeComponents(ComponentLocationFilter filter) {
    initializeComponents(filter, true);
  }

  private void initializeAdditionalComponent(Predicate<ComponentAst> componentModelPredicate) {
    checkState(currentComponentInitializationState.isInitializationAlreadyDone(),
               "initializeComponents must have been called before initializeAdditionalComponent");

    // For the time being we only support going for the same "apply start phase" setting as in the original initialization
    // request.
    createComponentsAndApplyLifecycle(getRequestBuilder(currentComponentInitializationState.isApplyStartPhaseRequested(),
                                                        true).build(componentModelPredicate));
  }

  @Override
  public void initializeComponent(Location location, boolean applyStartPhase) {
    createComponentsAndApplyLifecycle(getRequestBuilder(applyStartPhase, false).build(location));
  }

  @Override
  public void initializeComponents(ComponentLocationFilter filter, boolean applyStartPhase) {
    createComponentsAndApplyLifecycle(getRequestBuilder(applyStartPhase, false).build(filter));
  }

  @Override
  public void initializeComponents(Predicate<ComponentAst> componentModelPredicate, boolean applyStartPhase) {
    createComponentsAndApplyLifecycle(getRequestBuilder(applyStartPhase, false).build(componentModelPredicate));
  }

  private ComponentInitializationRequest.Builder getRequestBuilder(boolean applyStartPhase, boolean keepPrevious) {
    return new ComponentInitializationRequest.Builder(postProcessedGraph,
                                                      baseGraph,
                                                      currentComponentInitializationState,
                                                      MuleArtifactContext::isAlwaysEnabledComponent,
                                                      applyStartPhase,
                                                      keepPrevious);
  }

  private void createComponentsAndApplyLifecycle(ComponentInitializationRequest initializationRequest) {
    applyLifecycle(createComponents(initializationRequest,
                                    getParentComponentModelInitializerAdapter(initializationRequest
                                        .isApplyStartPhaseRequested())),
                   initializationRequest.isApplyStartPhaseRequested());
  }

  public Optional<ComponentModelInitializerAdapter> getParentComponentModelInitializerAdapter(boolean applyStartPhase) {
    return parentComponentModelInitializer
        .map(componentModelInitializer -> componentModelPredicate -> componentModelInitializer
            .initializeComponents(componentModelPredicate, applyStartPhase));
  }

  private void validateModel(ArtifactAst artifactAst) throws ConfigurationException {
    if (dslDeclarationValidationEnabled) {
      doValidateModel(artifactAst, v -> v.getClass().getAnnotation(IgnoreOnLazyInit.class) == null
          || v.getClass().getAnnotation(IgnoreOnLazyInit.class).forceDslDeclarationValidation());
    } else {
      doValidateModel(artifactAst, v -> v.getClass().getAnnotation(IgnoreOnLazyInit.class) == null);
    }
  }

  private void initializeComponentsFromParent(Optional<ComponentModelInitializerAdapter> parentComponentModelInitializerAdapter) {
    if (parentComponentModelInitializerAdapter.isPresent()) {
      parentComponentModelInitializerAdapter.get()
          .initializeComponents(componentModel -> postProcessedGraph.getMissingDependencies()
              .stream()
              .anyMatch(missingDep -> missingDep.isSatisfiedBy(componentModel)));
    } else {
      postProcessedGraph.getMissingDependencies().stream().forEach(missingDep -> {
        if (LOGGER.isDebugEnabled()) {
          LOGGER.debug("Ignoring dependency {} because it does not exist.", missingDep);
        }
      });
    }
  }

  private void validateRequestedComponentExists(Location location, ArtifactAst postProcessedMinimalArtifactAst) {
    if (postProcessedMinimalArtifactAst.recursiveStream()
        .noneMatch(comp -> comp.getLocation() != null
            && comp.getLocation().getLocation().equals(location.toString()))) {
      throw new NoSuchComponentModelException(createStaticMessage("No object found at location "
          + location.toString()));
    }
  }

  private void cleanupAndResetComponentsState() {
    // First unregister any already initialized/started component
    unregisterBeans(currentComponentInitializationState.getTrackedBeansInOrder());

    objectProviders.clear();
    resetMuleSecurityManager();
  }

  private void doRegisterErrors(ComponentInitializationRequest initializationRequest) {
    if (currentComponentInitializationState.isAllErrorTypesRegistered()) {
      // Once all error types from the full artifact AST have been registered there is no point in doing it again
      return;
    }

    if (initializationRequest.isKeepPreviousRequested()) {
      // We do not currently have the capability to add error types to the repository incrementally, hence we need to register
      // all error types this time.
      // Note that using the full AST directly does not work the same as a FilteredArtifactAst that filters nothing. This
      // is because the FilteredArtifactAst has some special case for error handlers.
      doRegisterErrors(filteredArtifactAst(getApplicationModel(), c -> true));
      currentComponentInitializationState.setAllErrorTypesRegistered();
    } else {
      // If we do not care about previously registered errors, we can discover the error types just from the filtered AST
      doRegisterErrors(initializationRequest.getFilteredAstToInitialize());
    }
  }

  private List<Object> createComponents(ComponentInitializationRequest initializationRequest,
                                        Optional<ComponentModelInitializerAdapter> parentComponentModelInitializerAdapter) {
    return withContextClassLoader(getMuleContext().getExecutionClassLoader(), () -> {
      // Checks if the current request is compatible with the already created components, so can we avoid doing anything.
      if (currentComponentInitializationState.isRequestSatisfied(initializationRequest)) {
        return emptyList();
      }

      initializationRequest.validateRequestedAst(this::validateModel);
      ArtifactAst minimalAst = initializationRequest.getFilteredAstToInitialize();

      initializationRequest.getLocation()
          .ifPresent(location -> validateRequestedComponentExists(location, minimalAst));

      // We always initialize all components in the parent that are referenced from the *full* AST.
      // For that reason, we don't need to make any particular consideration when "keep previous" is requested.
      // Take that into account if we ever decide to change that.
      initializeComponentsFromParent(parentComponentModelInitializerAdapter);

      if (!initializationRequest.isKeepPreviousRequested()) {
        cleanupAndResetComponentsState();
      }

      // This has to be called after all previous state has been cleared because the unregister/cleanup process requires the
      // errorTypeRepository as it was during its initialization.
      doRegisterErrors(initializationRequest);

      // Remembers the currently requested locations in order to skip future requests if they are compatible.
      currentComponentInitializationState.update(initializationRequest);

      // Registers the bean definitions for the artifact components in the minimal model.
      List<Pair<String, ComponentAst>> components =
          createApplicationComponents((DefaultListableBeanFactory) this.getBeanFactory(), minimalAst, false);

      // Prepares/configures any recently discovered object providers
      prepareObjectProviders(currentComponentInitializationState.takeObjectProvidersToConfigure());

      // Finally, creates the beans corresponding to the requested components.
      LOGGER.debug("Will create beans: {}", components);
      return createBeans(components);
    });
  }

  /**
   * Apart from calling {@link #createApplicationComponents(DefaultListableBeanFactory, ArtifactAst, boolean)} from the
   * superclass, will handle orphan processors. That is, processors that are part of the minimal app but for which the containing
   * flow is not.
   */
  @Override
  protected List<Pair<String, ComponentAst>> doCreateApplicationComponents(DefaultListableBeanFactory beanFactory,
                                                                           ArtifactAst minimalAppModel,
                                                                           boolean mustBeRoot,
                                                                           Map<ComponentAst, SpringComponentModel> springComponentModels) {
    final List<Pair<String, ComponentAst>> applicationComponents =
        super.doCreateApplicationComponents(beanFactory, minimalAppModel, mustBeRoot, springComponentModels);

    final Set<ComponentAst> orphanComponents = resolveOrphanComponents(minimalAppModel);
    LOGGER.debug("orphanComponents found: {}", orphanComponents);

    // Handle orphan named components...
    orphanComponents.stream()
        .filter(cm -> asList(SOURCE, OPERATION, SCOPE, SUB_FLOW).contains(cm.getComponentType()))
        .filter(cm -> cm.getComponentId().isPresent())
        .forEach(cm -> {
          final String nameAttribute = cm.getComponentId().get();
          LOGGER.debug("Registering orphan named component '{}'...", nameAttribute);

          applicationComponents.add(0, new Pair<>(nameAttribute, cm));
          final SpringComponentModel springCompModel = springComponentModels.get(cm);
          final BeanDefinition beanDef = springCompModel.getBeanDefinition();
          if (beanDef != null) {
            beanFactory.registerBeanDefinition(cm.getComponentId().get(), beanDef);
            postProcessBeanDefinition(springCompModel, beanFactory, cm.getComponentId().get());
          }
        });

    // Handle orphan components without name, rely on the location.
    orphanComponents.stream()
        .filter(cm -> !isIgnored(cm))
        .forEach(cm -> {
          final SpringComponentModel springCompModel = springComponentModels.get(cm);
          final BeanDefinition beanDef = springCompModel.getBeanDefinition();
          if (beanDef != null) {
            final String beanName = cm.getComponentId().orElse(uniqueValue(beanDef.getBeanClassName()));

            LOGGER.debug("Registering orphan un-named component '{}'...", beanName);
            applicationComponents.add(new Pair<>(beanName, cm));
            beanFactory.registerBeanDefinition(beanName, beanDef);
            postProcessBeanDefinition(springCompModel, beanFactory, beanName);
          }
        });

    LOGGER.debug("applicationComponents to be created: {}", applicationComponents);
    return applicationComponents;
  }

  /**
   * Creates the beans based on the application component model names that were enabled by the minimal application model. It also
   * populates the list of bean names created and returns the list of beans instantiated, the list of beans is sorted based on
   * dependencies between components (even between configuration components, flow->config and config->config dependencies from the
   * DSL).
   *
   * @param applicationComponentNames name of components to be created.
   * @return List beans created for the given component names sorted by precedence.
   */
  private List<Object> createBeans(List<Pair<String, ComponentAst>> applicationComponentNames) {
    currentComponentInitializationState.startTrackingBeans();
    Map<Pair<String, ComponentAst>, Object> objects = new LinkedHashMap<>();
    // Create beans only once by calling the lookUp at the Registry
    applicationComponentNames.forEach(componentPair -> {
      try {
        Object object = getRegistry().lookupByName(componentPair.getFirst()).orElse(null);
        if (object != null) {
          // MessageProcessorChainBuilder has to be manually created and added to the registry in order to be able
          // to dispose it later
          if (object instanceof MessageProcessorChainBuilder mpcb) {
            handleChainBuilder(mpcb, componentPair, objects);
          } else if (object instanceof TransactionManagerFactory txMgrFy) {
            handleTxManagerFactory(txMgrFy);
          }
          objects.put(componentPair, object);
        }
      } catch (Exception e) {
        currentComponentInitializationState
            .commitTrackedBeansContainedIn(objects.keySet().stream()
                .map(Pair::getFirst)
                .toList());
        safeUnregisterBean(componentPair.getFirst());

        throw new MuleRuntimeException(e);
      }
    });

    // A Map to access the componentName by the bean instance
    Map<Object, Pair<String, ComponentAst>> componentNames = new HashMap<>();
    objects.entrySet().forEach(entry -> {
      Object object = entry.getValue();
      Pair<String, ComponentAst> component = entry.getKey();
      componentNames.put(object, component);
    });

    // TODO: Once is implemented MULE-17778 we should use graph to get the order for disposing beans
    currentComponentInitializationState
        .commitTrackedBeansContainedIn(objects.keySet().stream()
            .map(Pair::getFirst)
            .toList());

    // Sort in order to later initialize and start components according to their dependencies
    List<Object> sortedObjects = new ArrayList<>(objects.values());
    sort(sortedObjects, (o1, o2) -> postProcessedGraph.dependencyComparator().compare(componentNames.get(o1).getSecond(),
                                                                                      componentNames.get(o2).getSecond()));
    return sortedObjects;
  }

  private void handleChainBuilder(MessageProcessorChainBuilder object, Pair<String, ComponentAst> componentPair,
                                  Map<Pair<String, ComponentAst>, Object> objects) {
    Pair<String, ComponentAst> chainKey =
        new Pair<>(componentPair.getFirst() + "@" + object.hashCode(), componentPair.getSecond());
    MessageProcessorChain messageProcessorChain = object.build();
    try {
      initialiseIfNeeded(messageProcessorChain, getMuleContext());
    } catch (InitialisationException e) {
      unregisterBeans(objects.keySet().stream()
          .map(Pair::getFirst)
          .toList());
      throw new IllegalStateException("Couldn't initialise an instance of a MessageProcessorChain", e);
    }
    try {
      getMuleRegistry().registerObject(chainKey.getFirst(), messageProcessorChain);
    } catch (RegistrationException e) {
      // Unregister any already created component
      unregisterBeans(objects.keySet().stream()
          .map(Pair::getFirst)
          .toList());
      throw new IllegalStateException("Couldn't register an instance of a MessageProcessorChain", e);
    }
    objects.put(chainKey, messageProcessorChain);
  }

  private void handleTxManagerFactory(TransactionManagerFactory object) {
    try {
      registerObject(getMuleContext(), OBJECT_TRANSACTION_MANAGER,
                     object.create(getMuleContext().getConfiguration()));
    } catch (Exception e) {
      throw new IllegalStateException("Couldn't register an instance of a TransactionManager", e);
    }
  }

  private void resetMuleSecurityManager() {
    SecurityManager securityManager = getMuleRegistry().get(OBJECT_SECURITY_MANAGER);

    if (securityManager != null) {
      securityManager.getProviders().forEach(p -> securityManager.removeProvider(p.getName()));
      securityManager.getEncryptionStrategies().forEach(s -> securityManager.removeEncryptionStrategy(s.getName()));
    }
  }

  @Override
  protected void onObjectProviderDiscovered(ConfigurableObjectProvider objectProvider) {
    super.onObjectProviderDiscovered(objectProvider);
    currentComponentInitializationState.registerObjectProviderToConfigure(objectProvider);
  }

  @Override
  public void close() {
    beanNamesBeingInitialized.remove();
    currentComponentInitializationState.clear();
    super.close();
  }

  private void unregisterBeans(List<String> beans) {
    doUnregisterBeans(new LinkedList<>(beans).descendingIterator());
    componentLocator.removeComponents();
  }

  /**
   * Apply the stop and dispose phases and unregister the bean from the registry. The phases are applied to each bean at a time.
   *
   * @param beanNames {@link Iterator} of bean names to be stopped, disposed and unregistered.
   */
  private void doUnregisterBeans(Iterator<String> beanNames) {
    while (beanNames.hasNext()) {
      String beanName = beanNames.next();
      try {
        unregisterObject(getMuleContext(), beanName);
      } catch (Exception e) {
        LOGGER.error(
                     format("Exception unregistering an object during lazy initialization of component %s, exception message is %s",
                            beanName, e.getMessage()));
        throw new MuleRuntimeException(createStaticMessage("There was an error while unregistering component '%s'", beanName), e);
      }
    }
  }

  private void safeUnregisterBean(String beanName) {
    try {
      unregisterObject(getMuleContext(), beanName);
    } catch (RegistrationException e) {
      // Nothing to do...
    }
  }

  /*
   * Just register the locations, do not do any initialization!
   */
  @Override
  protected void loadBeanDefinitions(DefaultListableBeanFactory beanFactory) {
    getApplicationModel()
        .filteredComponents(cm -> !isIgnored(cm))
        .forEach(cm -> componentLocator.addComponentLocation(cm.getLocation()));
  }

  /**
   * Adapter for {@link ComponentModelInitializer} that hides the lifecycle phase from component model creation logic.
   */
  @FunctionalInterface
  private interface ComponentModelInitializerAdapter {

    void initializeComponents(Predicate<ComponentAst> componentModelPredicate);

  }

}
