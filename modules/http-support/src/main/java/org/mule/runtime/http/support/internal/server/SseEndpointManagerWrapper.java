/*
 * Copyright 2023 Salesforce, Inc. All rights reserved.
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.http.support.internal.server;

import org.mule.sdk.api.http.sse.server.SseEndpointManager;

public class SseEndpointManagerWrapper implements SseEndpointManager {

  private final org.mule.runtime.http.api.sse.server.SseEndpointManager delegate;

  public SseEndpointManagerWrapper(org.mule.runtime.http.api.sse.server.SseEndpointManager delegate) {
    this.delegate = delegate;
  }

  @Override
  public void stop() {
    delegate.stop();
  }

  @Override
  public void start() {
    delegate.start();
  }

  @Override
  public void dispose() {
    delegate.dispose();
  }
}
