/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.core.internal.processor.strategy;

import static java.lang.System.getProperty;
import static org.mule.runtime.core.internal.processor.strategy.WorkQueueStreamProcessingStrategyFactory.WaitStrategy.LITE_BLOCKING;

import org.mule.runtime.api.scheduler.Scheduler;
import org.mule.runtime.core.api.MuleContext;
import org.mule.runtime.core.api.processor.strategy.ProcessingStrategyFactory;

import java.util.function.Supplier;

/**
 * Abstract {@link ProcessingStrategyFactory} to be used by implementations that de-multiplex incoming messages using a
 * ring-buffer which can then be subscribed to n times.
 * <p>
 * Processing strategies created with this factory are not suitable for transactional flows and will fail if used with an active
 * transaction by default.
 *
 * @since 4.2
 */
public abstract class AbstractStreamWorkQueueProcessingStrategyFactory extends AbstractStreamProcessingStrategyFactory {

  protected static final String DEFAULT_WAIT_STRATEGY =
      getProperty(SYSTEM_PROPERTY_PREFIX + "DEFAULT_WAIT_STRATEGY", LITE_BLOCKING.name());

  protected static final String RING_BUFFER_SCHEDULER_NAME_SUFFIX = ".ring-buffer";

  private String waitStrategy = DEFAULT_WAIT_STRATEGY;

  /**
   * Configure the wait strategy used to wait for new events on ring-buffer.
   *
   * @param waitStrategy
   */
  public void setWaitStrategy(String waitStrategy) {
    this.waitStrategy = waitStrategy;
  }

  protected String getWaitStrategy() {
    return waitStrategy;
  }

  protected Supplier<Scheduler> getRingBufferSchedulerSupplier(MuleContext muleContext, String schedulersNamePrefix) {
    return () -> muleContext.getSchedulerService()
        .customScheduler(muleContext.getSchedulerBaseConfig()
            .withName(schedulersNamePrefix + RING_BUFFER_SCHEDULER_NAME_SUFFIX)
            .withMaxConcurrentTasks(getSubscriberCount()).withWaitAllowed(true));
  }

}
