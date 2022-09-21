/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.core.internal.processor.strategy;

import static org.mule.runtime.core.api.processor.ReactiveProcessor.ProcessingType.CPU_LITE_ASYNC;
import static org.mule.runtime.core.api.rx.Exceptions.unwrap;
import static org.mule.runtime.core.api.rx.Exceptions.wrapFatal;

import static reactor.core.publisher.Flux.deferContextual;
import static reactor.core.publisher.Flux.from;
import static reactor.core.publisher.Mono.just;

import org.mule.runtime.core.api.MuleContext;
import org.mule.runtime.core.api.construct.FlowConstruct;
import org.mule.runtime.core.api.construct.Pipeline;
import org.mule.runtime.core.api.event.CoreEvent;
import org.mule.runtime.core.api.processor.Processor;
import org.mule.runtime.core.api.processor.ReactiveProcessor;
import org.mule.runtime.core.api.processor.ReactiveProcessor.ProcessingType;
import org.mule.runtime.core.api.processor.Sink;
import org.mule.runtime.core.api.processor.strategy.ProcessingStrategy;
import org.mule.runtime.core.api.processor.strategy.ProcessingStrategyFactory;

/**
 * Processing strategy that processes the {@link Pipeline} in the caller thread and does not schedule the processing of any
 * {@link Processor} in a different thread pool regardless of their {@link ProcessingType}.
 * <p/>
 * When individual {@link Processor}'s execute non-blocking operations using additional threads internally (e.g. an outbound HTTP
 * request) the {@link Pipeline} will block until the operation response is available before continuing processing in the same
 * thread.
 */
public class BlockingProcessingStrategyFactory implements ProcessingStrategyFactory {

  public static final ProcessingStrategy BLOCKING_PROCESSING_STRATEGY_INSTANCE = new BlockingProcessingStrategy();

  @Override
  public ProcessingStrategy create(MuleContext muleContext, String schedulersNamePrefix) {
    return BLOCKING_PROCESSING_STRATEGY_INSTANCE;
  }

  @Override
  public Class<? extends ProcessingStrategy> getProcessingStrategyType() {
    return BLOCKING_PROCESSING_STRATEGY_INSTANCE.getClass();
  }

  private static class BlockingProcessingStrategy extends AbstractProcessingStrategy {

    @Override
    public boolean isSynchronous() {
      return true;
    }

    @Override
    public Sink createSink(FlowConstruct flowConstruct, ReactiveProcessor pipeline) {
      return new StreamPerEventSink(pipeline, event -> {
      });
    }

    @Override
    public ReactiveProcessor onProcessor(ReactiveProcessor processor) {
      if (needsMonoBlock(processor)) {
        return publisher -> deferContextual(ctx -> from(publisher).handle((event, sink) -> {
          try {
            CoreEvent result = just(event).transform(processor)
                .onErrorStop()
                .contextWrite(ctx)
                .block();
            if (result != null) {
              sink.next(result);
            }
          } catch (Throwable throwable) {
            sink.error(wrapFatal(unwrap(throwable)));
          }
        }));
      } else {
        return processor;
      }
    }

    /**
     * This strategy adds a Mono.block call in order to preserve the thread because it's a precondition for transactions to work.
     * However, there are some operations that don't need to use a Mono.block because they have a synchronous execution. It allows
     * us to make a performance optimization for those operations. This method is intended to detect which operations do need a
     * Mono.block and which don't.
     *
     * @param processor The processor.
     * @return true if a Mono.block call is needed to wait for operation completion.
     */
    private static boolean needsMonoBlock(ReactiveProcessor processor) {
      if (processor instanceof ComponentInnerProcessor) {
        return !((ComponentInnerProcessor) processor).isBlocking();
      } else {
        return processor.getProcessingType() == CPU_LITE_ASYNC;
      }
    }
  }
}
