/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.core.processor.strategy;

import org.mule.runtime.core.api.MuleContext;
import org.mule.runtime.core.api.processor.strategy.ProcessingStrategy;
import org.mule.runtime.core.api.scheduler.SchedulerConfig;

/**
 * Creates {@link RingBufferProcessingStrategy} instance that implements the reactor pattern by de-multiplexes incoming messages
 * onto a single event-loop using a ring-buffer.
 *
 * This processing strategy is not suitable for transactional flows and will fail if used with an active transaction.
 *
 * @since 4.0
 */
public class ReactorProcessingStrategyFactory extends AbstractRingBufferProcessingStrategyFactory {

  @Override
  public void setSubscriberCount(int subscriberCount) {
    throw new UnsupportedOperationException("ReactorProcessingStrategy does not support more than 1 subscriber event-loop");
  }

  @Override
  public ProcessingStrategy create(MuleContext muleContext, String schedulersNamePrefix) {
    return new RingBufferProcessingStrategy(() -> muleContext.getSchedulerService()
        .customScheduler(SchedulerConfig.config().withName(schedulersNamePrefix + RING_BUFFER_SCHEDULER_NAME_SUFFIX)
            .withMaxConcurrentTasks(getSubscriberCount() + 1)),

                                            getBufferSize(),
                                            1,
                                            getWaitStrategy(),
                                            muleContext);
  }

}
