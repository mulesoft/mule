/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.core.internal.processor.strategy;

import org.mule.runtime.api.lifecycle.Disposable;
import org.mule.runtime.core.api.construct.BackPressureReason;
import org.mule.runtime.core.api.event.CoreEvent;
import org.mule.runtime.core.api.processor.Sink;

import reactor.core.publisher.Flux;
import reactor.core.publisher.FluxSink;

/**
 * {@link Sink} implementation that uses a {@link Flux} for each thread that dispatches events to it.
 */
public class ReactorSinkProviderBasedSink implements Sink, Disposable {

  private volatile boolean disposing = false;

  private final ReactorSinkProvider sinkProvider;

  public ReactorSinkProviderBasedSink(ReactorSinkProvider sinkProvider) {
    this.sinkProvider = sinkProvider;
  }

  FluxSink<CoreEvent> createSink() {
    return sinkProvider.getSink();
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
  }
}
