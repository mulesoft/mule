/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.module.extension.internal.runtime.resolver;

import static org.mule.runtime.api.util.Preconditions.checkArgument;
import static org.mule.runtime.core.api.lifecycle.LifecycleUtils.initialiseIfNeeded;

import org.mule.runtime.api.exception.MuleException;
import org.mule.runtime.api.lifecycle.Initialisable;
import org.mule.runtime.api.lifecycle.InitialisationException;
import org.mule.runtime.api.lifecycle.Lifecycle;
import org.mule.runtime.api.component.AbstractComponent;
import org.mule.runtime.core.api.MuleContext;
import org.mule.runtime.module.extension.internal.runtime.objectbuilder.ObjectBuilder;

/**
 * A {@link ValueResolver} which wraps an {@link ObjectBuilder} and calls {@link ObjectBuilder#build(ValueResolvingContext)} on
 * each {@link #resolve(ValueResolvingContext)}.
 * <p/>
 * It implements {@link Lifecycle} and propagates all lifecycle events to the underlying {@code builder}
 *
 * @param <T> the generic type for the instances built.
 * @since 3.7.0
 */
public class ObjectBuilderValueResolver<T> extends AbstractComponent implements ValueResolver<T>, Initialisable {

  private final ObjectBuilder<T> builder;
  private final MuleContext muleContext;

  public ObjectBuilderValueResolver(ObjectBuilder<T> builder, MuleContext muleContext) {
    checkArgument(builder != null, "builder cannot be null");
    this.builder = builder;
    this.muleContext = muleContext;
  }

  /**
   * Delegates to {@code builder}
   *
   * @param context a {@link ValueResolvingContext}
   * @return the {@code builder}'s output object
   * @throws MuleException
   */
  @Override
  public T resolve(ValueResolvingContext context) throws MuleException {
    return builder.build(context);
  }

  /**
   * @return {@code true} if {@code builder} is dynamic
   */
  @Override
  public boolean isDynamic() {
    return builder.isDynamic();
  }

  @Override
  public void initialise() throws InitialisationException {
    initialiseIfNeeded(builder, true, muleContext);
  }

}
