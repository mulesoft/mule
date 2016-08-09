/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.core.util;

import org.mule.runtime.api.metadata.DataType;
import org.mule.runtime.core.api.MuleEvent;
import org.mule.runtime.core.api.MuleRuntimeException;
import org.mule.runtime.core.api.expression.ExpressionManager;
import org.mule.runtime.core.config.i18n.CoreMessages;
import org.mule.runtime.core.metadata.TypedValue;

import java.util.regex.Pattern;

/**
 * This class acts as a wrapper for configuration attributes that support simple text, expression or regular expressions. It can
 * be extended to support other cases too.
 */
public class AttributeEvaluator {

  private static final Pattern SINGLE_EXPRESSION_REGEX_PATTERN = Pattern.compile("^#\\[(?:(?!#\\[).)*\\]$");

  private enum AttributeType {
    EXPRESSION, PARSE_EXPRESSION, STATIC_VALUE
  }

  private final String attributeValue;
  private ExpressionManager expressionManager;
  private AttributeType attributeType;

  public AttributeEvaluator(String attributeValue) {
    this.attributeValue = attributeValue;
  }

  public AttributeEvaluator initialize(final ExpressionManager expressionManager) {
    this.expressionManager = expressionManager;
    resolveAttributeType();
    return this;
  }

  private void resolveAttributeType() {
    if (attributeValue != null && SINGLE_EXPRESSION_REGEX_PATTERN.matcher(attributeValue).matches()) {
      this.attributeType = AttributeType.EXPRESSION;
    } else if (attributeValue != null && isParseExpression(attributeValue)) {
      this.attributeType = AttributeType.PARSE_EXPRESSION;
    } else {
      this.attributeType = AttributeType.STATIC_VALUE;
    }
  }

  private boolean isParseExpression(String attributeValue) {
    final int beginExpression = attributeValue.indexOf("#[");
    if (beginExpression == -1) {
      return false;
    }
    String remainingString = attributeValue.substring(beginExpression + "#[".length());
    return remainingString.contains("]");
  }

  public boolean isExpression() {
    return this.attributeType.equals(AttributeType.EXPRESSION);
  }

  public boolean isParseExpression() {
    return attributeType.equals(AttributeType.PARSE_EXPRESSION);
  }

  public TypedValue resolveTypedValue(MuleEvent event) {
    if (isExpression()) {
      return expressionManager.evaluateTyped(attributeValue, event);
    } else if (isParseExpression()) {
      final String value = expressionManager.parse(attributeValue, event);
      return new TypedValue(value, DataType.builder().type(String.class).build());
    } else {
      Class<?> type = attributeValue == null ? Object.class : String.class;
      return new TypedValue(attributeValue, DataType.builder().type(type).build());
    }
  }

  public Object resolveValue(MuleEvent event) {
    if (isExpression()) {
      return expressionManager.evaluate(attributeValue, event);
    } else if (isParseExpression()) {
      return expressionManager.parse(attributeValue, event);
    } else {
      return attributeValue;
    }
  }

  public Integer resolveIntegerValue(MuleEvent event) {
    final Object value = resolveValue(event);
    if (value == null) {
      return null;
    }
    if (value instanceof Number) {
      return ((Number) value).intValue();
    } else if (value instanceof String) {
      return Integer.parseInt((String) value);
    } else {
      throw new MuleRuntimeException(CoreMessages
          .createStaticMessage(String.format("Value was required as integer but is of type: %s", value.getClass().getName())));
    }
  }

  public String resolveStringValue(MuleEvent event) {
    final Object value = resolveValue(event);
    if (value == null) {
      return null;
    }
    return value.toString();
  }

  public Boolean resolveBooleanValue(MuleEvent event) {
    final Object value = resolveValue(event);
    if (value == null || value instanceof Boolean) {
      return (Boolean) value;
    }
    return Boolean.valueOf(value.toString());
  }

  public String getRawValue() {
    return attributeValue;
  }
}
