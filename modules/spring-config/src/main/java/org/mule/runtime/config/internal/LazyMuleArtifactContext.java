/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.config.internal;

import static java.lang.String.format;
import static java.util.Collections.reverse;
import static java.util.Optional.empty;
import static java.util.Optional.of;
import static java.util.stream.Collectors.toList;
import static org.mule.runtime.api.component.location.Location.builderFromStringRepresentation;
import static org.mule.runtime.api.connectivity.ConnectivityTestingService.CONNECTIVITY_TESTING_SERVICE_KEY;
import static org.mule.runtime.api.metadata.MetadataService.METADATA_SERVICE_KEY;
import static org.mule.runtime.api.metadata.MetadataService.NON_LAZY_METADATA_SERVICE_KEY;
import static org.mule.runtime.api.store.ObjectStoreManager.BASE_PERSISTENT_OBJECT_STORE_KEY;
import static org.mule.runtime.api.util.Preconditions.checkState;
import static org.mule.runtime.api.value.ValueProviderService.VALUE_PROVIDER_SERVICE_KEY;
import static org.mule.runtime.config.api.dsl.CoreDslConstants.CONFIGURATION_IDENTIFIER;
import static org.mule.runtime.config.internal.LazyConnectivityTestingService.NON_LAZY_CONNECTIVITY_TESTING_SERVICE;
import static org.mule.runtime.config.internal.LazyValueProviderService.NON_LAZY_VALUE_PROVIDER_SERVICE;
import static org.mule.runtime.core.api.config.MuleProperties.OBJECT_SECURITY_MANAGER;
import static org.mule.runtime.core.api.lifecycle.LifecycleUtils.disposeIfNeeded;
import static org.mule.runtime.core.api.lifecycle.LifecycleUtils.initialiseIfNeeded;
import static org.mule.runtime.core.api.lifecycle.LifecycleUtils.startIfNeeded;
import static org.mule.runtime.core.api.util.ClassUtils.withContextClassLoader;
import static org.mule.runtime.core.privileged.registry.LegacyRegistryUtils.unregisterObject;
import org.mule.runtime.api.component.ConfigurationProperties;
import org.mule.runtime.api.component.location.Location;
import org.mule.runtime.api.connectivity.ConnectivityTestingService;
import org.mule.runtime.api.exception.MuleException;
import org.mule.runtime.api.lifecycle.Initialisable;
import org.mule.runtime.api.lifecycle.Startable;
import org.mule.runtime.api.metadata.MetadataService;
import org.mule.runtime.api.util.Reference;
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
import org.mule.runtime.core.api.util.func.CheckedConsumer;
import org.mule.runtime.core.internal.connectivity.DefaultConnectivityTestingService;
import org.mule.runtime.core.internal.lifecycle.phases.DefaultLifecycleObjectSorter;
import org.mule.runtime.core.internal.metadata.MuleMetadataService;
import org.mule.runtime.core.internal.security.DefaultMuleSecurityManager;
import org.mule.runtime.core.internal.store.SharedPartitionedPersistentObjectStore;
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
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.Predicate;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.BeansException;
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

  private TrackingPostProcessor trackingPostProcessor = new TrackingPostProcessor();

  private Optional<ComponentModelInitializer> parentComponentModelInitializer;

  private BeanDependencyResolver registryBeanDependencyResolver =
      beanName -> getDependencyResolver().resolveComponentDependencies(beanName).stream()
          .map(componentName -> getRegistry().lookupByName(componentName).orElse(null))
          .filter(component -> component != null)
          .collect(toList());

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
                                 ComponentBuildingDefinitionProvider runtimeComponentBuildingDefinitionProvider)
      throws BeansException {
    super(muleContext, artifactConfigResources, artifactDeclaration, optionalObjectsController,
          extendArtifactProperties(artifactProperties), artifactType, pluginsClassLoaders, parentConfigurationProperties,
          disableXmlValidations, runtimeComponentBuildingDefinitionProvider);
    // Changes the component locator in order to allow accessing any component by location even when they are prototype
    this.componentLocator = new SpringConfigurationComponentLocator();
    // By default when a lazy context is created none of its components are enabled...
    this.applicationModel.executeOnEveryMuleComponentTree(componentModel -> componentModel.setEnabled(false));
    enableMuleObjects();

    this.parentComponentModelInitializer = parentComponentModelInitializer;

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
      muleContext.getCustomizationService().overrideDefaultServiceImpl(BASE_PERSISTENT_OBJECT_STORE_KEY,
                                                                       new SharedPartitionedPersistentObjectStore<>(new File(sharedPartitionatedPersistentObjectStorePath)));

    }
  }

  /**
   * Custom logic to only enable those components that should be created when MuleContext is created.
   * MuleConfiguration for instance is immutable and once the MuleContext is started we cannot change its values.
   * TransactionManagerFactory should be created before a TransactionManager is defined in the configuration.
   */
  private void enableMuleObjects() {
    ConfigurationDependencyResolver dependencyResolver = new ConfigurationDependencyResolver(this.applicationModel,
                                                                                             componentBuildingDefinitionRegistry);
    new MinimalApplicationModelGenerator(dependencyResolver, true)
        .getMinimalModel(componentModel -> {
          if (componentModel.getIdentifier().equals(CONFIGURATION_IDENTIFIER)) {
            return true;
          }
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
        });
  }

  private static Map<String, String> extendArtifactProperties(Map<String, String> artifactProperties) {
    Map<String, String> extendedArtifactProperties = new HashMap<>(artifactProperties);
    extendedArtifactProperties.put(MuleDeploymentProperties.MULE_LAZY_INIT_DEPLOYMENT_PROPERTY, "true");
    return extendedArtifactProperties;
  }

  @Override
  protected void prepareBeanFactory(ConfigurableListableBeanFactory beanFactory) {
    super.prepareBeanFactory(beanFactory);
    trackingPostProcessor = new TrackingPostProcessor();
    addBeanPostProcessors(beanFactory, trackingPostProcessor);
  }

  private void applyLifecycle(List<String> createdComponentModels, boolean applyStartPhase) {
    muleContext.withLifecycleLock(() -> {
      ComponentConfigurationLifecycleObjectSorter componentConfigurationLifecycleObjectSorter =
          new ComponentConfigurationLifecycleObjectSorter(registryBeanDependencyResolver);
      Map<Object, String> componentNames = new HashMap<>();

      createdComponentModels.forEach(componentName -> {

        Optional<Object> objectOptional = getRegistry().lookupByName(componentName);
        objectOptional.ifPresent(object -> {
          componentConfigurationLifecycleObjectSorter.addObject(componentName, object);
          componentNames.put(object, componentName);
        });
      });
      if (muleContext.isInitialised()) {
        for (Object object : componentConfigurationLifecycleObjectSorter.getSortedObjects()) {
          try {
            applyLifecycleMessageProcessorChainBuilder(object,
                                                       messageProcessorChain -> initialiseIfNeeded(messageProcessorChain,
                                                                                                   muleContext))
                                                                                                       .ifPresent(messageProcessorChain -> {
                                                                                                         try {
                                                                                                           muleContext
                                                                                                               .getRegistry()
                                                                                                               .registerObject(messageProcessorChainInstancesKey(componentNames
                                                                                                                   .get(object)),
                                                                                                                               messageProcessorChain
                                                                                                                                   .getMessageProcessors());
                                                                                                         } catch (RegistrationException e) {
                                                                                                           throw new RuntimeException(e);
                                                                                                         }
                                                                                                       });
            muleContext.getRegistry().applyLifecycle(object, Initialisable.PHASE_NAME);
          } catch (MuleException e) {
            throw new RuntimeException(e);
          }
        }
      }
      if (applyStartPhase && muleContext.isStarted()) {
        for (Object object : componentConfigurationLifecycleObjectSorter.getSortedObjects()) {
          try {
            if (object instanceof MessageProcessorChainBuilder) {
              startIfNeeded(muleContext.getRegistry().get(messageProcessorChainInstancesKey(componentNames.get(object))));
            }
            muleContext.getRegistry().applyLifecycle(object, Initialisable.PHASE_NAME, Startable.PHASE_NAME);
          } catch (MuleException e) {
            throw new RuntimeException(e);
          }
        }
      }
    });
  }

  class ComponentConfigurationLifecycleObjectSorter extends DefaultLifecycleObjectSorter {

    private final BeanDependencyResolver beanDependencyResolver;

    public ComponentConfigurationLifecycleObjectSorter(BeanDependencyResolver beanDependencyResolver) {
      // Only Object.class type is defined as a single bucket as we just need to sort the configuration components (DSL)
      super(new Class[] {Object.class});
      this.beanDependencyResolver = beanDependencyResolver;
    }

    /**
     * Implementation that handles duplicates. A->B->C, so if we add B, C will be added but later A will be added and its
     * dependencies were already added before.
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

  private Optional<MessageProcessorChain> applyLifecycleMessageProcessorChainBuilder(Object object,
                                                                                     CheckedConsumer<MessageProcessorChain> messageProcessorChainConsumer) {
    if (object instanceof MessageProcessorChainBuilder) {
      MessageProcessorChain messageProcessorChain = ((MessageProcessorChainBuilder) object).build();
      messageProcessorChainConsumer.accept(messageProcessorChain);
      return of(messageProcessorChain);
    }
    return empty();
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
  public void initializeComponents(Predicate<org.mule.runtime.config.internal.model.ComponentModel> componentModelPredicate,
                                   boolean applyStartPhase) {
    applyLifecycle(createComponents(of(componentModelPredicate), empty(),
                                    getParentComponentModelInitializerAdapter(applyStartPhase)),
                   applyStartPhase);
  }

  public Optional<ComponentModelInitializerAdapter> getParentComponentModelInitializerAdapter(
                                                                                              boolean applyStartPhase) {
    return parentComponentModelInitializer
        .map(componentModelInitializer -> componentModelPredicate1 -> componentModelInitializer
            .initializeComponents(componentModelPredicate1, applyStartPhase));
  }

  private List<String> createComponents(Optional<Predicate> predicateOptional, Optional<Location> locationOptional,
                                        Optional<ComponentModelInitializerAdapter> parentComponentModelInitializerAdapter) {
    checkState(predicateOptional.isPresent() != locationOptional.isPresent(), "predicate or location has to be passed");

    List<String> alreadyCreatedApplicationComponents = generateListOfComponentsToBeDisposed();
    trackingPostProcessor.startTracking();

    Reference<List<String>> createdComponents = new Reference<>();
    withContextClassLoader(muleContext.getExecutionClassLoader(), () -> {
      applicationModel.executeOnEveryMuleComponentTree(componentModel -> componentModel.setEnabled(false));

      ConfigurationDependencyResolver dependencyResolver = new ConfigurationDependencyResolver(this.applicationModel,
                                                                                               componentBuildingDefinitionRegistry);
      MinimalApplicationModelGenerator minimalApplicationModelGenerator =
          new MinimalApplicationModelGenerator(dependencyResolver);
      Reference<ApplicationModel> minimalApplicationModel = new Reference<>();
      predicateOptional
          .ifPresent(predicate -> minimalApplicationModel.set(minimalApplicationModelGenerator.getMinimalModel(predicate)));
      locationOptional
          .ifPresent(location -> minimalApplicationModel.set(minimalApplicationModelGenerator.getMinimalModel(location)));

      // First unregister any already initialized/started component
      unregisterBeans(alreadyCreatedApplicationComponents);
      if (alreadyCreatedApplicationComponents.contains(OBJECT_SECURITY_MANAGER)) {
        try {
          // Has to be created before as the factory for SecurityManager (MuleSecurityManagerConfigurator) is expecting to
          // retrieve
          // it (through MuleContext and registry) while creating it. See
          // org.mule.runtime.core.api.security.MuleSecurityManagerConfigurator.doGetObject
          muleContext.getRegistry().registerObject(OBJECT_SECURITY_MANAGER, new DefaultMuleSecurityManager());
        } catch (RegistrationException e) {
          throw new IllegalStateException("Couldn't create a new instance of Mule security manager", e);
        }
      }
      objectProviders.clear();

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

      List<String> applicationComponents =
          createApplicationComponents((DefaultListableBeanFactory) this.getBeanFactory(), minimalApplicationModel.get(), false);

      createdComponents.set(applicationComponents);

      super.prepareObjectProviders();

      // This is required to force the execution of postProcessAfterInitialization() for each created component
      applicationComponents.forEach(component -> getRegistry().lookupByName(component).get());
    });

    trackingPostProcessor.stopTracking();
    List<String> createdComponentNames = createdComponents.get();
    trackingPostProcessor.intersection(createdComponentNames);

    return createdComponentNames;
  }

  private List<String> generateListOfComponentsToBeDisposed() {
    List<String> alreadyCreatedApplicationComponents = new ArrayList<>();
    // SecurityManager has to be unregistered explicitly in order to allow registering providers on each request.
    if (!trackingPostProcessor.getBeansTracked().isEmpty()) {
      alreadyCreatedApplicationComponents.add(OBJECT_SECURITY_MANAGER);
    }
    alreadyCreatedApplicationComponents.addAll(trackingPostProcessor.getBeansTracked());
    // Reset the tracker
    trackingPostProcessor.reset();

    reverse(alreadyCreatedApplicationComponents);
    return alreadyCreatedApplicationComponents;
  }

  @Override
  protected void prepareObjectProviders() {
    // Do not prepare object providers at this point. No components are going to be created yet. This will be done when creating
    // lazy components
  }

  @Override
  public void close() {
    doUnregisterBeans(generateListOfComponentsToBeDisposed());
    super.close();
  }

  private void unregisterBeans(List<String> beanNames) {
    if (muleContext.isStarted()) {
      doUnregisterBeans(beanNames);
    }
    removeFromComponentLocator(beanNames);
  }

  private void doUnregisterBeans(List<String> beanNames) {
    beanNames.forEach(beanName -> {
      try {
        getRegistry().lookupByName(beanName)
            .ifPresent(object -> {
              if (object instanceof MessageProcessorChainBuilder) {
                disposeIfNeeded(muleContext.getRegistry().get(messageProcessorChainInstancesKey(beanName)), LOGGER);
                try {
                  unregisterObject(muleContext, messageProcessorChainInstancesKey(beanName));
                } catch (RegistrationException e) {
                  throw new RuntimeException(e);
                }
              }
            });
        unregisterObject(muleContext, beanName);
      } catch (Exception e) {
        logger
            .warn(format("Exception unregistering an object during lazy initialization of component %s, exception message is %s",
                         beanName, e.getMessage()));
        if (logger.isDebugEnabled()) {
          logger.debug(e.getMessage(), e);
        }
      }
    });
  }

  private String messageProcessorChainInstancesKey(String componentName) {
    return componentName + "_" + componentName.hashCode();
  }

  private void removeFromComponentLocator(List<String> locations) {
    locations.forEach(location -> {
      componentLocator.removeComponent(builderFromStringRepresentation(location).build());
    });
  }

  /**
   * Adapter for {@link ComponentModelInitializer} that hides the lifecycle phase from component model creation logic.
   */
  @FunctionalInterface
  private interface ComponentModelInitializerAdapter {

    void initializeComponents(Predicate<ComponentModel> componentModelPredicate);

  }

}
