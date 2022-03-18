/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.tooling.internal;

import static org.mockito.Mockito.RETURNS_DEEP_STUBS;
import static org.mockito.Mockito.mock;

import org.mule.runtime.api.component.location.ComponentLocation;
import org.mule.runtime.api.el.BindingContext;
import org.mule.runtime.api.el.ExpressionExecutionException;
import org.mule.runtime.api.el.ValidationResult;
import org.mule.runtime.api.metadata.DataType;
import org.mule.runtime.api.metadata.TypedValue;
import org.mule.runtime.core.api.el.ExpressionManager;
import org.mule.runtime.core.api.el.ExpressionManagerSession;
import org.mule.runtime.core.api.event.CoreEvent;
import org.mule.runtime.core.api.expression.ExpressionRuntimeException;

import java.util.Iterator;

public class ToolingExpressionManager implements ExpressionManager {

  private final ExpressionManager delegate;

  public ToolingExpressionManager() {
    delegate = mock(ExpressionManager.class, RETURNS_DEEP_STUBS);
  }

  @Override
  public TypedValue evaluate(String expression) throws ExpressionRuntimeException {
    return delegate.evaluate(expression);
  }

  @Override
  public TypedValue evaluate(String expression, CoreEvent event) throws ExpressionRuntimeException {
    return delegate.evaluate(expression, event);
  }

  @Override
  public TypedValue evaluate(String expression, CoreEvent event,
                             ComponentLocation componentLocation)
      throws ExpressionRuntimeException {
    return delegate.evaluate(expression, event, componentLocation);
  }

  @Override
  public TypedValue evaluate(String expression, CoreEvent event,
                             BindingContext context)
      throws ExpressionRuntimeException {
    return delegate.evaluate(expression, event, context);
  }

  @Override
  public TypedValue evaluate(String expression, CoreEvent event,
                             ComponentLocation componentLocation,
                             BindingContext context)
      throws ExpressionRuntimeException {
    return delegate.evaluate(expression, event, componentLocation, context);
  }

  @Override
  public TypedValue evaluate(String expression,
                             DataType expectedOutputType)
      throws ExpressionRuntimeException {
    return delegate.evaluate(expression, expectedOutputType);
  }

  @Override
  public TypedValue evaluate(String expression,
                             DataType expectedOutputType,
                             BindingContext context,
                             CoreEvent event)
      throws ExpressionRuntimeException {
    return delegate.evaluate(expression, expectedOutputType, context, event);
  }

  @Override
  public TypedValue evaluate(String expression,
                             DataType expectedOutputType,
                             BindingContext context,
                             CoreEvent event,
                             ComponentLocation componentLocation,
                             boolean failOnNull)
      throws ExpressionRuntimeException {
    return delegate.evaluate(expression, expectedOutputType, context, event, componentLocation, failOnNull);
  }

  @Override
  public boolean evaluateBoolean(String expression, CoreEvent event,
                                 ComponentLocation componentLocation)
      throws ExpressionRuntimeException {
    return delegate.evaluateBoolean(expression, event, componentLocation);
  }

  @Override
  public boolean evaluateBoolean(String expression, CoreEvent event,
                                 ComponentLocation componentLocation,
                                 boolean nullReturnsTrue, boolean nonBooleanReturnsTrue)
      throws ExpressionRuntimeException {
    return delegate.evaluateBoolean(expression, event, componentLocation, nullReturnsTrue, nonBooleanReturnsTrue);
  }

  @Override
  public boolean evaluateBoolean(String expression, BindingContext bindingCtx,
                                 ComponentLocation componentLocation,
                                 boolean nullReturnsTrue, boolean nonBooleanReturnsTrue)
      throws ExpressionRuntimeException {
    return delegate.evaluateBoolean(expression, bindingCtx, componentLocation, nullReturnsTrue, nonBooleanReturnsTrue);
  }

  @Override
  public Iterator<TypedValue<?>> split(String expression,
                                       CoreEvent event,
                                       ComponentLocation componentLocation,
                                       BindingContext bindingContext)
      throws ExpressionRuntimeException {
    return delegate.split(expression, event, componentLocation, bindingContext);
  }

  @Override
  public Iterator<TypedValue<?>> split(String expression,
                                       CoreEvent event,
                                       BindingContext bindingContext)
      throws ExpressionRuntimeException {
    return delegate.split(expression, event, bindingContext);
  }

  @Override
  public String parseLogTemplate(String template, CoreEvent event,
                                 ComponentLocation componentLocation,
                                 BindingContext bindingContext)
      throws ExpressionRuntimeException {
    return delegate.parseLogTemplate(template, event, componentLocation, bindingContext);
  }

  @Override
  public ExpressionManagerSession openSession(BindingContext context) {
    return delegate.openSession(context);
  }

  @Override
  public ExpressionManagerSession openSession(
                                              ComponentLocation componentLocation, CoreEvent event,
                                              BindingContext context) {
    return delegate.openSession(componentLocation, event, context);
  }

  @Override
  public boolean isExpression(String expression) {
    return delegate.isExpression(expression);
  }

  @Override
  public boolean isValid(String expression) {
    return delegate.isValid(expression);
  }

  @Override
  public void addGlobalBindings(BindingContext bindingContext) {
    delegate.addGlobalBindings(bindingContext);
  }

  @Override
  public TypedValue<?> evaluate(String expression, BindingContext context) throws ExpressionExecutionException {
    return delegate.evaluate(expression, context);
  }

  @Override
  public TypedValue<?> evaluate(String expression,
                                DataType expectedOutputType,
                                BindingContext context)
      throws ExpressionExecutionException {
    return delegate.evaluate(expression, expectedOutputType, context);
  }

  @Override
  public TypedValue<?> evaluateLogExpression(String expression,
                                             BindingContext context)
      throws ExpressionExecutionException {
    return delegate.evaluateLogExpression(expression, context);
  }

  @Override
  public ValidationResult validate(String expression) {
    return delegate.validate(expression);
  }

  @Override
  public Iterator<TypedValue<?>> split(String expression,
                                       BindingContext context) {
    return delegate.split(expression, context);
  }
}
