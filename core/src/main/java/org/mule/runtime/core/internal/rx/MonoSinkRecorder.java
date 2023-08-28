/*
 * Copyright 2023 Salesforce, Inc. All rights reserved.
 */
package org.mule.runtime.core.internal.rx;

import java.util.function.Consumer;

import reactor.core.publisher.Mono;
import reactor.core.publisher.MonoSink;

/**
 * Utility class for using with {@link Mono#create(Consumer)}.
 *
 * @param <T> The type of values in the mono
 */
public class MonoSinkRecorder<T> implements Consumer<MonoSink<T>> {

  private MonoSink<T> monoSink;

  @Override
  public void accept(MonoSink<T> fluxSink) {
    this.monoSink = fluxSink;
  }

  public MonoSink<T> getMonoSink() {
    return monoSink;
  }
}
