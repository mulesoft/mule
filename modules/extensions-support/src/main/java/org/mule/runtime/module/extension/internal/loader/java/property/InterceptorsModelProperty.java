/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.module.extension.internal.loader.java.property;

import static com.google.common.collect.ImmutableList.copyOf;
import org.mule.runtime.api.meta.model.ModelProperty;
import org.mule.runtime.extension.api.runtime.Interceptable;
import org.mule.runtime.extension.api.runtime.InterceptorFactory;
import org.mule.runtime.extension.api.runtime.operation.Interceptor;

import java.util.LinkedList;
import java.util.List;

/**
 * A model property which allows the enriched component to specify its
 * own set of interceptors through a list of {@link InterceptorFactory}
 *
 * @since 4.0
 */
public final class InterceptorsModelProperty implements ModelProperty {

  private final List<InterceptorFactory> interceptorFactories;

  /**
   * Creates a new instance which holds a copy of the given {@code interceptorFactories}
   *
   * @param interceptorFactories a {@link List} of {@link InterceptorFactory}
   */
  public InterceptorsModelProperty(List<InterceptorFactory> interceptorFactories) {
    this.interceptorFactories = interceptorFactories != null
        ? new LinkedList<>(interceptorFactories)
        : new LinkedList<>();
  }

  /**
   * Returns a {@link List} which items are {@link InterceptorFactory} instances
   * that are to be used to provision the {@link Interceptor interceptors} for the
   * configurations created from this model.
   * <p>
   * The order of the factories in the list will be the same as the order of the resulting
   * {@link Interceptor interceptors}. However, just like it's explained in the
   * {@link Interceptable} interface, the order is not guaranteed to be respected although
   * it should be expressed anyway.
   *
   * @return an immutable {@link List}. Can be empty but must never be {@code null}
   */
  public List<InterceptorFactory> getInterceptorFactories() {
    return copyOf(interceptorFactories);
  }

  /**
   * Adds the given {@code interceptorFactory} as the last interceptor in
   * the list
   *
   * @param interceptorFactory a {@link InterceptorFactory}
   */
  public void addInterceptorFactory(InterceptorFactory interceptorFactory) {
    interceptorFactories.add(interceptorFactory);
  }

  /**
   * /**
   * Adds the given {@code interceptorFactory} to the list at the
   * given {@code position}
   *
   * @param interceptorFactory a {@link InterceptorFactory}
   * @param position           a valid list index
   */
  public void addInterceptorFactory(InterceptorFactory interceptorFactory, int position) {
    interceptorFactories.add(position, interceptorFactory);
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public String getName() {
    return "interceptorsModelProperty";
  }

  /**
   * @return {@code false}
   */
  @Override
  public boolean isPublic() {
    return false;
  }
}
