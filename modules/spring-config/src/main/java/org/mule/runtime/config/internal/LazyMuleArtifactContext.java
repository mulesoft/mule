/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.config.internal;

import static com.google.common.collect.ImmutableSet.copyOf;
import static com.google.common.collect.Sets.newHashSet;
import static java.util.Arrays.asList;
import static java.util.Collections.emptyList;
import static java.util.Collections.sort;
import static java.util.Optional.empty;
import static java.util.Optional.of;
import static java.util.stream.Collectors.toCollection;
import static java.util.stream.Collectors.toList;
import static java.util.stream.Collectors.toSet;
import static org.apache.commons.lang3.exception.ExceptionUtils.hasCause;
import static org.mule.runtime.api.component.TypedComponentIdentifier.ComponentType.OPERATION;
import static org.mule.runtime.api.component.TypedComponentIdentifier.ComponentType.SCOPE;
import static org.mule.runtime.api.component.TypedComponentIdentifier.ComponentType.SOURCE;
import static org.mule.runtime.api.connectivity.ConnectivityTestingService.CONNECTIVITY_TESTING_SERVICE_KEY;
import static org.mule.runtime.api.i18n.I18nMessageFactory.createStaticMessage;
import static org.mule.runtime.api.metadata.MetadataService.METADATA_SERVICE_KEY;
import static org.mule.runtime.api.metadata.MetadataService.NON_LAZY_METADATA_SERVICE_KEY;
import static org.mule.runtime.api.store.ObjectStoreManager.BASE_IN_MEMORY_OBJECT_STORE_KEY;
import static org.mule.runtime.api.util.Preconditions.checkState;
import static org.mule.runtime.api.value.ValueProviderService.VALUE_PROVIDER_SERVICE_KEY;
import static org.mule.runtime.ast.api.util.MuleAstUtils.resolveOrphanComponents;
import static org.mule.runtime.ast.graph.api.ArtifactAstDependencyGraphFactory.generateFor;
import static org.mule.runtime.config.internal.LazyConnectivityTestingService.NON_LAZY_CONNECTIVITY_TESTING_SERVICE;
import static org.mule.runtime.config.internal.LazyValueProviderService.NON_LAZY_VALUE_PROVIDER_SERVICE;
import static org.mule.runtime.config.internal.dsl.model.extension.xml.MacroExpansionModuleModel.DEFAULT_GLOBAL_ELEMENTS;
import static org.mule.runtime.config.internal.parsers.generic.AutoIdUtils.uniqueValue;
import static org.mule.runtime.core.api.config.MuleDeploymentProperties.MULE_LAZY_INIT_DEPLOYMENT_PROPERTY;
import static org.mule.runtime.core.api.config.MuleProperties.OBJECT_SECURITY_MANAGER;
import static org.mule.runtime.core.api.lifecycle.LifecycleUtils.initialiseIfNeeded;
import static org.mule.runtime.core.api.util.ClassUtils.withContextClassLoader;
import static org.mule.runtime.core.internal.metadata.cache.MetadataCacheManager.METADATA_CACHE_MANAGER_KEY;
import static org.mule.runtime.core.internal.store.SharedPartitionedPersistentObjectStore.SHARED_PERSISTENT_OBJECT_STORE_KEY;
import static org.mule.runtime.core.privileged.registry.LegacyRegistryUtils.unregisterObject;

import org.mule.runtime.api.component.ConfigurationProperties;
import org.mule.runtime.api.component.location.Location;
import org.mule.runtime.api.config.custom.CustomizationService;
import org.mule.runtime.api.connectivity.ConnectivityTestingService;
import org.mule.runtime.api.exception.MuleException;
import org.mule.runtime.api.exception.MuleRuntimeException;
import org.mule.runtime.api.i18n.I18nMessageFactory;
import org.mule.runtime.api.lifecycle.Initialisable;
import org.mule.runtime.api.lifecycle.InitialisationException;
import org.mule.runtime.api.lifecycle.Startable;
import org.mule.runtime.api.lock.LockFactory;
import org.mule.runtime.api.metadata.MetadataService;
import org.mule.runtime.api.store.ObjectStoreManager;
import org.mule.runtime.api.util.Pair;
import org.mule.runtime.api.value.ValueProviderService;
import org.mule.runtime.app.declaration.api.ArtifactDeclaration;
import org.mule.runtime.ast.api.ArtifactAst;
import org.mule.runtime.ast.api.ComponentAst;
import org.mule.runtime.ast.graph.api.ArtifactAstDependencyGraph;
import org.mule.runtime.config.internal.dsl.model.NoSuchComponentModelException;
import org.mule.runtime.config.internal.dsl.model.SpringComponentModel;
import org.mule.runtime.config.internal.dsl.processor.ObjectTypeVisitor;
import org.mule.runtime.config.internal.model.ComponentModel;
import org.mule.runtime.core.api.MuleContext;
import org.mule.runtime.core.api.config.bootstrap.ArtifactType;
import org.mule.runtime.core.api.transaction.TransactionManagerFactory;
import org.mule.runtime.core.internal.connectivity.DefaultConnectivityTestingService;
import org.mule.runtime.core.internal.metadata.MuleMetadataService;
import org.mule.runtime.core.internal.metadata.cache.DefaultPersistentMetadataCacheManager;
import org.mule.runtime.core.internal.metadata.cache.DelegateMetadataCacheManager;
import org.mule.runtime.core.internal.security.DefaultMuleSecurityManager;
import org.mule.runtime.core.internal.store.SharedPartitionedPersistentObjectStore;
import org.mule.runtime.core.internal.util.store.MuleObjectStoreManager;
import org.mule.runtime.core.internal.value.MuleValueProviderService;
import org.mule.runtime.core.privileged.processor.chain.MessageProcessorChain;
import org.mule.runtime.core.privileged.processor.chain.MessageProcessorChainBuilder;
import org.mule.runtime.core.privileged.registry.RegistrationException;
import org.mule.runtime.dsl.api.ConfigResource;
import org.mule.runtime.dsl.api.component.ComponentBuildingDefinition;
import org.mule.runtime.dsl.api.component.ComponentBuildingDefinitionProvider;
import org.mule.runtime.extension.api.runtime.config.ConfigurationProvider;

import java.io.File;
import java.io.IOException;
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
import java.util.Spliterator;
import java.util.function.Predicate;
import java.util.stream.Stream;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.BeansException;
import org.springframework.beans.factory.NoSuchBeanDefinitionException;
import org.springframework.beans.factory.config.BeanDefinition;
import org.springframework.beans.factory.config.ConfigurableListableBeanFactory;
import org.springframework.beans.factory.support.DefaultListableBeanFactory;

/**
 * Implementation of {@link MuleArtifactContext} that allows to create configuration components lazily.
 * <p/>
 * Components will be created upon request to use the from the exposed services.
 *
 * @since 4.0
 */
public class LazyMuleArtifactContext extends MuleArtifactContext
    implements LazyComponentInitializerAdapter, ComponentModelInitializer {

  public static final String SHARED_PARTITIONED_PERSISTENT_OBJECT_STORE_PATH = "_sharedPartitionatedPersistentObjectStorePath";

  private static final Logger LOGGER = LoggerFactory.getLogger(LazyMuleArtifactContext.class);
  private static final String DEFAULT_METADATA_CACHE_MANAGER_KEY = "_defaultPersistentMetadataCacheManager";
  private static final String LAZY_MULE_OBJECT_STORE_MANAGER = "_muleLazyObjectStoreManager";

  private TrackingPostProcessor trackingPostProcessor;

  private final Optional<ComponentModelInitializer> parentComponentModelInitializer;

  private final ArtifactAstDependencyGraph graph;

  private final Set<String> currentComponentLocationsRequested = new HashSet<>();
  private boolean appliedStartedPhaseRequest = false;

  /**
   * Parses configuration files creating a spring ApplicationContext which is used as a parent registry using the SpringRegistry
   * registry implementation to wraps the spring ApplicationContext
   *
   * @param muleContext the {@link MuleContext} that own this context
   * @param artifactDeclaration the mule configuration defined programmatically
   * @param optionalObjectsController the {@link OptionalObjectsController} to use. Cannot be {@code null} @see
   *        org.mule.runtime.config.internal.SpringRegistry
   * @param parentConfigurationProperties
   * @param disableXmlValidations {@code true} when loading XML configs it will not apply validations.
   * @param runtimeComponentBuildingDefinitionProvider provider for the runtime
   *        {@link org.mule.runtime.dsl.api.component.ComponentBuildingDefinition}s
   * @since 4.0
   */
  public LazyMuleArtifactContext(MuleContext muleContext, ConfigResource[] artifactConfigResources,
                                 ArtifactDeclaration artifactDeclaration, OptionalObjectsController optionalObjectsController,
                                 Map<String, String> artifactProperties, ArtifactType artifactType,
                                 List<ClassLoader> pluginsClassLoaders,
                                 Optional<ComponentModelInitializer> parentComponentModelInitializer,
                                 Optional<ConfigurationProperties> parentConfigurationProperties, boolean disableXmlValidations,
                                 ComponentBuildingDefinitionProvider runtimeComponentBuildingDefinitionProvider,
                                 LockFactory runtimeLockFactory)
      throws BeansException {
    super(muleContext, artifactConfigResources, artifactDeclaration, optionalObjectsController,
          extendArtifactProperties(artifactProperties), artifactType, pluginsClassLoaders, parentConfigurationProperties,
          disableXmlValidations, runtimeComponentBuildingDefinitionProvider);

    // Changes the component locator in order to allow accessing any component by location even when they are prototype
    this.componentLocator = new SpringConfigurationComponentLocator();

    this.parentComponentModelInitializer = parentComponentModelInitializer;

    final CustomizationService customizationService = muleContext.getCustomizationService();
    customizationService.overrideDefaultServiceImpl(CONNECTIVITY_TESTING_SERVICE_KEY,
                                                    new LazyConnectivityTestingService(this, () -> getRegistry()
                                                        .<ConnectivityTestingService>lookupByName(NON_LAZY_CONNECTIVITY_TESTING_SERVICE)
                                                        .get()));
    customizationService.registerCustomServiceClass(NON_LAZY_CONNECTIVITY_TESTING_SERVICE,
                                                    DefaultConnectivityTestingService.class);
    customizationService.overrideDefaultServiceImpl(METADATA_SERVICE_KEY,
                                                    new LazyMetadataService(this, () -> getRegistry()
                                                        .<MetadataService>lookupByName(NON_LAZY_METADATA_SERVICE_KEY)
                                                        .get()));
    customizationService.registerCustomServiceClass(NON_LAZY_METADATA_SERVICE_KEY, MuleMetadataService.class);
    customizationService.overrideDefaultServiceImpl(VALUE_PROVIDER_SERVICE_KEY,
                                                    new LazyValueProviderService(this, () -> getRegistry()
                                                        .<ValueProviderService>lookupByName(NON_LAZY_VALUE_PROVIDER_SERVICE)
                                                        .get(), muleContext::getConfigurationComponentLocator));
    customizationService.registerCustomServiceClass(NON_LAZY_VALUE_PROVIDER_SERVICE,
                                                    MuleValueProviderService.class);

    customizationService.overrideDefaultServiceImpl(LAZY_COMPONENT_INITIALIZER_SERVICE_KEY, this);

    String sharedPartitionatedPersistentObjectStorePath = artifactProperties.get(SHARED_PARTITIONED_PERSISTENT_OBJECT_STORE_PATH);
    if (sharedPartitionatedPersistentObjectStorePath != null) {
      // We need to first define this service so it would be later initialized
      muleContext.getCustomizationService().registerCustomServiceClass(SHARED_PERSISTENT_OBJECT_STORE_KEY,
                                                                       SharedPartitionedPersistentObjectStore.class);
      muleContext.getCustomizationService().overrideDefaultServiceImpl(SHARED_PERSISTENT_OBJECT_STORE_KEY,
                                                                       new SharedPartitionedPersistentObjectStore<>(new File(sharedPartitionatedPersistentObjectStorePath),
                                                                                                                    runtimeLockFactory));
      MuleObjectStoreManager osm = new MuleObjectStoreManager();
      osm.setBasePersistentStoreKey(SHARED_PERSISTENT_OBJECT_STORE_KEY);
      osm.setBaseTransientStoreKey(BASE_IN_MEMORY_OBJECT_STORE_KEY);
      try {
        getMuleRegistry().registerObject(LAZY_MULE_OBJECT_STORE_MANAGER, osm);
      } catch (RegistrationException e) {
        throw new MuleRuntimeException(e);
      }

      muleContext.getCustomizationService().registerCustomServiceClass(DEFAULT_METADATA_CACHE_MANAGER_KEY,
                                                                       DefaultPersistentMetadataCacheManager.class);
      muleContext.getCustomizationService().overrideDefaultServiceImpl(METADATA_CACHE_MANAGER_KEY,
                                                                       new DelegateMetadataCacheManager(() -> {
                                                                         DefaultPersistentMetadataCacheManager defaultPersistentMetadataCacheManager =
                                                                             (DefaultPersistentMetadataCacheManager) getRegistry()
                                                                                 .lookupByName(DEFAULT_METADATA_CACHE_MANAGER_KEY)
                                                                                 .get();
                                                                         defaultPersistentMetadataCacheManager
                                                                             .setLockFactory(runtimeLockFactory);
                                                                         defaultPersistentMetadataCacheManager
                                                                             .setObjectStoreManager(getRegistry()
                                                                                 .<ObjectStoreManager>lookupByName(LAZY_MULE_OBJECT_STORE_MANAGER)
                                                                                 .get());
                                                                         return defaultPersistentMetadataCacheManager;
                                                                       }));
    }

    initialize();
    //Graph should be generated after the initialize() method since the applicationModel will change by macro expanding XmlSdk components.
    this.graph = generateFor(getApplicationModel());
  }

  @Override
  protected boolean isRuntimeMode() {
    return false;
  }

  @Override
  protected void prepareBeanFactory(ConfigurableListableBeanFactory beanFactory) {
    super.prepareBeanFactory(beanFactory);
    trackingPostProcessor = new TrackingPostProcessor();
    addBeanPostProcessors(beanFactory, trackingPostProcessor);
  }

  private static Map<String, String> extendArtifactProperties(Map<String, String> artifactProperties) {
    Map<String, String> extendedArtifactProperties = new HashMap<>(artifactProperties);
    extendedArtifactProperties.put(MULE_LAZY_INIT_DEPLOYMENT_PROPERTY, "true");
    return extendedArtifactProperties;
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
        .filter(component -> component instanceof ConfigurationProvider)
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
      LOGGER.debug("Initializing component '{}'...", object.toString());
      try {
        if (object instanceof MessageProcessorChain) {
          // When created it will be initialized
        } else {
          getMuleRegistry().applyLifecycle(object, Initialisable.PHASE_NAME);
        }
      } catch (MuleException e) {
        throw new MuleRuntimeException(e);
      }
    }
  }

  private void startComponent(List<Object> components) {
    for (Object object : components) {
      LOGGER.debug("Starting component '{}'...", object.toString());
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

  @Override
  public void initializeComponent(Location location, boolean applyStartPhase) {
    applyLifecycle(createComponents(empty(), of(location), applyStartPhase,
                                    getParentComponentModelInitializerAdapter(applyStartPhase)),
                   applyStartPhase);
  }

  @Override
  public void initializeComponents(ComponentLocationFilter filter, boolean applyStartPhase) {
    applyLifecycle(createComponents(of(o -> {
      ComponentModel componentModel = (ComponentModel) o;
      if (componentModel.getComponentLocation() != null) {
        return filter.accept(componentModel.getComponentLocation());
      }
      return false;
    }), empty(), applyStartPhase, getParentComponentModelInitializerAdapter(applyStartPhase)), applyStartPhase);
  }

  @Override
  public void initializeComponents(Predicate<ComponentAst> componentModelPredicate,
                                   boolean applyStartPhase) {
    applyLifecycle(createComponents(of(componentModelPredicate), empty(), applyStartPhase,
                                    getParentComponentModelInitializerAdapter(applyStartPhase)),
                   applyStartPhase);
  }

  public Optional<ComponentModelInitializerAdapter> getParentComponentModelInitializerAdapter(boolean applyStartPhase) {
    return parentComponentModelInitializer
        .map(componentModelInitializer -> componentModelPredicate -> componentModelInitializer
            .initializeComponents(componentModelPredicate, applyStartPhase));
  }

  private List<Object> createComponents(Optional<Predicate<ComponentAst>> predicateOptional, Optional<Location> locationOptional,
                                        boolean applyStartPhase,
                                        Optional<ComponentModelInitializerAdapter> parentComponentModelInitializerAdapter) {
    checkState(predicateOptional.isPresent() != locationOptional.isPresent(), "predicate or location has to be passed");
    return withContextClassLoader(getMuleContext().getExecutionClassLoader(), () -> {
      // User input components to be initialized...
      final Predicate<ComponentAst> basePredicate =
          predicateOptional.orElseGet(() -> comp -> comp.getLocation() != null &&
              comp.getLocation().getLocation().equals(locationOptional.get().toString()));

      final ArtifactAst minimalApplicationModel = buildMinimalApplicationModel(basePredicate);

      if (locationOptional.map(loc -> minimalApplicationModel.recursiveStream()
          .noneMatch(comp -> comp.getLocation() != null
              && comp.getLocation().getLocation().equals(loc.toString())))
          .orElse(false)) {
        throw new NoSuchComponentModelException(createStaticMessage("No object found at location "
            + locationOptional.get().toString()));
      }

      Set<String> requestedLocations = locationOptional.map(location -> (Set<String>) newHashSet(location.toString()))
          .orElseGet(() -> getApplicationModel().recursiveStream()
              .filter(basePredicate)
              .filter(comp -> comp.getLocation() != null)
              .map(comp -> comp.getLocation().getLocation())
              .collect(toSet()));

      if (copyOf(currentComponentLocationsRequested).equals(copyOf(requestedLocations)) &&
          appliedStartedPhaseRequest == applyStartPhase) {
        // Same minimalApplication has been requested, so we don't need to recreate the same beans.
        return emptyList();
      }

      if (parentComponentModelInitializerAdapter.isPresent()) {
        parentComponentModelInitializerAdapter.get()
            .initializeComponents(componentModel -> graph.getMissingDependencies()
                .stream()
                .anyMatch(missingDep -> missingDep.isSatisfiedBy(componentModel)));
      } else {
        graph.getMissingDependencies().stream().forEach(missingDep -> {
          if (LOGGER.isDebugEnabled()) {
            LOGGER.debug("Ignoring dependency {} because it does not exist.", missingDep);
          }
        });
      }

      // First unregister any already initialized/started component
      unregisterBeans(trackingPostProcessor.getBeansTracked());

      currentComponentLocationsRequested.clear();
      currentComponentLocationsRequested.addAll(requestedLocations);
      appliedStartedPhaseRequest = applyStartPhase;

      // Clean up resources...
      trackingPostProcessor.reset();
      objectProviders.clear();
      resetMuleSecurityManager();

      List<Pair<String, ComponentAst>> applicationComponents =
          createApplicationComponents((DefaultListableBeanFactory) this.getBeanFactory(), minimalApplicationModel, false);

      super.prepareObjectProviders();

      return createBeans(applicationComponents);
    });
  }

  private ArtifactAst buildMinimalApplicationModel(final Predicate<ComponentAst> basePredicate) {
    final Predicate<? super ComponentAst> txManagerPredicate = componentModel -> {
      final ObjectTypeVisitor objectTypeVisitor = new ObjectTypeVisitor(componentModel);
      return componentBuildingDefinitionRegistry.getBuildingDefinition(componentModel.getIdentifier())
          .map(componentBuildingDefinition -> {
            componentBuildingDefinition.getTypeDefinition().visit(objectTypeVisitor);
            return TransactionManagerFactory.class.isAssignableFrom(objectTypeVisitor.getType());
          }).orElse(false);
    };

    final Predicate<? super ComponentAst> alwaysEnabledPredicate =
        componentModel -> componentBuildingDefinitionRegistry.getBuildingDefinition(componentModel.getIdentifier())
            .map(ComponentBuildingDefinition::isAlwaysEnabled).orElse(false);

    final Predicate<? super ComponentAst> languageConstructPredicate =
        componentModel -> beanDefinitionFactory.isLanguageConstructComponent(componentModel.getIdentifier());

    final ArtifactAst predicatedModel = graph.minimalArtifactFor(basePredicate
        .or(txManagerPredicate)
        .or(alwaysEnabledPredicate)
        .or(languageConstructPredicate));

    final Set<String> allNamespaces = predicatedModel.recursiveStream()
        .map(comp -> comp.getIdentifier().getNamespaceUri())
        .collect(toSet());

    return new ArtifactAst() {

      @Override
      public Stream<ComponentAst> topLevelComponentsStream() {
        return Stream.concat(getDefaultGlobalElements(), predicatedModel.topLevelComponentsStream());
      }

      @Override
      public Spliterator<ComponentAst> topLevelComponentsSpliterator() {
        return Stream.concat(getDefaultGlobalElements(), predicatedModel.topLevelComponentsStream()).spliterator();
      }

      @Override
      public Stream<ComponentAst> recursiveStream() {
        return Stream.concat(getDefaultGlobalElements()
            .flatMap(comp -> comp.recursiveStream()), predicatedModel.recursiveStream());
      }

      @Override
      public Spliterator<ComponentAst> recursiveSpliterator() {
        return Stream.concat(getDefaultGlobalElements()
            .flatMap(comp -> comp.recursiveStream()), predicatedModel.recursiveStream()).spliterator();
      }

      private Stream<ComponentAst> getDefaultGlobalElements() {
        // defaultGlobalElements from XML DSK components are not referenced from the app, so we need to initialize those if there
        // is any operation that may use it in the app.
        return getApplicationModel().topLevelComponentsStream()
            .filter(comp -> DEFAULT_GLOBAL_ELEMENTS.equals(comp.getIdentifier().getName())
                && allNamespaces.contains(comp.getIdentifier().getNamespaceUri()))
            .flatMap(comp -> comp.directChildrenStream());
      }
    };
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
    LOGGER.debug("orphanComponents found: {}", orphanComponents.toString());

    // Handle orphan named components...
    orphanComponents.stream()
        .filter(cm -> asList(SOURCE, OPERATION, SCOPE).contains(cm.getComponentType()))
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

    if (LOGGER.isDebugEnabled()) {
      LOGGER.debug("applicationComponents to be created: {}", applicationComponents.toString());
    }
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
    trackingPostProcessor.startTracking();
    Map<Pair<String, ComponentAst>, Object> objects = new LinkedHashMap<>();
    // Create beans only once by calling the lookUp at the Registry
    applicationComponentNames.forEach(componentPair -> {
      try {
        Object object = getRegistry().lookupByName(componentPair.getFirst()).orElse(null);
        if (object != null) {
          // MessageProcessorChainBuilder has to be manually created and added to the registry in order to be able
          // to dispose it later
          if (object instanceof MessageProcessorChainBuilder) {
            handleChainBuilder((MessageProcessorChainBuilder) object, componentPair, objects);
          } else if (object instanceof TransactionManagerFactory) {
            handleTxManagerFactory((TransactionManagerFactory) object);
          }
          objects.put(componentPair, object);
        }
      } catch (Exception e) {
        trackingPostProcessor.stopTracking();
        trackingPostProcessor.intersection(objects.keySet().stream().map(pair -> pair.getFirst()).collect(toList()));
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
    trackingPostProcessor.stopTracking();
    trackingPostProcessor.intersection(objects.keySet().stream().map(pair -> pair.getFirst()).collect(toList()));

    // Sort in order to later initialize and start components according to their dependencies
    List<Object> sortedObjects = new ArrayList<>(objects.values());
    sort(sortedObjects, (o1, o2) -> graph.dependencyComparator().compare(componentNames.get(o1).getSecond(),
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
      unregisterBeans(objects.keySet().stream().map(p -> p.getFirst()).collect(toList()));
      throw new IllegalStateException("Couldn't initialise an instance of a MessageProcessorChain", e);
    }
    try {
      getMuleRegistry().registerObject(chainKey.getFirst(), messageProcessorChain);
    } catch (RegistrationException e) {
      // Unregister any already created component
      unregisterBeans(objects.keySet().stream().map(p -> p.getFirst()).collect(toList()));
      throw new IllegalStateException("Couldn't register an instance of a MessageProcessorChain", e);
    }
    objects.put(chainKey, messageProcessorChain);
  }

  private void handleTxManagerFactory(TransactionManagerFactory object) {
    try {
      getMuleContext().setTransactionManager(object.create(getMuleContext().getConfiguration()));
    } catch (Exception e) {
      throw new IllegalStateException("Couldn't register an instance of a TransactionManager", e);
    }
  }

  private void resetMuleSecurityManager() {
    boolean registerMuleSecurityManager = false;
    // Always unregister first the default security manager from Mule.
    try {
      getMuleRegistry().unregisterObject(OBJECT_SECURITY_MANAGER);
      registerMuleSecurityManager = true;
    } catch (Exception e) {
      // NoSuchBeanDefinitionException can be ignored
      if (!hasCause(e, NoSuchBeanDefinitionException.class)) {
        throw new MuleRuntimeException(createStaticMessage("Error while unregistering Mule security manager"),
                                       e);
      }
    }
    if (registerMuleSecurityManager) {
      try {
        // Has to be created before as the factory for SecurityManager (MuleSecurityManagerConfigurator) is expecting to
        // retrieve it (through MuleContext and registry) while creating it. See
        // org.mule.runtime.core.api.security.MuleSecurityManagerConfigurator.doGetObject
        getMuleRegistry().registerObject(OBJECT_SECURITY_MANAGER, new DefaultMuleSecurityManager());
      } catch (RegistrationException e) {
        throw new MuleRuntimeException(createStaticMessage("Couldn't register a new instance of Mule security manager in the registry"),
                                       e);
      }
    }
  }

  @Override
  protected void prepareObjectProviders() {
    // Do not prepare object providers at this point. No components are going to be created yet. This will be done when creating
    // lazy components
  }

  @Override
  public void close() {
    if (trackingPostProcessor != null) {
      trackingPostProcessor.stopTracking();
      trackingPostProcessor.reset();
    }

    appliedStartedPhaseRequest = false;
    currentComponentLocationsRequested.clear();

    super.close();
  }

  private void unregisterBeans(List<String> beans) {
    doUnregisterBeans(beans.stream()
        .collect(toCollection(LinkedList::new)).descendingIterator());
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
        logger.error(String
            .format("Exception unregistering an object during lazy initialization of component %s, exception message is %s",
                    beanName, e.getMessage()));
        throw new MuleRuntimeException(I18nMessageFactory
            .createStaticMessage("There was an error while unregistering component '%s'", beanName), e);
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

  @Override
  protected void loadBeanDefinitions(DefaultListableBeanFactory beanFactory) throws IOException {
    getApplicationModel().recursiveStream()
        .filter(cm -> !beanDefinitionFactory.isComponentIgnored(cm.getIdentifier()))
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
