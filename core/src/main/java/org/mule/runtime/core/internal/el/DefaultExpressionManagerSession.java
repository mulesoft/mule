/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.core.internal.el;

import static java.lang.Thread.currentThread;
import static org.mule.runtime.core.internal.el.DefaultExpressionManager.resolveBoolean;

import org.mule.runtime.api.el.CompiledExpression;
import org.mule.runtime.api.el.ExpressionExecutionException;
import org.mule.runtime.api.metadata.DataType;
import org.mule.runtime.api.metadata.TypedValue;
import org.mule.runtime.api.util.LazyValue;
import org.mule.runtime.core.api.el.ExpressionManagerSession;
import org.mule.runtime.core.api.expression.ExpressionRuntimeException;

import java.util.Iterator;


class DefaultExpressionManagerSession implements ExpressionManagerSession {

  private final LazyValue<ExpressionLanguageSessionAdaptor> session;
  private final ClassLoader evaluationClassLoader;

  public DefaultExpressionManagerSession(LazyValue<ExpressionLanguageSessionAdaptor> session, ClassLoader evaluationClassLoader) {
    this.session = session;
    this.evaluationClassLoader = evaluationClassLoader;
  }

  @Override
  public TypedValue<?> evaluate(String expression) throws ExpressionExecutionException {
    ClassLoader originalLoader = currentThread().getContextClassLoader();

    try {
      currentThread().setContextClassLoader(evaluationClassLoader);
      return session.get().evaluate(expression);
    } finally {
      currentThread().setContextClassLoader(originalLoader);
    }
  }

  @Override
  public TypedValue<?> evaluate(String expression, DataType expectedOutputType) throws ExpressionExecutionException {
    ClassLoader originalLoader = currentThread().getContextClassLoader();

    try {
      currentThread().setContextClassLoader(evaluationClassLoader);
      return session.get().evaluate(expression, expectedOutputType);
    } finally {
      currentThread().setContextClassLoader(originalLoader);
    }
  }

  @Override
  public TypedValue<?> evaluate(String expression, long timeout) throws ExpressionExecutionException {
    ClassLoader originalLoader = currentThread().getContextClassLoader();

    try {
      currentThread().setContextClassLoader(evaluationClassLoader);
      return session.get().evaluate(expression, timeout);
    } finally {
      currentThread().setContextClassLoader(originalLoader);
    }
  }

  @Override
  public boolean evaluateBoolean(String expression, boolean nullReturnsTrue, boolean nonBooleanReturnsTrue)
      throws ExpressionRuntimeException {
    return resolveBoolean(evaluate(expression, DataType.BOOLEAN).getValue(), nullReturnsTrue, nonBooleanReturnsTrue, expression);
  }

  @Override
  public TypedValue<?> evaluateLogExpression(String expression) throws ExpressionExecutionException {
    ClassLoader originalLoader = currentThread().getContextClassLoader();

    try {
      currentThread().setContextClassLoader(evaluationClassLoader);
      return session.get().evaluateLogExpression(expression);
    } finally {
      currentThread().setContextClassLoader(originalLoader);
    }
  }

  @Override
  public Iterator<TypedValue<?>> split(String expression) {
    ClassLoader originalLoader = currentThread().getContextClassLoader();

    try {
      currentThread().setContextClassLoader(evaluationClassLoader);
      return session.get().split(expression);
    } finally {
      currentThread().setContextClassLoader(originalLoader);
    }
  }

  @Override
  public TypedValue<?> evaluate(CompiledExpression expression) throws ExpressionExecutionException {
    ClassLoader originalLoader = currentThread().getContextClassLoader();

    try {
      currentThread().setContextClassLoader(evaluationClassLoader);
      return session.get().evaluate(expression);
    } finally {
      currentThread().setContextClassLoader(originalLoader);
    }
  }

  @Override
  public TypedValue<?> evaluate(CompiledExpression expression, DataType expectedOutputType) throws ExpressionExecutionException {
    ClassLoader originalLoader = currentThread().getContextClassLoader();

    try {
      currentThread().setContextClassLoader(evaluationClassLoader);
      return session.get().evaluate(expression, expectedOutputType);
    } finally {
      currentThread().setContextClassLoader(originalLoader);
    }
  }

  @Override
  public TypedValue<?> evaluate(CompiledExpression expression, long timeout) throws ExpressionExecutionException {
    ClassLoader originalLoader = currentThread().getContextClassLoader();

    try {
      currentThread().setContextClassLoader(evaluationClassLoader);
      return session.get().evaluate(expression, timeout);
    } finally {
      currentThread().setContextClassLoader(originalLoader);
    }
  }

  @Override
  public TypedValue<?> evaluateLogExpression(CompiledExpression expression) throws ExpressionExecutionException {
    ClassLoader originalLoader = currentThread().getContextClassLoader();

    try {
      currentThread().setContextClassLoader(evaluationClassLoader);
      return session.get().evaluateLogExpression(expression);
    } finally {
      currentThread().setContextClassLoader(originalLoader);
    }
  }

  @Override
  public Iterator<TypedValue<?>> split(CompiledExpression expression) {
    ClassLoader originalLoader = currentThread().getContextClassLoader();

    try {
      currentThread().setContextClassLoader(evaluationClassLoader);
      return session.get().split(expression);
    } finally {
      currentThread().setContextClassLoader(originalLoader);
    }
  }

  @Override
  public void close() {
    if (session.isComputed()) {
      session.get().close();
    }
  }
}
