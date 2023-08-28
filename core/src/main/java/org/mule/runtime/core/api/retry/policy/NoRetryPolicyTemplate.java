/*
 * Copyright 2023 Salesforce, Inc. All rights reserved.
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
