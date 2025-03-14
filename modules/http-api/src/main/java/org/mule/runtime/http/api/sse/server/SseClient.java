/*
 * Copyright 2023 Salesforce, Inc. All rights reserved.
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.http.api.sse.server;

import org.mule.api.annotation.Experimental;
import org.mule.api.annotation.NoImplement;

import java.io.IOException;
import java.util.function.Consumer;

/**
 * Server-side abstraction of a connected client.
 * <p>
 * This API is EXPERIMENTAL. Do not use it until it is stable.
 * 
 * @since 4.9.3, 4.10.0
 */
@Experimental
@NoImplement
public interface SseClient extends AutoCloseable {

  /**
   * Sends an event to the client represented by this interface.
   *
   * @param name       the event name (topic).
   * @param data       the data as string.
   * @param id         event id (used to resume an event stream on reconnection).
   * @param retryDelay new retry delay, in milliseconds.
   */
  void sendEvent(String name, String data, String id, Long retryDelay) throws IOException;

  /**
   * Equivalent to call {@code sendEvent(name, data, id, null);}
   *
   * @param name the event name (topic).
   * @param data the data as string.
   * @param id   event id (used to resume an event stream on reconnection).
   */
  default void sendEvent(String name, String data, String id) throws IOException {
    sendEvent(name, data, id, null);
  }

  /**
   * Equivalent to call {@code sendEvent(name, data, null, null);}
   *
   * @param name the event name (topic).
   * @param data the data as string.
   */
  default void sendEvent(String name, String data) throws IOException {
    sendEvent(name, data, null, null);
  }

  /**
   * Equivalent to call {@code sendEvent("message", data, null, null);}
   * <p>
   * Note: If you want to send a message without a topic, you can call {@code sendEvent(null, data);}
   *
   * @param data the data as string.
   */
  default void sendEvent(String data) throws IOException {
    sendEvent("message", data, null, null);
  }

  /**
   * Sends a comment.
   *
   * @param comment the comment. TODO: What's the format of a "comment"?.
   */
  void sendComment(String comment);

  /**
   * The callback will be called when the client closes its connection or there is an error sending the response.
   *
   * @param callback to be called in that situation.
   */
  void onClose(Consumer<Exception> callback);

  /**
   * @return unique identifier of this client.
   */
  String getClientId();

  /**
   * Closes the connection.
   */
  @Override
  void close() throws IOException;
}
