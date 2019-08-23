/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.http.api.ws.exception;

import org.mule.runtime.http.api.ws.WebSocket;

/**
 * Specialization of {@link WebSocketRuntimeException} that indicates that the referred {@link WebSocket} has already been
 * closed and thus is not usable.
 *
 * @since 4.2.2
 */
public class WebSocketClosedException extends WebSocketRuntimeException {

  /**
   * Creates a new instance
   *
   * @param webSocket the referred {@link WebSocket}
   */
  public WebSocketClosedException(WebSocket webSocket) {
    this(webSocket, null);
  }

  /**
   * Creates a new instance
   *
   * @param webSocket the referred {@link WebSocket}
   * @param cause     the exception's cause
   */
  public WebSocketClosedException(WebSocket webSocket, Throwable cause) {
    super("WebSocket " + webSocket.getId() + " has already been closed.", webSocket, cause);
  }
}
