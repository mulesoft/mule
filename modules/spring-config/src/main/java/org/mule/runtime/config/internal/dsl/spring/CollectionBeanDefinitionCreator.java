/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.config.internal.dsl.spring;

import static java.util.stream.Collectors.toCollection;

import org.mule.runtime.ast.api.ComponentAst;
import org.mule.runtime.ast.api.ComponentParameterAst;
import org.mule.runtime.config.internal.dsl.model.SpringComponentModel;
import org.mule.runtime.config.internal.dsl.processor.ObjectTypeVisitor;

import java.util.Collection;
import java.util.Map;
import java.util.function.Consumer;

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
  boolean handleRequest(Map<ComponentAst, SpringComponentModel> springComponentModels,
                        CreateBeanDefinitionRequest createBeanDefinitionRequest,
                        Consumer<SpringComponentModel> componentBeanDefinitionHandler) {
    if (createBeanDefinitionRequest.getComponentModelHierarchy().isEmpty()) {
      return false;
    }


    ComponentAst componentModel = createBeanDefinitionRequest.getComponentModel();
    ObjectTypeVisitor objectTypeVisitor = createBeanDefinitionRequest.retrieveTypeVisitor();
    if (Collection.class.isAssignableFrom(objectTypeVisitor.getType())) {
      createBeanDefinitionRequest.getSpringComponentModel().setType(objectTypeVisitor.getType());

      final ComponentAst paramOwnerComponentModel = createBeanDefinitionRequest.getComponentModelHierarchy()
          .get(createBeanDefinitionRequest.getComponentModelHierarchy().size() - 1);
      final ComponentParameterAst param =
          paramOwnerComponentModel.getParameter(componentModel.getGenerationInformation().getSyntax().get().getAttributeName());
      Collection<ComponentAst> items = (Collection<ComponentAst>) param.getValue().getRight();

      ManagedList<Object> managedList = items.stream()
          .map(springComponentModels::get)
          .map(innerSpringComp -> innerSpringComp.getBeanDefinition() == null
              ? innerSpringComp.getBeanReference()
              : innerSpringComp.getBeanDefinition())
          .collect(toCollection(ManagedList::new));

      createBeanDefinitionRequest.getSpringComponentModel()
          .setBeanDefinition(BeanDefinitionBuilder.genericBeanDefinition(objectTypeVisitor.getType())
              .addConstructorArgValue(managedList).getBeanDefinition());

      componentBeanDefinitionHandler.accept(createBeanDefinitionRequest.getSpringComponentModel());

      return true;
    }
    return false;
  }
}
