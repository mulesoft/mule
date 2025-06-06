/*
 * Copyright 2023 Salesforce, Inc. All rights reserved.
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.http.support.internal.server;

import org.mule.sdk.api.http.sse.server.SseClient;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.util.function.Consumer;

public class SseClientWrapper implements SseClient {

  private final org.mule.runtime.http.api.sse.server.SseClient sseClient;

  public SseClientWrapper(org.mule.runtime.http.api.sse.server.SseClient sseClient) {
    this.sseClient = sseClient;
  }

  @Override
  public void sendEvent(String name, String data, String id, Long retryDelay) throws IOException {
    sseClient.sendEvent(name, data, id, retryDelay);
  }

  @Override
  public void sendEvent(String name, String data, String id) throws IOException {
    sseClient.sendEvent(name, data, id);
  }

  @Override
  public void sendEvent(String name, String data) throws IOException {
    sseClient.sendEvent(name, data);
  }

  @Override
  public void sendEvent(String data) throws IOException {
    sseClient.sendEvent(data);
  }

  @Override
  public void sendComment(String comment) {
    sseClient.sendComment(comment);
  }

  @Override
  public void onClose(Consumer<Throwable> callback) {
    sseClient.onClose(callback);
  }

  @Override
  public String getClientId() {
    return sseClient.getClientId();
  }

  @Override
  public InetSocketAddress getRemoteAddress() {
    return sseClient.getRemoteAddress();
  }

  @Override
  public void close() throws IOException {
    sseClient.close();
  }
}
