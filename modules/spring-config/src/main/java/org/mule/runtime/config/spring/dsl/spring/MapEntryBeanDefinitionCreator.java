/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.config.spring.dsl.spring;

import static org.mule.runtime.config.spring.dsl.spring.DslSimpleType.SIMPLE_TYPE_VALUE_PARAMETER_NAME;
import static org.mule.runtime.config.spring.dsl.spring.DslSimpleType.isSimpleType;
import static org.springframework.beans.factory.support.BeanDefinitionBuilder.genericBeanDefinition;
import org.mule.runtime.config.spring.dsl.api.ComponentBuildingDefinition;
import org.mule.runtime.config.spring.dsl.api.MapEntry;
import org.mule.runtime.config.spring.dsl.api.TypeDefinition.MapEntryType;
import org.mule.runtime.config.spring.dsl.model.ComponentModel;
import org.mule.runtime.config.spring.dsl.processor.ObjectTypeVisitor;

import java.util.List;
import java.util.stream.Collectors;

import org.springframework.beans.factory.config.BeanDefinition;
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
 *  <parsers-test:complex-type-entry key="1">
 *      <parsers-test:parameter-collection-parser firstname="Pablo" lastname="La Greca" age="32"/>
 *  </parsers-test:complex-type-entry>
 * </pre>
 *
 * @since 4.0
 */
public class MapEntryBeanDefinitionCreator extends BeanDefinitionCreator {

  private static final String ENTRY_TYPE_KEY_PARAMETER_NAME = "key";

  @Override
  boolean handleRequest(CreateBeanDefinitionRequest createBeanDefinitionRequest) {
    ObjectTypeVisitor objectTypeVisitor = new ObjectTypeVisitor(createBeanDefinitionRequest.getComponentModel());
    createBeanDefinitionRequest.getComponentBuildingDefinition().getTypeDefinition().visit(objectTypeVisitor);
    Class<?> type = objectTypeVisitor.getType();
    if (!(MapEntryType.class.isAssignableFrom(type))) {
      return false;
    }
    ComponentModel componentModel = createBeanDefinitionRequest.getComponentModel();
    ComponentBuildingDefinition componentBuildingDefinition = createBeanDefinitionRequest.getComponentBuildingDefinition();
    componentModel.setType(type);
    final Object key = componentModel.getParameters().get(ENTRY_TYPE_KEY_PARAMETER_NAME);
    Object keyBeanDefinition = getConvertibleBeanDefinition(objectTypeVisitor.getMapEntryType().get().getKeyType(), key,
                                                            componentBuildingDefinition.getKeyTypeConverter());
    Object value;
    Class valueType = objectTypeVisitor.getMapEntryType().get().getValueType();
    if (isSimpleType(valueType) || componentModel.getInnerComponents().isEmpty()) {
      value = getConvertibleBeanDefinition(objectTypeVisitor.getMapEntryType().get().getValueType(),
                                           componentModel.getParameters().get(SIMPLE_TYPE_VALUE_PARAMETER_NAME),
                                           componentBuildingDefinition.getTypeConverter());
    } else if (List.class.isAssignableFrom(objectTypeVisitor.getMapEntryType().get().getValueType())) {
      if (componentModel.getInnerComponents().isEmpty()) {
        String valueParameter = componentModel.getParameters().get(SIMPLE_TYPE_VALUE_PARAMETER_NAME);
        value = getConvertibleBeanDefinition(valueType, valueParameter, componentBuildingDefinition.getTypeConverter());
      } else {
        ManagedList<Object> managedList = componentModel
            .getInnerComponents().stream().map(childComponent -> childComponent.getBeanDefinition() != null
                ? childComponent.getBeanDefinition() : childComponent.getBeanReference())
            .collect(Collectors.toCollection(ManagedList::new));

        value = genericBeanDefinition(ObjectTypeVisitor.DEFAULT_COLLECTION_TYPE).addConstructorArgValue(managedList)
            .getBeanDefinition();
      }
    } else {
      ComponentModel childComponentModel = componentModel.getInnerComponents().get(0);
      BeanDefinition beanDefinition = childComponentModel.getBeanDefinition();
      value = beanDefinition != null ? beanDefinition : childComponentModel.getBeanReference();
    }
    AbstractBeanDefinition beanDefinition = genericBeanDefinition(MapEntry.class).addConstructorArgValue(keyBeanDefinition)
        .addConstructorArgValue(value).getBeanDefinition();

    componentModel.setBeanDefinition(beanDefinition);
    return true;

  }
}
