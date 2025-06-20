/*
 * Copyright 2023 Salesforce, Inc. All rights reserved.
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.module.extension.internal.runtime.resolver;

import static org.mule.runtime.api.util.Preconditions.checkArgument;

import org.mule.runtime.api.exception.MuleException;
import org.mule.runtime.api.lifecycle.Initialisable;
import org.mule.runtime.api.lifecycle.InitialisationException;
import org.mule.runtime.api.metadata.DataType;
import org.mule.runtime.api.transformation.TransformationService;
import org.mule.runtime.core.api.el.ExtendedExpressionManager;
import org.mule.runtime.core.api.transformer.Transformer;
import org.mule.runtime.core.internal.util.attribute.AttributeEvaluator;
import org.mule.runtime.module.extension.api.runtime.resolver.ValueResolver;
import org.mule.runtime.module.extension.api.runtime.resolver.ValueResolvingContext;

import javax.inject.Inject;

/**
 * A {@link ValueResolver} which evaluates expressions and tries to ensure that the output is always of a certain type.
 * <p>
 * If the expression does not return a value of that type, then it tries to locate a {@link Transformer} which can do the
 * transformation from the obtained type to the expected one.
 * <p>
 * It resolves the expressions by making use of the {@link AttributeEvaluator} so that it's compatible with simple expressions and
 * templates alike
 *
 * @param <T>
 * @since 3.7.0
 */
public class TypeSafeExpressionValueResolver<T> implements ValueResolver<T>, Initialisable {

  private final Class<T> expectedType;
  private final String expression;
  private TypeSafeValueResolverWrapper<T> delegate;

  @Inject
  private TransformationService transformationService;

  @Inject
  private ExtendedExpressionManager extendedExpressionManager;

  private final DataType expectedDataType;

  public TypeSafeExpressionValueResolver(String expression, Class<T> expectedType, DataType expectedDataType) {
    checkArgument(expectedType != null, "expected type cannot be null");
    this.expression = expression;
    this.expectedType = expectedType;
    this.expectedDataType = expectedDataType;
  }

  @Override
  public T resolve(ValueResolvingContext context) throws MuleException {
    return delegate.resolve(context);
  }

  /**
   * @return {@code true}
   */
  @Override
  public boolean isDynamic() {
    return true;
  }

  @Override
  public void initialise() throws InitialisationException {
    ExpressionValueResolver resolver = new ExpressionValueResolver(expression, expectedDataType);
    resolver.setExtendedExpressionManager(extendedExpressionManager);
    resolver.initialise();

    delegate = new TypeSafeValueResolverWrapper<>(resolver, expectedType);
    delegate.setTransformationService(transformationService);

    delegate.initialise();
  }

  public void setTransformationService(TransformationService transformationService) {
    this.transformationService = transformationService;
  }

  public void setExtendedExpressionManager(ExtendedExpressionManager extendedExpressionManager) {
    this.extendedExpressionManager = extendedExpressionManager;
  }

  @Override
  public String toString() {
    return "TypeSafeExpressionValueResolver: " + expression;
  }
}
