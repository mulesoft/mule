/*
 * Copyright 2023 Salesforce, Inc. All rights reserved.
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
