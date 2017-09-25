/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.core.privileged.util;

import static java.util.Arrays.asList;
import static java.util.regex.Pattern.DOTALL;
import static java.util.regex.Pattern.compile;
import static org.mule.runtime.api.el.BindingContextUtils.NULL_BINDING_CONTEXT;
import static org.mule.runtime.api.metadata.DataType.OBJECT;
import static org.mule.runtime.api.metadata.DataType.STRING;
import static org.mule.runtime.core.api.el.ExpressionManager.DEFAULT_EXPRESSION_POSTFIX;
import static org.mule.runtime.core.api.el.ExpressionManager.DEFAULT_EXPRESSION_PREFIX;

import org.mule.runtime.api.metadata.DataType;
import org.mule.runtime.api.metadata.TypedValue;
import org.mule.runtime.core.api.el.ExtendedExpressionManager;
import org.mule.runtime.core.api.event.CoreEvent;

import java.io.InputStream;
import java.io.Serializable;
import java.util.Iterator;
import java.util.List;
import java.util.function.Function;
import java.util.regex.Pattern;

/**
 * This class acts as a wrapper for configuration attributes that support simple text, expression or regular expressions. It can
 * be extended to support other cases too.
 */
public class AttributeEvaluator {

  private static final Pattern SINGLE_EXPRESSION_REGEX_PATTERN = compile("^#\\[(?:(?!#\\[).)*]$", DOTALL);
  private static final List<Class<?>> BLACK_LIST_TYPES =
      asList(Object.class, InputStream.class, Iterator.class, Serializable.class);

  private String attributeValue;
  private ExtendedExpressionManager expressionManager;
  private Function<CoreEvent, TypedValue> expressionResolver;

  /**
   * Creates a new Attribute Evaluator instance with a given attribute value
   *
   * @param attributeValue the value for an attribute, this value can be treated as {@link AttributeType#EXPRESSION},
   *        {@link AttributeType#PARSE_EXPRESSION} or as a {@link AttributeType#STATIC_VALUE}
   */
  public AttributeEvaluator(String attributeValue) {
    this(attributeValue, null);
  }

  /**
   * Creates a new Attribute Evaluator instance with a given attribute value and the expected {@link DataType}
   *
   * @param attributeValue the value for an attribute, this value can be treated as {@link AttributeType#EXPRESSION},
   *        {@link AttributeType#PARSE_EXPRESSION} or as a {@link AttributeType#STATIC_VALUE}
   * @param expectedDataType specifies that the expression should be evaluated a coerced to the given expected {@link DataType}.
   *        This value will be ignored for {@link AttributeType#PARSE_EXPRESSION} and {@link AttributeType#STATIC_VALUE}
   */
  public AttributeEvaluator(String attributeValue, DataType expectedDataType) {
    this.attributeValue = sanitize(attributeValue);

    switch (resolveAttributeType()) {
      case EXPRESSION:
        if (!(expectedDataType == null || BLACK_LIST_TYPES.contains(expectedDataType.getType()))) {
          expressionResolver =
              event -> expressionManager.evaluate(this.attributeValue, expectedDataType, NULL_BINDING_CONTEXT, event);
        } else {
          expressionResolver = event -> expressionManager.evaluate(this.attributeValue, event);
        }
        break;
      case PARSE_EXPRESSION:
        expressionResolver = event -> new TypedValue<>(expressionManager.parse(this.attributeValue, event, null), STRING);
        break;
      case STATIC_VALUE:
        expressionResolver = event -> new TypedValue<>(this.attributeValue, this.attributeValue == null ? OBJECT : STRING);
    }
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

  private AttributeType resolveAttributeType() {
    if (attributeValue != null && SINGLE_EXPRESSION_REGEX_PATTERN.matcher(attributeValue).matches()) {
      return AttributeType.EXPRESSION;
    } else if (attributeValue != null && isParseExpression(attributeValue)) {
      return AttributeType.PARSE_EXPRESSION;
    } else {
      return AttributeType.STATIC_VALUE;
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

  public <T> TypedValue<T> resolveTypedValue(CoreEvent event) {
    return expressionResolver.apply(event);
  }

  public <T> T resolveValue(CoreEvent event) {
    final TypedValue<T> resolveTypedValue = resolveTypedValue(event);
    return resolveTypedValue.getValue();
  }

  public String getRawValue() {
    return attributeValue;
  }

  private enum AttributeType {
    EXPRESSION, PARSE_EXPRESSION, STATIC_VALUE
  }
}
