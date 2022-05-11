/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.config.internal.dsl.spring;

import static java.util.stream.Collectors.toCollection;

import org.mule.runtime.ast.api.ComponentAst;
import org.mule.runtime.config.internal.dsl.model.SpringComponentModel;

import java.util.Collection;
import java.util.Map;

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
class CollectionBeanDefinitionCreator extends BeanDefinitionCreator<CreateParamBeanDefinitionRequest> {

  @Override
  boolean handleRequest(Map<ComponentAst, SpringComponentModel> springComponentModels,
                        CreateParamBeanDefinitionRequest request) {
    if (request.getComponentHierarchy().isEmpty()) {
      return false;
    }

    Class<Object> type = request.getSpringComponentModel().getType();
    if (Collection.class.isAssignableFrom(type)) {
      doHandleRequest(springComponentModels, request, type);
      return true;
    }

    return false;
  }

  private void doHandleRequest(Map<ComponentAst, SpringComponentModel> springComponentModels,
                               CreateParamBeanDefinitionRequest request, Class<Object> type) {
    request.getSpringComponentModel().setType(type);

    Collection<ComponentAst> items = (Collection<ComponentAst>) request.getParam().getValue().getRight();

    items.forEach(request.getNestedComponentParamProcessor());

    ManagedList<Object> managedList = items.stream()
        .map(springComponentModels::get)
        .map(innerSpringComp -> innerSpringComp.getBeanDefinition() == null
            ? innerSpringComp.getBeanReference()
            : innerSpringComp.getBeanDefinition())
        .collect(toCollection(ManagedList::new));

    request.getSpringComponentModel()
        .setBeanDefinition(BeanDefinitionBuilder.genericBeanDefinition(type)
            .addConstructorArgValue(managedList).getBeanDefinition());
  }
}
