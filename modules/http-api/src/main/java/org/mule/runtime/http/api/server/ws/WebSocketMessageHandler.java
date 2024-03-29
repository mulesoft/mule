/*
 * Copyright 2023 Salesforce, Inc. All rights reserved.
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
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
