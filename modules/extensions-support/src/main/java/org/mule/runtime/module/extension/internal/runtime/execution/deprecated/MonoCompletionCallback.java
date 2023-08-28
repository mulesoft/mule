/*
 * Copyright 2023 Salesforce, Inc. All rights reserved.
 */
package org.mule.runtime.module.extension.internal.runtime.execution.deprecated;

import org.mule.runtime.extension.api.runtime.operation.Result;
import org.mule.runtime.extension.api.runtime.process.CompletionCallback;

import reactor.core.publisher.MonoSink;

/**
 * Implementation of {@link CompletionCallback} which works using project Reactor.
 *
 * @since 4.0
 * @deprecated since 4.3
 */
@Deprecated
final class MonoCompletionCallback implements CompletionCallback<Object, Object> {

  private final MonoSink<Object> sink;

  MonoCompletionCallback(MonoSink<Object> sink) {
    this.sink = sink;
  }

  @Override
  public void success(Result<Object, Object> result) {
    sink.success(result);
  }

  @Override
  public void error(Throwable e) {
    sink.error(e);
  }
}
