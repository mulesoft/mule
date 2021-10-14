/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.core.internal.processor.strategy;

import org.mule.runtime.core.api.construct.FlowConstruct;
import org.mule.runtime.core.api.processor.ReactiveProcessor;
import org.mule.runtime.core.internal.rx.FluxSinkRecorder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import reactor.core.publisher.FluxSink;
import org.mule.runtime.core.api.event.CoreEvent;

import java.util.concurrent.atomic.AtomicLong;
import java.util.function.Consumer;

public class DefaultCacheSinkProvider extends AbstractCacheSinkProvider {

  private static final Logger LOGGER = LoggerFactory.getLogger(DefaultCacheSinkProvider.class);

  private final Consumer<CoreEvent> eventConsumer;
  private final ReactiveProcessor processor;

  /**
   * Creates a {@link DefaultCacheSinkProvider}.
   *
   * @param processor     the processor to process events emitted onto stream, typically this processor will represent the flow
   *                      pipeline.
   * @param eventConsumer event consumer called just before {@link CoreEvent}'s emission.
   */
  public DefaultCacheSinkProvider(ReactiveProcessor processor, Consumer<CoreEvent> eventConsumer) {
    this.processor = processor;
    this.eventConsumer = eventConsumer;
  }

  @Override
  public FluxSink<org.mule.runtime.core.api.event.CoreEvent> getSink(AtomicLong disposableSinks, FlowConstruct flowConstruct) {
    disposableSinks.incrementAndGet();
    final FluxSinkRecorder<CoreEvent> recorder = new FluxSinkRecorder<>();
    recorder.flux()
        .doOnNext(eventConsumer)
        .transform(processor)
        .subscribe(null, e -> {
          LOGGER.error("Exception reached PS subscriber for flow '" + flowConstruct.getName() + "'", e);
          disposableSinks.decrementAndGet();
        }, disposableSinks::decrementAndGet);

    return recorder.getFluxSink();
  }
}
