/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.core.internal.el;

import static java.lang.String.format;
import static org.mule.runtime.api.el.BindingContextUtils.NULL_BINDING_CONTEXT;
import static org.mule.runtime.api.el.BindingContextUtils.PAYLOAD;
import static org.mule.runtime.api.el.BindingContextUtils.addEventBuindingsToBuilder;
import static org.mule.runtime.api.i18n.I18nMessageFactory.createStaticMessage;
import static org.mule.runtime.core.api.el.ExpressionManager.DEFAULT_EXPRESSION_POSTFIX;
import static org.mule.runtime.core.api.el.ExpressionManager.DEFAULT_EXPRESSION_PREFIX;
import static org.mule.runtime.core.internal.el.DefaultExpressionManager.DW_PREFIX;
import static org.mule.runtime.core.internal.el.DefaultExpressionManager.DW_PREFIX_LENGTH;
import static org.mule.runtime.core.internal.el.DefaultExpressionManager.PREFIX_EXPR_SEPARATOR;
import static org.mule.runtime.core.internal.event.NullEventFactory.getNullEvent;

import org.mule.runtime.api.el.BindingContext;
import org.mule.runtime.api.el.CompiledExpression;
import org.mule.runtime.api.el.ExpressionExecutionException;
import org.mule.runtime.api.el.ExpressionLanguage;
import org.mule.runtime.api.event.Event;

/**
 * Utilities for using {@link ExpressionLanguage} instances
 *
 * @since 4.3.0
 */
public final class ExpressionLanguageUtils {

  private static final BindingContext COMPILATION_BINDING_CONTEXT =
      addEventBuindingsToBuilder(getNullEvent(), NULL_BINDING_CONTEXT).build();

  private ExpressionLanguageUtils() {}

  /**
   * Compiles the given {@code expression} using generic and precalculated {@link BindingContext}.
   * Said {@link BindingContext} will already have all the usual bindings added by
   * {@link org.mule.runtime.api.el.BindingContextUtils#addEventBuindingsToBuilder(Event, BindingContext)} plus all the
   * extension modules and functions.
   * <p>
   * If you want to use your own {@link BindingContext} then hit {@link ExpressionLanguage#compile(String, BindingContext)}
   * directly.
   *
   * @param expression         the expression to compile.
   * @param expressionLanguage the {@link ExpressionLanguage} used for compilation
   * @return a {@link CompiledExpression}
   */
  public static CompiledExpression compile(String expression, ExpressionLanguage expressionLanguage) {
    return expressionLanguage.compile(expression, COMPILATION_BINDING_CONTEXT);
  }

  /**
   * Returns a sanitized version of the given {@code expression}
   *
   * @param expression the expression to sanitize
   * @return the sanitized expression
   */
  public static String sanitize(String expression) {
    String sanitizedExpression;
    if (expression.startsWith(DEFAULT_EXPRESSION_PREFIX)) {
      if (!expression.endsWith(DEFAULT_EXPRESSION_POSTFIX)) {
        throw new ExpressionExecutionException(createStaticMessage(format("Unbalanced brackets in expression '%s'", expression)));
      }
      sanitizedExpression =
          expression.substring(DEFAULT_EXPRESSION_PREFIX.length(), expression.length() - DEFAULT_EXPRESSION_POSTFIX.length());
    } else {
      sanitizedExpression = expression;
    }

    if (sanitizedExpression.startsWith(DW_PREFIX + PREFIX_EXPR_SEPARATOR)
        // Handle DW functions that start with dw:: without removing dw:
        && !sanitizedExpression.substring(DW_PREFIX_LENGTH, DW_PREFIX_LENGTH + 1).equals(PREFIX_EXPR_SEPARATOR)) {
      sanitizedExpression = sanitizedExpression.substring(DW_PREFIX_LENGTH);
    }
    return sanitizedExpression;
  }

  /**
   * @param expression the expression to test
   * @return Whether the given {@code sanitized} is a sanitized version of the {@code payload} expression
   */
  public static boolean isSanitizedPayload(String expression) {
    return PAYLOAD.equals(expression);
  }
}
