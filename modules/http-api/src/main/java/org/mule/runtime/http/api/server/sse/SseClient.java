/*
 * Copyright 2023 Salesforce, Inc. All rights reserved.
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.http.api.server.sse;

import java.io.IOException;

/**
 * Server-side abstraction of a connected client.
 */
public interface SseClient extends AutoCloseable {

  /**
   * Sends an event to the client represented by this interface.
   * 
   * @param name the event name (topic).
   * @param data the data as string.
   * @param id   event id (used to resume an event stream on reconnection).
   */
  void sendEvent(String name, String data, String id) throws IOException;

  /**
   * Equivalent to call {@code sendEvent(name, data, null);}
   * 
   * @param name the event name (topic).
   * @param data the data as string.
   */
  default void sendEvent(String name, String data) throws IOException {
    sendEvent(name, data, null);
  }

  /**
   * Equivalent to call {@code sendEvent("message", data, null);}
   * <p>
   * Note: If you want to send a message without topic, you can call {@code sendEvent(null, data);}
   * 
   * @param data the data as string.
   */
  default void sendEvent(String data) throws IOException {
    sendEvent("message", data, null);
  }

  /**
   * Sends a comment.
   * 
   * @param comment the comment. TODO: What's the format of a "comment"?.
   */
  void sendComment(String comment);

  /**
   * The callback will be called when the client closes its connection.
   * 
   * @param callback to be called when the client closes its connection.
   */
  void onClose(Runnable callback);

  /**
   * Closes the connection.
   */
  @Override
  void close() throws IOException;
}
