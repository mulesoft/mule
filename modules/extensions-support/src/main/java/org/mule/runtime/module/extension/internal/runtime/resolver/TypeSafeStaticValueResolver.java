/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.module.extension.internal.runtime.resolver;

import static org.mule.runtime.core.util.ClassUtils.isInstance;
import org.mule.runtime.api.exception.MuleException;
import org.mule.runtime.api.metadata.DataType;
import org.mule.runtime.core.api.Event;
import org.mule.runtime.core.api.MuleContext;

/**
 * {@link ValueResolver} implementation which always returns the same value, but if it is required,
 * the value will be converted to the required type using the mule transformers.
 *
 * @param <T> Type of the value to resolver
 * @since 4.0
 */
public class TypeSafeStaticValueResolver<T> implements ValueResolver<T> {

  private Resolver<T> resolver;

  public TypeSafeStaticValueResolver(Object value, Class<T> expectedType, MuleContext muleContext) {
    TypeSafeTransformer typeSafeTransformer = new TypeSafeTransformer(muleContext);
    if (isInstance(ValueResolver.class, value)) {
      resolver = (event) -> (T) ((ValueResolver) value).resolve(event);
    } else {
      if (isInstance(expectedType, value)) {
        resolver = (event) -> (T) value;
      } else {
        resolver = new CachedResolver(event -> (T) typeSafeTransformer.transform(value, DataType.fromObject(value),
                                                                                 DataType.fromType(expectedType), event));
      }
    }
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public T resolve(Event event) throws MuleException {
    return resolver.resolve(event);
  }

  @Override
  public boolean isDynamic() {
    return false;
  }

  @FunctionalInterface
  private interface Resolver<T> {

    T resolve(Event event) throws MuleException;

  }

  /**
   * {@link Resolver} implementation which caches the value of the resolution of the given {@param resolver} so
   * the resolution logic is executed once.
   */
  private class CachedResolver implements Resolver<T> {

    private Resolver<T> resolver;

    private CachedResolver(Resolver<T> resolver) {
      this.resolver = (event -> {
        T resolvedValue = resolver.resolve(event);
        this.resolver = (newEvent -> resolvedValue);
        return resolvedValue;
      });
    }

    @Override
    public T resolve(Event event) throws MuleException {
      return resolver.resolve(event);
    }
  }
}
