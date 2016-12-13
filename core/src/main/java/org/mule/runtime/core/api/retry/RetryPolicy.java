/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.core.api.retry;

import org.mule.runtime.core.retry.PolicyStatus;

import java.util.function.Predicate;

import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

/**
 * A RetryPolicy takes some action each time an exception occurs and returns a {@link PolicyStatus} which indicates whether the
 * policy is exhausted or should continue to retry.
 */
public interface RetryPolicy {

  PolicyStatus applyPolicy(Throwable cause);

  void applyOn(Mono<?> publisher, Predicate<Throwable> predicate);

  void applyOn(Flux<?> publisher, Predicate<Throwable> predicate);
}
