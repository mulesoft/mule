/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.module.extension.internal.runtime.source;

import org.mule.runtime.extension.api.runtime.source.SourceCompletionCallback;

import org.reactivestreams.Publisher;
import reactor.core.publisher.MonoSink;

/**
 * Implementation of {@link SourceCompletionCallback} which works using
 * project Reactor.
 *
 * @since 4.0
 */
final class ReactorSourceCompletionCallback implements SourceCompletionCallback {

  private final MonoSink<Void> sink;

  /**
   * Creates a new instance
   * @param sink a {@link MonoSink} used to complete the underlying {@link Publisher}
   */
  public ReactorSourceCompletionCallback(MonoSink<Void> sink) {
    this.sink = sink;
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public void success() {
    sink.success();
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public void error(Throwable t) {
    sink.error(t);
  }
}
