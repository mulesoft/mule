/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.core.api.retry.policy;

import org.mule.runtime.api.scheduler.Scheduler;

import java.util.concurrent.CompletableFuture;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.function.Supplier;

/**
 * This policy is basically a placeholder. It does not attempt to retry at all.
 */
public final class NoRetryPolicyTemplate extends AbstractPolicyTemplate {

  public RetryPolicy createRetryInstance() {
    return new NoRetryPolicy();
  }

  @Override
  public boolean isEnabled() {
    return false;
  }

  protected static class NoRetryPolicy implements RetryPolicy {

    public PolicyStatus applyPolicy(Throwable cause) {
      return PolicyStatus.policyExhausted(cause);
    }

    @Override
    public <T> CompletableFuture<T> applyPolicy(Supplier<CompletableFuture<T>> completableFutureSupplier,
                                                Predicate<Throwable> shouldRetry, Consumer<Throwable> onRetry,
                                                Consumer<Throwable> onExhausted, Function<Throwable, Throwable> errorFunction,
                                                Scheduler retryScheduler) {
      return completableFutureSupplier.get();
    }
  }

  public String toString() {
    return "NoRetryPolicy{}";
  }
}
