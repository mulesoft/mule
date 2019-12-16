/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.core.privileged.util;

import static java.util.regex.Pattern.DOTALL;
import static java.util.regex.Pattern.compile;
import static org.mule.runtime.core.api.el.ExpressionManager.DEFAULT_EXPRESSION_POSTFIX;
import static org.mule.runtime.core.api.el.ExpressionManager.DEFAULT_EXPRESSION_PREFIX;
import static org.mule.runtime.core.internal.el.ExpressionLanguageUtils.compile;

import org.mule.runtime.api.el.BindingContext;
import org.mule.runtime.api.metadata.DataType;
import org.mule.runtime.api.metadata.TypedValue;
import org.mule.runtime.core.api.el.ExpressionManagerSession;
import org.mule.runtime.core.api.el.ExtendedExpressionManager;
import org.mule.runtime.core.api.event.CoreEvent;
import org.mule.runtime.core.privileged.util.attribute.AttributeEvaluatorDelegate;
import org.mule.runtime.core.privileged.util.attribute.ExpressionAttributeEvaluatorDelegate;
import org.mule.runtime.core.privileged.util.attribute.ParseAttributeEvaluatorDelegate;
import org.mule.runtime.core.privileged.util.attribute.StaticAttributeEvaluatorDelegate;

import java.util.regex.Pattern;

/**
 * This class acts as a wrapper for component attributes that support simple text, expression, parse expressions or templates.
 * <p>
 * It can be extended to support other cases too.
 */
public final class AttributeEvaluator {

  private static final Pattern SINGLE_EXPRESSION_REGEX_PATTERN = compile("^#\\[(?:(?!#\\[).)*]$", DOTALL);
  private static final Pattern SANITIZE_PATTERN = compile("\r|\t");

  private final String attributeValue;
  private final DataType expectedDataType;

  private AttributeEvaluatorDelegate evaluator;
  private ExtendedExpressionManager expressionManager;

  /**
   * Creates a new Attribute Evaluator instance with a given attribute value
   *
   * @param attributeValue the value for an attribute, this value can be treated as expression, parse_expression or static.
   */
  public AttributeEvaluator(String attributeValue) {
    this(attributeValue, null);
  }

  /**
   * Creates a new Attribute Evaluator instance with a given attribute value and the expected {@link DataType}
   *
   * @param attributeValue   the value for an attribute, this value can be treated as expression, parse_expression or static.
   * @param expectedDataType specifies that the expression should be evaluated a coerced to the given expected {@link DataType}.
   */
  public AttributeEvaluator(String attributeValue, DataType expectedDataType) {
    this.attributeValue = sanitize(attributeValue);
    this.expectedDataType = expectedDataType;
  }

  public AttributeEvaluator initialize(final ExtendedExpressionManager expressionManager) {
    this.expressionManager = expressionManager;
    this.evaluator = getEvaluator(expectedDataType);
    return this;
  }

  private String sanitize(String attributeValue) {
    if (attributeValue != null) {
      attributeValue = SANITIZE_PATTERN.matcher(attributeValue.trim()).replaceAll("");
    }
    return attributeValue;
  }

  private AttributeEvaluatorDelegate getEvaluator(DataType expectedDataType) {
    if (attributeValue != null) {
      if (SINGLE_EXPRESSION_REGEX_PATTERN.matcher(attributeValue).matches()) {
        return new ExpressionAttributeEvaluatorDelegate(compile(attributeValue, expressionManager), expectedDataType);
      }
      if (isParseExpression(attributeValue)) {
        return new ParseAttributeEvaluatorDelegate(attributeValue);
      }
    }
    return new StaticAttributeEvaluatorDelegate(attributeValue);
  }

  private boolean isParseExpression(String attributeValue) {
    final int beginExpression = attributeValue.indexOf(DEFAULT_EXPRESSION_PREFIX);
    if (beginExpression == -1) {
      return false;
    }
    String remainingString = attributeValue.substring(beginExpression + DEFAULT_EXPRESSION_PREFIX.length());
    return remainingString.contains(DEFAULT_EXPRESSION_POSTFIX);
  }

  /**
   * @deprecated use the {@link #resolveTypedValue(ExpressionManagerSession)}) with a proper created session}
   */
  @Deprecated
  public <T> TypedValue<T> resolveTypedValueFromContext(BindingContext context) {
    return evaluator.resolve(context, expressionManager);
  }

  public <T> TypedValue<T> resolveTypedValue(ExpressionManagerSession session) {
    return evaluator.resolve(session);
  }

  public <T> TypedValue<T> resolveTypedValue(CoreEvent event) {
    return evaluator.resolve(event, expressionManager);
  }

  public <T> T resolveValue(CoreEvent event) {
    final TypedValue<T> resolveTypedValue = resolveTypedValue(event);
    return resolveTypedValue.getValue();
  }

  public String getRawValue() {
    return attributeValue;
  }
}
