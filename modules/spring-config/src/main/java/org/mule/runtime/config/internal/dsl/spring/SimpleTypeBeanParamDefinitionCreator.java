/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.config.internal.dsl.spring;

import org.mule.runtime.ast.api.ComponentParameterAst;

/**
 * Bean definition creator for elements that end up representing simple types.
 * <p>
 * Elements that represent a simple type have the form {@code <element value="simpleValue"/>} or
 * {@code <element>simpleValue</element>}
 *
 * @since 4.4
 */
class SimpleTypeBeanParamDefinitionCreator extends SimpleTypeBeanBaseDefinitionCreator<CreateParamBeanDefinitionRequest> {

  protected boolean doHandleRequest(CreateParamBeanDefinitionRequest createBeanDefinitionRequest, Class<?> type) {
    final ComponentParameterAst param = createBeanDefinitionRequest.getParam();
    this.setConvertibleBeanDefinition(createBeanDefinitionRequest, type,
                                      (String) param.getValue().mapLeft(expr -> "#[" + expr + "]").getValue().orElse(null));
    return true;
  }

}
