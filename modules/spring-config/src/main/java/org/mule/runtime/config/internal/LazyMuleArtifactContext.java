/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.config.internal;

import static com.google.common.collect.ImmutableList.copyOf;
import static java.util.Collections.emptyList;
import static java.util.Optional.empty;
import static java.util.Optional.of;
import static java.util.stream.Collectors.toCollection;
import static java.util.stream.Collectors.toList;
import static org.apache.commons.lang3.exception.ExceptionUtils.hasCause;
import static org.mule.runtime.api.connectivity.ConnectivityTestingService.CONNECTIVITY_TESTING_SERVICE_KEY;
import static org.mule.runtime.api.i18n.I18nMessageFactory.createStaticMessage;
import static org.mule.runtime.api.metadata.MetadataService.METADATA_SERVICE_KEY;
import static org.mule.runtime.api.metadata.MetadataService.NON_LAZY_METADATA_SERVICE_KEY;
import static org.mule.runtime.api.store.ObjectStoreManager.BASE_IN_MEMORY_OBJECT_STORE_KEY;
import static org.mule.runtime.api.util.Preconditions.checkState;
import static org.mule.runtime.api.value.ValueProviderService.VALUE_PROVIDER_SERVICE_KEY;
import static org.mule.runtime.config.api.dsl.CoreDslConstants.CONFIGURATION_IDENTIFIER;
import static org.mule.runtime.config.internal.LazyConnectivityTestingService.NON_LAZY_CONNECTIVITY_TESTING_SERVICE;
import static org.mule.runtime.config.internal.LazyValueProviderService.NON_LAZY_VALUE_PROVIDER_SERVICE;
import static org.mule.runtime.core.api.config.MuleProperties.OBJECT_MULE_CONFIGURATION;
import static org.mule.runtime.core.api.config.MuleProperties.OBJECT_SECURITY_MANAGER;
import static org.mule.runtime.core.api.lifecycle.LifecycleUtils.initialiseIfNeeded;
import static org.mule.runtime.core.api.util.ClassUtils.withContextClassLoader;
import static org.mule.runtime.core.internal.metadata.cache.MetadataCacheManager.METADATA_CACHE_MANAGER_KEY;
import static org.mule.runtime.core.internal.store.SharedPartitionedPersistentObjectStore.SHARED_PERSISTENT_OBJECT_STORE_KEY;
import static org.mule.runtime.core.privileged.registry.LegacyRegistryUtils.unregisterObject;

import org.mule.runtime.api.component.ConfigurationProperties;
import org.mule.runtime.api.component.location.Location;
import org.mule.runtime.api.connectivity.ConnectivityTestingService;
import org.mule.runtime.api.exception.MuleException;
import org.mule.runtime.api.exception.MuleRuntimeException;
import org.mule.runtime.api.lifecycle.Initialisable;
import org.mule.runtime.api.lifecycle.InitialisationException;
import org.mule.runtime.api.lifecycle.Startable;
import org.mule.runtime.api.lock.LockFactory;
import org.mule.runtime.api.metadata.MetadataService;
import org.mule.runtime.api.store.ObjectStoreManager;
import org.mule.runtime.api.value.ValueProviderService;
import org.mule.runtime.app.declaration.api.ArtifactDeclaration;
import org.mule.runtime.config.internal.dsl.model.ConfigurationDependencyResolver;
import org.mule.runtime.config.internal.dsl.model.MinimalApplicationModelGenerator;
import org.mule.runtime.config.internal.model.ApplicationModel;
import org.mule.runtime.config.internal.model.ComponentModel;
import org.mule.runtime.core.api.MuleContext;
import org.mule.runtime.core.api.config.MuleDeploymentProperties;
import org.mule.runtime.core.api.config.bootstrap.ArtifactType;
import org.mule.runtime.core.api.transaction.TransactionManagerFactory;
import org.mule.runtime.core.internal.connectivity.DefaultConnectivityTestingService;
import org.mule.runtime.core.internal.lifecycle.phases.DefaultLifecycleObjectSorter;
import org.mule.runtime.core.internal.metadata.MuleMetadataService;
import org.mule.runtime.core.internal.metadata.cache.DefaultPersistentMetadataCacheManager;
import org.mule.runtime.core.internal.security.DefaultMuleSecurityManager;
import org.mule.runtime.core.internal.store.SharedPartitionedPersistentObjectStore;
import org.mule.runtime.core.internal.util.store.MuleObjectStoreManager;
import org.mule.runtime.core.internal.value.MuleValueProviderService;
import org.mule.runtime.core.privileged.processor.chain.MessageProcessorChain;
import org.mule.runtime.core.privileged.processor.chain.MessageProcessorChainBuilder;
import org.mule.runtime.core.privileged.registry.RegistrationException;
import org.mule.runtime.dsl.api.ConfigResource;
import org.mule.runtime.dsl.api.component.ComponentBuildingDefinitionProvider;
import org.mule.runtime.dsl.api.component.TypeDefinition;
import org.mule.runtime.dsl.api.component.TypeDefinitionVisitor;

import java.io.File;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.Predicate;
import java.util.function.Supplier;

import com.google.common.collect.ImmutableSet;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.BeansException;
import org.springframework.beans.factory.NoSuchBeanDefinitionException;
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

  private List<String> beansCreated = new ArrayList<>();

  private Optional<ComponentModelInitializer> parentComponentModelInitializer;

  private ConfigurationDependencyResolver dependencyResolver;
  private Set<String> applicationComponentLocationsCreated = new HashSet<>();
  private boolean appliedStartedPhase = false;


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
   * @param runtimeComponentBuildingDefinitionProvider provider for the runtime {@link org.mule.runtime.dsl.api.component.ComponentBuildingDefinition}s
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
          disableXmlValidations, runtimeComponentBuildingDefinitionProvider, runtimeLockFactory);
    // Changes the component locator in order to allow accessing any component by location even when they are prototype
    this.componentLocator = new SpringConfigurationComponentLocator();
    // By default when a lazy context is created none of its components are enabled...
    this.applicationModel.executeOnEveryMuleComponentTree(componentModel -> componentModel.setEnabled(false));
    enableMuleObjects();

    this.parentComponentModelInitializer = parentComponentModelInitializer;

    dependencyResolver = new ConfigurationDependencyResolver(this.applicationModel, componentBuildingDefinitionRegistry,
                                                             componentIdentifier -> beanDefinitionFactory
                                                                 .isLanguageConstructComponent(componentIdentifier));


    muleContext.getCustomizationService().overrideDefaultServiceImpl(CONNECTIVITY_TESTING_SERVICE_KEY,
                                                                     new LazyConnectivityTestingService(this, () -> getRegistry()
                                                                         .<ConnectivityTestingService>lookupByName(NON_LAZY_CONNECTIVITY_TESTING_SERVICE)
                                                                         .get()));
    muleContext.getCustomizationService().registerCustomServiceClass(NON_LAZY_CONNECTIVITY_TESTING_SERVICE,
                                                                     DefaultConnectivityTestingService.class);
    muleContext.getCustomizationService().overrideDefaultServiceImpl(METADATA_SERVICE_KEY,
                                                                     new LazyMetadataService(this, () -> getRegistry()
                                                                         .<MetadataService>lookupByName(NON_LAZY_METADATA_SERVICE_KEY)
                                                                         .get()));
    muleContext.getCustomizationService().registerCustomServiceClass(NON_LAZY_METADATA_SERVICE_KEY, MuleMetadataService.class);
    muleContext.getCustomizationService().overrideDefaultServiceImpl(VALUE_PROVIDER_SERVICE_KEY,
                                                                     new LazyValueProviderService(this, () -> getRegistry()
                                                                         .<ValueProviderService>lookupByName(NON_LAZY_VALUE_PROVIDER_SERVICE)
                                                                         .get(),
                                                                                                  muleContext::getConfigurationComponentLocator));
    muleContext.getCustomizationService().registerCustomServiceClass(NON_LAZY_VALUE_PROVIDER_SERVICE,
                                                                     MuleValueProviderService.class);

    muleContext.getCustomizationService().overrideDefaultServiceImpl(LAZY_COMPONENT_INITIALIZER_SERVICE_KEY, this);

    String sharedPartitionatedPersistentObjectStorePath = artifactProperties.get(SHARED_PARTITIONED_PERSISTENT_OBJECT_STORE_PATH);
    if (sharedPartitionatedPersistentObjectStorePath != null) {
      // We need to first define this service so it would be later initialized
      muleContext.getCustomizationService().registerCustomServiceClass(SHARED_PERSISTENT_OBJECT_STORE_KEY,
                                                                       SharedPartitionedPersistentObjectStore.class);
      muleContext.getCustomizationService().overrideDefaultServiceImpl(SHARED_PERSISTENT_OBJECT_STORE_KEY,
                                                                       new SharedPartitionedPersistentObjectStore<>(new File(sharedPartitionatedPersistentObjectStorePath),
                                                                                                                    runtimeLockFactory));
      // Create a custom ObjectStoreManager that defines a different key for persistent object store
      Supplier<ObjectStoreManager> osmSupplier = () -> {
        MuleObjectStoreManager osm = new MuleObjectStoreManager();
        osm.setBasePersistentStoreKey(SHARED_PERSISTENT_OBJECT_STORE_KEY);
        osm.setBaseTransientStoreKey(BASE_IN_MEMORY_OBJECT_STORE_KEY);
        osm.setSchedulerService(muleContext.getSchedulerService());
        osm.setMuleContext(muleContext);
        try {
          // We have to manually initialise this component
          this.muleContext.getRegistry().applyLifecycle(osm, Initialisable.PHASE_NAME);
        } catch (MuleException e) {
          throw new MuleRuntimeException(createStaticMessage("Error while initializing a shared object store manager"), e);
        }
        return osm;
      };

      muleContext.getCustomizationService().overrideDefaultServiceImpl(METADATA_CACHE_MANAGER_KEY,
                                                                       new DefaultPersistentMetadataCacheManager(osmSupplier,
                                                                                                                 runtimeLockFactory));
    }
  }

  /**
   * Custom logic to only enable those components that should be created when MuleContext is created.
   * TransactionManagerFactory should be created before a TransactionManager is defined in the configuration.
   */
  private void enableMuleObjects() {
    ConfigurationDependencyResolver dependencyResolver = new ConfigurationDependencyResolver(this.applicationModel,
                                                                                             componentBuildingDefinitionRegistry,
                                                                                             componentIdentifier -> beanDefinitionFactory
                                                                                                 .isLanguageConstructComponent(componentIdentifier));
    MinimalApplicationModelGenerator minimalApplicationModelGenerator =
        new MinimalApplicationModelGenerator(dependencyResolver, true);
    minimalApplicationModelGenerator
        .getMinimalModel(minimalApplicationModelGenerator.getComponentModels(componentModel -> {
          AtomicBoolean transactionFactoryType = new AtomicBoolean(false);
          TypeDefinitionVisitor visitor = new TypeDefinitionVisitor() {

            @Override
            public void onType(Class<?> type) {
              transactionFactoryType.set(TransactionManagerFactory.class.isAssignableFrom(type));
            }

            @Override
            public void onConfigurationAttribute(String attributeName, Class<?> enforcedClass) {}

            @Override
            public void onMapType(TypeDefinition.MapEntryType mapEntryType) {}
          };
          return componentBuildingDefinitionRegistry.getBuildingDefinition(componentModel.getIdentifier())
              .map(componentBuildingDefinition -> {
                componentBuildingDefinition.getTypeDefinition().visit(visitor);
                return transactionFactoryType.get();
              }).orElse(false);
        }));
  }

  private static Map<String, String> extendArtifactProperties(Map<String, String> artifactProperties) {
    Map<String, String> extendedArtifactProperties = new HashMap<>(artifactProperties);
    extendedArtifactProperties.put(MuleDeploymentProperties.MULE_LAZY_INIT_DEPLOYMENT_PROPERTY, "true");
    return extendedArtifactProperties;
  }

  private void applyLifecycle(List<Object> components, boolean applyStartPhase) {
    muleContext.withLifecycleLock(() -> {
      if (muleContext.isInitialised()) {
        for (Object object : components) {
          try {
            if (object instanceof MessageProcessorChain) {
              // When created it will be initialized
            } else {
              muleContext.getRegistry().applyLifecycle(object, Initialisable.PHASE_NAME);
            }
          } catch (MuleException e) {
            throw new RuntimeException(e);
          }
        }
      }
      if (applyStartPhase && muleContext.isStarted()) {
        for (Object object : components) {
          try {
            if (object instanceof MessageProcessorChain) {
              // Has to be ignored as when it is registered it will be started too
            } else {
              muleContext.getRegistry().applyLifecycle(object, Initialisable.PHASE_NAME, Startable.PHASE_NAME);
            }
          } catch (MuleException e) {
            throw new RuntimeException(e);
          }
        }
      }
    });
  }

  private static BeanDependencyResolver getBeanDependencyResolver(ConfigurationDependencyResolver configurationDependencyResolver,
                                                                  Map<String, Object> components) {
    return beanName -> configurationDependencyResolver
        .resolveComponentDependencies(beanName).stream()
        .map(componentName -> components.get(componentName))
        .filter(Objects::nonNull)
        .collect(toList());
  }

  class ComponentConfigurationLifecycleObjectSorter extends DefaultLifecycleObjectSorter {

    private final BeanDependencyResolver beanDependencyResolver;

    public ComponentConfigurationLifecycleObjectSorter(BeanDependencyResolver beanDependencyResolver) {
      // Only Object.class type is defined as a single bucket as we just need to sort the configuration components (DSL)
      super(new Class[] {Object.class});
      this.beanDependencyResolver = beanDependencyResolver;
    }

    /**
     * Implementation that handles duplicates.
     * Whenever a new object is added, then all its dependencies are added as well.
     * So if one of those dependencies wants to be added again, it's ignored.
     */
    @Override
    protected int doAddObject(String name, Object object, List<Object> bucket) {
      if (bucket.contains(object)) {
        return 0;
      }
      List<Object> dependencies = beanDependencyResolver.resolveBeanDependencies(name)
          .stream().filter(dependency -> !bucket.contains(dependency)).collect(toList());
      bucket.addAll(dependencies);
      bucket.add(object);
      return dependencies.size() + 1;
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
  public void initializeComponents(Predicate<org.mule.runtime.config.internal.model.ComponentModel> componentModelPredicate,
                                   boolean applyStartPhase) {
    applyLifecycle(createComponents(of(componentModelPredicate), empty(), applyStartPhase,
                                    getParentComponentModelInitializerAdapter(applyStartPhase)),
                   applyStartPhase);
  }

  public Optional<ComponentModelInitializerAdapter> getParentComponentModelInitializerAdapter(
                                                                                              boolean applyStartPhase) {
    return parentComponentModelInitializer
        .map(componentModelInitializer -> componentModelPredicate -> componentModelInitializer
            .initializeComponents(componentModelPredicate, applyStartPhase));
  }

  private List<Object> createComponents(Optional<Predicate> predicateOptional, Optional<Location> locationOptional,
                                        boolean applyStartPhase,
                                        Optional<ComponentModelInitializerAdapter> parentComponentModelInitializerAdapter) {
    checkState(predicateOptional.isPresent() != locationOptional.isPresent(), "predicate or location has to be passed");
    return withContextClassLoader(muleContext.getExecutionClassLoader(), () -> {
      applicationModel.executeOnEveryMuleComponentTree(componentModel -> componentModel.setEnabled(false));

      MinimalApplicationModelGenerator minimalApplicationModelGenerator =
          new MinimalApplicationModelGenerator(dependencyResolver);
      // User input components to be initialized...
      List<ComponentModel> componentModelsToBuildMinimalModel = new ArrayList<>();
      predicateOptional
          .ifPresent(predicate -> componentModelsToBuildMinimalModel
              .addAll(minimalApplicationModelGenerator.getComponentModels(predicate)));
      locationOptional
          .ifPresent(location -> componentModelsToBuildMinimalModel
              .add(minimalApplicationModelGenerator.findComponentModel(location)));

      Set<String> applicationComponentLocations = new HashSet<>();
      componentModelsToBuildMinimalModel.stream().forEach(componentModel -> {
        if (componentModel.getComponentLocation() != null) {
          applicationComponentLocations.add(componentModel.getComponentLocation().getLocation());
        }
      });

      if (ImmutableSet.copyOf(applicationComponentLocationsCreated).equals(ImmutableSet.copyOf(applicationComponentLocations))
          && appliedStartedPhase == applyStartPhase) {
        // Same minimalApplication has been requested, so we don't need to recreate the same beans.
        return emptyList();
      }
      ApplicationModel minimalApplicationModel =
          minimalApplicationModelGenerator.getMinimalModel(componentModelsToBuildMinimalModel);

      if (parentComponentModelInitializerAdapter.isPresent()) {
        List<String> missingComponentNames = dependencyResolver.getMissingDependencies().stream()
            .filter(dependencyNode -> dependencyNode.isTopLevel())
            .map(dependencyNode -> dependencyNode.getComponentName())
            .collect(toList());
        parentComponentModelInitializerAdapter.get().initializeComponents(componentModel -> {
          if (componentModel.getNameAttribute() != null) {
            return missingComponentNames.contains(componentModel.getNameAttribute());
          }
          return false;
        });
      } else {
        dependencyResolver.getMissingDependencies().stream().forEach(globalElementName -> {
          if (LOGGER.isDebugEnabled()) {
            LOGGER.debug(String.format("Ignoring dependency %s because it does not exists", globalElementName));
          }
        });
      }

      // Force initialization of configuration component...
      resetMuleConfiguration(minimalApplicationModelGenerator);

      // First unregister any already initialized/started component
      unregisterBeans(beansCreated);
      // Clean up resources...
      applicationComponentLocationsCreated.clear();
      beansCreated.clear();
      objectProviders.clear();
      resetMuleSecurityManager();

      // Creates and registers beanDefinitions
      List<String> applicationComponents =
          createApplicationComponents((DefaultListableBeanFactory) this.getBeanFactory(), minimalApplicationModel, false);
      applicationComponentLocationsCreated.addAll(applicationComponentLocations);
      appliedStartedPhase = applyStartPhase;

      super.prepareObjectProviders();

      List<Object> sortedObjects = createBeans(applicationComponents);
      return sortedObjects;
    });
  }

  /**
   * Creates the beans based on the application component model names that were enabled by the minimal application model.
   * It also populates the list of bean names created and returns the list of beans instantiated, the list of beans
   * is sorted based on dependencies between components (even between configuration components, flow->config and
   * config->config dependencies from the DSL).
   *
   * @param applicationComponentNames name of components to be created.
   * @return List beans created for the given component names sorted by precedence.
   */
  private List<Object> createBeans(List<String> applicationComponentNames) {
    Map<String, Object> objects = new LinkedHashMap<>();
    // Create beans only once by calling the lookUp at the Registry
    applicationComponentNames.forEach(componentName -> {
      Object object = getRegistry().lookupByName(componentName).orElse(null);
      if (object != null) {
        // MessageProcessorChainBuilder has to be manually created and added to the registry in order to be able
        // to dispose it later
        if (object instanceof MessageProcessorChainBuilder) {
          String chainKey = componentName + "@" + object.hashCode();
          MessageProcessorChain messageProcessorChain = ((MessageProcessorChainBuilder) object).build();
          try {
            initialiseIfNeeded(messageProcessorChain, this.muleContext);
          } catch (InitialisationException e) {
            unregisterBeans(copyOf(objects.keySet()));
            throw new IllegalStateException("Couldn't initialise an instance of a MessageProcessorChain", e);
          }
          try {
            getMuleContext().getRegistry().registerObject(chainKey, messageProcessorChain);
          } catch (RegistrationException e) {
            // Unregister any already created component
            unregisterBeans(copyOf(objects.keySet()));
            throw new IllegalStateException("Couldn't register an instance of a MessageProcessorChain", e);
          }
          objects.put(chainKey, messageProcessorChain);
        }
        objects.put(componentName, object);
      }
    });

    // Sorter in order to later initialize and start components according to their dependencies
    ComponentConfigurationLifecycleObjectSorter componentConfigurationLifecycleObjectSorter =
        new ComponentConfigurationLifecycleObjectSorter(getBeanDependencyResolver(getDependencyResolver(), objects));
    // A Map to access the componentName by the bean instance
    Map<Object, String> componentNames = new HashMap<>();
    objects.entrySet().forEach(entry -> {
      Object object = entry.getValue();
      String componentName = entry.getKey();
      componentConfigurationLifecycleObjectSorter.addObject(componentName, object);
      componentNames.put(object, componentName);
    });
    List<Object> sortedObjects = componentConfigurationLifecycleObjectSorter.getSortedObjects();
    // Register the bean names to be later disposed
    sortedObjects.forEach(object -> beansCreated.add(componentNames.get(object)));
    return sortedObjects;
  }

  private void resetMuleSecurityManager() {
    boolean registerMuleSecurityManager = false;
    // Always unregister first the default security manager from Mule.
    try {
      muleContext.getRegistry().unregisterObject(OBJECT_SECURITY_MANAGER);
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
        muleContext.getRegistry().registerObject(OBJECT_SECURITY_MANAGER, new DefaultMuleSecurityManager());
      } catch (RegistrationException e) {
        throw new MuleRuntimeException(createStaticMessage("Couldn't register a new instance of Mule security manager in the registry"),
                                       e);
      }
    }
  }

  private void resetMuleConfiguration(MinimalApplicationModelGenerator minimalApplicationModelGenerator) {
    // Always unregister first the default configuration from Mule.
    try {
      muleContext.getRegistry().unregisterObject(OBJECT_MULE_CONFIGURATION);
    } catch (Exception e) {
      // NoSuchBeanDefinitionException can be ignored
      if (!hasCause(e, NoSuchBeanDefinitionException.class)) {
        throw new MuleRuntimeException(createStaticMessage("Error while unregistering Mule configuration"),
                                       e);
      }
    }
    // Just enable the MuleConfiguration componentModel so it values will be applied on this initialization
    minimalApplicationModelGenerator
        .getMinimalModel(minimalApplicationModelGenerator
            .getComponentModels(componentModel -> componentModel.getIdentifier().equals(CONFIGURATION_IDENTIFIER)));
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
   * Apply the stop and dispose phases and unregister the bean from the registry.
   * The phases are applied to each bean at a time.
   *
   * @param beanNames {@link Iterator} of bean names to be stopped, disposed and unregistered.
   */
  private void doUnregisterBeans(Iterator<String> beanNames) {
    while (beanNames.hasNext()) {
      String beanName = beanNames.next();
      try {
        unregisterObject(muleContext, beanName);
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

  /**
   * Adapter for {@link ComponentModelInitializer} that hides the lifecycle phase from component model creation logic.
   */
  @FunctionalInterface
  private interface ComponentModelInitializerAdapter {

    void initializeComponents(Predicate<ComponentModel> componentModelPredicate);

  }

}
