/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.module.extension.internal.runtime.execution;

import org.mule.runtime.extension.api.runtime.operation.Result;
import org.mule.runtime.extension.api.runtime.process.CompletionCallback;

import reactor.core.publisher.MonoSink;

/**
 * Implementation of {@link CompletionCallback} which works using
 * project Reactor.
 *
 * @since 4.0
 */
final class ReactorCompletionCallback implements CompletionCallback<Object, Object> {

  private final MonoSink<Object> sink;

  ReactorCompletionCallback(MonoSink<Object> sink) {
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
