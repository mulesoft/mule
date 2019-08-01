/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.http.api.server.ws;

import static java.util.concurrent.TimeUnit.MINUTES;

/**
 * Handler for an inbound WebSocket requests
 *
 * @since 4.2.0
 */
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

  /**
   * @return Timeout in milliseconds for closing idle connections
   * @since 4.3.0 - 4.2.2
   */
  default long getIdleSocketTimeoutMills() {
    return MINUTES.toMillis(15);
  }
}
