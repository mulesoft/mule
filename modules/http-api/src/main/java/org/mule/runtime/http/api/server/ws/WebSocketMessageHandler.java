/*
 * Copyright 2023 Salesforce, Inc. All rights reserved.
 */
package org.mule.runtime.http.api.server.ws;

import org.mule.runtime.http.api.ws.WebSocket;

/**
 * Handles the arrival of a message into an inbound {@link WebSocket}
 *
 * @since 4.2.0
 */
public interface WebSocketMessageHandler {

  /**
   * This method is invoked when a new message is received
   *
   * @param message the message
   */
  void onMessage(WebSocketMessage message);
}
