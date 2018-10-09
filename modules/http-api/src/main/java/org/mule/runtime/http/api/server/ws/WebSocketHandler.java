/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.http.api.server.ws;

import org.mule.api.annotation.Experimental;

/**
 * Handler for an inbound WebSocket requests
 *
 * @since 4.1.5 as experimental
 */
@Experimental
public interface WebSocketHandler {

  /**
   * @return The path of the WebSocket endpoint
   */
  String getPath();

  /**
   * @return The {@link WebSocketConnectionHandler} for the {@link #getPath()}
   */
  WebSocketConnectionHandler getConnectionHandler();

  /**
   * @return The {@link WebSocketMessageHandler} for the {@link #getPath()}
   */
  WebSocketMessageHandler getMessageHandler();
}
