/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.core.api.retry.policy;

import org.mule.runtime.api.scheduler.Scheduler;
import org.mule.runtime.core.api.retry.RetryCallback;
import org.mule.runtime.core.api.retry.RetryContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executor;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.function.Supplier;

/**
 * This policy is basically a placeholder. It does not attempt to retry at all.
 */
public final class NoRetryPolicyTemplate extends AbstractPolicyTemplate {

  private static final Logger LOGGER = LoggerFactory.getLogger(NoRetryPolicyTemplate.class);

  @Override
  public RetryPolicy createRetryInstance() {
    return new NoRetryPolicy();
  }

  @Override
  public boolean isEnabled() {
    return false;
  }

  protected static class NoRetryPolicy implements RetryPolicy {

    @Override
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

  @Override
  public String toString() {
    return "NoRetryPolicy{}";
  }
  
  private static class DoNothingRetryCallback implements RetryCallback {

	  
	private static final Logger INNER_LOGGER = LoggerFactory.getLogger(NoRetryPolicyTemplate.DoNothingRetryCallback.class);
	private NoRetryPolicyTemplate noRetryPolicyTemplate;
	
	public DoNothingRetryCallback(NoRetryPolicyTemplate aNoRetryPolicyTemplate) {
		noRetryPolicyTemplate = aNoRetryPolicyTemplate;
	}

	@Override
	public void doWork(RetryContext aContext) {
		INNER_LOGGER.info("No nothing with " + aContext);
	}

	@Override
	public String getWorkDescription() {
		return "Do Nothing";
	}

	@Override
	public Object getWorkOwner() {
		return noRetryPolicyTemplate;
	}
	  
  }
  
  /**
	 * 
	 */
	@Override
	public RetryContext execute(RetryCallback aCallback, Executor aWorkManager) throws Exception {
		LOGGER.debug("Skip aCallback={}",aCallback);
		return super.execute(new DoNothingRetryCallback(this), aWorkManager);
	}
}
