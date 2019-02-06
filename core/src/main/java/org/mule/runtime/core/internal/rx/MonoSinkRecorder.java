/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.core.internal.rx;

import java.util.function.Consumer;

import reactor.core.publisher.Mono;
import reactor.core.publisher.MonoSink;

/**
 * Utility class for using with {@link Mono#create(Consumer)}.
 *
 * @param <T> The type of values in the mono
 * @param <S>
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
