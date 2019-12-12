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
import org.mule.runtime.api.el.ExpressionCompilationException;
import org.mule.runtime.api.el.ExpressionExecutionException;
import org.mule.runtime.api.el.ExpressionLanguage;
import org.mule.runtime.api.el.ModuleElementName;
import org.mule.runtime.api.metadata.MediaType;
import org.mule.runtime.api.util.LazyValue;

import java.util.List;
import java.util.Optional;

public final class ExpressionLanguageUtils {

  private static final BindingContext COMPILATION_BINDING_CONTEXT =
      addEventBuindingsToBuilder(getNullEvent(), NULL_BINDING_CONTEXT).build();

  private ExpressionLanguageUtils() {}

  public static CompiledExpression compile(String expression, ExpressionLanguage expressionLanguage) {

    return expressionLanguage.compile(expression, COMPILATION_BINDING_CONTEXT);
    //return new LazyCompiledExpression(expression,
    //                                  expressionLanguage,
    //                                  addEventBuindingsToBuilder(getNullEvent(), NULL_BINDING_CONTEXT).build());
  }

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

  public static boolean isPayloadExpression(String sanitized) {
    return sanitized.equals(PAYLOAD);
  }

  private static class LazyCompiledExpression implements CompiledExpressionDecorator {

    private final String expression;
    private final ExpressionLanguage expressionLanguage;
    private final BindingContext bindingContext;
    private final LazyValue<CompiledExpression> delegate;

    private LazyCompiledExpression(String expression, ExpressionLanguage expressionLanguage, BindingContext bindingContext) {
      this.expression = expression;
      this.expressionLanguage = expressionLanguage;
      this.bindingContext = bindingContext;
      delegate = new LazyValue<>(() -> {
        try {
          return this.expressionLanguage.compile(this.expression, this.bindingContext);
        } catch (ExpressionCompilationException e) {
          //TODO : handle with backwards comp
          throw e;
        }
      });
    }

    @Override
    public String expression() {
      return expression;
    }

    @Override
    public Optional<MediaType> outputType() {
      return delegate.get().outputType();
    }

    @Override
    public List<ModuleElementName> externalDependencies() {
      return delegate.get().externalDependencies();
    }

    @Override
    public CompiledExpression getDelegate() {
      return delegate.get();
    }
  }
}
