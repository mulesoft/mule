/*
 * Copyright 2023 Salesforce, Inc. All rights reserved.
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.module.extension.api.http.client;

import org.mule.runtime.http.api.sse.ServerSentEvent;
import org.mule.runtime.http.api.sse.client.SseListener;
import org.mule.runtime.module.extension.api.http.message.muletosdk.ServerSentEventWrapper;

public class SseListenerWrapper implements SseListener {

  private final org.mule.sdk.api.http.sse.client.SseListener listener;

  public SseListenerWrapper(org.mule.sdk.api.http.sse.client.SseListener listener) {
    this.listener = listener;
  }

  @Override
  public void onEvent(ServerSentEvent event) {
    listener.onEvent(new ServerSentEventWrapper(event));
  }

  @Override
  public void onClose() {
    listener.onClose();
  }
}
