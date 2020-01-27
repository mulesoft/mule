/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.core.internal.processor.strategy;

import static java.lang.Integer.MAX_VALUE;
import static java.lang.Integer.getInteger;
import static java.lang.Integer.max;
import static java.lang.Long.max;
import static java.lang.Runtime.getRuntime;
import static java.lang.String.format;
import static java.lang.System.currentTimeMillis;
import static java.lang.System.getProperty;
import static java.lang.Thread.currentThread;
import static java.util.Objects.requireNonNull;
import static java.util.concurrent.TimeUnit.MILLISECONDS;
import static org.mule.runtime.api.util.MuleSystemProperties.MULE_LIFECYCLE_FAIL_ON_FIRST_DISPOSE_ERROR;
import static org.mule.runtime.core.api.processor.ReactiveProcessor.ProcessingType.CPU_LITE;
import static org.slf4j.LoggerFactory.getLogger;
import static reactor.util.concurrent.Queues.isPowerOfTwo;

import org.mule.runtime.api.scheduler.Scheduler;
import org.mule.runtime.core.api.MuleContext;
import org.mule.runtime.core.api.construct.FlowConstruct;
import org.mule.runtime.core.api.processor.strategy.ProcessingStrategy;
import org.mule.runtime.core.api.processor.strategy.ProcessingStrategyFactory;

import java.util.concurrent.CountDownLatch;
import java.util.function.Supplier;

import org.slf4j.Logger;

/**
 * Abstract {@link ProcessingStrategyFactory} to be used by implementations that de-multiplex incoming messages.
 * <p>
 * Processing strategies created with this factory are not suitable for transactional flows and will fail if used with an active
 * transaction by default.
 *
 * @since 4.0
 */
public abstract class AbstractStreamProcessingStrategyFactory extends AbstractProcessingStrategyFactory {

  private static final Logger LOGGER = getLogger(AbstractStreamProcessingStrategyFactory.class);

  protected static final String SYSTEM_PROPERTY_PREFIX = AbstractStreamProcessingStrategyFactory.class.getName() + ".";
  protected static final int CORES = getInteger(SYSTEM_PROPERTY_PREFIX + "AVAILABLE_CORES", getRuntime().availableProcessors());
  protected static final int DEFAULT_BUFFER_SIZE = getInteger(SYSTEM_PROPERTY_PREFIX + "DEFAULT_BUFFER_SIZE", 1024);
  protected static final int FLOW_DISPATCH_WORKERS = getInteger(SYSTEM_PROPERTY_PREFIX + "FLOW_DISPATCH_WORKERS", 0);

  // Use one subscriber for every two cores available, or 1 subscriber for 1 core. This value is high for most scenarios but
  // required to achieve absolute minimum latency for the scenarios where this is important.
  protected static final int DEFAULT_SUBSCRIBER_COUNT =
      getInteger(SYSTEM_PROPERTY_PREFIX + "DEFAULT_SUBSCRIBER_COUNT", max(1, (CORES / 2)));
  private int bufferSize = DEFAULT_BUFFER_SIZE;
  private int subscriberCount = DEFAULT_SUBSCRIBER_COUNT;

  /**
   * Configure the size of the ring-buffer size used to buffer and de-multiplexes events from multiple source threads. This value
   * must be a power-of two.
   * <p>
   * Ring buffers typically use a power of two because it means that the rollover at the end of the buffer can be achieved using a
   * bit mask rather than having to explicitly compare the head/tail pointer with the end of the buffer.
   *
   * @param bufferSize buffer size to use.
   */
  public void setBufferSize(int bufferSize) {
    if (!isPowerOfTwo(bufferSize)) {
      throw new IllegalArgumentException("bufferSize must be a power of 2 : " + bufferSize);
    }
    this.bufferSize = bufferSize;
  }

  /**
   * Configure the number of ring-buffer subscribers.
   *
   * @param subscriberCount
   */
  public void setSubscriberCount(int subscriberCount) {
    this.subscriberCount = subscriberCount;
  }

  protected int getBufferSize() {
    return bufferSize;
  }

  protected int getSubscriberCount() {
    return subscriberCount;
  }

  @Override
  public Class<? extends ProcessingStrategy> getProcessingStrategyType() {
    return AbstractStreamProcessingStrategy.class;
  }

  protected Supplier<Scheduler> getCpuLightSchedulerSupplier(MuleContext muleContext, String schedulersNamePrefix) {
    return () -> muleContext.getSchedulerService()
        .cpuLightScheduler(muleContext.getSchedulerBaseConfig()
            .withName(schedulersNamePrefix + "." + CPU_LITE.name()));
  }

  protected int resolveParallelism() {
    return max(CORES, getMaxConcurrency());
  }

  /**
   * Abstract {@link ProcessingStrategy} to be used by implementations that de-multiplex incoming messages using a ring-buffer
   * which can then be subscribed to n times.
   * <p/>
   * This processing strategy is not suitable for transactional flows and will fail if used with an active transaction.
   *
   * @since 4.0
   */
  abstract static class AbstractStreamProcessingStrategy extends AbstractProcessingStrategy {

    final protected int subscribers;
    final protected int maxConcurrency;
    final protected boolean maxConcurrencyEagerCheck;
    final protected ClassLoader executionClassloader;

    protected AbstractStreamProcessingStrategy(int subscribers, int maxConcurrency, boolean maxConcurrencyEagerCheck) {
      this.subscribers = requireNonNull(subscribers);
      this.maxConcurrency = requireNonNull(maxConcurrency);
      this.maxConcurrencyEagerCheck = maxConcurrency < MAX_VALUE && maxConcurrencyEagerCheck;
      this.executionClassloader = currentThread().getContextClassLoader();
    }

    protected void awaitSubscribersCompletion(FlowConstruct flowConstruct, final long shutdownTimeout,
                                              CountDownLatch completionLatch, long startMillis) {
      try {
        if (!completionLatch.await(max(startMillis - currentTimeMillis() + shutdownTimeout, 0l), MILLISECONDS)) {
          if (getProperty(MULE_LIFECYCLE_FAIL_ON_FIRST_DISPOSE_ERROR) != null) {
            throw new IllegalStateException(format("Subscribers of ProcessingStrategy for flow '%s' not completed in %d ms",
                                                   flowConstruct.getName(),
                                                   shutdownTimeout));
          } else {
            LOGGER.warn("Subscribers of ProcessingStrategy for flow '{}' not completed in {} ms", flowConstruct.getName(),
                        shutdownTimeout);
          }
        }
      } catch (InterruptedException e) {
        currentThread().interrupt();
        if (getProperty(MULE_LIFECYCLE_FAIL_ON_FIRST_DISPOSE_ERROR) != null) {
          throw new IllegalStateException(format("Subscribers of ProcessingStrategy for flow '%s' not completed before thread interruption",
                                                 flowConstruct.getName()));
        } else {
          LOGGER.warn("Subscribers of ProcessingStrategy for flow '{}' not completed before thread interruption",
                      flowConstruct.getName());
        }
      }
    }

  }
}
