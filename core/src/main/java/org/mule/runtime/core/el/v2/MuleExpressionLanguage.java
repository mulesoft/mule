/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.core.el.v2;

import static java.util.ServiceLoader.load;
import org.mule.runtime.api.metadata.DataType;
import org.mule.runtime.api.metadata.TypedValue;
import org.mule.runtime.core.api.el.v2.BindingContext;
import org.mule.runtime.core.api.el.v2.ExpressionExecutor;
import org.mule.runtime.core.api.el.v2.ExpressionLanguage;
import org.mule.runtime.core.api.el.v2.ValidationResult;

import java.util.Iterator;

public class MuleExpressionLanguage implements ExpressionLanguage {

  private ExpressionExecutor expressionExecutor;
  private BindingContext globalBindingContext;

  public MuleExpressionLanguage() {
    Iterator<ExpressionExecutor> executors = load(ExpressionExecutor.class).iterator();
    while (executors.hasNext()) {
      //TODO: MULE-10410 - define how to handle dw and mvel at the same time
      this.expressionExecutor = executors.next();
      break;
    }
    // TODO: MULE-10424 - Define bindings to use in Mule 4
    this.globalBindingContext = BindingContext.builder().build();
  }

  @Override
  public TypedValue evaluate(String expression, BindingContext context) {
    return expressionExecutor.evaluate(expression, context);
  }

  @Override
  public TypedValue evaluate(String expression, DataType expectedOutputType, BindingContext context) {
    return expressionExecutor.evaluate(expression, expectedOutputType, context);
  }

  @Override
  public ValidationResult validate(String expression, BindingContext context) {
    return expressionExecutor.validate(expression, context);
  }
}
