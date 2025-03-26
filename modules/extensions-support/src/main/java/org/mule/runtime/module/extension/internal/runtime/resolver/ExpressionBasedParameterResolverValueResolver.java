/*
 * Copyright 2023 Salesforce, Inc. All rights reserved.
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.module.extension.internal.runtime.resolver;

import org.mule.runtime.api.exception.MuleException;
import org.mule.runtime.api.lifecycle.Initialisable;
import org.mule.runtime.api.lifecycle.InitialisationException;
import org.mule.runtime.api.metadata.DataType;
import org.mule.runtime.api.transformation.TransformationService;
import org.mule.runtime.core.api.el.ExtendedExpressionManager;
import org.mule.runtime.extension.api.runtime.parameter.ParameterResolver;
import org.mule.runtime.module.extension.api.runtime.resolver.ValueResolver;
import org.mule.runtime.module.extension.api.runtime.resolver.ValueResolvingContext;

import jakarta.inject.Inject;

/**
 * {@link ValueResolver} implementation for {@link ParameterResolver} that are resolved from an expression
 *
 * @since 4.0
 */
public class ExpressionBasedParameterResolverValueResolver<T> implements ExpressionBasedValueResolver<ParameterResolver<T>>,
    Initialisable {

  private final String expression;
  private final DataType expectedDataType;

  @Inject
  private TransformationService transformationService;

  @Inject
  private ExtendedExpressionManager extendedExpressionManager;

  private final Class<T> type;
  private TypeSafeExpressionValueResolver<T> delegateResolver;

  public ExpressionBasedParameterResolverValueResolver(String expression, Class<T> type, DataType expectedDataType) {
    this.expression = expression;
    this.type = type;
    this.expectedDataType = expectedDataType;
  }

  @Override
  public void initialise() throws InitialisationException {
    delegateResolver = new TypeSafeExpressionValueResolver<>(expression, type, expectedDataType);
    delegateResolver.setExtendedExpressionManager(extendedExpressionManager);
    delegateResolver.setTransformationService(transformationService);

    delegateResolver.initialise();
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public ParameterResolver<T> resolve(ValueResolvingContext context) throws MuleException {
    return new ExpressionBasedParameterResolver<>(expression, delegateResolver, context);
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public boolean isDynamic() {
    return true;
  }

  public void setTransformationService(TransformationService transformationService) {
    this.transformationService = transformationService;
  }

  public void setExtendedExpressionManager(ExtendedExpressionManager extendedExpressionManager) {
    this.extendedExpressionManager = extendedExpressionManager;
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public String getExpression() {
    return expression;
  }
}
