/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.config.internal.dsl.spring;

import static org.mule.runtime.config.internal.model.ApplicationModel.EXCEPTION_STRATEGY_REFERENCE_IDENTIFIER;
import static org.mule.runtime.config.internal.model.ApplicationModel.REFERENCE_ATTRIBUTE;

import org.mule.runtime.config.internal.model.ComponentModel;
import org.mule.runtime.config.internal.dsl.model.SpringComponentModel;
import org.mule.runtime.core.api.exception.FlowExceptionHandler;

import org.springframework.beans.factory.config.RuntimeBeanReference;

/**
 * Processor of the chain of responsibility that knows how to create the
 * {@link org.springframework.beans.factory.config.BeanDefinition} for an exception strategy reference element.
 *
 * @since 4.0
 */
class ExceptionStrategyRefBeanDefinitionCreator extends BeanDefinitionCreator {

  @Override
  boolean handleRequest(CreateBeanDefinitionRequest createBeanDefinitionRequest) {
    ComponentModel componentModel = createBeanDefinitionRequest.getComponentModel();
    if (componentModel.getIdentifier().equals(EXCEPTION_STRATEGY_REFERENCE_IDENTIFIER)) {
      componentModel.setType(FlowExceptionHandler.class);
      ((SpringComponentModel) componentModel)
          .setBeanReference(new RuntimeBeanReference(componentModel.getParameters().get(REFERENCE_ATTRIBUTE)));
      return true;
    }
    return false;
  }
}
