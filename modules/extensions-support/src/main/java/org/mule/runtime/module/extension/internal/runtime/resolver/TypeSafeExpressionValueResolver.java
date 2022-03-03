/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.module.extension.internal.runtime.resolver;

import static org.mule.runtime.api.util.Preconditions.checkArgument;
import org.mule.runtime.api.artifact.Registry;
import org.mule.runtime.api.exception.MuleException;
import org.mule.runtime.api.lifecycle.Initialisable;
import org.mule.runtime.api.lifecycle.InitialisationException;
import org.mule.runtime.api.metadata.DataType;
import org.mule.runtime.api.transformation.TransformationService;
import org.mule.runtime.core.api.MuleContext;
import org.mule.runtime.core.api.el.ExtendedExpressionManager;
import org.mule.runtime.core.api.transformer.Transformer;
import org.mule.runtime.core.privileged.util.AttributeEvaluator;

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

  @Inject
  private MuleContext muleContext;

  @Inject
  private Registry registry;

  private DataType expectedDataType;
  private Boolean melDefault;
  private Boolean melAvailable;

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
    ExpressionValueResolver resolver = new ExpressionValueResolver(expression, expectedDataType, melDefault, melAvailable);
    resolver.setExtendedExpressionManager(extendedExpressionManager);
    resolver.setRegistry(registry);
    resolver.initialise();

    delegate = new TypeSafeValueResolverWrapper<>(resolver, expectedType);
    delegate.setTransformationService(transformationService);
    delegate.setMuleContext(muleContext);

    delegate.initialise();
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
}
