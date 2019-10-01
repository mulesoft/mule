/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.config.internal;

import static java.util.Arrays.asList;
import static java.util.Collections.sort;
import static java.util.Optional.empty;
import static java.util.Optional.of;
import static java.util.stream.Collectors.toCollection;
import static java.util.stream.Collectors.toList;
import static org.apache.commons.lang3.exception.ExceptionUtils.hasCause;
import static org.mule.runtime.api.component.TypedComponentIdentifier.ComponentType.OPERATION;
import static org.mule.runtime.api.component.TypedComponentIdentifier.ComponentType.SCOPE;
import static org.mule.runtime.api.component.TypedComponentIdentifier.ComponentType.SOURCE;
import static org.mule.runtime.api.connectivity.ConnectivityTestingService.CONNECTIVITY_TESTING_SERVICE_KEY;
import static org.mule.runtime.api.i18n.I18nMessageFactory.createStaticMessage;
import static org.mule.runtime.api.metadata.MetadataService.METADATA_SERVICE_KEY;
import static org.mule.runtime.api.metadata.MetadataService.NON_LAZY_METADATA_SERVICE_KEY;
import static org.mule.runtime.api.store.ObjectStoreManager.BASE_PERSISTENT_OBJECT_STORE_KEY;
import static org.mule.runtime.api.util.Preconditions.checkState;
import static org.mule.runtime.api.value.ValueProviderService.VALUE_PROVIDER_SERVICE_KEY;
import static org.mule.runtime.ast.api.util.MuleAstUtils.resolveOrphanComponents;
import static org.mule.runtime.ast.graph.api.ArtifactAstDependencyGraphFactory.generateFor;
import static org.mule.runtime.config.api.dsl.CoreDslConstants.CONFIGURATION_IDENTIFIER;
import static org.mule.runtime.config.internal.LazyConnectivityTestingService.NON_LAZY_CONNECTIVITY_TESTING_SERVICE;
import static org.mule.runtime.config.internal.LazyValueProviderService.NON_LAZY_VALUE_PROVIDER_SERVICE;
import static org.mule.runtime.config.internal.parsers.generic.AutoIdUtils.uniqueValue;
import static org.mule.runtime.core.api.config.MuleDeploymentProperties.MULE_LAZY_INIT_DEPLOYMENT_PROPERTY;
import static org.mule.runtime.core.api.config.MuleProperties.OBJECT_MULE_CONFIGURATION;
import static org.mule.runtime.core.api.config.MuleProperties.OBJECT_SECURITY_MANAGER;
import static org.mule.runtime.core.api.lifecycle.LifecycleUtils.initialiseIfNeeded;
import static org.mule.runtime.core.api.util.ClassUtils.withContextClassLoader;
import static org.mule.runtime.core.privileged.registry.LegacyRegistryUtils.unregisterObject;

import org.mule.runtime.api.component.ConfigurationProperties;
import org.mule.runtime.api.component.location.Location;
import org.mule.runtime.api.config.custom.CustomizationService;
import org.mule.runtime.api.connectivity.ConnectivityTestingService;
import org.mule.runtime.api.exception.MuleException;
import org.mule.runtime.api.exception.MuleRuntimeException;
import org.mule.runtime.api.lifecycle.Initialisable;
import org.mule.runtime.api.lifecycle.InitialisationException;
import org.mule.runtime.api.lifecycle.Startable;
import org.mule.runtime.api.metadata.MetadataService;
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
import org.mule.runtime.core.internal.security.DefaultMuleSecurityManager;
import org.mule.runtime.core.internal.store.SharedPartitionedPersistentObjectStore;
import org.mule.runtime.core.internal.value.MuleValueProviderService;
import org.mule.runtime.core.privileged.processor.chain.MessageProcessorChain;
import org.mule.runtime.core.privileged.processor.chain.MessageProcessorChainBuilder;
import org.mule.runtime.core.privileged.registry.RegistrationException;
import org.mule.runtime.dsl.api.ConfigResource;
import org.mule.runtime.dsl.api.component.ComponentBuildingDefinition;
import org.mule.runtime.dsl.api.component.ComponentBuildingDefinitionProvider;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.function.Predicate;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.BeansException;
import org.springframework.beans.factory.NoSuchBeanDefinitionException;
import org.springframework.beans.factory.config.BeanDefinition;
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

  private final List<String> beansCreated = new ArrayList<>();

  private final Optional<ComponentModelInitializer> parentComponentModelInitializer;

  private final ArtifactAstDependencyGraph graph;

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
                                 ComponentBuildingDefinitionProvider runtimeComponentBuildingDefinitionProvider)
      throws BeansException {
    super(muleContext, artifactConfigResources, artifactDeclaration, optionalObjectsController,
          extendArtifactProperties(artifactProperties), artifactType, pluginsClassLoaders, parentConfigurationProperties,
          disableXmlValidations, runtimeComponentBuildingDefinitionProvider);

    // Changes the component locator in order to allow accessing any component by location even when they are prototype
    this.componentLocator = new SpringConfigurationComponentLocator();

    graph = generateFor(applicationModel);

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
      customizationService.overrideDefaultServiceImpl(BASE_PERSISTENT_OBJECT_STORE_KEY,
                                                      new SharedPartitionedPersistentObjectStore<>(new File(sharedPartitionatedPersistentObjectStorePath)));

    }

    initialize();
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
      if (applyStartPhase && getMuleContext().isStarted()) {
        startComponent(components);
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
        throw new RuntimeException(e);
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
        throw new RuntimeException(e);
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
    applyLifecycle(createComponents(empty(), of(location), getParentComponentModelInitializerAdapter(applyStartPhase)),
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
    }), empty(), getParentComponentModelInitializerAdapter(applyStartPhase)), applyStartPhase);
  }

  @Override
  public void initializeComponents(Predicate<ComponentAst> componentModelPredicate,
                                   boolean applyStartPhase) {
    applyLifecycle(createComponents(of(componentModelPredicate), empty(),
                                    getParentComponentModelInitializerAdapter(applyStartPhase)),
                   applyStartPhase);
  }

  public Optional<ComponentModelInitializerAdapter> getParentComponentModelInitializerAdapter(
                                                                                              boolean applyStartPhase) {
    return parentComponentModelInitializer
        .map(componentModelInitializer -> componentModelPredicate -> componentModelInitializer
            .initializeComponents(componentModelPredicate, applyStartPhase));
  }

  private List<Object> createComponents(Optional<Predicate<ComponentAst>> predicateOptional, Optional<Location> locationOptional,
                                        Optional<ComponentModelInitializerAdapter> parentComponentModelInitializerAdapter) {
    checkState(predicateOptional.isPresent() != locationOptional.isPresent(), "predicate or location has to be passed");
    return withContextClassLoader(getMuleContext().getExecutionClassLoader(), () -> {
      // First unregister any already initialized/started component
      unregisterBeans(beansCreated);
      // Clean up resources...
      beansCreated.clear();
      objectProviders.clear();
      resetMuleSecurityManager();

      // Force initialization of configuration component...
      resetMuleConfiguration();

      // User input components to be initialized...
      final Predicate<ComponentAst> basePredicate =
          predicateOptional.orElseGet(() -> comp -> comp.getLocation() != null &&
              comp.getLocation().getLocation().equals(locationOptional.get().toString()));

      final Predicate<? super ComponentAst> txManagerPredicate = componentModel -> {
        final ObjectTypeVisitor objectTypeVisitor = new ObjectTypeVisitor((ComponentModel) componentModel);
        return componentBuildingDefinitionRegistry.getBuildingDefinition(componentModel.getIdentifier())
            .map(componentBuildingDefinition -> {
              componentBuildingDefinition.getTypeDefinition().visit(objectTypeVisitor);
              return TransactionManagerFactory.class.isAssignableFrom(objectTypeVisitor.getType());
            }).orElse(false);
      };

      final Predicate<? super ComponentAst> configPredicate = componentModel -> componentModel.getIdentifier()
          .equals(CONFIGURATION_IDENTIFIER);

      final Predicate<? super ComponentAst> alwaysEnabledPredicate =
          componentModel -> componentBuildingDefinitionRegistry.getBuildingDefinition(componentModel.getIdentifier())
              .map(ComponentBuildingDefinition::isAlwaysEnabled).orElse(false);

      final ArtifactAst minimalApplicationModel = graph.minimalArtifactFor(basePredicate
          .or(txManagerPredicate)
          .or(configPredicate)
          .or(alwaysEnabledPredicate));

      if (locationOptional.map(loc -> minimalApplicationModel.recursiveStream()
          .noneMatch(comp -> comp.getLocation() != null
              && comp.getLocation().getLocation().equals(loc.toString())))
          .orElse(false)) {
        throw new NoSuchComponentModelException(createStaticMessage("No object found at location "
            + locationOptional.get().toString()));
      }

      List<Pair<String, ComponentAst>> applicationComponents =
          createApplicationComponents((DefaultListableBeanFactory) this.getBeanFactory(), minimalApplicationModel, false);

      super.prepareObjectProviders();

      return createBeans(applicationComponents);
    });
  }

  /**
   * Apart from calling {@link #createApplicationComponents(DefaultListableBeanFactory, ArtifactAst, boolean)} from the
   * superclass, will handle orphan processors. That is, processors that are part of the minimal app but for which the containing
   * flow is not.
   */
  @Override
  protected List<Pair<String, ComponentAst>> createApplicationComponents(DefaultListableBeanFactory beanFactory,
                                                                         ArtifactAst minimalAppModel,
                                                                         boolean mustBeRoot) {
    final List<Pair<String, ComponentAst>> applicationComponents =
        super.createApplicationComponents(beanFactory, minimalAppModel, mustBeRoot);

    final Set<ComponentAst> orphanComponents = resolveOrphanComponents(minimalAppModel);
    LOGGER.debug("orphanComponents found: {}", orphanComponents.toString());

    // Handle orphan named components...
    orphanComponents.stream()
        .filter(cm -> asList(SOURCE, OPERATION, SCOPE).contains(cm.getComponentType()))
        .filter(cm -> cm.getName().isPresent())
        .forEach(cm -> {
          final String nameAttribute = cm.getName().get();
          LOGGER.debug("Registering orphan named component '{}'...", nameAttribute);

          applicationComponents.add(0, new Pair<>(nameAttribute, cm));
          final BeanDefinition beanDef = ((SpringComponentModel) cm).getBeanDefinition();
          if (beanDef != null) {
            beanFactory.registerBeanDefinition(cm.getName().get(), beanDef);
            postProcessBeanDefinition((SpringComponentModel) cm, beanFactory, cm.getName().get());
          }
        });

    // Handle orphan components without name, rely on the location.
    orphanComponents.stream()
        .forEach(cm -> {
          final BeanDefinition beanDef = ((SpringComponentModel) cm).getBeanDefinition();
          if (beanDef != null) {
            final String beanName = cm.getName().orElse(uniqueValue(beanDef.getBeanClassName()));

            LOGGER.debug("Registering orphan un-named component '{}'...", beanName);
            applicationComponents.add(new Pair<>(beanName, cm));
            beanFactory.registerBeanDefinition(beanName, beanDef);
            postProcessBeanDefinition((SpringComponentModel) cm, beanFactory, beanName);
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
    Map<Pair<String, ComponentAst>, Object> objects = new LinkedHashMap<>();
    // Create beans only once by calling the lookUp at the Registry
    applicationComponentNames.forEach(componentPair -> {
      Object object = getRegistry().lookupByName(componentPair.getFirst()).orElse(null);
      if (object != null) {
        // MessageProcessorChainBuilder has to be manually created and added to the registry in order to be able
        // to dispose it later
        if (object instanceof MessageProcessorChainBuilder) {
          Pair<String, ComponentAst> chainKey =
              new Pair<>(componentPair.getFirst() + "@" + object.hashCode(), componentPair.getSecond());
          MessageProcessorChain messageProcessorChain = ((MessageProcessorChainBuilder) object).build();
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
        } else if (object instanceof TransactionManagerFactory) {
          try {
            getMuleContext()
                .setTransactionManager(((TransactionManagerFactory) object).create(getMuleContext().getConfiguration()));
          } catch (Exception e) {
            throw new IllegalStateException("Couldn't register an instance of a TransactionManager", e);
          }
        }
        objects.put(componentPair, object);
      }
    });

    // A Map to access the componentName by the bean instance
    Map<Object, Pair<String, ComponentAst>> componentNames = new HashMap<>();
    objects.entrySet().forEach(entry -> {
      Object object = entry.getValue();
      Pair<String, ComponentAst> component = entry.getKey();
      componentNames.put(object, component);
    });

    // Sort in order to later initialize and start components according to their dependencies
    List<Object> sortedObjects = new ArrayList<>(objects.values());
    sort(sortedObjects, (o1, o2) -> graph.dependencyComparator().compare(componentNames.get(o1).getSecond(),
                                                                         componentNames.get(o2).getSecond()));
    sortedObjects.forEach(object -> beansCreated.add(componentNames.get(object).getFirst()));
    return sortedObjects;
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

  private void resetMuleConfiguration() {
    // Always unregister first the default configuration from Mule.
    try {
      getMuleRegistry().unregisterObject(OBJECT_MULE_CONFIGURATION);
    } catch (Exception e) {
      // NoSuchBeanDefinitionException can be ignored
      if (!hasCause(e, NoSuchBeanDefinitionException.class)) {
        throw new MuleRuntimeException(createStaticMessage("Error while unregistering Mule configuration"),
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
    beansCreated.clear();
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
        logger.warn(String
            .format("Exception unregistering an object during lazy initialization of component %s, exception message is %s",
                    beanName, e.getMessage()));
        if (logger.isDebugEnabled()) {
          logger.debug(e.getMessage(), e);
        }
      }
    }
  }

  @Override
  protected void loadBeanDefinitions(DefaultListableBeanFactory beanFactory) throws IOException {
    applicationModel.recursiveStream()
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
