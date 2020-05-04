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
import org.mule.runtime.dsl.api.component.ComponentBuildingDefinition;

import java.util.Map;

import org.springframework.beans.factory.support.BeanDefinitionBuilder;
import org.springframework.beans.factory.support.ManagedList;

/**
 * {@code BeanDefinitionCreator} that handles components that define a map in the configuration.
 *
 * <p>
 *
 * <pre>
 *  <parsers-test:complex-type-map>
 *      <parsers-test:complex-type-entry key="1">
 *          <parsers-test:parameter-collection-parser firstname="Pablo" lastname="La Greca" age="32"/>
 *      </parsers-test:complex-type-entry>
 *      <parsers-test:complex-type-entry key="2">
 *          <parsers-test:parameter-collection-parser firstname="Mariano" lastname="Gonzalez" age="31"/>
 *      </parsers-test:complex-type-entry>
 *  </parsers-test:complex-type-map>
 * </pre>
 *
 * @since 4.0
 */
class MapBeanDefinitionCreator extends BeanDefinitionCreator {

  @Override
  boolean handleRequest(Map<ComponentAst, SpringComponentModel> springComponentModels,
                        CreateBeanDefinitionRequest createBeanDefinitionRequest) {
    ComponentAst componentModel = createBeanDefinitionRequest.getComponentModel();
    ComponentBuildingDefinition componentBuildingDefinition = createBeanDefinitionRequest.getComponentBuildingDefinition();
    Class<?> type = createBeanDefinitionRequest.retrieveTypeVisitor().getType();
    if (Map.class.isAssignableFrom(type) && componentBuildingDefinition.getObjectFactoryType() == null) {
      ManagedList managedList = componentModel.directChildrenStream()
          .map(springComponentModels::get)
          .map(SpringComponentModel::getBeanDefinition)
          .collect(toCollection(ManagedList::new));
      createBeanDefinitionRequest.getSpringComponentModel()
          .setBeanDefinition(BeanDefinitionBuilder.genericBeanDefinition(MapFactoryBean.class)
              .addConstructorArgValue(managedList).addConstructorArgValue(type).getBeanDefinition());
      return true;
    }
    return false;
  }
}
