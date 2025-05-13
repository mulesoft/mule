/*
 * Copyright 2023 Salesforce, Inc. All rights reserved.
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.http.api.sse.client;

import org.mule.api.annotation.Experimental;
import org.mule.api.annotation.NoImplement;
import org.mule.runtime.http.api.domain.message.response.HttpResponse;

import java.util.function.Consumer;

/**
 * A consumer of server-sent events.
 * <p>
 * This API is EXPERIMENTAL. Do not use it until it is stable.
 *
 * @since 4.9.3, 4.10.0
 */
@Experimental
@NoImplement
public interface SseSource extends AutoCloseable {

  int READY_STATUS_CONNECTING = 0;
  int READY_STATUS_OPEN = 1;
  int READY_STATUS_CLOSED = 2;

  /**
   * Sends the initiator request
   */
  void open();

  /**
   * Opens the source with a given response. It does not send the initiator request but assumes that it was already sent, and
   * expects that the response has status {@code 200} and Content Type {@code text/event-stream}.
   * 
   * @param response the SSE initiator response.
   * @throws IllegalStateException if the response is not a successful SSE initiator response.
   * @since 4.9.6, 4.10.0
   */
  void open(HttpResponse response);

  /**
   * @return the readyState.
   */
  int getReadyState();

  /**
   * Registers a {@link SseListener listener} for a specific event name (a.k.a. topic, a.k.a. type).
   *
   * @param eventName The event name that the {@link SseListener listener} will handle.
   * @param listener  The event handler.
   */
  void register(String eventName, SseListener listener);

  /**
   * Registers a fallback {@link SseListener listener} for all the events that aren't handled by any listener registered with
   * {@link #register(String, SseListener)}.
   *
   * @param listener The event handler.
   */
  void register(SseListener listener);

  /**
   * Registers a callback to be called when an error occurs.
   *
   * @param onConnectionFailure to be called when an error occurs.
   */
  void doOnConnectionFailure(Consumer<SseFailureContext> onConnectionFailure);

  /**
   * Aborts reconnection if it's scheduled, and closes the connection if established.
   */
  @Override
  void close();
}
