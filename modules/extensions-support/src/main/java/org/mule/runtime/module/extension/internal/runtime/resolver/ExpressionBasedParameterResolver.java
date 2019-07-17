/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.module.extension.internal.runtime.resolver;

import org.mule.runtime.api.exception.MuleException;
import org.mule.runtime.api.exception.MuleRuntimeException;
import org.mule.runtime.api.lifecycle.Initialisable;
import org.mule.runtime.api.lifecycle.InitialisationException;
import org.mule.runtime.api.metadata.DataType;
import org.mule.runtime.api.transformation.TransformationService;
import org.mule.runtime.core.api.MuleContext;
import org.mule.runtime.core.api.el.ExtendedExpressionManager;
import org.mule.runtime.extension.api.runtime.parameter.ParameterResolver;

import java.util.Optional;

import javax.inject.Inject;

/**
 * {@link ParameterResolver} implementation for the parameters that are resolved from an expression
 *
 * @param <T> Concrete parameter type to be resolved
 * @since 4.0
 */
class ExpressionBasedParameterResolver<T> implements ParameterResolver<T>, Initialisable {

  private final String expression;
  private final ValueResolvingContext context;
  private final DataType expectedDataType;
  private TypeSafeExpressionValueResolver<T> valueResolver;

  @Inject
  private TransformationService transformationService;

  @Inject
  private ExtendedExpressionManager extendedExpressionManager;

  @Inject
  private MuleContext muleContext;

  private Class<T> type;
  private Boolean melDefault;
  private Boolean melAvailable;

  ExpressionBasedParameterResolver(String expression, Class<T> type, ValueResolvingContext context, DataType expectedDataType) {
    this.expression = expression;
    this.type = type;
    this.context = context;
    this.expectedDataType = expectedDataType;
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public T resolve() {
    try {
      return valueResolver.resolve(context);
    } catch (MuleException e) {
      throw new MuleRuntimeException(e);
    }
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public Optional<String> getExpression() {
    return Optional.ofNullable(expression);
  }

  @Override
  public void initialise() throws InitialisationException {
    valueResolver = new TypeSafeExpressionValueResolver<>(expression, type, expectedDataType);
    valueResolver.setExtendedExpressionManager(extendedExpressionManager);
    valueResolver.setTransformationService(transformationService);
    valueResolver.setMuleContext(muleContext);
    valueResolver.setMelDefault(melDefault);
    valueResolver.setMelAvailable(melAvailable);

    valueResolver.initialise();
  }

  public void setTransformationService(TransformationService transformationService) {
    this.transformationService = transformationService;
  }

  public void setExtendedExpressionManager(ExtendedExpressionManager extendedExpressionManager) {
    this.extendedExpressionManager = extendedExpressionManager;
  }

  public void setMuleContext(MuleContext muleContext) {
    this.muleContext = muleContext;
  }

  public void setMelDefault(Boolean melDefault) {
    this.melDefault = melDefault;
  }

  public void setMelAvailable(Boolean melAvailable) {
    this.melAvailable = melAvailable;
  }

  @Override
  public int hashCode() {
    return this.resolve().hashCode();
  }

  @Override
  public boolean equals(Object obj) {
    if (obj instanceof ExpressionBasedParameterResolver) {
      ExpressionBasedParameterResolver other = (ExpressionBasedParameterResolver) obj;
      return this.resolve().equals(other.resolve());
    }
    return false;
  }
}
