/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.module.extension.internal.runtime.resolver;

import static org.mule.runtime.api.util.Preconditions.checkArgument;
import static org.mule.runtime.core.util.ClassUtils.isInstance;
import org.apache.commons.lang.StringUtils;
import org.mule.runtime.api.exception.MuleException;
import org.mule.runtime.api.metadata.TypedValue;
import org.mule.runtime.core.api.Event;
import org.mule.runtime.core.api.MuleContext;
import org.mule.runtime.core.util.AttributeEvaluator;

import java.util.function.BiConsumer;

/**
 * A {@link ValueResolver} which evaluates a MEL expressions
 * <p>
 * It resolves the expressions by making use of the {@link AttributeEvaluator} so that it's compatible with simple
 * expressions and templates alike
 *
 * @param <T>
 * @since 4.0
 */
public class ExpressionValueResolver<T> implements ValueResolver<T> {

  final AttributeEvaluator evaluator;
  private final MuleContext muleContext;
  private boolean evaluatorInitialized = false;
  private BiConsumer<AttributeEvaluator, MuleContext> evaluatorInitialiser = (evaluator, context) -> {
    synchronized (context) {
      if (!evaluatorInitialized) {
        evaluator.initialize(context.getExpressionManager());
        evaluatorInitialiser = (e, c) -> {
        };
        evaluatorInitialized = true;
      }
    }
  };

  public ExpressionValueResolver(String expression, MuleContext muleContext) {
    checkArgument(!StringUtils.isBlank(expression), "Expression cannot be blank or null");

    this.evaluator = new AttributeEvaluator(expression);
    this.muleContext = muleContext;
  }

  @Override
  public T resolve(Event event) throws MuleException {
    initEvaluator();
    TypedValue typedValue = evaluator.resolveTypedValue(event, Event.builder(event));

    Object value = typedValue.getValue();

    if (isInstance(ValueResolver.class, value)) {
      value = ((ValueResolver) value).resolve(event);
    }

    return (T) value;
  }

  void initEvaluator() {
    evaluatorInitialiser.accept(evaluator, muleContext);
  }

  /**
   * @return {@code true}
   */
  @Override
  public boolean isDynamic() {
    return true;
  }
}
