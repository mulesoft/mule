/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.module.extension.internal.runtime.resolver;

import org.mule.runtime.api.exception.MuleException;
import org.mule.runtime.core.api.event.BaseEvent;
import org.mule.runtime.extension.api.runtime.parameter.ParameterResolver;

/**
 * {@link ValueResolver} implementation for {@link ParameterResolver} that are not resolved from an
 * expression.
 * <p>
 * This {@link ParameterResolverValueResolverWrapper} delegates the resolution to the given {@link ValueResolver} from
 * the constructor and wraps the value into a {@link StaticParameterResolver}
 *
 * @since 4.0
 * @see ParameterResolver
 */
public class ParameterResolverValueResolverWrapper<T> implements ValueResolver<ParameterResolver<T>> {

  private ValueResolver resolver;

  public ParameterResolverValueResolverWrapper(ValueResolver resolver) {
    this.resolver = resolver;
  }

  /**
   * Resolves the value of {@link this#resolver} using the given {@link BaseEvent} and wraps it into a
   * {@link StaticParameterResolver}
   *
   * @param context a {@link ValueResolvingContext} to resolve the {@link ValueResolver}
   * @return an {@link ParameterResolver} with the resolved value
   * @throws MuleException if it fails to resolve the value
   */
  @Override
  public ParameterResolver<T> resolve(ValueResolvingContext context) throws MuleException {
    return new StaticParameterResolver<>((T) resolver.resolve(context));
  }

  @Override
  public boolean isDynamic() {
    return resolver.isDynamic();
  }
}
