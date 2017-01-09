/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.config.spring;

import static java.util.Collections.emptyList;
import static org.mule.runtime.api.i18n.I18nMessageFactory.createStaticMessage;
import static org.mule.runtime.core.api.config.MuleProperties.OBJECT_CONNECTIVITY_TESTING_SERVICE;
import static org.mule.runtime.core.api.config.MuleProperties.OBJECT_METADATA_SERVICE;
import static org.mule.runtime.core.api.lifecycle.LifecycleUtils.initialiseIfNeeded;
import static org.mule.runtime.core.api.lifecycle.LifecycleUtils.startIfNeeded;
import static org.mule.runtime.core.util.ClassUtils.withContextClassLoader;
import org.mule.runtime.api.exception.MuleException;
import org.mule.runtime.api.exception.MuleRuntimeException;
import org.mule.runtime.api.lifecycle.InitialisationException;
import org.mule.runtime.api.metadata.MetadataService;
import org.mule.runtime.config.spring.dsl.model.ApplicationModel;
import org.mule.runtime.config.spring.dsl.model.MinimalApplicationModelGenerator;
import org.mule.runtime.core.api.MuleContext;
import org.mule.runtime.core.api.connectivity.ConnectivityTestingService;
import org.mule.runtime.core.config.ConfigResource;
import org.mule.runtime.core.config.bootstrap.ArtifactType;
import org.mule.runtime.api.dsl.config.ArtifactConfiguration;

import java.util.List;
import java.util.Map;

import org.springframework.beans.BeansException;
import org.springframework.beans.factory.support.BeanDefinitionReader;
import org.springframework.beans.factory.support.DefaultListableBeanFactory;
import org.xml.sax.SAXException;
import org.xml.sax.SAXParseException;

/**
 * Implementation of {@link MuleArtifactContext} that allows to create configuration components
 * lazily.
 * <p/>
 * Components will be created upon request to use the from the exposed services.
 *
 * @since 4.0
 */
public class LazyMuleArtifactContext extends MuleArtifactContext implements LazyComponentInitializer {

  private ConnectivityTestingService lazyConnectivityTestingService;
  private MetadataService metadataService;

  /**
   * Parses configuration files creating a spring ApplicationContext which is used as a parent registry using the SpringRegistry
   * registry implementation to wraps the spring ApplicationContext
   *
   * @param muleContext               the {@link MuleContext} that own this context
   * @param artifactConfiguration     the mule configuration defined programmatically
   * @param optionalObjectsController the {@link OptionalObjectsController} to use. Cannot be {@code null} @see
   *                                  org.mule.runtime.config.spring.SpringRegistry
   * @since 4.0
   */
  public LazyMuleArtifactContext(MuleContext muleContext, ConfigResource[] artifactConfigResources,
                                 ArtifactConfiguration artifactConfiguration, OptionalObjectsController optionalObjectsController,
                                 Map<String, String> artifactProperties, ArtifactType artifactType)
      throws BeansException {
    super(muleContext, artifactConfigResources, artifactConfiguration, optionalObjectsController, artifactProperties,
          artifactType);
  }

  @Override
  protected XmlConfigurationDocumentLoader newXmlConfigurationDocumentLoader() {
    return new XmlConfigurationDocumentLoader(() -> new NoOpErrorHandler());
  }

  private void createComponents(DefaultListableBeanFactory beanFactory, ApplicationModel applicationModel, boolean mustBeRoot) {
    applyLifecycle(super.createApplicationComponents(beanFactory, applicationModel, mustBeRoot));
  }

  private void applyLifecycle(List<String> createdComponentModels) {
    if (muleContext.isInitialised()) {
      for (String createdComponentModelName : createdComponentModels) {
        Object object = muleContext.getRegistry().get(createdComponentModelName);
        try {
          initialiseIfNeeded(object, true, muleContext);
        } catch (InitialisationException e) {
          throw new RuntimeException(e);
        }
      }
    }
    if (muleContext.isStarted()) {
      for (String createdComponentModelName : createdComponentModels) {
        Object object = muleContext.getRegistry().get(createdComponentModelName);
        try {
          startIfNeeded(object);
        } catch (MuleException e) {
          throw new RuntimeException(e);
        }
      }
    }
  }

  @Override
  protected void createInitialApplicationComponents(DefaultListableBeanFactory beanFactory,
                                                    BeanDefinitionReader beanDefinitionReader) {
    if (useNewParsingMechanism) {
      createComponents(beanFactory, applicationModel, true);
    } else {
      throw new MuleRuntimeException(createStaticMessage("Could not create mule application since lazy init is enabled but there are component in the configuration "
          +
          "that are not parsed with the new mechanism " + getOldParsingMechanismComponentIdentifiers()));
    }
  }

  @Override
  public void initializeComponent(String componentName) {
    withContextClassLoader(muleContext.getExecutionClassLoader(), () -> {
      if (muleContext.getRegistry().get(componentName) != null) {
        return;
      }
      MinimalApplicationModelGenerator minimalApplicationModelGenerator =
          new MinimalApplicationModelGenerator(this.applicationModel, componentBuildingDefinitionRegistry);
      ApplicationModel minimalApplicationModel;
      if (!componentName.contains("/")) {
        minimalApplicationModel = minimalApplicationModelGenerator.getMinimalModelByName(componentName);
      } else {
        minimalApplicationModel = minimalApplicationModelGenerator.getMinimalModelByPath(componentName);
      }
      createComponents((DefaultListableBeanFactory) this.getBeanFactory(), minimalApplicationModel, false);
    });
  }

  @Override
  public ConnectivityTestingService getConnectivityTestingService() {
    if (lazyConnectivityTestingService == null) {
      lazyConnectivityTestingService =
          new LazyConnectivityTestingService(this, muleContext.getRegistry().get(OBJECT_CONNECTIVITY_TESTING_SERVICE));
    }
    return lazyConnectivityTestingService;
  }

  @Override
  public MetadataService getMetadataService() {
    if (metadataService == null) {
      metadataService = new LazyMetadataService(this, muleContext.getRegistry().get(OBJECT_METADATA_SERVICE));
    }
    return metadataService;
  }

  /**
   * {@link XmlGathererErrorHandler} implementation that doesn't handle errors.
   */
  class NoOpErrorHandler implements XmlGathererErrorHandler {

    @Override
    public List<SAXParseException> getErrors() {
      return emptyList();
    }

    @Override
    public void warning(SAXParseException exception) throws SAXException {

    }

    @Override
    public void error(SAXParseException exception) throws SAXException {

    }

    @Override
    public void fatalError(SAXParseException exception) throws SAXException {

    }

  }
}
