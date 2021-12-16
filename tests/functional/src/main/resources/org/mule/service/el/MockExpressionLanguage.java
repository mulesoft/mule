/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.service.el;

import org.mule.runtime.api.el.BindingContext;
import org.mule.runtime.api.el.ExpressionExecutionException;
import org.mule.runtime.api.el.ExpressionLanguage;
import org.mule.runtime.api.el.ValidationResult;
import org.mule.runtime.api.metadata.DataType;
import org.mule.runtime.api.metadata.TypedValue;

import java.util.Iterator;

public class MockExpressionLanguage implements ExpressionLanguage {

  public void addGlobalBindings(BindingContext bindingContext) {
    //Do nothing
  }

  public TypedValue<?> evaluate(String expression, BindingContext context) throws ExpressionExecutionException {
    throw new UnsupportedOperationException();
  }

  public TypedValue<?> evaluate(String expression, DataType expectedOutputType, BindingContext context)
    throws ExpressionExecutionException {
    throw new UnsupportedOperationException();
  }

  public TypedValue<?> evaluateLogExpression(String expression, BindingContext context) throws ExpressionExecutionException {
    throw new UnsupportedOperationException();
  }

  public ValidationResult validate(String expression) {
    throw new UnsupportedOperationException();
  }

  public Iterator<TypedValue<?>> split(String expression, BindingContext context) {
    throw new UnsupportedOperationException();
  }

}
