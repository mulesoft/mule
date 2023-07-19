/*
 * Copyright 2023 Salesforce, Inc. All rights reserved.
 */
package org.mule.runtime.core.internal.policy;

import org.mule.runtime.core.api.event.CoreEvent;
import org.mule.runtime.extension.api.runtime.operation.CompletableComponentExecutor.ExecutorCallback;

import reactor.core.publisher.MonoSink;
import reactor.core.publisher.SynchronousSink;

/**
 * An {@link ExecutorCallback} which propagates the result of components in the form of a {@link CoreEvent}.
 * <p>
 * It propagates the generated event through a {@link MonoSink} received in the constructor
 *
 * @since 4.3.0
 */
public class SynchronousSinkExecutorCallback implements ExecutorCallback {

  private final SynchronousSink<CoreEvent> sink;

  /**
   * Creates a new instance
   *
   * @param sink the {@link MonoSink} through which the produced {@link CoreEvent} will be propagated
   */
  public SynchronousSinkExecutorCallback(SynchronousSink<CoreEvent> sink) {
    this.sink = sink;
  }

  @Override
  public void complete(Object value) {
    try {
      sink.next((CoreEvent) value);
    } catch (Throwable t) {
      sink.error(t);
    }
  }

  @Override
  public void error(Throwable e) {
    sink.error(e);
  }
}
