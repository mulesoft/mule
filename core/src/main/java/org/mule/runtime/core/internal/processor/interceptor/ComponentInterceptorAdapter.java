/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.core.internal.processor.interceptor;

import org.mule.runtime.api.component.location.ComponentLocation;
import org.mule.runtime.api.interception.InterceptionEvent;
import org.mule.runtime.api.interception.ProcessorParameterValue;
import org.mule.runtime.core.internal.interception.DefaultInterceptionEvent;

import java.util.Map;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;

/**
 * Wraps different interceptors so they can be used seamlessly from {@link ReactiveInterceptionAction} and
 * {@link ReactiveAroundInterceptorAdapter}.
 *
 * @since 4.4
 */
interface ComponentInterceptorAdapter {

  /**
   * @return {@code true} if the adapted interceptor implements a {@code before} or {@code after} method.
   */
  boolean implementsBeforeOrAfter();

  /**
   * @return {@code true} if the adapted interceptor implements an {@code around} method.
   */
  boolean implementsAround();

  void before(ComponentLocation location, Map<String, ProcessorParameterValue> resolvedParams,
              DefaultInterceptionEvent interceptionEvent);

  CompletableFuture<InterceptionEvent> around(ComponentLocation location, Map<String, ProcessorParameterValue> resolvedParams,
                                              DefaultInterceptionEvent interceptionEvent,
                                              ReactiveInterceptionAction reactiveInterceptionAction);

  void after(ComponentLocation location, DefaultInterceptionEvent interceptionEvent, Optional<Throwable> thrown);

  default boolean isErrorMappingRequired(ComponentLocation location) {
    return false;
  }

  /**
   * @return The classLoader to use as TCCL when executing the adapted interceptor.
   */
  ClassLoader getClassLoader();

}
