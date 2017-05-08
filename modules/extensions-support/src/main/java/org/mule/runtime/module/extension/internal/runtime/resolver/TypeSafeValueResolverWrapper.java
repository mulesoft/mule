/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.module.extension.internal.runtime.resolver;

import static org.mule.runtime.core.util.ClassUtils.isInstance;
import org.mule.runtime.api.exception.MuleException;
import org.mule.runtime.api.lifecycle.Initialisable;
import org.mule.runtime.api.lifecycle.InitialisationException;
import org.mule.runtime.api.metadata.DataType;
import org.mule.runtime.core.api.Event;
import org.mule.runtime.core.api.TransformationService;
import org.mule.runtime.core.api.transformer.Transformer;

import javax.inject.Inject;

/**
 * {@link ValueResolver} wrapper implementation which wraps another {@link ValueResolver} and ensures that the output is always of
 * a certain type.
 * <p>
 * If the returned value of the {@link this#valueResolverDelegate} is not of the desired type, it tries to locate a
 * {@link Transformer} which can do the transformation from the obtained type to the expected one.
 *
 * @param <T>
 * @since 4.0
 */
public class TypeSafeValueResolverWrapper<T> implements ValueResolver<T>, Initialisable {

  private final Class<T> expectedType;
  private ValueResolver valueResolverDelegate;
  private Resolver<T> resolver;

  @Inject
  private TransformationService transformationService;

  public TypeSafeValueResolverWrapper(ValueResolver valueResolverDelegate, Class<T> expectedType) {
    this.expectedType = expectedType;
    this.valueResolverDelegate = valueResolverDelegate;
  }

  @Override
  public T resolve(Event event) throws MuleException {
    return resolver.resolve(event);
  }

  @Override
  public boolean isDynamic() {
    return valueResolverDelegate.isDynamic();
  }

  @Override
  public void initialise() throws InitialisationException {
    TypeSafeTransformer typeSafeTransformer = new TypeSafeTransformer(transformationService);
    resolver = (event -> {
      Object resolvedValue = valueResolverDelegate.resolve(event);
      return isInstance(expectedType, resolvedValue)
          ? (T) resolvedValue
          : (T) typeSafeTransformer.transform(resolvedValue, DataType.fromObject(resolvedValue), DataType.fromType(expectedType));
    });

    if (!valueResolverDelegate.isDynamic()) {
      resolver = new CachedResolver(resolver);
    }
  }

  public void setTransformationService(TransformationService transformationService) {
    this.transformationService = transformationService;
  }

  @FunctionalInterface
  private interface Resolver<T> {

    T resolve(Event event) throws MuleException;
  }

  /**
   * {@link Resolver} implementation which caches the value of the resolution of the given {@param resolver} so the resolution
   * logic is executed once.
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
