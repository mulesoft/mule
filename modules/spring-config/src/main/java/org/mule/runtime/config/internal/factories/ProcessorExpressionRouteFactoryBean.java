/*
 * Copyright 2023 Salesforce, Inc. All rights reserved.
 */
package org.mule.runtime.config.internal.factories;

import org.mule.runtime.core.internal.routing.ProcessorExpressionRoute;
import org.mule.runtime.core.privileged.processor.chain.MessageProcessorChain;

public class ProcessorExpressionRouteFactoryBean extends AbstractProcessorRouteFactoryBean<ProcessorExpressionRoute> {

  private String expression;

  public void setExpression(String expression) {
    this.expression = expression;
  }

  @Override
  protected ProcessorExpressionRoute getProcessorRoute(MessageProcessorChain chain) {
    return new ProcessorExpressionRoute(expression, chain, initialSpanInfoProvider);
  }
}
