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
import org.mule.runtime.api.el.ExpressionLanguage;
import org.mule.runtime.api.el.ExpressionLanguageSession;

import java.util.function.Function;

public final class ExpressionLanguageUtils {

  private ExpressionLanguageUtils() {}

  private static final BindingContext COMPILATION_BINDING_CONTEXT =
      addEventBuindingsToBuilder(getNullEvent(), NULL_BINDING_CONTEXT).build();


  public static CompiledExpression compile(String expression, ExpressionLanguage expressionLanguage) {
    return expressionLanguage.compile(expression, COMPILATION_BINDING_CONTEXT);
  }

  public static <T> T withSession(ExpressionLanguage expressionLanguage,
                                  BindingContext bindingContext,
                                  Function<ExpressionLanguageSession, T> func) {
    try (ExpressionLanguageSession session = expressionLanguage.openSession(bindingContext)) {
      return func.apply(session);
    }
  }
}
