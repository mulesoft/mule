/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.core.privileged.util.attribute;

import static java.util.Arrays.asList;
import static org.mule.runtime.api.el.BindingContextUtils.NULL_BINDING_CONTEXT;

import org.mule.runtime.api.component.location.ComponentLocation;
import org.mule.runtime.api.el.BindingContext;
import org.mule.runtime.api.el.CompiledExpression;
import org.mule.runtime.api.metadata.DataType;
import org.mule.runtime.api.metadata.TypedValue;
import org.mule.runtime.core.api.el.ExpressionManagerSession;
import org.mule.runtime.core.api.el.ExtendedExpressionManager;
import org.mule.runtime.core.api.event.CoreEvent;

import java.io.InputStream;
import java.io.Serializable;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Set;

/**
 * {@link AttributeEvaluatorDelegate} implementation that resolves attributes with expression values that needs to be evaluated
 * with a given session or event.
 *
 * @since 4.2.0
 */
public final class ExpressionAttributeEvaluatorDelegate<T> implements AttributeEvaluatorDelegate<T> {

  private static final Set<Class<?>> BLACK_LIST_TYPES =
      new HashSet<>(asList(Object.class, InputStream.class, Iterator.class, Serializable.class));

  private final CompiledExpression expression;
  private final DataType expectedDataType;

  public ExpressionAttributeEvaluatorDelegate(CompiledExpression expression, DataType expectedDataType) {
    this.expression = expression;
    this.expectedDataType = expectedDataType;
  }

  @Override
  public TypedValue<T> resolve(CoreEvent event, ExtendedExpressionManager expressionManager) {
    ComponentLocation location = event.getContext().getOriginatingLocation();
    try (ExpressionManagerSession session = expressionManager.openSession(location, event, NULL_BINDING_CONTEXT)) {
      return resolveExpressionWithSession(session);
    }
  }

  @Override
  public TypedValue<T> resolve(ExpressionManagerSession session) {
    return resolveExpressionWithSession(session);
  }

  @Override
  public TypedValue<T> resolve(BindingContext context, ExtendedExpressionManager expressionManager) {
    try (ExpressionManagerSession session = expressionManager.openSession(context)) {
      return resolveExpressionWithSession(session);
    }
  }

  private TypedValue<T> resolveExpressionWithSession(ExpressionManagerSession session) {
    if (hasExpectedDataType()) {
      return (TypedValue<T>) session.evaluate(expression, expectedDataType);
    } else {
      return (TypedValue<T>) session.evaluate(expression);
    }
  }

  private boolean hasExpectedDataType() {
    return expectedDataType != null && !BLACK_LIST_TYPES.contains(expectedDataType.getType());
  }
}
