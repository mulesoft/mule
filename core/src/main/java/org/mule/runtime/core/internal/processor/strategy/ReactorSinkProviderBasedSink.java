/*
 * Copyright 2023 Salesforce, Inc. All rights reserved.
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.core.internal.processor.strategy;

import org.mule.runtime.api.lifecycle.Disposable;
import org.mule.runtime.core.api.construct.BackPressureReason;
import org.mule.runtime.core.api.event.CoreEvent;
import org.mule.runtime.core.api.processor.Sink;

import reactor.core.publisher.FluxSink;

/**
 * {@link Sink} that delegates the retrieval of a sink to a ReactorSinkProvider .
 */
public class ReactorSinkProviderBasedSink implements Sink, Disposable {

  private volatile boolean disposing = false;

  private final ReactorSinkProvider sinkProvider;

  /**
   * creates a {@link ReactorSinkProviderBasedSink}.
   *
   * @param sinkProvider the provider of {@link FluxSink<CoreEvent>}.
   */
  public ReactorSinkProviderBasedSink(ReactorSinkProvider sinkProvider) {
    this.sinkProvider = sinkProvider;
  }

  @Override
  public void accept(CoreEvent event) {
    if (disposing) {
      throw new IllegalStateException("Already disposed");
    }

    sinkProvider.getSink().next(event);
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
