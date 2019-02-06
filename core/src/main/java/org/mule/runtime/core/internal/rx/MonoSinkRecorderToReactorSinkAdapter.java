/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.core.internal.rx;

import org.mule.runtime.core.internal.exception.MessagingException;

import reactor.core.publisher.Mono;

/**
 * {@link SinkRecorderToReactorSinkAdapter} implementation to adapt {@link Mono}s.
 *
 * @param <T> The type of values to provide to the sink
 *
 * @since 4.2
 */
public class MonoSinkRecorderToReactorSinkAdapter<T> implements SinkRecorderToReactorSinkAdapter<T> {

  private final MonoSinkRecorder<T> adaptedMonoSinkRecorder;

  public MonoSinkRecorderToReactorSinkAdapter(MonoSinkRecorder<T> adaptedMonoSinkRecorder) {
    this.adaptedMonoSinkRecorder = adaptedMonoSinkRecorder;
  }

  @Override
  public void next() {
    adaptedMonoSinkRecorder.getMonoSink().success();
  }

  @Override
  public void next(T response) {
    adaptedMonoSinkRecorder.getMonoSink().success(response);
  }

  @Override
  public void error(MessagingException error) {
    adaptedMonoSinkRecorder.getMonoSink().error(error);
  }

}
