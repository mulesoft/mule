/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.core.processor.strategy;

import static java.util.concurrent.Executors.newFixedThreadPool;
import static reactor.core.publisher.Flux.from;
import static reactor.core.scheduler.Schedulers.fromExecutorService;

import org.mule.runtime.api.exception.MuleException;
import org.mule.runtime.core.api.Event;
import org.mule.runtime.core.api.MuleContext;
import org.mule.runtime.core.api.lifecycle.Startable;
import org.mule.runtime.core.api.lifecycle.Stoppable;
import org.mule.runtime.core.api.processor.MessageProcessorChainBuilder;
import org.mule.runtime.core.api.processor.NonBlockingMessageProcessor;
import org.mule.runtime.core.api.processor.ProcessingStrategy;
import org.mule.runtime.core.api.processor.Processor;

import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.function.Function;

import org.reactivestreams.Publisher;

/**
 * Allows Mule to use non-blocking execution model where possible and free up threads when performing IO operations.
 *
 * @since 3.7
 */
public class NonBlockingProcessingStrategyFactory implements ProcessingStrategyFactory {

  protected Integer maxThreads;
  protected Integer minThreads;
  protected Integer maxBufferSize;
  protected Long threadTTL;
  protected Long threadWaitTimeout;
  protected Integer poolExhaustedAction;

  @Override
  public ProcessingStrategy create() {
    final NonBlockingProcessingStrategy processingStrategy = new NonBlockingProcessingStrategy();

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

  // TODO MULE-10544 use internal executor, do not allow it to be passed form outside
  public ProcessingStrategy create(ExecutorService executorService) {
    final NonBlockingProcessingStrategy processingStrategy = new NonBlockingProcessingStrategy(executorService);

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

  public static class NonBlockingProcessingStrategy extends AbstractThreadingProfileProcessingStrategy
      implements Startable, Stoppable {

    private static final int DEFAULT_MAX_THREADS = 128;
    private ExecutorService executorService;

    public NonBlockingProcessingStrategy() {
      maxThreads = DEFAULT_MAX_THREADS;
    }

    public NonBlockingProcessingStrategy(ExecutorService executorService) {
      this.executorService = executorService;
    }

    @Override
    public void configureProcessors(List<Processor> processors,
                                    org.mule.runtime.core.api.processor.StageNameSource nameSource,
                                    MessageProcessorChainBuilder chainBuilder, MuleContext muleContext) {
      for (Processor processor : processors) {
        chainBuilder.chain(processor);
      }
    }

    @Override
    public Function<Publisher<Event>, Publisher<Event>> onProcessor(Processor processor,
                                                                    Function<Publisher<Event>, Publisher<Event>> publisherFunction) {
      if (processor instanceof NonBlockingMessageProcessor) {
        return publisher -> from(publisher).transform(publisherFunction).publishOn(fromExecutorService(executorService));
      } else {
        return super.onProcessor(processor, publisherFunction);
      }
    }

    @Override
    public void start() throws MuleException {
      executorService = newFixedThreadPool(maxThreads);
    }

    @Override
    public void stop() throws MuleException {
      executorService.shutdown();
    }
  }
}
