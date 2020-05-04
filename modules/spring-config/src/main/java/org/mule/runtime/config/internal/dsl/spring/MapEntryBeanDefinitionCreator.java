/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.config.internal.dsl.spring;

import static java.util.stream.Collectors.toCollection;
import static org.mule.runtime.dsl.api.component.DslSimpleType.SIMPLE_TYPE_VALUE_PARAMETER_NAME;
import static org.mule.runtime.dsl.api.component.DslSimpleType.isSimpleType;
import static org.springframework.beans.factory.support.BeanDefinitionBuilder.genericBeanDefinition;

import org.mule.runtime.ast.api.ComponentAst;
import org.mule.runtime.config.internal.dsl.model.SpringComponentModel;
import org.mule.runtime.config.internal.dsl.processor.ObjectTypeVisitor;
import org.mule.runtime.dsl.api.component.ComponentBuildingDefinition;
import org.mule.runtime.dsl.api.component.MapEntry;
import org.mule.runtime.dsl.api.component.TypeDefinition.MapEntryType;

import java.util.List;
import java.util.Map;

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
                        CreateBeanDefinitionRequest createBeanDefinitionRequest) {
    ComponentAst componentModel = createBeanDefinitionRequest.getComponentModel();
    ObjectTypeVisitor objectTypeVisitor = createBeanDefinitionRequest.retrieveTypeVisitor();
    Class<?> type = objectTypeVisitor.getType();
    if (!(MapEntryType.class.isAssignableFrom(type))) {
      return false;
    }
    ComponentBuildingDefinition componentBuildingDefinition = createBeanDefinitionRequest.getComponentBuildingDefinition();
    createBeanDefinitionRequest.getSpringComponentModel().setType(type);
    final Object key = componentModel.getRawParameterValue(ENTRY_TYPE_KEY_PARAMETER_NAME).orElse(null);
    Object keyBeanDefinition = getConvertibleBeanDefinition(objectTypeVisitor.getMapEntryType().get().getKeyType(), key,
                                                            componentBuildingDefinition.getKeyTypeConverter());


    Object value =
        // MULE-11984: Check that generated map entries are not empty
        componentModel.getRawParameterValue(ENTRY_TYPE_VALUE_REF_PARAMETER_NAME)
            .map(paramName -> (Object) new RuntimeBeanReference(paramName))
            .orElseGet(() -> getValue(springComponentModels, objectTypeVisitor, componentModel, componentBuildingDefinition));

    AbstractBeanDefinition beanDefinition = genericBeanDefinition(MapEntry.class).addConstructorArgValue(keyBeanDefinition)
        .addConstructorArgValue(value).getBeanDefinition();

    createBeanDefinitionRequest.getSpringComponentModel().setBeanDefinition(beanDefinition);
    return true;
  }

  private Object getValue(Map<ComponentAst, SpringComponentModel> springComponentModels,
                          ObjectTypeVisitor objectTypeVisitor, ComponentAst componentModel,
                          ComponentBuildingDefinition componentBuildingDefinition) {
    Object value;
    Class valueType = objectTypeVisitor.getMapEntryType().get().getValueType();
    if (isSimpleType(valueType) || componentModel.directChildrenStream().count() == 0) {
      value = getConvertibleBeanDefinition(objectTypeVisitor.getMapEntryType().get().getValueType(),
                                           componentModel.getRawParameterValue(SIMPLE_TYPE_VALUE_PARAMETER_NAME).orElse(null),
                                           componentBuildingDefinition.getTypeConverter());
    } else if (List.class.isAssignableFrom(objectTypeVisitor.getMapEntryType().get().getValueType())) {
      if (componentModel.directChildrenStream().count() == 0) {
        String valueParameter = componentModel.getRawParameterValue(SIMPLE_TYPE_VALUE_PARAMETER_NAME).orElse(null);
        value = getConvertibleBeanDefinition(valueType, valueParameter, componentBuildingDefinition.getTypeConverter());
      } else {
        ManagedList<Object> managedList = componentModel.directChildrenStream()
            .map(springComponentModels::get)
            .map(childSpringComponent -> childSpringComponent.getBeanDefinition() != null
                ? childSpringComponent.getBeanDefinition()
                : childSpringComponent.getBeanReference())
            .collect(toCollection(ManagedList::new));

        value = genericBeanDefinition(ObjectTypeVisitor.DEFAULT_COLLECTION_TYPE).addConstructorArgValue(managedList)
            .getBeanDefinition();
      }
    } else {
      value = componentModel.directChildrenStream()
          .findFirst()
          .map(springComponentModels::get)
          .map(childSpringComponent -> {
            BeanDefinition beanDefinition = childSpringComponent.getBeanDefinition();
            return beanDefinition != null ? beanDefinition : childSpringComponent.getBeanReference();
          })
          .orElse(null);
    }
    return value;
  }
}
