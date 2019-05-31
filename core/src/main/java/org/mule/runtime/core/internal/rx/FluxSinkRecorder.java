/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.core.internal.rx;

import org.mule.runtime.core.internal.exception.MessagingException;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;

import reactor.core.publisher.Flux;
import reactor.core.publisher.FluxSink;

/**
 * Utility class for using with {@link Flux#create(Consumer)}.
 *
 * @param <T> The type of values in the flux
 */
public class FluxSinkRecorder<T> implements Consumer<FluxSink<T>> {

  private volatile FluxSink<T> fluxSink;

  // If a fluxSink as not yet been accepted, events are buffered until one is accepted
  private final List<Runnable> bufferedEvents = new ArrayList<>();

  @Override
  public void accept(FluxSink<T> fluxSink) {
    synchronized (this) {
      this.fluxSink = fluxSink;
    }
    bufferedEvents.forEach(e -> e.run());
  }

  public FluxSink<T> getFluxSink() {
    return fluxSink;
  }

  public void next(T response) {
    boolean present = true;
    synchronized (this) {
      if (fluxSink == null) {
        present = false;
        bufferedEvents.add(() -> fluxSink.next(response));
      }
    }

    if (present) {
      fluxSink.next(response);
    }
  }

  public void error(MessagingException error) {
    boolean present = true;
    synchronized (this) {
      if (fluxSink == null) {
        present = false;
        bufferedEvents.add(() -> {
          fluxSink.error(error);
        });
      }
    }

    if (present) {
      fluxSink.error(error);
    }
  }
}
