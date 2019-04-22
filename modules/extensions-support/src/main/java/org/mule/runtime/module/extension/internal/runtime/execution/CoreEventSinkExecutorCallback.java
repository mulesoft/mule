/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.module.extension.internal.runtime.execution;

import org.mule.runtime.core.api.event.CoreEvent;
import org.mule.runtime.extension.api.runtime.operation.CompletableComponentExecutor.ExecutorCallback;

import java.util.function.Function;

import reactor.core.publisher.MonoSink;

/**
 * An {@link ExecutorCallback} which propagates the result of components in the form of a {@link CoreEvent}.
 * <p>
 * It propagates the generated event through a {@link MonoSink} received in the constructor
 *
 * @since 4.3.0
 */
public class CoreEventSinkExecutorCallback implements ExecutorCallback {

  private final MonoSink<CoreEvent> sink;
  private final Function<Object, CoreEvent> valueMapper;
  private final Function<Throwable, Throwable> exceptionMapper;

  /**
   * Creates a new instance
   *
   * @param sink            the {@link MonoSink} through which the produced {@link CoreEvent} will be propagated
   * @param valueMapper     a {@link Function} to be used to transform a generic value into a {@link CoreEvent}
   * @param exceptionMapper a {@link Function} to be used to map errors thrown by the operation.
   */
  public CoreEventSinkExecutorCallback(MonoSink<CoreEvent> sink,
                                       Function<Object, CoreEvent> valueMapper,
                                       Function<Throwable, Throwable> exceptionMapper) {
    this.sink = sink;
    this.valueMapper = valueMapper;
    this.exceptionMapper = exceptionMapper;
  }

  @Override
  public void complete(Object value) {
    try {
      sink.success(valueMapper.apply(value));
    } catch (Throwable t) {
      error(t);
    }
  }

  @Override
  public void error(Throwable e) {
    sink.error(exceptionMapper.apply(e));
  }
}
