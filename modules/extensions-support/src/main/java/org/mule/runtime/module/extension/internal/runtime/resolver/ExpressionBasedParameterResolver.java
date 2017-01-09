/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.module.extension.internal.runtime.resolver;

import static org.mule.metadata.java.api.utils.JavaTypeUtils.getType;
import org.mule.metadata.api.model.MetadataType;
import org.mule.runtime.api.exception.MuleException;
import org.mule.runtime.api.exception.MuleRuntimeException;
import org.mule.runtime.core.api.Event;
import org.mule.runtime.core.api.MuleContext;
import org.mule.runtime.extension.api.runtime.operation.ParameterResolver;

import java.util.Optional;

/**
 * {@link ParameterResolver} implementation for the parameters that are resolved from an expression
 *
 * @param <T> Concrete parameter type to be resolved
 * @since 4.0
 */
class ExpressionBasedParameterResolver<T> implements ParameterResolver<T> {

  private final String expression;
  private final Event event;
  private final TypeSafeExpressionValueResolver<T> valueResolver;

  ExpressionBasedParameterResolver(String expression, MetadataType metadataType, MuleContext muleContext, Event event) {
    this.expression = expression;
    this.event = event;
    this.valueResolver = new TypeSafeExpressionValueResolver<>(expression, getType(metadataType), muleContext);
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public T resolve() {
    try {
      return valueResolver.resolve(event);
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
}
