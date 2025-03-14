/*
 * Copyright 2023 Salesforce, Inc. All rights reserved.
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.http.api.sse;

import java.util.function.Consumer;

/**
 * A consumer of server-sent events.
 */
public interface ServerSentEventSource {

  int READY_STATUS_CONNECTING = 0;
  int READY_STATUS_OPEN = 1;
  int READY_STATUS_CLOSED = 2;

  /**
   * Sends the initiator request
   */
  void open();

  /**
   * @return the readyState.
   */
  int getReadyState();

  /**
   * Registers a {@link ServerSentEventListener listener} for a specific event name (a.k.a. topic, a.k.a. type).
   * 
   * @param eventName The event name that the {@link ServerSentEventListener listener} will handle.
   * @param listener  The event handler.
   */
  void register(String eventName, ServerSentEventListener listener);

  /**
   * Registers a fallback {@link ServerSentEventListener listener} for all the events that aren't handled by any listener
   * registered with {@link #register(String, ServerSentEventListener)}.
   * 
   * @param listener The event handler.
   */
  void register(ServerSentEventListener listener);

  /**
   * Registers a callback to be called when an error occurs.
   * <p>
   * Don't use this callback for reconnection/retry, it has to be done automatically by the source and should be configured using
   * a {@link SseRetryConfig}.
   * 
   * @param onErrorCallback to be called when an error occurs.
   */
  void doOnConnectionFailure(Consumer<SseFailureContext> onErrorCallback);
}
