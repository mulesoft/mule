/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.config.internal.dsl.spring;

import static org.mule.runtime.api.meta.model.parameter.ParameterGroupModel.DEFAULT_GROUP_NAME;

import org.mule.runtime.ast.api.ComponentAst;
import org.mule.runtime.ast.api.ComponentParameterAst;

/**
 * Bean definition creator for elements that end up representing simple types.
 * <p>
 * Elements that represent a simple type have the form {@code <element value="simpleValue"/>} or
 * {@code <element>simpleValue</element>}
 *
 * @since 4.4
 */
class SimpleTypeBeanComponentDefinitionCreator extends SimpleTypeBeanBaseDefinitionCreator<CreateComponentBeanDefinitionRequest> {

  @Override
  protected boolean doHandleRequest(CreateComponentBeanDefinitionRequest createBeanDefinitionRequest, Class<?> type) {
    ComponentAst component = createBeanDefinitionRequest.getComponent();
    final ComponentParameterAst valueParam = component.getParameter(DEFAULT_GROUP_NAME, "value");

    if (valueParam == null || valueParam.getResolvedRawValue() == null) {
      return false;
    }

    this.setConvertibleBeanDefinition(createBeanDefinitionRequest, type, valueParam.getResolvedRawValue());
    return true;
  }

}
