/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.extension.oauth2.internal;

import static org.mule.runtime.api.metadata.MediaType.ANY;

import org.mule.runtime.api.el.BindingContext;
import org.mule.runtime.api.message.Attributes;
import org.mule.runtime.api.metadata.DataType;
import org.mule.runtime.api.metadata.TypedValue;
import org.mule.runtime.core.api.MuleContext;
import org.mule.runtime.extension.api.runtime.operation.ParameterResolver;
import org.mule.runtime.extension.api.runtime.operation.Result;

public class DeferredExpressionResolver {

  private final MuleContext muleContext;

  public DeferredExpressionResolver(MuleContext muleContext) {
    this.muleContext = muleContext;
  }

  public <T> T resolveExpression(ParameterResolver<T> expr, Result<Object, ? extends Attributes> result) {
    if (expr == null) {
      return null;
    } else if (!expr.getExpression().isPresent()
        || !muleContext.getExpressionManager().isExpression(expr.getExpression().get())) {
      return expr.resolve();
    } else {
      BindingContext resultContext = BindingContext.builder()
          .addBinding("payload",
                      new TypedValue(result.getOutput(), DataType.builder().fromObject(result.getOutput())
                          .mediaType(result.getMediaType().orElse(ANY)).build()))
          .addBinding("attributes",
                      new TypedValue(result.getAttributes().get(), DataType.fromObject(result.getAttributes().get())))
          .addBinding("dataType",
                      new TypedValue(DataType.builder().fromObject(result.getOutput()).mediaType(result.getMediaType().get())
                          .build(), DataType.fromType(DataType.class)))
          .build();

      return (T) muleContext.getExpressionManager().evaluate(expr.getExpression().get(), resultContext).getValue();
    }
  }

  public <T> String getExpression(ParameterResolver<T> expr) {
    if (expr == null) {
      return null;
    } else if (!expr.getExpression().isPresent()
        || !muleContext.getExpressionManager().isExpression(expr.getExpression().get())) {
      return (String) expr.resolve();
    } else {
      return expr.getExpression().get();
    }
  }

}
