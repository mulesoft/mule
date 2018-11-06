/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.core.internal.el;

import org.mule.runtime.api.metadata.DataType;
import org.mule.runtime.api.metadata.TypedValue;
import org.mule.runtime.core.api.expression.ExpressionRuntimeException;

import java.util.Iterator;

public interface ExpressionLanguageSessionAdaptor extends AutoCloseable {


  TypedValue<?> evaluate(String expression) throws ExpressionRuntimeException;

  TypedValue<?> evaluate(String expression, DataType expectedOutputType) throws ExpressionRuntimeException;

  TypedValue<?> evaluate(String expression, long timeout) throws ExpressionRuntimeException;

  TypedValue<?> evaluateLogExpression(String expression) throws ExpressionRuntimeException;

  Iterator<TypedValue<?>> split(String expression);

  @Override
  void close();
}
