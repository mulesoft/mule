/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.module.extension.internal.runtime.resolver;

import org.mule.runtime.api.exception.MuleException;
import org.mule.runtime.api.metadata.DataType;
import org.mule.runtime.api.metadata.TypedValue;
import org.mule.runtime.core.api.Event;

/**
 * {@link ValueResolver} implementation for {@link TypedValue} that are not resolved from an
 * expression.
 * <p>
 * This {@link TypedValueValueResolverWrapper} delegates the resolution to the given {@link ValueResolver} from
 * the constructor.
 *
 * @since 4.0
 * @see TypedValue
 */
public final class TypedValueValueResolverWrapper<T> implements ValueResolver<TypedValue<T>> {

  private ValueResolver resolver;

  public TypedValueValueResolverWrapper(ValueResolver resolver) {
    this.resolver = resolver;
  }

  /**
   * Resolves the value of {@link this#resolver} using the given {@link Event} and returns the correspondent
   * {@link TypedValue}
   *
   * @param context a {@link ValueResolvingContext} to resolve the {@link ValueResolver}
   * @return The {@link TypedValue} of the resolved value
   * @throws MuleException if it fails to resolve the value
   */
  @Override
  public TypedValue<T> resolve(ValueResolvingContext context) throws MuleException {
    Object resolve = resolver.resolve(context);
    return new TypedValue<>((T) resolver.resolve(context), DataType.fromObject(resolve));
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public boolean isDynamic() {
    return resolver.isDynamic();
  }
}
