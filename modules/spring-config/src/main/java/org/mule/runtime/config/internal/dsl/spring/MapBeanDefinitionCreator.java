/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.config.internal.dsl.spring;

import static java.util.stream.Collectors.toCollection;
import static org.springframework.beans.factory.support.BeanDefinitionBuilder.genericBeanDefinition;

import org.mule.runtime.ast.api.ComponentAst;
import org.mule.runtime.config.internal.dsl.model.SpringComponentModel;
import org.mule.runtime.dsl.api.component.ComponentBuildingDefinition;

import java.util.Collection;
import java.util.Map;
import java.util.function.Consumer;

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
class MapBeanDefinitionCreator extends BeanDefinitionCreator<CreateParamBeanDefinitionRequest> {

  @Override
  boolean handleRequest(Map<ComponentAst, SpringComponentModel> springComponentModels,
                        CreateParamBeanDefinitionRequest createBeanDefinitionRequest,
                        Consumer<ComponentAst> nestedComponentParamProcessor,
                        Consumer<SpringComponentModel> componentBeanDefinitionHandler) {

    if (createBeanDefinitionRequest.getComponentModelHierarchy().isEmpty()) {
      return false;
    }

    ComponentBuildingDefinition componentBuildingDefinition = createBeanDefinitionRequest.getComponentBuildingDefinition();
    Class<?> type = createBeanDefinitionRequest.getSpringComponentModel().getType();
    if (Map.class.isAssignableFrom(type) && componentBuildingDefinition.getObjectFactoryType() == null) {

      Collection<ComponentAst> items = (Collection<ComponentAst>) createBeanDefinitionRequest.getParam().getValue().getRight();

      items.forEach(nestedComponentParamProcessor);

      ManagedList managedList = items.stream()
          .map(springComponentModels::get)
          .map(SpringComponentModel::getBeanDefinition)
          .collect(toCollection(ManagedList::new));

      createBeanDefinitionRequest.getSpringComponentModel()
          .setBeanDefinition(genericBeanDefinition(MapFactoryBean.class)
              .addConstructorArgValue(managedList).addConstructorArgValue(type).getBeanDefinition());

      componentBeanDefinitionHandler.accept(createBeanDefinitionRequest.getSpringComponentModel());

      return true;
    }
    return false;
  }
}
