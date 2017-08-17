/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.module.extension.internal.runtime.connectivity;

import org.mule.runtime.api.connection.ConnectionException;
import org.mule.runtime.extension.api.runtime.connectivity.ReconnectionCallback;

import reactor.core.publisher.MonoSink;

/**
 * Reactor based implementation of {@link ReconnectionCallback}
 *
 * @since 4.0
 */
public class ReactiveReconnectionCallback implements ReconnectionCallback {

  private final MonoSink<Void> sink;

  /**
   * Creates a new instance
   *
   * @param sink the completion sink
   */
  public ReactiveReconnectionCallback(MonoSink<Void> sink) {
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
  public void failed(ConnectionException e) {
    sink.error(e);
  }
}
