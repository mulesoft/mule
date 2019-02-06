/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
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
    adaptedFluxSinkRecorder.getFluxSink().next(null);
  }

  @Override
  public void next(T response) {
    adaptedFluxSinkRecorder.getFluxSink().next(response);
  }

  @Override
  public void error(MessagingException error) {
    adaptedFluxSinkRecorder.getFluxSink().error(error);
  }

}
