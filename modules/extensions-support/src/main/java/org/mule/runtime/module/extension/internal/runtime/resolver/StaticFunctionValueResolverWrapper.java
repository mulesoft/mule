/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.module.extension.internal.runtime.resolver;

import org.mule.runtime.api.exception.MuleException;
import org.mule.runtime.core.api.Event;

import java.util.function.Function;

/**
 * {@link ValueResolver} implementation for {@link Function}s that are not resolved from an
 * expression.
 * <p>
 * This {@link StaticFunctionValueResolverWrapper} delegates the resolution to the given {@link ValueResolver} from
 * the constructor.
 *
 * @since 4.0
 */
public final class StaticFunctionValueResolverWrapper<T> implements ValueResolver<Function<Event, T>> {

  private ValueResolver<T> resolver;

  public StaticFunctionValueResolverWrapper(ValueResolver<T> resolver) {
    this.resolver = resolver;
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public Function<Event, T> resolve(Event event) throws MuleException {
    T result = resolver.resolve(event);
    return (functionEvent) -> result;
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public boolean isDynamic() {
    return false;
  }
}
