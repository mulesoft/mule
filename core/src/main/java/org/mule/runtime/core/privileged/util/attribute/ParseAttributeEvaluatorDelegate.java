/*
 * Copyright 2023 Salesforce, Inc. All rights reserved.
 */
package org.mule.runtime.core.privileged.util.attribute;

import static org.mule.runtime.api.metadata.DataType.STRING;

import org.mule.runtime.api.el.BindingContext;
import org.mule.runtime.api.metadata.TypedValue;
import org.mule.runtime.core.api.el.ExpressionManagerSession;
import org.mule.runtime.core.api.el.ExtendedExpressionManager;
import org.mule.runtime.core.api.event.CoreEvent;

/**
 * {@link AttributeEvaluatorDelegate} that resolves attribute values with multiple expressions in a given string.
 *
 * @since 4.2.0
 */
public final class ParseAttributeEvaluatorDelegate implements AttributeEvaluatorDelegate<String> {

  private String attributeValue;

  public ParseAttributeEvaluatorDelegate(String attributeValue) {
    this.attributeValue = attributeValue;
  }

  @Override
  public TypedValue<String> resolve(CoreEvent event, ExtendedExpressionManager expressionManager) {
    return new TypedValue<>(expressionManager.parse(attributeValue, event, null), STRING);
  }

  @Override
  public TypedValue<String> resolve(BindingContext context, ExtendedExpressionManager expressionManager) {
    throw buildNoEventException();
  }

  @Override
  public TypedValue<String> resolve(ExpressionManagerSession session) {
    throw buildNoEventException();
  }

  private UnsupportedOperationException buildNoEventException() {
    return new UnsupportedOperationException("Cannot use a PARSE_EXPRESSION attribute type without an event.");
  }
}
