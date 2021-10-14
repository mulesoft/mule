/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.core.internal.processor.strategy;

import static java.lang.String.format;
import static java.lang.System.currentTimeMillis;
import static java.lang.System.getProperty;
import static java.lang.Thread.currentThread;
import static java.lang.Thread.yield;
import static org.mule.runtime.api.util.MuleSystemProperties.MULE_LIFECYCLE_FAIL_ON_FIRST_DISPOSE_ERROR;

import org.mule.runtime.api.lifecycle.Disposable;
import org.mule.runtime.core.api.construct.BackPressureReason;
import org.mule.runtime.core.api.construct.FlowConstruct;
import org.mule.runtime.core.api.event.CoreEvent;
import org.mule.runtime.core.api.processor.Sink;

import java.util.concurrent.atomic.AtomicLong;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import reactor.core.publisher.Flux;
import reactor.core.publisher.FluxSink;

/**
 * {@link Sink} implementation that uses a {@link Flux} for each thread that dispatches events to it.
 */
public class StreamPerThreadSink implements Sink, Disposable {

  private static final Logger LOGGER = LoggerFactory.getLogger(StreamPerThreadSink.class);

  private final FlowConstruct flowConstruct;

  private volatile boolean disposing = false;

  // We add this counter so we can count the amount of finished sinks when disposing
  // The previous way involved having a strong reference to the thread, which caused MULE-19209
  private AtomicLong disposableSinks = new AtomicLong();
  private AbstractCacheSinkProvider sinkProvider;

  public StreamPerThreadSink(FlowConstruct flowConstruct, AbstractCacheSinkProvider sinkProvider) {
    this.flowConstruct = flowConstruct;
    this.sinkProvider = sinkProvider;
  }

  FluxSink<CoreEvent> createSink() {
    return sinkProvider.getSink(disposableSinks, flowConstruct);
  }

  @Override
  public void accept(CoreEvent event) {
    if (disposing) {
      throw new IllegalStateException("Already disposed");
    }

    sinkProvider.accept(this, event);
  }

  @Override
  public BackPressureReason emit(CoreEvent event) {
    accept(event);
    return null;
  }

  @Override
  public void dispose() {
    disposing = true;
    sinkProvider.dispose();

    final long shutdownTimeout = flowConstruct.getMuleContext().getConfiguration().getShutdownTimeout();
    long startMillis = currentTimeMillis();

    while (disposableSinks.get() != 0
        && currentTimeMillis() <= shutdownTimeout + startMillis
        && !currentThread().isInterrupted()) {
      yield();
    }

    if (currentThread().isInterrupted()) {
      if (getProperty(MULE_LIFECYCLE_FAIL_ON_FIRST_DISPOSE_ERROR) != null) {
        throw new IllegalStateException(format("TX Subscribers of ProcessingStrategy for flow '%s' not completed before thread interruption",
                                               flowConstruct.getName()));
      } else {
        LOGGER.warn("TX Subscribers of ProcessingStrategy for flow '{}' not completed before thread interruption",
                    flowConstruct.getName());
      }
      sinkProvider.invalidateAll();
    } else if (disposableSinks.get() != 0) {
      if (getProperty(MULE_LIFECYCLE_FAIL_ON_FIRST_DISPOSE_ERROR) != null) {
        throw new IllegalStateException(format("TX Subscribers of ProcessingStrategy for flow '%s' not completed in %d ms",
                                               flowConstruct.getName(),
                                               shutdownTimeout));
      } else {
        LOGGER.warn("TX Subscribers of ProcessingStrategy for flow '{}' not completed in {} ms", flowConstruct.getName(),
                    shutdownTimeout);
      }
      sinkProvider.invalidateAll();
    }
  }
}
