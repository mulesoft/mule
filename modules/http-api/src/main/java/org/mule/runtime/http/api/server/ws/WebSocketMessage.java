/*
 * Copyright 2023 Salesforce, Inc. All rights reserved.
 */
package org.mule.runtime.http.api.server.ws;

import org.mule.api.annotation.NoImplement;
import org.mule.runtime.api.metadata.TypedValue;
import org.mule.runtime.http.api.ws.WebSocket;

import java.io.InputStream;

/**
 * A message received into an inbound {@link WebSocket}
 *
 * @since 4.2.0
 */
@NoImplement
public interface WebSocketMessage {

  /**
   * @return The {@link WebSocket} that received the message
   */
  WebSocket getSocket();

  /**
   * @return The request that initiated the {@link WebSocket} connection
   */
  WebSocketRequest getRequest();

  /**
   * @return The message content
   */
  TypedValue<InputStream> getContent();
}
