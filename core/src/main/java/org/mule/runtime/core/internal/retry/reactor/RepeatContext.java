/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.core.internal.retry.reactor;

import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

/**
 * Context provided to repeat predicate {@link Repeat#onlyIf(java.util.function.Predicate)} and
 * the repeat callback {@link Repeat#doOnRepeat(java.util.function.Consumer)}.
 *
 * @param <T> Application context type
 */
public interface RepeatContext<T> extends Context<T> {

  /**
   * Returns the value provided in the companion Flux for repeats.
   * <ul>
   *   <li>For {@link Flux#retryWhen(java.util.function.Function)} and {@link Mono#retryWhen(java.util.function.Function)},
   *      value is set to null and the exception is returned by {@link #getException()}.</li>
   *   <li>For {@link Flux#repeatWhen(java.util.function.Function)} and {@link Mono#repeatWhen(java.util.function.Function)},
   *      value is the number of items emitted in the last attempt.
   *   <li>For {@link Mono#repeatWhenEmpty(java.util.function.Function)} and {@link Mono#repeatWhenEmpty(int, java.util.function.Function)},
   *      value is a zero-based incrementing Long, which is {@link #getAttempts()} - 1.
   * </ul>
   * @return value the value emitted on the companion Flux for repeats.
   */
  public Long companionValue();
}
