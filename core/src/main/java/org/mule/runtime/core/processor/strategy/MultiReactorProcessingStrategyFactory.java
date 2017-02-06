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
 * Creates {@link RingBufferProcessingStrategy} instance that implements a modified reactor pattern by de-multiplexing incoming
 * messages onto {@code n} event-loop's, defined via the {@code subscriberCount} configuration using a ring-buffer.
 *
 * This processing strategy is not suitable for transactional flows and will fail if used with an active transaction.
 *
 * @since 4.0
 */
public class MultiReactorProcessingStrategyFactory extends ReactorProcessingStrategyFactory {

  @Override
  public ProcessingStrategy create(MuleContext muleContext, String schedulersNamePrefix) {
    return new RingBufferProcessingStrategy(() -> muleContext.getSchedulerService()
        .customScheduler(SchedulerConfig.config().withName(schedulersNamePrefix + RING_BUFFER_SCHEDULER_NAME_SUFFIX)
            .withMaxConcurrentTasks(getSubscriberCount() + 1)),
                                            getBufferSize(),
                                            getSubscriberCount(),
                                            getWaitStrategy(),
                                            muleContext);
  }
}
