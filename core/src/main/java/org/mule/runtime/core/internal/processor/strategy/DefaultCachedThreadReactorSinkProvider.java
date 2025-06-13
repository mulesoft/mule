/*
 * Copyright 2023 Salesforce, Inc. All rights reserved.
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.core.internal.processor.strategy;

import static org.mule.runtime.api.config.MuleRuntimeFeature.USE_TRANSACTION_SINK_INDEX;
import static org.mule.runtime.api.util.MuleSystemProperties.MULE_LIFECYCLE_FAIL_ON_FIRST_DISPOSE_ERROR;

import static java.lang.String.format;
import static java.lang.System.currentTimeMillis;
import static java.lang.System.getProperty;
import static java.lang.Thread.currentThread;

import org.mule.runtime.api.config.FeatureFlaggingService;
import org.mule.runtime.core.api.construct.FlowConstruct;
import org.mule.runtime.core.api.event.CoreEvent;
import org.mule.runtime.core.api.processor.ReactiveProcessor;
import org.mule.runtime.core.internal.rx.FluxSinkRecorder;

import java.util.concurrent.atomic.AtomicLong;
import java.util.function.Consumer;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import reactor.core.publisher.Flux;
import reactor.core.publisher.FluxSink;

/**
 * Default implementation of {@link AbstractCachedThreadReactorSinkProvider} that uses a {@link Flux} for each thread that
 * dispatches events to it.
 */
public class DefaultCachedThreadReactorSinkProvider extends AbstractCachedThreadReactorSinkProvider {

  private static final Logger LOGGER = LoggerFactory.getLogger(DefaultCachedThreadReactorSinkProvider.class);

  // We add this counter so we can count the amount of finished sinks when disposing
  // The previous way involved having a strong reference to the thread, which caused MULE-19209
  private final AtomicLong disposableSinks = new AtomicLong();

  private final Consumer<CoreEvent> eventConsumer;
  private final FlowConstruct flowConstruct;
  private final ReactiveProcessor processor;

  /**
   * Creates a {@link DefaultCachedThreadReactorSinkProvider}.
   *
   * @param processor     the processor to process events emitted onto stream, typically this processor will represent the flow
   *                      pipeline.
   * @param eventConsumer event consumer called just before {@link CoreEvent}'s emission.
   */
  public DefaultCachedThreadReactorSinkProvider(FlowConstruct flowConstruct, ReactiveProcessor processor,
                                                Consumer<CoreEvent> eventConsumer,
                                                FeatureFlaggingService featureFlaggingService) {
    super(featureFlaggingService.isEnabled(USE_TRANSACTION_SINK_INDEX));
    this.flowConstruct = flowConstruct;
    this.processor = processor;
    this.eventConsumer = eventConsumer;
  }

  @Override
  public FluxSink<CoreEvent> createSink() {
    disposableSinks.incrementAndGet();
    final FluxSinkRecorder<CoreEvent> recorder = new FluxSinkRecorder<>();
    recorder.flux()
        .doOnNext(eventConsumer)
        .transform(processor)
        .subscribe(null, e -> {
          LOGGER.atError()
              .setCause(e)
              .log("Exception reached PS subscriber for flow '{}'", flowConstruct.getName());
          disposableSinks.decrementAndGet();
        }, disposableSinks::decrementAndGet);

    return recorder.getFluxSink();
  }

  @Override
  public void dispose() {
    super.dispose();
    final long shutdownTimeout = flowConstruct.getMuleContext().getConfiguration().getShutdownTimeout();
    long startMillis = currentTimeMillis();

    while (disposableSinks.get() != 0
        && currentTimeMillis() <= shutdownTimeout + startMillis
        && !currentThread().isInterrupted()) {
      Thread.yield();
    }

    if (currentThread().isInterrupted()) {
      if (getProperty(MULE_LIFECYCLE_FAIL_ON_FIRST_DISPOSE_ERROR) != null) {
        throw new IllegalStateException(format("TX Subscribers of ProcessingStrategy for flow '%s' not completed before thread interruption",
                                               flowConstruct.getName()));
      } else {
        LOGGER.warn("TX Subscribers of ProcessingStrategy for flow '{}' not completed before thread interruption",
                    flowConstruct.getName());
      }
      invalidateAll();
    } else if (disposableSinks.get() != 0) {
      if (getProperty(MULE_LIFECYCLE_FAIL_ON_FIRST_DISPOSE_ERROR) != null) {
        throw new IllegalStateException(format("TX Subscribers of ProcessingStrategy for flow '%s' not completed in %d ms",
                                               flowConstruct.getName(),
                                               shutdownTimeout));
      } else {
        LOGGER.warn("TX Subscribers of ProcessingStrategy for flow '{}' not completed in {} ms", flowConstruct.getName(),
                    shutdownTimeout);
      }
      invalidateAll();
    }
  }
}
