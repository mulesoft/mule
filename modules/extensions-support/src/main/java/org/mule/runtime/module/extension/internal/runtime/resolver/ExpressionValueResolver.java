/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.module.extension.internal.runtime.resolver;

import static java.lang.Boolean.valueOf;
import static java.lang.System.getProperty;
import static org.apache.commons.lang3.StringUtils.isBlank;
import static org.mule.runtime.api.util.MuleSystemProperties.MULE_MEL_AS_DEFAULT;
import static org.mule.runtime.api.util.Preconditions.checkArgument;
import static org.mule.runtime.core.api.config.MuleProperties.COMPATIBILITY_PLUGIN_INSTALLED;
import static org.mule.runtime.core.api.lifecycle.LifecycleUtils.initialiseIfNeeded;
import static org.mule.runtime.core.api.util.ClassUtils.isInstance;
import static org.mule.runtime.core.internal.el.DefaultExpressionManager.hasDwExpression;
import static org.mule.runtime.core.internal.el.DefaultExpressionManager.hasMelExpression;

import org.mule.runtime.api.artifact.Registry;
import org.mule.runtime.api.exception.MuleException;
import org.mule.runtime.api.lifecycle.Initialisable;
import org.mule.runtime.api.lifecycle.InitialisationException;
import org.mule.runtime.api.metadata.DataType;
import org.mule.runtime.api.metadata.TypedValue;
import org.mule.runtime.core.api.el.ExtendedExpressionManager;
import org.mule.runtime.core.privileged.util.AttributeEvaluator;

import javax.inject.Inject;

/**
 * A {@link ValueResolver} which evaluates a MEL expressions
 * <p>
 * It resolves the expressions by making use of the {@link AttributeEvaluator} so that it's compatible with simple expressions and
 * templates alike
 *
 * @param <T>
 * @since 4.0
 */
public class ExpressionValueResolver<T> implements ExpressionBasedValueResolver<T>, Initialisable {

  @Inject
  private ExtendedExpressionManager extendedExpressionManager;

  @Inject
  private Registry registry;

  final AttributeEvaluator evaluator;
  private final String expression;

  private Boolean melDefault;
  private Boolean melAvailable;
  private boolean isMelExpression;

  ExpressionValueResolver(String expression, DataType expectedDataType) {
    checkArgument(!isBlank(expression), "Expression cannot be blank or null");
    this.expression = expression;
    this.evaluator = new AttributeEvaluator(expression, expectedDataType);
  }

  public ExpressionValueResolver(String expression, DataType expectedDataType, Boolean melDefault, Boolean melAvailable) {
    this(expression, expectedDataType);
    this.melDefault = melDefault;
    this.melAvailable = melAvailable;
  }

  public ExpressionValueResolver(String expression) {
    checkArgument(!isBlank(expression), "Expression cannot be blank or null");
    this.expression = expression;
    this.evaluator = new AttributeEvaluator(expression);

  }

  void setExtendedExpressionManager(ExtendedExpressionManager extendedExpressionManager) {
    this.extendedExpressionManager = extendedExpressionManager;
  }

  @Override
  public void initialise() throws InitialisationException {
    initialiseIfNeeded(extendedExpressionManager);
    getEvaluator().initialize(extendedExpressionManager);
    if (melDefault == null) {
      melDefault = valueOf(getProperty(MULE_MEL_AS_DEFAULT, "false"));
    }

    if (melAvailable == null) {
      melAvailable = registry.lookupByName(COMPATIBILITY_PLUGIN_INSTALLED).isPresent();
    }

    if (isMelAvailable() &&
        (!hasDwExpression(expression) && !hasMelExpression(expression) && melDefault)
        || hasMelExpression(expression)) {
      isMelExpression = true;
    }
  }

  @Override
  public T resolve(ValueResolvingContext context) throws MuleException {
    TypedValue<T> typedValue = resolveTypedValue(context);

    Object value = typedValue.getValue();

    if (isInstance(ValueResolver.class, value)) {
      value = ((ValueResolver) value).resolve(context);
    }

    return (T) value;
  }

  protected <V> TypedValue<V> resolveTypedValue(ValueResolvingContext context) {
    if (isMelExpression) {
      return evaluator.resolveTypedValue(context.getEvent());
    } else {
      if (context.getSession() != null) {
        return evaluator.resolveTypedValue(context.getSession());
      } else {
        return evaluator.resolveTypedValue(context.getEvent());
      }
    }
  }

  /**
   * @return {@code true}
   */
  @Override
  public boolean isDynamic() {
    return true;
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public String getExpression() {
    return expression;
  }

  public boolean isMelAvailable() {
    return melAvailable;
  }

  public void setRegistry(Registry registry) {
    this.registry = registry;
  }

  private AttributeEvaluator getEvaluator() {
    return evaluator;
  }
}
