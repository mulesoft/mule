/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.core.internal.policy;

import org.mule.runtime.extension.api.runtime.operation.CompletableComponentExecutor.ExecutorCallback;

import reactor.core.publisher.MonoSink;

/**
 * {@link ExecutorCallback} implementation which propagates the completion signals to a {@link MonoSink} which will be provided
 * asynchronously through the {@link #setSink(MonoSink)} method.
 * <p>
 * If the completion signals arrive before {@link #setSink(MonoSink)} is invoked, then the result is stored an applied immediately
 * after obtaining the sink.
 *
 * @param <T> the generic type of the completion value
 * @since 4.3.0
 */
public class DeferredMonoSinkExecutorCallback<T> implements ExecutorCallback {

  private MonoSink sink;
  private Object value;
  private Throwable error;

  public void setSink(MonoSink sink) {
    synchronized (this) {
      this.sink = sink;
      if (value != null) {
        sink.success(value);
      } else if (error != null) {
        sink.error(error);
      }
    }
  }

  @Override
  public void complete(Object value) {
    synchronized (this) {
      if (sink != null) {
        sink.success(value);
      } else {
        this.value = value;
      }
    }
  }

  @Override
  public void error(Throwable e) {
    synchronized (this) {
      if (sink != null) {
        sink.error(e);
      } else {
        error = e;
      }
    }
  }
}
