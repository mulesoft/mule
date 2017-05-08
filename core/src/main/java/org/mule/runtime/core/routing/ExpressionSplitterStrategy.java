/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.core.routing;

import org.mule.runtime.api.el.BindingContext;
import org.mule.runtime.api.metadata.TypedValue;
import org.mule.runtime.core.api.Event;
import org.mule.runtime.core.api.el.ExpressionManager;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

public class ExpressionSplitterStrategy {

  private ExpressionManager expressionManager;

  public ExpressionSplitterStrategy(ExpressionManager expressionManager) {
    this.expressionManager = expressionManager;
  }

  public List<?> splitMessage(Event event, String expression) {
    Iterator<TypedValue<?>> result = expressionManager.split(expression, 0, event, BindingContext.builder().build());
    if (result != null) {
      List<TypedValue> values = new ArrayList<>();
      result.forEachRemaining(value -> {
        values.add(value);
      });
      return values;
    } else {
      return new ArrayList<>();
    }
  }

}
