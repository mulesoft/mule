/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.module.extension.internal.loader;

import static com.google.common.collect.ImmutableList.copyOf;
import static com.google.common.collect.ImmutableList.of;
import static org.mule.runtime.core.api.lifecycle.LifecycleUtils.disposeIfNeeded;
import static org.mule.runtime.core.api.lifecycle.LifecycleUtils.initialiseIfNeeded;
import static org.mule.runtime.core.api.lifecycle.LifecycleUtils.safeStopIfNeeded;
import static org.mule.runtime.core.api.lifecycle.LifecycleUtils.startIfNeeded;
import static org.slf4j.LoggerFactory.getLogger;
import org.mule.runtime.core.api.MuleContext;
import org.mule.runtime.api.exception.MuleException;
import org.mule.runtime.api.lifecycle.InitialisationException;
import org.mule.runtime.api.lifecycle.Lifecycle;
import org.mule.runtime.extension.api.runtime.Interceptable;
import org.mule.runtime.extension.api.runtime.operation.Interceptor;

import java.util.List;

import javax.inject.Inject;

import org.slf4j.Logger;

/**
 * Base class for implementation of {@link Interceptable}.
 * <p>
 * Besides holding and managing a {@link List} of {@link Interceptor interceptors}, this class also implements {@link Lifecycle}
 * making sure that all the lifecycle phases are propagated to all the contained {@link #interceptors}.
 * <p>
 * It also provides behaviour for performing dependency injection into those {@link #interceptors}, including holding a
 * {@link #muleContext} instance which is declared as an injection target.
 *
 * @since 4.0
 */
public abstract class AbstractInterceptable implements Interceptable, Lifecycle {

  private static final Logger LOGGER = getLogger(AbstractInterceptable.class);

  private final List<Interceptor> interceptors;

  @Inject
  protected MuleContext muleContext;

  /**
   * Creates a new instance with the given {@code interceptor}
   *
   * @param interceptors a {@link List} with {@link Interceptor interceptors}. Can be empty or even {@code null}
   */
  public AbstractInterceptable(List<Interceptor> interceptors) {
    this.interceptors = interceptors != null ? copyOf(interceptors) : of();
  }

  /**
   * Propagates this phase to all the {@link #interceptors}
   *
   * @throws InitialisationException in case of error
   */
  @Override
  public void initialise() throws InitialisationException {
    for (Interceptor interceptor : interceptors) {
      initialiseIfNeeded(interceptor, true, muleContext);
    }
  }

  /**
   * Propagates this phase to all the {@link #interceptors}
   *
   * @throws MuleException in case of error
   */
  @Override
  public void start() throws MuleException {
    startIfNeeded(interceptors);
  }

  /**
   * Propagates this phase to all the {@link #interceptors}. All the {@link Interceptor interceptors} held will receive the phase
   * regardless of any (or all) of them throwing exceptions, in which case, exceptions will be logged.
   *
   * @throws MuleException if case of error
   */
  @Override
  public void stop() throws MuleException {
    safeStopIfNeeded(interceptors, LOGGER);
  }

  /**
   * Propagates this phase to all the {@link #interceptors}. All the {@link Interceptor interceptors} held will receive the phase
   * regardless of any (or all) of them throwing exceptions, in which case, exceptions will be logged.
   *
   * @throws MuleException if case of error
   */

  @Override
  public void dispose() {
    disposeIfNeeded(interceptors, LOGGER);
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public final List<Interceptor> getInterceptors() {
    return interceptors;
  }
}
