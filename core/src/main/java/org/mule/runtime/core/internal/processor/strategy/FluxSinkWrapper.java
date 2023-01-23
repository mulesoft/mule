/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */

package org.mule.runtime.core.internal.processor.strategy;

import org.mule.runtime.core.api.event.CoreEvent;
import reactor.core.Disposable;
import reactor.core.publisher.FluxSink;
import reactor.util.context.Context;

import java.util.function.LongConsumer;

public class FluxSinkWrapper implements FluxSink<CoreEvent> {

  private final FluxSink<CoreEvent> sink;
  private boolean isBeingUsed;

  public FluxSinkWrapper(FluxSink<CoreEvent> sink) {
    this.sink = sink;
  }

  @Override
  public FluxSink<CoreEvent> next(CoreEvent coreEvent) {
    isBeingUsed = true;
    try {
      return sink.next(coreEvent);
    } finally {
      isBeingUsed = false;
    }
  }

  @Override
  public void complete() {
    sink.complete();
  }

  @Override
  public void error(Throwable e) {
    sink.error(e);
  }

  @Override
  public Context currentContext() {
    return sink.currentContext();
  }

  @Override
  public long requestedFromDownstream() {
    return sink.requestedFromDownstream();
  }

  @Override
  public boolean isCancelled() {
    return sink.isCancelled();
  }

  @Override
  public FluxSink<CoreEvent> onRequest(LongConsumer consumer) {
    return sink.onRequest(consumer);
  }

  @Override
  public FluxSink<CoreEvent> onCancel(Disposable d) {
    return sink.onCancel(d);
  }

  @Override
  public FluxSink<CoreEvent> onDispose(Disposable d) {
    return sink.onDispose(d);
  }

  FluxSink<CoreEvent> getDelegate() {
    return sink;
  }

  public boolean isBeingUsed() {
    return isBeingUsed;
  }
}
