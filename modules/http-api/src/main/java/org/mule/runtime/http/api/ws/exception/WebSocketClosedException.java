/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.http.api.ws.exception;

import org.mule.runtime.http.api.ws.WebSocket;

public class WebSocketClosedException extends AbstractWebSocketRuntimeException {

  public WebSocketClosedException(WebSocket webSocket) {
    this(webSocket, null);
  }

  public WebSocketClosedException(WebSocket webSocket, Throwable cause) {
    super("WebSocket " + webSocket.getId() + " has already been closed.", webSocket, cause);
  }
}
