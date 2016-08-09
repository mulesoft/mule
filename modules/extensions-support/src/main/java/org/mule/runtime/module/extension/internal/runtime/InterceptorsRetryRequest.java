/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.module.extension.internal.runtime;

import org.mule.runtime.extension.api.runtime.operation.Interceptor;
import org.mule.runtime.extension.api.runtime.RetryRequest;

import java.util.HashSet;
import java.util.Set;

/**
 * Implementation of {@link RetryRequest} which has the ability to link every requests to a {@link Interceptor} and keep a history
 * of interceptors which already performed a request.
 * <p/>
 * This is useful to prevent badly written {@link Interceptor interceptors} from generating an endless loop by requesting retries
 * over and over again. If an interceptor already in the {@link #history} sends the request for a second time an
 * {@link IllegalStateException} is thrown.
 *
 * @since 4.0
 */
final class InterceptorsRetryRequest implements RetryRequest {

  private final Set<Object> history = new HashSet<>();
  private final Interceptor owner;
  private boolean requested = false;

  /**
   * Creates a new instance, copying the history from the {@code previous} request.
   * <p/>
   * If {@code previous} is {@code null}, then a blank history is used.
   *
   * @param interceptor the {@link Interceptor} to which this instance will be handed over
   * @param previous a {@link InterceptorsRetryRequest} which was previously handed to this or another interceptor. Could also be
   *        {@code null}
   */
  public InterceptorsRetryRequest(Interceptor interceptor, InterceptorsRetryRequest previous) {
    owner = interceptor;
    if (previous != null) {
      history.addAll(previous.history);
    }
  }

  /**
   * {@inheritDoc}
   * 
   * @throws IllegalStateException if the {@link #owner} is already in the {@link #history}
   */
  @Override
  public void request() {
    if (!history.add(owner)) {
      throw new IllegalStateException("A Interceptor requested to retry the same failed operation twice but only one retry is allowed "
          + "per execution. Interceptor is " + owner);
    }
    requested = true;
  }

  /**
   * @return Whether a retry has been requested on {@code this} instance
   */
  boolean isRetryRequested() {
    return requested;
  }
}
