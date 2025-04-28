/*
 * Copyright 2023 Salesforce, Inc. All rights reserved.
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.config.internal.dsl.spring;

import static org.mule.runtime.api.component.Component.ANNOTATIONS_PROPERTY_NAME;
import static org.mule.runtime.api.component.Component.NS_MULE_DOCUMENTATION;
import static org.mule.runtime.api.component.Component.Annotations.SOURCE_ELEMENT_ANNOTATION_KEY;
import static org.mule.runtime.config.internal.dsl.spring.ObjectFactoryClassRepository.IS_EAGER_INIT;
import static org.mule.runtime.config.internal.dsl.spring.ObjectFactoryClassRepository.IS_PROTOTYPE;
import static org.mule.runtime.config.internal.dsl.spring.ObjectFactoryClassRepository.IS_SINGLETON;
import static org.mule.runtime.core.internal.execution.LocationExecutionContextProvider.maskPasswords;

import static java.util.stream.Collectors.toMap;

import static javax.xml.namespace.QName.valueOf;
import static org.springframework.beans.factory.support.BeanDefinitionBuilder.genericBeanDefinition;
import static org.springframework.beans.factory.support.BeanDefinitionBuilder.rootBeanDefinition;

import org.mule.runtime.api.component.Component;
import org.mule.runtime.api.util.LazyValue;
import org.mule.runtime.ast.api.ComponentAst;
import org.mule.runtime.config.internal.dsl.model.SpringComponentModel;
import org.mule.runtime.dsl.api.component.ComponentBuildingDefinition;
import org.mule.runtime.module.extension.api.runtime.resolver.ValueResolver;

import java.util.HashMap;
import java.util.Map;
import java.util.Map.Entry;

import javax.xml.namespace.QName;

import org.springframework.beans.factory.support.BeanDefinitionBuilder;

/**
 * Processor in the chain of responsibility that knows how to handle a generic {@code ComponentBuildingDefinition}.
 *
 * @since 4.0
 */
abstract class CommonBeanBaseDefinitionCreator<R extends CreateBeanDefinitionRequest> extends BeanDefinitionCreator<R> {

  private final ObjectFactoryClassRepository objectFactoryClassRepository;
  private final boolean disableTrimWhitespaces;

  public CommonBeanBaseDefinitionCreator(ObjectFactoryClassRepository objectFactoryClassRepository,
                                         boolean disableTrimWhitespaces) {
    this.objectFactoryClassRepository = objectFactoryClassRepository;
    this.disableTrimWhitespaces = disableTrimWhitespaces;
  }

  @Override
  public boolean handleRequest(Map<ComponentAst, SpringComponentModel> springComponentModels, R request) {
    ComponentBuildingDefinition buildingDefinition = request.getComponentBuildingDefinition();
    if (buildingDefinition == null) {
      return false;
    }
    BeanDefinitionBuilder beanDefinitionBuilder =
        createBeanDefinitionBuilder(request.getSpringComponentModel(), buildingDefinition);
    processAnnotations(request.getSpringComponentModel(), beanDefinitionBuilder);
    processComponentDefinitionModel(springComponentModels, request, buildingDefinition, beanDefinitionBuilder);

    return true;
  }

  private BeanDefinitionBuilder createBeanDefinitionBuilder(SpringComponentModel componentModel,
                                                            ComponentBuildingDefinition buildingDefinition) {
    if (buildingDefinition.getObjectFactoryType() != null) {
      return createBeanDefinitionBuilderFromObjectFactory(componentModel, buildingDefinition);
    } else {
      return genericBeanDefinition(componentModel.getType());
    }
  }

  protected final void processAnnotations(SpringComponentModel componentModel, BeanDefinitionBuilder beanDefinitionBuilder) {
    if (Component.class.isAssignableFrom(componentModel.getType())
        // ValueResolver end up generating pojos from the extension whose class is enhanced to have annotations
        || ValueResolver.class.isAssignableFrom(componentModel.getType())) {
      Map<QName, Object> annotations =
          processMetadataAnnotationsHelper(beanDefinitionBuilder, componentModel.getComponent());
      if (!annotations.isEmpty()) {
        beanDefinitionBuilder.addPropertyValue(ANNOTATIONS_PROPERTY_NAME, annotations);
      }
    }
  }

  private Map<QName, Object> processMetadataAnnotationsHelper(BeanDefinitionBuilder builder, ComponentAst componentModel) {
    Map<QName, Object> annotations = new HashMap<>();

    if (componentModel != null
        && Component.class.isAssignableFrom(builder.getBeanDefinition().getBeanClass())) {
      addMetadataAnnotationsFromDocAttributes(annotations, componentModel);
      builder.getBeanDefinition().getPropertyValues().addPropertyValue(ANNOTATIONS_PROPERTY_NAME, annotations);
    }

    return annotations;
  }

  /**
   * Populates the passed beanAnnotations with the other passed parameters.
   *
   * @param beanAnnotations the map with annotations to populate
   * @param component       the parser metadata for the object being created
   */
  protected void addMetadataAnnotationsFromDocAttributes(Map<QName, Object> beanAnnotations,
                                                         ComponentAst component) {
    String sourceCode = component.getMetadata().getSourceCode().orElse(null);

    if (sourceCode != null) {
      beanAnnotations.put(SOURCE_ELEMENT_ANNOTATION_KEY, maskPasswords(sourceCode));
    }

    beanAnnotations.putAll(component.getAnnotations().entrySet().stream()
        .collect(toMap(e -> valueOf(e.getKey()), Entry::getValue)));

    beanAnnotations.putAll(component.getMetadata().getDocAttributes().entrySet().stream()
        .collect(toMap(e -> new QName(NS_MULE_DOCUMENTATION, e.getKey()), Entry::getValue)));
  }

  private BeanDefinitionBuilder createBeanDefinitionBuilderFromObjectFactory(final SpringComponentModel componentModel,
                                                                             final ComponentBuildingDefinition componentBuildingDefinition) {
    Class<?> objectFactoryType = componentBuildingDefinition.getObjectFactoryType();

    return rootBeanDefinition(objectFactoryClassRepository
        .getObjectFactoryClass(objectFactoryType, componentModel.getType()))
        .addPropertyValue(IS_SINGLETON, !componentBuildingDefinition.isPrototype())
        .addPropertyValue(IS_PROTOTYPE, componentBuildingDefinition.isPrototype())
        .addPropertyValue(IS_EAGER_INIT, new LazyValue<>(() -> !componentModel.getBeanDefinition().isLazyInit()));

  }

  protected abstract void processComponentDefinitionModel(Map<ComponentAst, SpringComponentModel> springComponentModels,
                                                          final R request,
                                                          ComponentBuildingDefinition componentBuildingDefinition,
                                                          final BeanDefinitionBuilder beanDefinitionBuilder);

  protected final void processObjectConstructionParameters(Map<ComponentAst, SpringComponentModel> springComponentModels,
                                                           ComponentAst ownerComponent, final ComponentAst componentModel,
                                                           CreateBeanDefinitionRequest createBeanDefinitionRequest,
                                                           final BeanDefinitionBuilderHelper beanDefinitionBuilderHelper) {
    new ComponentConfigurationBuilder<>(springComponentModels, ownerComponent, componentModel, createBeanDefinitionRequest,
                                        beanDefinitionBuilderHelper, disableTrimWhitespaces)
        .processConfiguration();

  }

}
