/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.core.internal.routing;

import org.mule.runtime.core.api.el.ExpressionManager;
import org.mule.runtime.core.api.event.CoreEvent;
import org.mule.runtime.core.api.processor.Processor;
import org.mule.runtime.core.privileged.routing.RoutePathNotFoundException;

import java.util.Optional;

import javax.inject.Inject;

/**
 * Routes the event to a single<code>MessageProcessor</code> using an expression to evaluate the event being processed and find
 * the first route that can be used.
 * <p>
 * If a default route has been configured and no match has been found, the default route will be used. Otherwise it continues the
 * execution through the next MP in the chain.
 */
public class ChoiceRouter extends AbstractSelectiveRouter {

  private ExpressionManager expressionManager;

  @Override
  protected Optional<Processor> selectProcessor(CoreEvent event) {
    return getConditionalMessageProcessors().stream()
        .filter(cmp -> expressionManager.evaluateBoolean(cmp.getExpression(), event, getLocation(), false, true))
        .findFirst()
        .map(cmp -> cmp.getMessageProcessor());
  }

  @Override
  protected Processor getProcessorToRoute(CoreEvent event) throws RoutePathNotFoundException {
    try {
      return super.getProcessorToRoute(event);
    } catch (RoutePathNotFoundException e) {
      return event1 -> event1;
    }
  }

  @Inject
  public void setExpressionManager(ExpressionManager expressionManager) {
    this.expressionManager = expressionManager;
  }

}
