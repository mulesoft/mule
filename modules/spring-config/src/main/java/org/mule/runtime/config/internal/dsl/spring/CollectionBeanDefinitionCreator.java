/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.config.internal.dsl.spring;

import org.mule.runtime.config.internal.model.ComponentModel;
import org.mule.runtime.config.internal.dsl.model.SpringComponentModel;
import org.mule.runtime.config.internal.dsl.processor.ObjectTypeVisitor;
import org.mule.runtime.dsl.api.component.ComponentBuildingDefinition;

import java.util.Collection;

import org.springframework.beans.factory.support.BeanDefinitionBuilder;
import org.springframework.beans.factory.support.ManagedList;

/**
 * {@code BeanDefinitionCreator} that handles components that contains a collection of elements.
 * <p>
 * 
 * <pre>
 *  <parsers-test:simple-type-child-list>
 *      <parsers-test:simple-type-child value="value1"/>
 *      <parsers-test:simple-type-child value="value2"/>
 *  </parsers-test:simple-type-child-list>
 * </pre>
 *
 * @since 4.0
 */
class CollectionBeanDefinitionCreator extends BeanDefinitionCreator {

  @Override
  boolean handleRequest(CreateBeanDefinitionRequest createBeanDefinitionRequest) {
    SpringComponentModel componentModel = (SpringComponentModel) createBeanDefinitionRequest.getComponentModel();
    ComponentBuildingDefinition componentBuildingDefinition = createBeanDefinitionRequest.getComponentBuildingDefinition();
    ObjectTypeVisitor objectTypeVisitor = new ObjectTypeVisitor(componentModel);
    componentBuildingDefinition.getTypeDefinition().visit(objectTypeVisitor);
    if (Collection.class.isAssignableFrom(objectTypeVisitor.getType())) {
      componentModel.setType(objectTypeVisitor.getType());
      ManagedList<Object> managedList = new ManagedList<>();
      for (ComponentModel innerComponent : componentModel.getInnerComponents()) {
        SpringComponentModel innerSpringComp = (SpringComponentModel) innerComponent;
        Object bean = innerSpringComp.getBeanDefinition() == null ? innerSpringComp.getBeanReference()
            : innerSpringComp.getBeanDefinition();
        managedList.add(bean);
      }
      componentModel.setBeanDefinition(BeanDefinitionBuilder.genericBeanDefinition(objectTypeVisitor.getType())
          .addConstructorArgValue(managedList).getBeanDefinition());
      return true;
    }
    return false;
  }
}
