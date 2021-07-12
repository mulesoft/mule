/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.config.internal.dsl.spring;

import static java.util.stream.Collectors.toMap;
import static org.mule.runtime.api.component.Component.ANNOTATIONS_PROPERTY_NAME;
import static org.mule.runtime.api.component.Component.Annotations.SOURCE_ELEMENT_ANNOTATION_KEY;
import static org.mule.runtime.core.privileged.execution.LocationExecutionContextProvider.maskPasswords;
import static org.springframework.beans.factory.support.BeanDefinitionBuilder.genericBeanDefinition;
import static org.springframework.beans.factory.support.BeanDefinitionBuilder.rootBeanDefinition;

import org.mule.runtime.api.component.Component;
import org.mule.runtime.api.util.LazyValue;
import org.mule.runtime.ast.api.ComponentAst;
import org.mule.runtime.ast.api.ComponentMetadataAst;
import org.mule.runtime.config.internal.dsl.model.SpringComponentModel;
import org.mule.runtime.dsl.api.component.ComponentBuildingDefinition;
import org.mule.runtime.module.extension.internal.runtime.resolver.ValueResolver;

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

  public CommonBeanBaseDefinitionCreator(ObjectFactoryClassRepository objectFactoryClassRepository) {
    this.objectFactoryClassRepository = objectFactoryClassRepository;
  }

  @Override
  public boolean handleRequest(Map<ComponentAst, SpringComponentModel> springComponentModels, R request) {
    ComponentBuildingDefinition buildingDefinition = request.getComponentBuildingDefinition();
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
      addMetadataAnnotationsFromDocAttributes(annotations, componentModel.getMetadata());
      builder.getBeanDefinition().getPropertyValues().addPropertyValue(ANNOTATIONS_PROPERTY_NAME, annotations);
    }

    return annotations;
  }

  /**
   * Populates the passed beanAnnotations with the other passed parameters.
   *
   * @param beanAnnotations the map with annotations to populate
   * @param metadata        the parser metadata for the object being created
   */
  protected void addMetadataAnnotationsFromDocAttributes(Map<QName, Object> beanAnnotations,
                                                         ComponentMetadataAst metadata) {
    String sourceCode = metadata.getSourceCode().orElse(null);

    if (sourceCode != null) {
      beanAnnotations.put(SOURCE_ELEMENT_ANNOTATION_KEY, maskPasswords(sourceCode));
    }

    beanAnnotations.putAll(metadata.getDocAttributes().entrySet().stream()
        .collect(toMap(e -> QName.valueOf(e.getKey()), Entry::getValue)));
  }

  private BeanDefinitionBuilder createBeanDefinitionBuilderFromObjectFactory(final SpringComponentModel componentModel,
                                                                             final ComponentBuildingDefinition componentBuildingDefinition) {
    Class<?> objectFactoryType = componentBuildingDefinition.getObjectFactoryType();

    return rootBeanDefinition(objectFactoryClassRepository
        .getObjectFactoryClass(componentBuildingDefinition, objectFactoryType, componentModel.getType(),
                               new LazyValue<>(() -> componentModel.getBeanDefinition().isLazyInit())));
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
                                        beanDefinitionBuilderHelper)
                                            .processConfiguration();

  }

}
