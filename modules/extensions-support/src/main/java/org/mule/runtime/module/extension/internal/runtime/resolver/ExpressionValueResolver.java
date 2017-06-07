/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.module.extension.internal.runtime.resolver;

import static org.mule.runtime.api.util.Preconditions.checkArgument;
import static org.mule.runtime.core.api.util.ClassUtils.isInstance;
import org.mule.runtime.api.exception.MuleException;
import org.mule.runtime.api.metadata.DataType;
import org.mule.runtime.api.metadata.TypedValue;
import org.mule.runtime.core.api.el.ExtendedExpressionManager;
import org.mule.runtime.core.api.util.AttributeEvaluator;

import java.util.function.BiConsumer;

import javax.inject.Inject;

import org.apache.commons.lang.StringUtils;

/**
 * A {@link ValueResolver} which evaluates a MEL expressions
 * <p>
 * It resolves the expressions by making use of the {@link AttributeEvaluator} so that it's compatible with simple expressions and
 * templates alike
 *
 * @param <T>
 * @since 4.0
 */
public class ExpressionValueResolver<T> implements ValueResolver<T> {

  @Inject
  private ExtendedExpressionManager extendedExpressionManager;
  final AttributeEvaluator evaluator;
  private boolean evaluatorInitialized = false;
  private BiConsumer<AttributeEvaluator, ExtendedExpressionManager> evaluatorInitialiser =
      (evaluator, extendedExpressionManager) -> {
        synchronized (extendedExpressionManager) {
          if (!evaluatorInitialized) {
            evaluator.initialize(extendedExpressionManager);
            evaluatorInitialiser = (e, c) -> {
            };
            evaluatorInitialized = true;
          }
        }
      };

  ExpressionValueResolver(String expression, DataType expectedDataType) {
    checkArgument(!StringUtils.isBlank(expression), "Expression cannot be blank or null");

    this.evaluator = new AttributeEvaluator(expression, expectedDataType);
  }

  public ExpressionValueResolver(String expression) {
    checkArgument(!StringUtils.isBlank(expression), "Expression cannot be blank or null");

    this.evaluator = new AttributeEvaluator(expression);
  }

  void setExtendedExpressionManager(ExtendedExpressionManager extendedExpressionManager) {
    this.extendedExpressionManager = extendedExpressionManager;
  }

  @Override
  public T resolve(ValueResolvingContext context) throws MuleException {
    initEvaluator();
    TypedValue typedValue = evaluator.resolveTypedValue(context.getEvent());

    Object value = typedValue.getValue();

    if (isInstance(ValueResolver.class, value)) {
      value = ((ValueResolver) value).resolve(context);
    }

    return (T) value;
  }

  void initEvaluator() {
    evaluatorInitialiser.accept(evaluator, extendedExpressionManager);
  }

  /**
   * @return {@code true}
   */
  @Override
  public boolean isDynamic() {
    return true;
  }
}
