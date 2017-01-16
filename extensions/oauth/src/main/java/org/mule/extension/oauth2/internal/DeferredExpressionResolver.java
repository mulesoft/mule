/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.extension.oauth2.internal;

import org.mule.runtime.core.api.Event;
import org.mule.runtime.core.api.MuleContext;
import org.mule.runtime.extension.api.runtime.operation.ParameterResolver;

public class DeferredExpressionResolver {

  private final MuleContext muleContext;

  public DeferredExpressionResolver(MuleContext muleContext) {
    this.muleContext = muleContext;
  }

  public <T> T resolveExpression(ParameterResolver<T> expr, Event event) {
    if (expr == null) {
      return null;
    } else if (!expr.getExpression().isPresent()
        || !muleContext.getExpressionManager().isExpression(expr.getExpression().get())) {
      return expr.resolve();
    } else {
      return (T) muleContext.getExpressionManager().evaluate(expr.getExpression().get(), event).getValue();
    }
  }

}
