/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.config.internal.dsl.spring;

import static java.util.stream.Collectors.toCollection;
import static org.mule.runtime.config.internal.dsl.processor.ObjectTypeVisitor.DEFAULT_COLLECTION_TYPE;
import static org.mule.runtime.dsl.api.component.DslSimpleType.SIMPLE_TYPE_VALUE_PARAMETER_NAME;
import static org.mule.runtime.dsl.api.component.DslSimpleType.isSimpleType;
import static org.springframework.beans.factory.support.BeanDefinitionBuilder.genericBeanDefinition;

import org.mule.runtime.ast.api.ComponentAst;
import org.mule.runtime.ast.api.ComponentParameterAst;
import org.mule.runtime.config.internal.dsl.model.SpringComponentModel;
import org.mule.runtime.dsl.api.component.ComponentBuildingDefinition;
import org.mule.runtime.dsl.api.component.MapEntry;
import org.mule.runtime.dsl.api.component.TypeDefinition.MapEntryType;

import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.function.Consumer;

import org.springframework.beans.factory.config.BeanDefinition;
import org.springframework.beans.factory.config.RuntimeBeanReference;
import org.springframework.beans.factory.support.AbstractBeanDefinition;
import org.springframework.beans.factory.support.ManagedList;

/**
 * {@code BeanDefinitionCreator} that handles component that define a map entry.
 * <p>
 *
 * <pre>
 *  <parsers-test:simple-type-entry key="key1" value="1"/>
 * </pre>
 *
 * or
 *
 * <pre>
 *  <parsers-test:simple-type-entry key="key1" value-ref="anotherDefinition"/>
 * </pre>
 *
 * or
 *
 * <pre>
 *  <parsers-test:complex-type-entry key="1">
 *      <parsers-test:parameter-collection-parser firstname="Pablo" lastname="La Greca" age="32"/>
 *  </parsers-test:complex-type-entry>
 * </pre>
 *
 * @since 4.0
 */
class MapEntryBeanDefinitionCreator extends BeanDefinitionCreator {

  private static final String ENTRY_TYPE_KEY_PARAMETER_NAME = "key";
  private static final String ENTRY_TYPE_VALUE_REF_PARAMETER_NAME = "value-ref";

  @Override
  boolean handleRequest(Map<ComponentAst, SpringComponentModel> springComponentModels,
                        CreateBeanDefinitionRequest createBeanDefinitionRequest,
                        Consumer<ComponentAst> nestedComponentParamProcessor,
                        Consumer<SpringComponentModel> componentBeanDefinitionHandler) {
    ComponentAst componentModel = createBeanDefinitionRequest.getComponentModel();
    Class<?> type = createBeanDefinitionRequest.getSpringComponentModel().getType();
    if (!(MapEntryType.class.isAssignableFrom(type))) {
      return false;
    }
    ComponentBuildingDefinition componentBuildingDefinition = createBeanDefinitionRequest.getComponentBuildingDefinition();
    createBeanDefinitionRequest.getSpringComponentModel().setType(type);
    final String key = componentModel.getParameter(ENTRY_TYPE_KEY_PARAMETER_NAME).getResolvedRawValue();
    MapEntryType mapEntryType = createBeanDefinitionRequest.getSpringComponentModel().getMapEntryType();
    Object keyBeanDefinition = getConvertibleBeanDefinition(mapEntryType.getKeyType(), key,
                                                            componentBuildingDefinition.getKeyTypeConverter());

    final ComponentParameterAst valueRefParam = componentModel.getParameter(ENTRY_TYPE_VALUE_REF_PARAMETER_NAME);
    Object value;
    // MULE-11984: Check that generated map entries are not empty
    if (valueRefParam != null) {
      value = new RuntimeBeanReference(valueRefParam.getResolvedRawValue());
    } else {
      value = getValue(springComponentModels, mapEntryType, componentModel, componentBuildingDefinition,
                       nestedComponentParamProcessor);
    }

    AbstractBeanDefinition beanDefinition = genericBeanDefinition(MapEntry.class).addConstructorArgValue(keyBeanDefinition)
        .addConstructorArgValue(value).getBeanDefinition();

    createBeanDefinitionRequest.getSpringComponentModel().setBeanDefinition(beanDefinition);

    componentBeanDefinitionHandler.accept(createBeanDefinitionRequest.getSpringComponentModel());

    return true;
  }

  private Object getValue(Map<ComponentAst, SpringComponentModel> springComponentModels,
                          MapEntryType mapEntryType, ComponentAst componentModel,
                          ComponentBuildingDefinition componentBuildingDefinition,
                          Consumer<ComponentAst> nestedComponentParamProcessor) {
    Class valueType = mapEntryType.getValueType();

    return componentModel.getParameter(SIMPLE_TYPE_VALUE_PARAMETER_NAME).getValue()
        .mapLeft(v -> getConvertibleBeanDefinition(valueType, "#[" + v + "]", componentBuildingDefinition.getTypeConverter()))
        .mapRight(v -> {
          if (isSimpleType(valueType)) {
            return getConvertibleBeanDefinition(valueType, v, componentBuildingDefinition.getTypeConverter());
          } else if (List.class.isAssignableFrom(valueType)) {
            final Collection<ComponentAst> values = (Collection<ComponentAst>) v;
            values.forEach(nestedComponentParamProcessor);
            ManagedList<Object> managedList = values.stream()
                .map(springComponentModels::get)
                .map(childSpringComponent -> childSpringComponent.getBeanDefinition() != null
                    ? childSpringComponent.getBeanDefinition()
                    : childSpringComponent.getBeanReference())
                .collect(toCollection(ManagedList::new));

            return genericBeanDefinition(DEFAULT_COLLECTION_TYPE)
                .addConstructorArgValue(managedList)
                .getBeanDefinition();
          } else {
            nestedComponentParamProcessor.accept((ComponentAst) v);
            final SpringComponentModel childSpringComponent = springComponentModels.get(v);
            BeanDefinition beanDefinition = childSpringComponent.getBeanDefinition();
            return beanDefinition != null ? beanDefinition : childSpringComponent.getBeanReference();
          }
        })
        .getValue()
        .orElse(null);
  }
}
