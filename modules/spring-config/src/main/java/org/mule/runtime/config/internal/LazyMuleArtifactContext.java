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
import static org.mule.runtime.api.component.location.Location.builderFromStringRepresentation;
import static org.mule.runtime.api.connectivity.ConnectivityTestingService.CONNECTIVITY_TESTING_SERVICE_KEY;
import static org.mule.runtime.api.metadata.MetadataService.METADATA_SERVICE_KEY;
import static org.mule.runtime.api.util.Preconditions.checkState;
import static org.mule.runtime.api.value.ValueProviderService.VALUE_PROVIDER_SERVICE_KEY;
import static org.mule.runtime.config.internal.LazyConnectivityTestingService.NON_LAZY_CONNECTIVITY_TESTING_SERVICE;
import static org.mule.runtime.config.internal.LazyMetadataService.NON_LAZY_METADATA_SERVICE;
import static org.mule.runtime.config.internal.LazyValueProviderService.NON_LAZY_VALUE_PROVIDER_SERVICE;
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
import org.mule.runtime.config.api.LazyComponentInitializer;
import org.mule.runtime.config.internal.dsl.model.ConfigurationDependencyResolver;
import org.mule.runtime.config.internal.dsl.model.MinimalApplicationModelGenerator;
import org.mule.runtime.config.internal.model.ApplicationModel;
import org.mule.runtime.core.api.MuleContext;
import org.mule.runtime.core.api.config.ConfigResource;
import org.mule.runtime.core.api.config.bootstrap.ArtifactType;
import org.mule.runtime.core.internal.connectivity.DefaultConnectivityTestingService;
import org.mule.runtime.core.internal.metadata.MuleMetadataService;
import org.mule.runtime.core.internal.value.MuleValueProviderService;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;

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
public class LazyMuleArtifactContext extends MuleArtifactContext implements LazyComponentInitializer, LazyComponentCreator {

  private TrackingPostProcessor trackingPostProcessor = new TrackingPostProcessor();

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
   * @since 4.0
   */
  public LazyMuleArtifactContext(MuleContext muleContext, ConfigResource[] artifactConfigResources,
                                 ArtifactDeclaration artifactDeclaration, OptionalObjectsController optionalObjectsController,
                                 Map<String, String> artifactProperties, ArtifactType artifactType,
                                 List<ClassLoader> pluginsClassLoaders,
                                 Optional<ConfigurationProperties> parentConfigurationProperties, boolean disableXmlValidations)
      throws BeansException {
    super(muleContext, artifactConfigResources, artifactDeclaration, optionalObjectsController, artifactProperties,
          artifactType, pluginsClassLoaders, parentConfigurationProperties, disableXmlValidations);
    this.applicationModel.executeOnEveryMuleComponentTree(componentModel -> componentModel.setEnabled(false));

    muleContext.getCustomizationService().overrideDefaultServiceImpl(CONNECTIVITY_TESTING_SERVICE_KEY,
                                                                     new LazyConnectivityTestingService(this, () -> getRegistry()
                                                                         .<ConnectivityTestingService>lookupByName(NON_LAZY_CONNECTIVITY_TESTING_SERVICE)
                                                                         .get()));
    muleContext.getCustomizationService().registerCustomServiceClass(NON_LAZY_CONNECTIVITY_TESTING_SERVICE,
                                                                     DefaultConnectivityTestingService.class);
    muleContext.getCustomizationService().overrideDefaultServiceImpl(METADATA_SERVICE_KEY,
                                                                     new LazyMetadataService(this, () -> getRegistry()
                                                                         .<MetadataService>lookupByName(NON_LAZY_METADATA_SERVICE)
                                                                         .get()));
    muleContext.getCustomizationService().registerCustomServiceClass(NON_LAZY_METADATA_SERVICE, MuleMetadataService.class);
    muleContext.getCustomizationService().overrideDefaultServiceImpl(VALUE_PROVIDER_SERVICE_KEY,
                                                                     new LazyValueProviderService(this, () -> getRegistry()
                                                                         .<ValueProviderService>lookupByName(NON_LAZY_VALUE_PROVIDER_SERVICE)
                                                                         .get(),
                                                                                                  muleContext::getConfigurationComponentLocator));
    muleContext.getCustomizationService().registerCustomServiceClass(NON_LAZY_VALUE_PROVIDER_SERVICE,
                                                                     MuleValueProviderService.class);


    muleContext.getCustomizationService().overrideDefaultServiceImpl(LAZY_COMPONENT_INITIALIZER_SERVICE_KEY, this);
  }

  @Override
  protected void prepareBeanFactory(ConfigurableListableBeanFactory beanFactory) {
    super.prepareBeanFactory(beanFactory);
    trackingPostProcessor = new TrackingPostProcessor();
    addBeanPostProcessors(beanFactory, trackingPostProcessor);
  }

  private void applyLifecycle(List<String> createdComponentModels) {
    muleContext.withLifecycleLock(() -> {
      if (muleContext.isInitialised()) {
        for (String createdComponentModelName : createdComponentModels) {
          Object object = getRegistry().lookupByName(createdComponentModelName).get();
          try {
            muleContext.getRegistry().applyLifecycle(object, Initialisable.PHASE_NAME);
          } catch (MuleException e) {
            throw new RuntimeException(e);
          }
        }
      }
      if (muleContext.isStarted()) {
        for (String createdComponentModelName : createdComponentModels) {
          Object object = getRegistry().lookupByName(createdComponentModelName).get();
          try {
            muleContext.getRegistry().applyLifecycle(object, Initialisable.PHASE_NAME, Startable.PHASE_NAME);
          } catch (MuleException e) {
            throw new RuntimeException(e);
          }
        }
      }
    });
  }

  @Override
  public void initializeComponent(Location location) {
    applyLifecycle(createComponents(empty(), of(location)));
  }

  @Override
  public void initializeComponents(ComponentLocationFilter filter) {
    applyLifecycle(createComponents(of(filter), empty()));
  }

  @Override
  public void createComponent(Location location) {
    createComponents(empty(), of(location));
  }

  private List<String> createComponents(Optional<ComponentLocationFilter> filterOptional, Optional<Location> locationOptional) {
    checkState(filterOptional.isPresent() != locationOptional.isPresent(), "filter or location has to be passed");

    List<String> alreadyCreatedApplicationComponents = new ArrayList<>();
    alreadyCreatedApplicationComponents.addAll(trackingPostProcessor.getBeansTracked());
    reverse(alreadyCreatedApplicationComponents);

    trackingPostProcessor.startTracking();

    Reference<List<String>> createdComponents = new Reference<>();
    withContextClassLoader(muleContext.getExecutionClassLoader(), () -> {
      // First unregister any already initialized/started component
      unregisterBeans(alreadyCreatedApplicationComponents);
      objectProviders.clear();

      ConfigurationDependencyResolver dependencyResolver = new ConfigurationDependencyResolver(this.applicationModel,
                                                                                               componentBuildingDefinitionRegistry);
      MinimalApplicationModelGenerator minimalApplicationModelGenerator =
          new MinimalApplicationModelGenerator(dependencyResolver);
      Reference<ApplicationModel> minimalApplicationModel = new Reference<>();
      filterOptional.ifPresent(filter -> minimalApplicationModel.set(minimalApplicationModelGenerator.getMinimalModel(filter)));
      locationOptional
          .ifPresent(location -> minimalApplicationModel.set(minimalApplicationModelGenerator.getMinimalModel(location)));

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

  @Override
  protected void prepareObjectProviders() {
    // Do not prepare object providers at this point. No components are going to be created yet. This will be done when creating
    // lazy components
  }

  private void unregisterBeans(List<String> beanNames) {
    if (muleContext.isStarted()) {
      beanNames.forEach(beanName -> {
        try {
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
    applicationModel.executeOnEveryMuleComponentTree(componentModel -> componentModel.setEnabled(false));

    // Refresh componentLocator
    removeFromComponentLocator(beanNames);
  }

  private void removeFromComponentLocator(List<String> locations) {
    locations.forEach(location -> {
      componentLocator.removeComponent(builderFromStringRepresentation(location).build());
    });
  }

}
