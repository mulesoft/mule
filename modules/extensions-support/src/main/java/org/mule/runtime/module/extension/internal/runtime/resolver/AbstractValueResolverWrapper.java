/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.module.extension.internal.runtime.resolver;

import static org.mule.runtime.core.api.lifecycle.LifecycleUtils.disposeIfNeeded;
import static org.mule.runtime.core.api.lifecycle.LifecycleUtils.initialiseIfNeeded;
import static org.mule.runtime.core.api.lifecycle.LifecycleUtils.startIfNeeded;
import static org.mule.runtime.core.api.lifecycle.LifecycleUtils.stopIfNeeded;
import org.mule.runtime.api.exception.MuleException;
import org.mule.runtime.api.lifecycle.InitialisationException;
import org.mule.runtime.api.lifecycle.Lifecycle;
import org.mule.runtime.core.api.MuleContext;

import javax.inject.Inject;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Base class for {@link ValueResolver} wrappers.
 * <p>
 * It propagates {@link Lifecycle} phases and DI to the wrapped resolver.
 *
 * @param <T> the generic type of the resolved values
 * @since 4.0
 */
public abstract class AbstractValueResolverWrapper<T> implements ValueResolver<T>, Lifecycle {

  private static final Logger LOGGER = LoggerFactory.getLogger(AbstractValueResolverWrapper.class);

  protected final ValueResolver<T> delegate;

  @Inject
  protected MuleContext muleContext;

  public AbstractValueResolverWrapper(ValueResolver<T> delegate) {
    this.delegate = delegate;
  }

  @Override
  public T resolve(ValueResolvingContext context) throws MuleException {
    return delegate.resolve(context);
  }

  @Override
  public boolean isDynamic() {
    return delegate.isDynamic();
  }

  @Override
  public void initialise() throws InitialisationException {
    initialiseIfNeeded(delegate, true, muleContext);
  }

  @Override
  public void start() throws MuleException {
    startIfNeeded(delegate);
  }

  @Override
  public void stop() throws MuleException {
    stopIfNeeded(delegate);
  }

  @Override
  public void dispose() {
    disposeIfNeeded(delegate, LOGGER);
  }

}
