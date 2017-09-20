/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.module.extension.internal.runtime.resolver;

import static org.mule.runtime.core.api.lifecycle.LifecycleUtils.initialiseIfNeeded;
import static org.mule.runtime.core.api.util.ClassUtils.isInstance;

import org.mule.runtime.api.exception.MuleException;
import org.mule.runtime.api.lifecycle.Initialisable;
import org.mule.runtime.api.lifecycle.InitialisationException;
import org.mule.runtime.api.metadata.DataType;
import org.mule.runtime.api.transformation.TransformationService;
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
  public T resolve(ValueResolvingContext context) throws MuleException {
    return resolver.resolve(context);
  }

  @Override
  public boolean isDynamic() {
    return valueResolverDelegate.isDynamic();
  }

  @Override
  public void initialise() throws InitialisationException {
    TypeSafeTransformer typeSafeTransformer = new TypeSafeTransformer(transformationService);
    initialiseIfNeeded(valueResolverDelegate);
    resolver = context -> {
      Object resolvedValue = valueResolverDelegate.resolve(context);
      return isInstance(expectedType, resolvedValue)
          ? (T) resolvedValue
          : typeSafeTransformer.transform(resolvedValue, DataType.fromObject(resolvedValue), DataType.fromType(expectedType));
    };

    if (!valueResolverDelegate.isDynamic()) {
      resolver = new CachedResolver(resolver);
    }
  }

  public void setTransformationService(TransformationService transformationService) {
    this.transformationService = transformationService;
  }

  @FunctionalInterface
  private interface Resolver<T> {

    T resolve(ValueResolvingContext context) throws MuleException;
  }

  /**
   * {@link Resolver} implementation which caches the value of the resolution of the given {@param resolver} so the resolution
   * logic is executed once.
   */
  private class CachedResolver implements Resolver<T> {

    private Resolver<T> resolver;

    private CachedResolver(Resolver<T> resolver) {
      this.resolver = context -> {
        T resolvedValue = resolver.resolve(context);
        this.resolver = c -> resolvedValue;
        return resolvedValue;
      };
    }

    @Override
    public T resolve(ValueResolvingContext context) throws MuleException {
      return resolver.resolve(context);
    }
  }
}
