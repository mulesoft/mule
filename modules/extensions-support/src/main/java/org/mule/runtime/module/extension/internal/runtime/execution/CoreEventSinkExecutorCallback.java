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

public class CoreEventSinkExecutorCallback implements ExecutorCallback {

  private final MonoSink<CoreEvent> sink;
  private final Function<Object, CoreEvent> valueMapper;
  private final Function<Throwable, Throwable> exceptionMapper;

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
