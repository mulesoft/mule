/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.core.processor.strategy;

import org.mule.runtime.core.api.MuleContext;
import org.mule.runtime.core.api.processor.MessageProcessorChainBuilder;
import org.mule.runtime.core.api.processor.Processor;
import org.mule.runtime.core.api.processor.strategy.ProcessingStrategy;
import org.mule.runtime.core.api.processor.strategy.ProcessingStrategyFactory;
import org.mule.runtime.core.api.scheduler.SchedulerService;
import org.mule.runtime.core.processor.AsyncInterceptingMessageProcessor;

import java.util.List;

import javax.resource.spi.work.WorkManager;

/**
 * This factory's strategy uses a {@link WorkManager} to schedule the processing of the pipeline of message processors in a single
 * worker thread.
 */
public class AsynchronousProcessingStrategyFactory implements ProcessingStrategyFactory {

  protected Integer maxThreads;
  protected Integer minThreads;
  protected Integer maxBufferSize;
  protected Long threadTTL;
  protected Long threadWaitTimeout;
  protected Integer poolExhaustedAction;

  @Override
  public ProcessingStrategy create() {
    final AsynchronousProcessingStrategy processingStrategy = new AsynchronousProcessingStrategy();

    if (maxThreads != null) {
      processingStrategy.setMaxThreads(maxThreads);
    }
    if (minThreads != null) {
      processingStrategy.setMinThreads(minThreads);
    }
    if (maxBufferSize != null) {
      processingStrategy.setMaxBufferSize(maxBufferSize);
    }
    if (threadTTL != null) {
      processingStrategy.setThreadTTL(threadTTL);
    }
    if (threadWaitTimeout != null) {
      processingStrategy.setThreadWaitTimeout(threadWaitTimeout);
    }
    if (poolExhaustedAction != null) {
      processingStrategy.setPoolExhaustedAction(poolExhaustedAction);
    }

    return processingStrategy;
  }

  public Integer getMaxThreads() {
    return maxThreads;
  }

  public void setMaxThreads(Integer maxThreads) {
    this.maxThreads = maxThreads;
  }

  public Integer getMinThreads() {
    return minThreads;
  }

  public void setMinThreads(Integer minThreads) {
    this.minThreads = minThreads;
  }

  public Integer getMaxBufferSize() {
    return maxBufferSize;
  }

  public void setMaxBufferSize(Integer maxBufferSize) {
    this.maxBufferSize = maxBufferSize;
  }

  public Long getThreadTTL() {
    return threadTTL;
  }

  public void setThreadTTL(Long threadTTL) {
    this.threadTTL = threadTTL;
  }

  public Long getThreadWaitTimeout() {
    return threadWaitTimeout;
  }

  public void setThreadWaitTimeout(Long threadWaitTimeout) {
    this.threadWaitTimeout = threadWaitTimeout;
  }

  public Integer getPoolExhaustedAction() {
    return poolExhaustedAction;
  }

  public void setPoolExhaustedAction(Integer poolExhaustedAction) {
    this.poolExhaustedAction = poolExhaustedAction;
  }

  public static class AsynchronousProcessingStrategy extends AbstractThreadingProfileProcessingStrategy {

    protected ProcessingStrategy synchronousProcessingStrategy = new SynchronousProcessingStrategyFactory().create();

    @Override
    public void configureProcessors(List<Processor> processors, SchedulerService schedulerService,
                                    MessageProcessorChainBuilder chainBuilder, MuleContext muleContext) {
      if (processors.size() > 0) {
        chainBuilder.chain(createAsyncMessageProcessor(schedulerService));
        synchronousProcessingStrategy.configureProcessors(processors, schedulerService, chainBuilder, muleContext);
      }
    }

    protected AsyncInterceptingMessageProcessor createAsyncMessageProcessor(SchedulerService schedulerService) {
      return new AsyncInterceptingMessageProcessor(schedulerService);
    }

  }
}
