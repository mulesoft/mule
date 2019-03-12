/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.core.internal.util.rx;

import java.util.function.LongConsumer;

import reactor.core.Disposable;
import reactor.core.publisher.MonoSink;
import reactor.util.context.Context;

public abstract class MonoSinkWrapper<T> implements MonoSink<T> {

  protected final MonoSink<T> delegate;

  public MonoSinkWrapper(MonoSink<T> delegate) {
    this.delegate = delegate;
  }

  @Override
  public Context currentContext() {
    return delegate.currentContext();
  }

  @Override
  public void success() {
    delegate.success();
  }

  @Override
  public void success(T value) {
    delegate.success(value);
  }

  @Override
  public void error(Throwable e) {
    delegate.error(e);
  }

  @Override
  public MonoSink<T> onRequest(LongConsumer consumer) {
    return delegate.onRequest(consumer);
  }

  @Override
  public MonoSink<T> onCancel(Disposable d) {
    return delegate.onCancel(d);
  }

  @Override
  public MonoSink<T> onDispose(Disposable d) {
    return delegate.onDispose(d);
  }
}
