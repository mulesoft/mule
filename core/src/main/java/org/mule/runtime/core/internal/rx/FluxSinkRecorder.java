/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.core.internal.rx;

import java.util.function.Consumer;

import reactor.core.publisher.Flux;
import reactor.core.publisher.FluxSink;

/**
 * Utility class for using with {@link Flux#create(Consumer)}.
 *
 * @param <T> The type of values in the flux
 * @param <S>
 */
public class FluxSinkRecorder<T> implements Consumer<FluxSink<T>> {

  private FluxSink<T> fluxSink;

  @Override
  public void accept(FluxSink<T> fluxSink) {
    this.fluxSink = fluxSink;
  }

  public FluxSink<T> getFluxSink() {
    return fluxSink;
  }
}
