/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.module.extension.internal.runtime.resolver;

import static java.util.Collections.newSetFromMap;
import static org.mule.runtime.core.api.lifecycle.LifecycleUtils.initialiseIfNeeded;
import static org.mule.runtime.core.api.lifecycle.LifecycleUtils.startIfNeeded;
import org.mule.runtime.api.exception.MuleException;
import org.mule.runtime.api.util.Reference;
import org.mule.runtime.core.api.MuleContext;
import org.mule.runtime.core.api.lifecycle.LifecycleState;

import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Implementation of {@link LifecycleAwareValueResolverWrapper} which depending on the lifecycle state of the muleContext, applies
 * the Initialise and Start phases on the returned objects.
 * <p>
 * This resolver keeps track of all the returned values, making sure that lifecycle is only applied once on each possible return
 * value.
 *
 * @param <T> the generic type of the returned values
 * @since 4.1
 */
public class LifecycleInitialiserValueResolverWrapper<T> extends LifecycleAwareValueResolverWrapper<T> {

  private final MuleContext muleContext;
  private final Set<Reference<Object>> producedObjects = newSetFromMap(new ConcurrentHashMap<>());

  /**
   * Creates a new instance
   *
   * @param delegate    the delegate resolver
   * @param muleContext the current {@link MuleContext}
   */
  public LifecycleInitialiserValueResolverWrapper(ValueResolver<T> delegate, MuleContext muleContext) {
    super(delegate);
    this.muleContext = muleContext;
  }

  @Override
  public T resolve(ValueResolvingContext context) throws MuleException {
    T value = super.resolve(context);

    if (producedObjects.add(new Reference<>(value))) {
      LifecycleState state = muleContext.getLifecycleManager().getState();

      if (state.isInitialising() || state.isInitialised()) {
        initialiseIfNeeded(value, muleContext);
      }

      if (state.isStarting() || state.isStarted()) {
        startIfNeeded(value);
      }
    }

    return value;
  }

  @Override
  public void dispose() {
    producedObjects.clear();
    super.dispose();
  }
}
