/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.config.spring.factories;

import org.mule.runtime.core.api.MuleContext;
import org.mule.runtime.core.api.processor.MessageProcessorChainBuilder;
import org.mule.runtime.core.processor.chain.ModuleOperationMessageProcessorChainBuilder;

import java.util.HashMap;
import java.util.Map;

import javax.inject.Inject;

public class ModuleOperationMessageProcessorChainFactoryBean extends MessageProcessorChainFactoryBean {

  private Map<String, String> properties = new HashMap<>();
  private Map<String, String> parameters = new HashMap<>();
  private boolean returnsVoid;
  @Inject
  private MuleContext muleContext;

  @Override
  protected MessageProcessorChainBuilder getBuilderInstance() {
    MessageProcessorChainBuilder builder =
        new ModuleOperationMessageProcessorChainBuilder(properties, parameters, returnsVoid, muleContext.getExpressionManager());
    return builder;
  }

  public void setProperties(Map<String, String> properties) {
    this.properties = properties;
  }

  public void setParameters(Map<String, String> parameters) {
    this.parameters = parameters;
  }

  public void setReturnsVoid(boolean returnsVoid) {
    this.returnsVoid = returnsVoid;
  }
}
