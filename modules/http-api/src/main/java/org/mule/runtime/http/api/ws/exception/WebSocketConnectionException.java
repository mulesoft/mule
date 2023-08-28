/*
 * Copyright 2023 Salesforce, Inc. All rights reserved.
 */
package org.mule.runtime.http.api.ws.exception;

import org.mule.runtime.http.api.ws.WebSocket;

/**
 * Specialization of {@link WebSocketRuntimeException} that indicates that the referred {@link WebSocket} is having connectivity
 * issues.
 *
 * @since 4.2.2
 */
public class WebSocketConnectionException extends WebSocketRuntimeException {

  /**
   * Creates a new instance
   *
   * @param webSocket the referred {@link WebSocket}
   */
  public WebSocketConnectionException(WebSocket webSocket) {
    this(webSocket, null);
  }

  /**
   * Creates a new instance
   *
   * @param webSocket the referred {@link WebSocket}
   * @param cause     the exception's cause
   */
  public WebSocketConnectionException(WebSocket webSocket, Throwable cause) {
    super("WebSocket " + webSocket.getId() + " connection is not usable.", webSocket, cause);
  }
}
