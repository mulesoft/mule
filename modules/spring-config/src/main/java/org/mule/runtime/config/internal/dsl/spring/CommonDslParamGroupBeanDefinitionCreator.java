/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.config.internal.dsl.spring;

import static org.mule.runtime.config.internal.dsl.spring.BeanDefinitionFactory.SPRING_PROTOTYPE_OBJECT;

import org.mule.runtime.ast.api.ComponentAst;
import org.mule.runtime.config.internal.dsl.model.SpringComponentModel;
import org.mule.runtime.dsl.api.component.ComponentBuildingDefinition;

import java.util.Map;

import org.springframework.beans.factory.support.AbstractBeanDefinition;
import org.springframework.beans.factory.support.BeanDefinitionBuilder;

/**
 * Processor in the chain of responsibility that knows how to handle a generic {@code ComponentBuildingDefinition}.
 *
 * @since 4.0
 */
class CommonDslParamGroupBeanDefinitionCreator extends CommonBeanBaseDefinitionCreator<CreateDslParamGroupBeanDefinitionRequest> {

  public CommonDslParamGroupBeanDefinitionCreator(ObjectFactoryClassRepository objectFactoryClassRepository,
                                                  boolean disableTrimWhitespaces, boolean enableByteBuddy) {
    super(objectFactoryClassRepository, disableTrimWhitespaces, enableByteBuddy);
  }

  @Override
  protected void processComponentDefinitionModel(Map<ComponentAst, SpringComponentModel> springComponentModels,
                                                 final CreateDslParamGroupBeanDefinitionRequest request,
                                                 ComponentBuildingDefinition componentBuildingDefinition,
                                                 final BeanDefinitionBuilder beanDefinitionBuilder) {
    processObjectConstructionParameters(springComponentModels, request.getParamOwnerComponent(), null, request,
                                        new BeanDefinitionBuilderHelper(beanDefinitionBuilder));
    if (componentBuildingDefinition.isPrototype()) {
      beanDefinitionBuilder.setScope(SPRING_PROTOTYPE_OBJECT);
    }
    AbstractBeanDefinition originalBeanDefinition = beanDefinitionBuilder.getBeanDefinition();
    request.getSpringComponentModel().setBeanDefinition(originalBeanDefinition);
  }

}
