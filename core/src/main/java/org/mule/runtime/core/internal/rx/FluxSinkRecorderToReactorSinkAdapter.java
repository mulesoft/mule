/*
 * Copyright 2023 Salesforce, Inc. All rights reserved.
 */
package org.mule.runtime.core.internal.rx;

import org.mule.runtime.core.internal.exception.MessagingException;

import reactor.core.publisher.Flux;

/**
 * {@link SinkRecorderToReactorSinkAdapter} implementation to adapt {@link Flux}es.
 *
 * @param <T> The type of values to provide to the sink
 *
 * @since 4.2
 */
public class FluxSinkRecorderToReactorSinkAdapter<T> implements SinkRecorderToReactorSinkAdapter<T> {

  private final FluxSinkRecorder<T> adaptedFluxSinkRecorder;

  public FluxSinkRecorderToReactorSinkAdapter(FluxSinkRecorder<T> adaptedFluxSinkRecorder) {
    this.adaptedFluxSinkRecorder = adaptedFluxSinkRecorder;
  }

  @Override
  public void next() {
    // Nothing to do
  }

  @Override
  public void next(T response) {
    adaptedFluxSinkRecorder.next(response);
  }

  @Override
  public void error(MessagingException error) {
    adaptedFluxSinkRecorder.error(error);
  }

}
