/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.core.internal.el;

import static org.mule.runtime.api.el.BindingContextUtils.NULL_BINDING_CONTEXT;
import static org.mule.runtime.api.el.BindingContextUtils.addEventBuindingsToBuilder;
import static org.mule.runtime.core.internal.event.NullEventFactory.getNullEvent;

import org.mule.runtime.api.el.BindingContext;
import org.mule.runtime.api.el.CompiledExpression;
import org.mule.runtime.api.el.ExpressionCompilationException;
import org.mule.runtime.api.el.ExpressionLanguage;
import org.mule.runtime.api.el.ExpressionLanguageSession;
import org.mule.runtime.api.el.ModuleElementName;
import org.mule.runtime.api.metadata.MediaType;
import org.mule.runtime.api.util.LazyValue;

import java.util.List;
import java.util.Optional;
import java.util.function.Function;

public final class ExpressionLanguageUtils {

  private ExpressionLanguageUtils() {}

  public static CompiledExpression compile(String expression, ExpressionLanguage expressionLanguage) {
    //return expressionLanguage.compile(expression, addEventBuindingsToBuilder(getNullEvent(), NULL_BINDING_CONTEXT).build());
    return new LazyCompiledExpression(expression,
                                      expressionLanguage,
                                      addEventBuindingsToBuilder(getNullEvent(), NULL_BINDING_CONTEXT).build());
  }

  public static <T> T withSession(ExpressionLanguage expressionLanguage,
                                  BindingContext bindingContext,
                                  Function<ExpressionLanguageSession, T> func) {
    try (ExpressionLanguageSession session = expressionLanguage.openSession(bindingContext)) {
      return func.apply(session);
    }
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
