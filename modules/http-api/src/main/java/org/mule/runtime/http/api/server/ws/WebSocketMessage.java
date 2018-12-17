/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
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
