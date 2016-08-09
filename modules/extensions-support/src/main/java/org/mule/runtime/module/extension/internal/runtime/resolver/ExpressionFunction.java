/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.module.extension.internal.runtime.resolver;

import static org.mule.metadata.java.api.utils.JavaTypeUtils.getType;
import org.mule.metadata.api.model.MetadataType;
import org.mule.runtime.core.api.MuleEvent;
import org.mule.runtime.core.api.MuleException;
import org.mule.runtime.core.api.MuleRuntimeException;

import java.util.function.Function;

import org.apache.commons.lang.builder.HashCodeBuilder;

/**
 * Represents a function that accepts a {@link MuleEvent} and evaluates a MEL expression that produces a result of the specified
 * type using a {@link TypeSafeExpressionValueResolver}.
 *
 * @since 4.0
 */
final class ExpressionFunction<T> implements Function<MuleEvent, T> {

  private final String expression;
  private final MetadataType type;

  ExpressionFunction(String expression, MetadataType type) {
    this.expression = expression;
    this.type = type;
  }

  @Override
  public T apply(MuleEvent event) {
    try {
      return new TypeSafeExpressionValueResolver<T>(expression, getType(type)).resolve(event);
    } catch (MuleException e) {
      throw new MuleRuntimeException(e);
    }
  }

  @Override
  public int hashCode() {
    return HashCodeBuilder.reflectionHashCode(this);
  }

  @Override
  public boolean equals(Object obj) {
    return obj instanceof ExpressionFunction && ((ExpressionFunction) obj).expression.equals(expression)
        && ((ExpressionFunction) obj).type.equals(type);
  }

}
