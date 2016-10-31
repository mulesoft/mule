/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.core.processor.strategy;

import static java.util.concurrent.Executors.newFixedThreadPool;
import static reactor.core.publisher.Flux.from;
import static reactor.core.scheduler.Schedulers.fromExecutor;
import static reactor.core.scheduler.Schedulers.fromExecutorService;
import org.mule.runtime.core.api.Event;
import org.mule.runtime.core.api.MuleContext;
import org.mule.runtime.api.exception.MuleException;
import org.mule.runtime.core.api.lifecycle.Startable;
import org.mule.runtime.core.api.lifecycle.Stoppable;
import org.mule.runtime.core.api.processor.NonBlockingMessageProcessor;
import org.mule.runtime.core.api.processor.Processor;
import org.mule.runtime.core.api.processor.MessageProcessorChainBuilder;

import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.function.Function;

import org.reactivestreams.Publisher;

/**
 * Allows Mule to use non-blocking execution model where possible and free up threads when performing IO operations.
 *
 * @since 3.7
 */
public class NonBlockingProcessingStrategy extends AbstractThreadingProfileProcessingStrategy implements Startable, Stoppable {

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
      chainBuilder.chain((Processor) processor);
    }
  }

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
