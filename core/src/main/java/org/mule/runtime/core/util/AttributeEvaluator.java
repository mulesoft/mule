/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.core.util;

import static java.util.regex.Pattern.DOTALL;
import static org.mule.runtime.api.metadata.DataType.BOOLEAN;
import static org.mule.runtime.api.metadata.DataType.OBJECT;
import static org.mule.runtime.api.metadata.DataType.STRING;
import static org.mule.runtime.core.api.el.ExpressionManager.DEFAULT_EXPRESSION_POSTFIX;
import static org.mule.runtime.core.api.el.ExpressionManager.DEFAULT_EXPRESSION_PREFIX;
import org.mule.runtime.api.el.BindingContext;
import org.mule.runtime.api.exception.MuleRuntimeException;
import org.mule.runtime.api.metadata.DataType;
import org.mule.runtime.api.metadata.TypedValue;
import org.mule.runtime.core.api.Event;
import org.mule.runtime.core.api.el.ExtendedExpressionManager;
import org.mule.runtime.core.config.i18n.CoreMessages;

import java.io.InputStream;
import java.util.Arrays;
import java.util.List;
import java.util.function.Function;
import java.util.regex.Pattern;

/**
 * This class acts as a wrapper for configuration attributes that support simple text, expression or regular expressions.
 * It can be extended to support other cases too.
 */
public class AttributeEvaluator {

  private static final Pattern SINGLE_EXPRESSION_REGEX_PATTERN = Pattern.compile("^#\\[(?:(?!#\\[).)*]$", DOTALL);
  private static final BindingContext NULL_BINDING_CONTEXT = BindingContext.builder().build();
  private static final List<Class<?>> BLACK_LIST_TYPES = Arrays.asList(Object.class, InputStream.class);
  public static final DataType INTEGER = DataType.fromType(Integer.class);
  private String attributeValue;
  private ExtendedExpressionManager expressionManager;
  private AttributeType attributeType;
  private Function<Event, TypedValue> expressionResolver;
  private Function<Event, String> parseResolver;

  /**
   * Creates a new Attribute Evaluator instance with a given attribute value
   *
   * @param attributeValue the value for an attribute, this value can be treated as {@link AttributeType#EXPRESSION},
   *                       {@link AttributeType#PARSE_EXPRESSION} or as a {@link AttributeType#STATIC_VALUE}
   */
  public AttributeEvaluator(String attributeValue) {
    this(attributeValue, null);
  }

  /**
   * Creates a new Attribute Evaluator instance with a given attribute value and the expected {@link DataType}
   *
   * @param attributeValue the value for an attribute, this value can be treated as {@link AttributeType#EXPRESSION},
   *                       {@link AttributeType#PARSE_EXPRESSION} or as a {@link AttributeType#STATIC_VALUE}
   * @param expectedDataType specifies that the expression should be evaluated a coerced to the given expected {@link DataType}.
   *                         This value will be ignored for {@link AttributeType#PARSE_EXPRESSION} and {@link AttributeType#STATIC_VALUE}
   */
  public AttributeEvaluator(String attributeValue, DataType expectedDataType) {
    this.attributeValue = sanitize(attributeValue);
    resolveAttributeType();

    switch (attributeType) {
      case EXPRESSION:
        configureExpressionAttribute(expectedDataType);
        break;
      case PARSE_EXPRESSION:
        configureParseAttribute();
        break;
      case STATIC_VALUE:
        configureStaticAttribute();
    }
  }

  private void configureStaticAttribute() {
    parseResolver = event -> this.attributeValue;
    expressionResolver = event -> new TypedValue<>(this.attributeValue, this.attributeValue == null ? OBJECT : STRING);
  }

  private void configureParseAttribute() {
    parseResolver = event -> expressionManager.parse(this.attributeValue, event, null);
    expressionResolver = event -> new TypedValue<>(parseResolver.apply(event), STRING);
  }

  private void configureExpressionAttribute(DataType expectedDataType) {
    expressionResolver = expectedDataType != null && !BLACK_LIST_TYPES.contains(expectedDataType.getType())
        ? event -> expressionManager.evaluate(this.attributeValue, expectedDataType, NULL_BINDING_CONTEXT, event)
        : event -> expressionManager.evaluate(this.attributeValue, event);
  }

  public AttributeEvaluator initialize(final ExtendedExpressionManager expressionManager) {
    this.expressionManager = expressionManager;
    return this;
  }

  private String sanitize(String attributeValue) {
    if (attributeValue != null) {
      attributeValue = attributeValue.trim().replaceAll("\r", "").replaceAll("\t", "");
    }

    return attributeValue;
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
    final int beginExpression = attributeValue.indexOf(DEFAULT_EXPRESSION_PREFIX);
    if (beginExpression == -1) {
      return false;
    }
    String remainingString = attributeValue.substring(beginExpression + DEFAULT_EXPRESSION_PREFIX.length());
    return remainingString.contains(DEFAULT_EXPRESSION_POSTFIX);
  }

  public boolean isExpression() {
    return this.attributeType.equals(AttributeType.EXPRESSION);
  }

  public boolean isParseExpression() {
    return attributeType.equals(AttributeType.PARSE_EXPRESSION);
  }

  public TypedValue resolveTypedValue(Event event) {
    return expressionResolver.apply(event);
  }

  public Object resolveValue(Event event) {
    return resolveTypedValue(event).getValue();
  }

  private TypedValue resolveValue(Event event, DataType expectedDataType) {
    return expressionManager.evaluate(this.attributeValue, expectedDataType, NULL_BINDING_CONTEXT, event);
  }

  public Integer resolveIntegerValue(Event event) {
    Object value = resolveValue(event, INTEGER).getValue();
    if (value == null) {
      return null;
    } else if (value instanceof Number) {
      return ((Number) value).intValue();
    } else if (value instanceof String) {
      return Integer.parseInt((String) value);
    } else {
      throw new MuleRuntimeException(CoreMessages
          .createStaticMessage(String.format("Value was required as integer but is of type: %s", value.getClass().getName())));
    }
  }

  public String resolveStringValue(Event event) {
    Object value = resolveValue(event, STRING).getValue();
    if (value == null) {
      return null;
    }
    return value.toString();
  }

  public Boolean resolveBooleanValue(Event event) {
    final Object value = resolveValue(event, BOOLEAN).getValue();
    if (value == null || value instanceof Boolean) {
      return (Boolean) value;
    }
    return Boolean.valueOf(value.toString());
  }

  public String getRawValue() {
    return attributeValue;
  }

  private enum AttributeType {
    EXPRESSION, PARSE_EXPRESSION, STATIC_VALUE
  }
}
