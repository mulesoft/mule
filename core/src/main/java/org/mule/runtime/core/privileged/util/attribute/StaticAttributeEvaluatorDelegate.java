/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.core.privileged.util.attribute;

import static org.mule.runtime.api.metadata.DataType.OBJECT;
import static org.mule.runtime.api.metadata.DataType.STRING;

import org.mule.runtime.api.el.BindingContext;
import org.mule.runtime.api.metadata.TypedValue;
import org.mule.runtime.core.api.el.ExpressionManagerSession;
import org.mule.runtime.core.api.el.ExtendedExpressionManager;
import org.mule.runtime.core.api.event.CoreEvent;

/**
 * {@link AttributeEvaluatorDelegate} that resolves attribute values that are static and wont change during execution.
 *
 * @since 4.2.0
 */
public final class StaticAttributeEvaluatorDelegate implements AttributeEvaluatorDelegate<Object> {

  private final TypedValue<Object> value;

  public StaticAttributeEvaluatorDelegate(String attributeValue) {
    this.value = new TypedValue<>(attributeValue, attributeValue == null ? OBJECT : STRING);
  }

  @Override
  public TypedValue<Object> resolve(CoreEvent event, ExtendedExpressionManager manager) {
    return value;
  }

  @Override
  public TypedValue<Object> resolve(ExpressionManagerSession session) {
    return value;
  }

  @Override
  public TypedValue<Object> resolve(BindingContext context, ExtendedExpressionManager expressionManager) {
    return value;
  }
}
